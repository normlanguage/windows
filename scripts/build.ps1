#requires -Version 7.0
[CmdletBinding()]
param([Parameter(Mandatory)][string]$NormExecutable)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$license = Join-Path $root 'windows/resources/META-INF/licenses/windows'
New-Item -ItemType Directory -Force $license | Out-Null
Copy-Item -LiteralPath (Join-Path $root 'LICENSE') -Destination (Join-Path $license 'LICENSE') -Force
$NormExecutable = (Resolve-Path -LiteralPath $NormExecutable).Path
& (Join-Path $root 'gradlew.bat') -p $root test publish --no-daemon
if ($LASTEXITCODE -ne 0) { throw 'Windows platform build or tests failed.' }
$version = [regex]::Match((Get-Content (Join-Path $root 'windows/module.norm') -Raw), 'artifact: "windows", version: "([^"]+)"').Groups[1].Value
$artifact = Get-Item (Join-Path $root "build/libs/windows-$version.jar")
$normHome = Join-Path $root ('.tmp/norm-home/' + (Get-FileHash $artifact.FullName).Hash.ToLowerInvariant())
$cache = Join-Path $normHome '.norm/cache/maven'
New-Item -ItemType Directory -Force $cache | Out-Null
Copy-Item -Path (Join-Path $root 'build/maven/*') -Destination $cache -Recurse -Force
$previous = $env:JDK_JAVA_OPTIONS
try {
    $env:JDK_JAVA_OPTIONS = "$previous --add-modules=jdk.httpserver -Duser.home=`"$normHome`""
    & $NormExecutable resolve (Join-Path $root 'windows')
    if ($LASTEXITCODE -ne 0) { throw 'Windows module resolution failed.' }
    & $NormExecutable test (Join-Path $root 'windows') --format json
    if ($LASTEXITCODE -ne 0) { throw 'Norm API tests failed.' }
    & (Join-Path $root 'tests/source.ps1') -NormExecutable $NormExecutable -NormHome $normHome
    & $NormExecutable package (Join-Path $root 'windows') --output (Join-Path $root 'build/repository')
    if ($LASTEXITCODE -ne 0) { throw 'Norm module packaging failed.' }
    & $NormExecutable docs (Join-Path $root 'windows') --output (Join-Path $root 'build/api') --skill (Join-Path $root 'SKILL.md')
    if ($LASTEXITCODE -ne 0) { throw 'Skill generation failed.' }
} finally { $env:JDK_JAVA_OPTIONS = $previous }
$archives = @(Get-ChildItem (Join-Path $root 'build/repository') -Recurse -Filter '*.nar')
if ($archives.Count -ne 1) { throw 'Use a clean output directory containing one module release.' }
& (Join-Path $root 'tests/package.ps1') -NormExecutable $NormExecutable -Archive $archives[0].FullName
