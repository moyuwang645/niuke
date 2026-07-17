$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

& .\mvnw.cmd -q test
if ($LASTEXITCODE -ne 0) {
    throw "Unit tests failed with exit code $LASTEXITCODE."
}

& .\mvnw.cmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) {
    throw "Package build failed with exit code $LASTEXITCODE."
}
