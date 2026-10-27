. (Join-Path $PSScriptRoot 'Find-Tools.ps1')
Set-Location (Join-Path $RepoRoot 'backend')
& $Maven -B -ntp -f user-pom.xml verify
if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed.' }
Set-Location (Join-Path $RepoRoot 'frontend')
& npm.cmd run build
if ($LASTEXITCODE -ne 0) { throw 'Team frontend build failed.' }
& npm.cmd exec vite -- build --config 'src/modules/user/vite.user.config.mjs'
if ($LASTEXITCODE -ne 0) { throw 'Standalone frontend build failed.' }
Write-Host 'All checks passed.'
