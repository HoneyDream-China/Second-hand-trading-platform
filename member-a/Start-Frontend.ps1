$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location (Join-Path $repoRoot 'frontend')
if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) { throw 'Node.js and npm are required.' }
if (-not (Test-Path -LiteralPath 'node_modules\vite\bin\vite.js')) {
    & npm.cmd ci --no-audit --no-fund
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
}
Write-Host 'Open http://127.0.0.1:5173/src/modules/user/index.html'
Write-Host 'Press Ctrl+C to stop.'
& node.exe 'node_modules/vite/bin/vite.js' --config 'src/modules/user/vite.user.config.mjs'
