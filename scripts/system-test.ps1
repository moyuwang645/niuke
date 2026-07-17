param(
    [int]$Port = 18080,
    [switch]$UseLocalDServices,
    [switch]$LoadOnly
)

$ErrorActionPreference = "Stop"

function Test-Port {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMs = 1500
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $result = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $result.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            return $false
        }
        $client.EndConnect($result)
        return $true
    }
    catch {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Invoke-Native {
    param(
        [scriptblock]$Command
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE."
    }
}

function Wait-Port {
    param(
        [string]$Name,
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port -HostName $HostName -Port $Port -TimeoutMs 500) {
            return
        }
        Start-Sleep -Milliseconds 500
    }

    throw "$Name did not open $HostName`:$Port within $TimeoutSeconds seconds."
}

function New-LocalServiceTempDir {
    param([string]$Prefix)

    $baseDir = Join-Path $repoRoot "target"
    if (Test-Path -LiteralPath "D:\") {
        $baseDir = "D:\codex-niuke-system-test"
        New-Item -ItemType Directory -Force -Path $baseDir | Out-Null
    }

    $tempDir = Join-Path $baseDir ("$Prefix-" + [Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
    $script:managedTempDirs += $tempDir
    return $tempDir
}

function Test-ManagedTempDirAllowed {
    param([string]$Path)

    $resolved = Resolve-Path -LiteralPath $Path
    $allowedRoots = @((Resolve-Path -LiteralPath (Join-Path $repoRoot "target")).Path)
    $dDriveRoot = "D:\codex-niuke-system-test"
    if (Test-Path -LiteralPath $dDriveRoot) {
        $allowedRoots += (Resolve-Path -LiteralPath $dDriveRoot).Path
    }

    foreach ($root in $allowedRoots) {
        if ($resolved.Path.StartsWith($root, [System.StringComparison]::OrdinalIgnoreCase)) {
            return $true
        }
    }
    return $false
}

function Start-LocalRedis {
    if (Test-Port -HostName "127.0.0.1" -Port 6379 -TimeoutMs 500) {
        return
    }

    $redisExe = "D:\redis\redis-server.exe"
    $redisConfig = "D:\redis\redis.windows.conf"
    if (-not (Test-Path -LiteralPath $redisExe) -or -not (Test-Path -LiteralPath $redisConfig)) {
        throw "Redis is not listening and D:\redis is not usable."
    }

    $job = Start-Job -ScriptBlock {
        param($exe, $config, $workDir)
        Set-Location $workDir
        & $exe $config
    } -ArgumentList $redisExe, $redisConfig, "D:\redis"

    $script:managedJobs += $job
    Wait-Port -Name "Redis" -HostName "127.0.0.1" -Port 6379 -TimeoutSeconds 15
}

function Start-LocalKafka {
    if (Test-Port -HostName "127.0.0.1" -Port 9092 -TimeoutMs 500) {
        return
    }

    $kafkaSource = "D:\kafka_2.13-2.8.2"
    if (-not (Test-Path -LiteralPath (Join-Path $kafkaSource "bin\windows\kafka-server-start.bat"))) {
        throw "Kafka is not listening and $kafkaSource is not usable."
    }
    if (Get-PSDrive -Name K -ErrorAction SilentlyContinue) {
        throw "Kafka local startup needs temporary K: mapping, but K: is already in use."
    }

    cmd.exe /c subst K: "$kafkaSource"
    $script:kafkaDriveMapped = $true

    $kafkaTarget = New-LocalServiceTempDir "system-test-kafka"
    $kafkaData = Join-Path $kafkaTarget "kafka-logs"
    $zookeeperData = Join-Path $kafkaTarget "zookeeper-data"
    $kafkaLogs = Join-Path $kafkaTarget "logs"
    New-Item -ItemType Directory -Force -Path $kafkaData, $zookeeperData, $kafkaLogs | Out-Null

    $zookeeperConfig = Join-Path $kafkaTarget "zookeeper.properties"
    $zookeeperDataPath = $zookeeperData.Replace("\", "/")
    (Get-Content -LiteralPath "K:\config\zookeeper.properties") `
        -replace "^dataDir=.*", "dataDir=$zookeeperDataPath" `
        -replace "^clientPort=.*", "clientPort=2181" |
        Set-Content -LiteralPath $zookeeperConfig -Encoding ASCII
    Add-Content -LiteralPath $zookeeperConfig -Encoding ASCII -Value "admin.enableServer=false"

    $kafkaConfig = Join-Path $kafkaTarget "server.properties"
    $dataPath = $kafkaData.Replace("\", "/")
    Get-Content -LiteralPath "K:\config\server.properties" |
        Set-Content -LiteralPath $kafkaConfig -Encoding ASCII
    Add-Content -LiteralPath $kafkaConfig -Encoding ASCII -Value @(
        "broker.id=1",
        "listeners=PLAINTEXT://127.0.0.1:9092",
        "advertised.listeners=PLAINTEXT://127.0.0.1:9092",
        "log.dirs=$dataPath",
        "zookeeper.connect=127.0.0.1:2181",
        "offsets.topic.replication.factor=1",
        "transaction.state.log.replication.factor=1",
        "transaction.state.log.min.isr=1"
    )

    if (-not (Test-Port -HostName "127.0.0.1" -Port 2181 -TimeoutMs 500)) {
        $zookeeperJob = Start-Job -ScriptBlock {
            param($scriptPath, $configPath, $workDir, $logDir)
            $env:LOG_DIR = $logDir
            Set-Location $workDir
            & $scriptPath $configPath
        } -ArgumentList "K:\bin\windows\zookeeper-server-start.bat", $zookeeperConfig, "K:\", $kafkaLogs

        $script:managedJobs += $zookeeperJob
        Wait-Port -Name "ZooKeeper" -HostName "127.0.0.1" -Port 2181 -TimeoutSeconds 30
    }

    $job = Start-Job -ScriptBlock {
        param($scriptPath, $configPath, $workDir, $logDir)
        $env:LOG_DIR = $logDir
        Set-Location $workDir
        & $scriptPath $configPath
    } -ArgumentList "K:\bin\windows\kafka-server-start.bat", $kafkaConfig, "K:\", $kafkaLogs

    $script:managedJobs += $job
    Wait-Port -Name "Kafka" -HostName "127.0.0.1" -Port 9092 -TimeoutSeconds 60

    foreach ($topic in @("comment", "follow", "like", "publish", "delete")) {
        Invoke-Native {
            & "K:\bin\windows\kafka-topics.bat" `
                    --bootstrap-server "127.0.0.1:9092" `
                --create `
                --if-not-exists `
                --topic $topic `
                --partitions 1 `
                --replication-factor 1
        }
    }
}

function Start-LocalElasticsearch {
    if (Test-Port -HostName "127.0.0.1" -Port 9300 -TimeoutMs 500) {
        return
    }

    $esHome = "D:\elasticsearch-6.4.3-official\elasticsearch-6.4.3"
    $javaHome = "D:\temurin-jdk8\jdk8u492-b09"
    if (-not (Test-Path -LiteralPath (Join-Path $esHome "bin\elasticsearch.bat"))) {
        throw "Elasticsearch 6.4.3 is not listening and $esHome is not usable."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $javaHome "bin\java.exe"))) {
        throw "JDK 8 is required for Elasticsearch 6.4.3 and $javaHome is not usable."
    }
    if (-not (Test-Path -LiteralPath (Join-Path $esHome "config\elasticsearch.keystore"))) {
        throw "Elasticsearch keystore is missing. Run elasticsearch-keystore.bat create once in $esHome."
    }

    $esTarget = New-LocalServiceTempDir "system-test-es"
    $esData = Join-Path $esTarget "data"
    $esLogs = Join-Path $esTarget "logs"
    $esTemp = Join-Path $esTarget "tmp"
    New-Item -ItemType Directory -Force -Path $esData, $esLogs, $esTemp | Out-Null

    $job = Start-Job -ScriptBlock {
        param($homeDir, $jdkDir, $workDir, $dataDir, $logsDir, $tempDir)
        $env:JAVA_HOME = $jdkDir
        $env:ES_TMPDIR = $tempDir
        $env:PATH = "$jdkDir\bin;$env:PATH"
        Set-Location $workDir
        & (Join-Path $homeDir "bin\elasticsearch.bat") `
            "-Ecluster.name=nowcoder" `
            "-Enode.name=niuke-system-test" `
            "-Enetwork.host=127.0.0.1" `
            "-Ehttp.port=9200" `
            "-Etransport.tcp.port=9300" `
            "-Epath.data=$dataDir" `
            "-Epath.logs=$logsDir"
    } -ArgumentList $esHome, $javaHome, $esTarget, $esData, $esLogs, $esTemp

    $script:managedJobs += $job
    Wait-Port -Name "Elasticsearch transport" -HostName "127.0.0.1" -Port 9300 -TimeoutSeconds 90
}

function Assert-MySqlReady {
    if (-not $env:COMMUNITY_DB_USERNAME) {
        $env:COMMUNITY_DB_USERNAME = "root"
    }
    if (-not $env:COMMUNITY_DB_PASSWORD) {
        throw "Set COMMUNITY_DB_PASSWORD before running the system test."
    }

    $mysql = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if (-not $mysql) {
        throw "mysql.exe is required to verify niuke_xuexi before running the system test."
    }

    $oldMysqlPwd = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $env:COMMUNITY_DB_PASSWORD
        & mysql "-u$($env:COMMUNITY_DB_USERNAME)" -e "USE niuke_xuexi; SHOW TABLES;" | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Cannot connect to MySQL database niuke_xuexi with COMMUNITY_DB_USERNAME/COMMUNITY_DB_PASSWORD."
        }
    }
    finally {
        $env:MYSQL_PWD = $oldMysqlPwd
    }
}

if ($LoadOnly) {
    return
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$managedJobs = @()
$managedTempDirs = @()
$kafkaDriveMapped = $false
$appJob = $null
try {
    if ($UseLocalDServices) {
        Start-LocalRedis
        Start-LocalKafka
        Start-LocalElasticsearch
    }

    Assert-MySqlReady

    $requirements = @(
        @{ Name = "MySQL"; HostName = "127.0.0.1"; Port = 3306 },
        @{ Name = "Redis"; HostName = "127.0.0.1"; Port = 6379 },
        @{ Name = "Kafka"; HostName = "127.0.0.1"; Port = 9092 },
        @{ Name = "Elasticsearch transport"; HostName = "127.0.0.1"; Port = 9300 }
    )

    $missing = @()
    foreach ($service in $requirements) {
        if (-not (Test-Port -HostName $service.HostName -Port $service.Port)) {
            $missing += "$($service.Name) at $($service.HostName):$($service.Port)"
        }
    }

    if ($missing.Count -gt 0) {
        throw "System test blocked. Missing services: $($missing -join ', ')."
    }

    Invoke-Native { & .\mvnw.cmd -q -DskipTests package }

    $jar = Join-Path $repoRoot "target\community-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $jar)) {
        throw "Package artifact not found: $jar"
    }

    $stdout = Join-Path $repoRoot "target\system-test-app.out.log"
    $stderr = Join-Path $repoRoot "target\system-test-app.err.log"
    $args = @(
        "-jar",
        $jar,
        "--server.port=$Port",
        "--spring.devtools.restart.enabled=false"
    )

    $appJob = Start-Job -ScriptBlock {
        param($javaArgs, $outFile, $errFile, $workDir)
        Set-Location $workDir
        & java @javaArgs > $outFile 2> $errFile
    } -ArgumentList (,$args), $stdout, $stderr, $repoRoot

    $managedJobs += $appJob

    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        if ($appJob.State -ne "Running") {
            throw "Application exited during startup. See $stdout and $stderr."
        }
        if (Test-Port -HostName "127.0.0.1" -Port $Port -TimeoutMs 500) {
            break
        }
        Start-Sleep -Milliseconds 500
    }

    if (-not (Test-Port -HostName "127.0.0.1" -Port $Port -TimeoutMs 500)) {
        throw "Application did not open port $Port within 60 seconds. See $stdout and $stderr."
    }

    $url = "http://127.0.0.1:$Port/community/index"
    $response = Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 10
    if ($response.StatusCode -ne 200) {
        throw "Expected HTTP 200 from $url, got $($response.StatusCode)."
    }
    if ($response.Content -notmatch "<html") {
        throw "Expected an HTML response from $url."
    }

    Write-Host "System test passed: $url returned HTTP 200."
}
finally {
    foreach ($job in $managedJobs) {
        Stop-Job $job -ErrorAction SilentlyContinue
        Remove-Job $job -Force -ErrorAction SilentlyContinue
    }
    if ($kafkaDriveMapped) {
        cmd.exe /c subst K: /d
    }
    foreach ($dir in $managedTempDirs) {
        if (Test-Path -LiteralPath $dir) {
            if (Test-ManagedTempDirAllowed -Path $dir) {
                Remove-Item -LiteralPath (Resolve-Path -LiteralPath $dir).Path -Recurse -Force
            }
        }
    }
}
