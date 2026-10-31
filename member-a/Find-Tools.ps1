$ErrorActionPreference = 'Stop'
$script:RepoRoot = Split-Path -Parent $PSScriptRoot
$javaCandidates = @(
    'C:\Program Files\Java\jdk-17',
    $env:JAVA_HOME
)
foreach ($candidate in $javaCandidates) {
    if ($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe'))) {
        $env:JAVA_HOME = $candidate
        break
    }
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    throw 'JDK 17 or 21 is required. Set JAVA_HOME before running this script.'
}
$script:Java = Join-Path $env:JAVA_HOME 'bin\java.exe'
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$script:Maven = if ($mavenCommand) { $mavenCommand.Source } else { $null }
$mavenCandidates = @(
    'D:\java\IntelliJ IDEA 2026.2.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
)
if ($env:MAVEN_HOME) { $mavenCandidates += Join-Path $env:MAVEN_HOME 'bin\mvn.cmd' }
if (-not $script:Maven) {
    foreach ($candidate in $mavenCandidates) {
        if (Test-Path -LiteralPath $candidate) { $script:Maven = $candidate; break }
    }
}
if (-not $script:Maven) { throw 'Maven not found. Add mvn.cmd to PATH or set MAVEN_HOME to IDEA bundled Maven.' }
