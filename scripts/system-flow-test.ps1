param(
    [int]$Port = 18081,
    [switch]$UseLocalDServices
)

$ErrorActionPreference = "Stop"

$flowPort = $Port
$flowUseLocalDServices = $UseLocalDServices
. (Join-Path $PSScriptRoot "system-test.ps1") -LoadOnly
$Port = $flowPort
$UseLocalDServices = $flowUseLocalDServices

function ConvertTo-SqlString {
    param([string]$Value)
    return "'" + ($Value -replace "'", "''") + "'"
}

function Get-Md5Hex {
    param([string]$Text)
    $md5 = [System.Security.Cryptography.MD5]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($Text)
        $hash = $md5.ComputeHash($bytes)
        return -join ($hash | ForEach-Object { $_.ToString("x2") })
    }
    finally {
        $md5.Dispose()
    }
}

function Invoke-MySql {
    param(
        [string]$Sql,
        [switch]$Scalar
    )

    $oldMysqlPwd = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $env:COMMUNITY_DB_PASSWORD
        $output = & mysql "-u$($env:COMMUNITY_DB_USERNAME)" `
            "-D" "niuke_xuexi" `
            "--batch" `
            "--raw" `
            "--skip-column-names" `
            "-e" $Sql 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL command failed: $output"
        }
        if ($Scalar) {
            return ($output | Select-Object -First 1)
        }
        return $output
    }
    finally {
        $env:MYSQL_PWD = $oldMysqlPwd
    }
}

function New-TestUser {
    param(
        [string]$NamePrefix,
        [string]$RunId
    )

    $username = "$NamePrefix$RunId"
    $email = "$username@example.test"
    $plainPassword = "codex-flow-password"
    $salt = "abcde"
    $passwordHash = Get-Md5Hex "$plainPassword$salt"
    $activationCode = "act$RunId"
    $headerUrl = "http://images.nowcoder.com/head/1t.png"

    $insertSql = @"
insert into user(username,password,salt,email,type,status,activation_code,header_url,create_time)
values($(ConvertTo-SqlString $username),$(ConvertTo-SqlString $passwordHash),$(ConvertTo-SqlString $salt),$(ConvertTo-SqlString $email),0,1,$(ConvertTo-SqlString $activationCode),$(ConvertTo-SqlString $headerUrl),NOW());
"@
    Invoke-MySql $insertSql | Out-Null
    $id = [int](Invoke-MySql -Scalar "select id from user where username=$(ConvertTo-SqlString $username) order by id desc limit 1;")
    return [pscustomobject]@{
        Id = $id
        Username = $username
        Password = $plainPassword
    }
}

function New-TestTicket {
    param(
        [int]$UserId,
        [string]$RunId,
        [string]$Suffix
    )

    $ticket = "ticket$RunId$Suffix"
    Invoke-MySql "insert into login_ticket(user_id,ticket,status,expired) values($UserId,$(ConvertTo-SqlString $ticket),0,DATE_ADD(NOW(), INTERVAL 1 DAY));" | Out-Null
    return $ticket
}

function Remove-StaleFlowRows {
    Invoke-MySql @"
delete from comment
where entity_type = 1
and entity_id in (
    select id from discuss_post
    where title regexp '^codex.*[0-9]{10,}$'
    and user_id not in (select id from user)
);
delete from discuss_post
where title regexp '^codex.*[0-9]{10,}$'
and user_id not in (select id from user);
"@ | Out-Null

    $idsOutput = Invoke-MySql "select id from user where username regexp '^codex_flow_[ab]_[0-9]+';"
    $ids = @($idsOutput | Where-Object { $_ -match '^\d+$' })
    if ($ids.Count -eq 0) {
        return
    }

    $idList = $ids -join ","
    Invoke-MySql @"
delete from login_ticket where user_id in ($idList);
delete from message where from_id in ($idList) or to_id in ($idList);
delete from comment where user_id in ($idList);
delete from discuss_post where user_id in ($idList);
delete from user where id in ($idList);
"@ | Out-Null
}

function Invoke-Page {
    param(
        [string]$Path,
        [string]$Ticket,
        [string]$Method = "GET",
        [hashtable]$Body
    )

    $params = @{
        UseBasicParsing = $true
        Uri = "$baseUrl$Path"
        Method = $Method
        TimeoutSec = 15
    }
    if ($Ticket) {
        $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $session.Cookies.Add((New-Object System.Net.Cookie("ticket", $Ticket, "/community", "127.0.0.1")))
        $params["WebSession"] = $session
    }
    if ($Body) {
        $params["Body"] = $Body
        $params["ContentType"] = "application/x-www-form-urlencoded"
    }

    return Invoke-WebRequest @params
}

function Get-ResponseText {
    param($Response)

    if ($Response.Content -is [byte[]]) {
        return [System.Text.Encoding]::UTF8.GetString($Response.Content)
    }
    return [string]$Response.Content
}

function Invoke-JsonPost {
    param(
        [string]$Path,
        [string]$Ticket,
        [hashtable]$Body,
        [int]$ExpectedCode = 0
    )

    $headers = @{
        "X-Requested-With" = "XMLHttpRequest"
    }
    $session = $null
    if ($Ticket) {
        $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        $session.Cookies.Add((New-Object System.Net.Cookie("ticket", $Ticket, "/community", "127.0.0.1")))
    }

    $params = @{
        UseBasicParsing = $true
        Uri = "$baseUrl$Path"
        Method = "Post"
        Headers = $headers
        Body = $Body
        ContentType = "application/x-www-form-urlencoded"
        TimeoutSec = 15
    }
    if ($session) {
        $params["WebSession"] = $session
    }

    $response = Invoke-WebRequest @params
    $content = Get-ResponseText $response
    $json = $content | ConvertFrom-Json
    if ([int]$json.code -ne $ExpectedCode) {
        throw "Expected JSON code $ExpectedCode from $Path, got $($json.code): $content"
    }
    return $json
}

function Wait-MySqlCount {
    param(
        [string]$Name,
        [string]$Sql,
        [int]$Minimum = 1,
        [int]$TimeoutSeconds = 20
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $count = [int](Invoke-MySql -Scalar $Sql)
        if ($count -ge $Minimum) {
            return $count
        }
        Start-Sleep -Milliseconds 500
    }
    throw "$Name did not reach count $Minimum within $TimeoutSeconds seconds."
}

function Assert-Content {
    param(
        [string]$Name,
        [object]$Content,
        [string]$Needle
    )

    if ($Content -is [byte[]]) {
        $Content = [System.Text.Encoding]::UTF8.GetString($Content)
    }
    if ($Content -notlike "*$Needle*") {
        throw "$Name did not contain expected text: $Needle"
    }
}

function Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host "[RUN] $Name"
    & $Action
    $script:passedSteps += $Name
    Write-Host "[PASS] $Name"
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$managedJobs = @()
$managedTempDirs = @()
$kafkaDriveMapped = $false
$appJob = $null
$baseUrl = "http://127.0.0.1:$Port/community"
$runId = (Get-Date).ToString("yyyyMMddHHmmss")
$createdUserIds = @()
$createdPostId = $null
$ticketA = $null
$ticketB = $null
$userA = $null
$userB = $null
$postTitle = "codex flow post $runId"
$postContent = "codex flow post content $runId"
$commentContent = "codex flow comment $runId"
$letterContent = "codex flow letter $runId"
$passedSteps = @()

try {
    if (-not $env:COMMUNITY_DB_USERNAME) {
        $env:COMMUNITY_DB_USERNAME = "root"
    }
    if (-not $env:COMMUNITY_DB_PASSWORD) {
        throw "Set COMMUNITY_DB_PASSWORD before running the system flow test."
    }

    if ($UseLocalDServices) {
        Start-LocalRedis
        Start-LocalKafka
        Start-LocalElasticsearch
    }

    Assert-MySqlReady
    Remove-StaleFlowRows
    Invoke-Native { & .\mvnw.cmd -q -DskipTests package }

    Step "seed two activated users and login tickets" {
        $script:userA = New-TestUser -NamePrefix "codex_flow_a_" -RunId $runId
        $script:userB = New-TestUser -NamePrefix "codex_flow_b_" -RunId $runId
        $script:createdUserIds = @($script:userA.Id, $script:userB.Id)
        $script:ticketA = New-TestTicket -UserId $script:userA.Id -RunId $runId -Suffix "a"
        $script:ticketB = New-TestTicket -UserId $script:userB.Id -RunId $runId -Suffix "b"
    }

    $jar = Join-Path $repoRoot "target\community-0.0.1-SNAPSHOT.jar"
    $stdout = Join-Path $repoRoot "target\system-flow-test-app.out.log"
    $stderr = Join-Path $repoRoot "target\system-flow-test-app.err.log"
    $args = @(
        "-jar",
        $jar,
        "--server.port=$Port",
        "--spring.devtools.restart.enabled=false"
    )
    $appJob = Start-Job -ScriptBlock {
        param($javaArgs, $outFile, $errFile, $workDir, $dbUser, $dbPassword)
        $env:COMMUNITY_DB_USERNAME = $dbUser
        $env:COMMUNITY_DB_PASSWORD = $dbPassword
        Set-Location $workDir
        & java @javaArgs > $outFile 2> $errFile
    } -ArgumentList (,$args), $stdout, $stderr, $repoRoot, $env:COMMUNITY_DB_USERNAME, $env:COMMUNITY_DB_PASSWORD
    $managedJobs += $appJob
    Wait-Port -Name "Application" -HostName "127.0.0.1" -Port $Port -TimeoutSeconds 60

    Step "anonymous homepage" {
        $response = Invoke-Page -Path "/index"
        if ($response.StatusCode -ne 200) {
            throw "Expected homepage HTTP 200, got $($response.StatusCode)."
        }
        Assert-Content -Name "homepage" -Content $response.Content -Needle "<html"
    }

    Step "anonymous AJAX is rejected" {
        Invoke-JsonPost -Path "/discuss/add" -Body @{ title = "x"; content = "x" } -ExpectedCode 403 | Out-Null
    }

    Step "authenticated settings page" {
        $response = Invoke-Page -Path "/user/setting" -Ticket $ticketA
        if ($response.StatusCode -ne 200) {
            throw "Expected setting page HTTP 200, got $($response.StatusCode)."
        }
        Assert-Content -Name "settings page" -Content $response.Content -Needle "head-image"
    }

    Step "publish discuss post" {
        $json = Invoke-JsonPost -Path "/discuss/add" -Ticket $ticketA -Body @{
            title = $postTitle
            content = $postContent
        }
        $script:createdPostId = [int](Invoke-MySql -Scalar "select id from discuss_post where user_id=$($userA.Id) order by id desc limit 1;")
        if ($script:createdPostId -le 0) {
            throw "Published post was not found in database."
        }
    }

    Step "view discuss detail" {
        $response = Invoke-Page -Path "/discuss/detail/$createdPostId" -Ticket $ticketA
        if ($response.StatusCode -ne 200) {
            throw "Expected discuss detail HTTP 200, got $($response.StatusCode)."
        }
        Assert-Content -Name "discuss detail" -Content $response.Content -Needle $runId
    }

    Step "comment on post as another user" {
        $response = Invoke-Page -Path "/comment/add/$createdPostId" -Ticket $ticketB -Method "POST" -Body @{
            entityType = "1"
            entityId = "$createdPostId"
            targetId = "0"
            content = $commentContent
        }
        if ($response.StatusCode -ne 200) {
            throw "Expected comment redirect final HTTP 200, got $($response.StatusCode)."
        }
        $commentRows = [int](Invoke-MySql -Scalar "select count(*) from comment where user_id=$($userB.Id) and entity_type=1 and entity_id=$createdPostId;")
        if ($commentRows -ne 1) {
            throw "Expected one inserted comment, got $commentRows."
        }
        $commentCount = [int](Invoke-MySql -Scalar "select comment_count from discuss_post where id=$createdPostId;")
        if ($commentCount -lt 1) {
            throw "Expected discuss_post.comment_count to be updated, got $commentCount."
        }
    }

    Step "like post as another user" {
        $json = Invoke-JsonPost -Path "/like" -Ticket $ticketB -Body @{
            entityType = "1"
            entityId = "$createdPostId"
            entityUserId = "$($userA.Id)"
            postId = "$createdPostId"
        }
        if ([int]$json.likeStatus -ne 1 -or [int]$json.likeCount -lt 1) {
            throw "Unexpected like result: $($json | ConvertTo-Json -Compress)"
        }
    }

    Step "follow author as another user" {
        $json = Invoke-JsonPost -Path "/follow" -Ticket $ticketB -Body @{
            entityType = "3"
            entityId = "$($userA.Id)"
        }
        $followResult = $json | ConvertTo-Json -Compress
        $followee = Invoke-Page -Path "/followee/$($userB.Id)" -Ticket $ticketB
        Assert-Content -Name "followee page" -Content $followee.Content -Needle $userA.Username
        $follower = Invoke-Page -Path "/follower/$($userA.Id)" -Ticket $ticketA
        Assert-Content -Name "follower page" -Content $follower.Content -Needle $userB.Username
    }

    Step "send and read private letter" {
        Invoke-JsonPost -Path "/letter/send" -Ticket $ticketB -Body @{
            toName = $userA.Username
            content = $letterContent
        } | Out-Null
        $conversationId = if ($userA.Id -lt $userB.Id) { "$($userA.Id)_$($userB.Id)" } else { "$($userB.Id)_$($userA.Id)" }
        Wait-MySqlCount -Name "private letter" -Sql "select count(*) from message where from_id=$($userB.Id) and to_id=$($userA.Id) and conversation_id=$(ConvertTo-SqlString $conversationId);" | Out-Null
        $letterList = Invoke-Page -Path "/letter/list" -Ticket $ticketA
        Assert-Content -Name "letter list" -Content $letterList.Content -Needle $userB.Username
        $letterDetail = Invoke-Page -Path "/letter/detail/$conversationId" -Ticket $ticketA
        Assert-Content -Name "letter detail" -Content $letterDetail.Content -Needle $runId
    }

    Step "receive comment, like, and follow notices" {
        Wait-MySqlCount -Name "comment notice" -Sql "select count(*) from message where from_id=1 and to_id=$($userA.Id) and conversation_id='comment';" | Out-Null
        Wait-MySqlCount -Name "like notice" -Sql "select count(*) from message where from_id=1 and to_id=$($userA.Id) and conversation_id='like';" | Out-Null
        Wait-MySqlCount -Name "follow notice" -Sql "select count(*) from message where from_id=1 and to_id=$($userA.Id) and conversation_id='follow';" | Out-Null
        $noticeList = Invoke-Page -Path "/notice/list" -Ticket $ticketA
        if ($noticeList.StatusCode -ne 200) {
            throw "Expected notice list HTTP 200, got $($noticeList.StatusCode)."
        }
        foreach ($topic in @("comment", "like", "follow")) {
            $noticeDetail = Invoke-Page -Path "/notice/detail/$topic" -Ticket $ticketA
            Assert-Content -Name "$topic notice detail" -Content $noticeDetail.Content -Needle $userB.Username
        }
    }

    Step "search published post" {
        Wait-MySqlCount -Name "Elasticsearch index trigger" -Sql "select count(*) from discuss_post where id=$createdPostId;" | Out-Null
        Start-Sleep -Seconds 3
        $encodedKey = [System.Uri]::EscapeDataString($runId)
        $response = Invoke-Page -Path "/search?key=$encodedKey" -Ticket $ticketA
        if ($response.StatusCode -ne 200) {
            throw "Expected search HTTP 200, got $($response.StatusCode)."
        }
        Assert-Content -Name "search page" -Content $response.Content -Needle $runId
    }

    Write-Host "System flow test passed. Steps: $($passedSteps.Count)."
}
finally {
    if ($appJob) {
        Stop-Job $appJob -ErrorAction SilentlyContinue
        Remove-Job $appJob -Force -ErrorAction SilentlyContinue
    }
    foreach ($job in $managedJobs) {
        Stop-Job $job -ErrorAction SilentlyContinue
        Remove-Job $job -Force -ErrorAction SilentlyContinue
    }
    if ($kafkaDriveMapped) {
        cmd.exe /c subst K: /d
    }
    if ($createdUserIds.Count -gt 0) {
        $ids = $createdUserIds -join ","
        try {
            $commentPostClause = ""
            $postDelete = ""
            if ($createdPostId) {
                $commentPostClause = " or entity_id=$createdPostId"
                $postDelete = "delete from discuss_post where id=$createdPostId;"
            }
            Invoke-MySql "delete from login_ticket where user_id in ($ids); delete from message where from_id in ($ids) or to_id in ($ids); delete from comment where user_id in ($ids)$commentPostClause; $postDelete delete from discuss_post where user_id in ($ids); delete from user where id in ($ids);" | Out-Null
        }
        catch {
            Write-Warning "Failed to clean up flow-test database rows: $($_.Exception.Message)"
        }
    }
    foreach ($dir in $managedTempDirs) {
        if (Test-Path -LiteralPath $dir) {
            if (Test-ManagedTempDirAllowed -Path $dir) {
                Remove-Item -LiteralPath (Resolve-Path -LiteralPath $dir).Path -Recurse -Force
            }
        }
    }
}
