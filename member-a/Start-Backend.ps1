. (Join-Path $PSScriptRoot 'Find-Tools.ps1')
Set-Location (Join-Path $RepoRoot 'backend')
& $Maven -B -ntp -f user-pom.xml '-DskipTests' package
if ($LASTEXITCODE -ne 0) { throw 'Backend build failed. Read the error above.' }
Write-Host 'User backend: http://127.0.0.1:8080. Press Ctrl+C to stop.'
& $Java -jar 'target/user/campus-user-1.0.0.jar'
