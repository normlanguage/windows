#requires -Version 7.0
[CmdletBinding()]
param([Parameter(Mandatory)][string]$NormExecutable, [Parameter(Mandatory)][string]$Archive)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$temporary = Join-Path $root ('.tmp/consumer-' + [Guid]::NewGuid().ToString('N'))
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Archive).Path)
try {
    $reader = [IO.StreamReader]::new($zip.GetEntry('module.json').Open())
    try { $manifest = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    if ($manifest.module.name -ne 'windows' -or $manifest.module.version -lt 1) { throw 'Unexpected module identity.' }
    if (-not $zip.GetEntry('java/graphs.json')) { throw 'Platform dependencies must be bundled.' }
} finally { $zip.Dispose() }
$version = $manifest.module.version
$consumer = Join-Path $temporary 'windows/acceptance'
$cache = Join-Path $temporary "home/.norm/cache/packages/github/windows/windows/$version"
New-Item -ItemType Directory -Force $cache, (Join-Path $consumer 'tests') | Out-Null
Copy-Item -LiteralPath $Archive, "$Archive.sha256" -Destination $cache
"Module module() { module(name: `"windows.acceptance`", version: 1, dependencies: [dependency(repository: `"github`", name: `"windows`", version: $version)]) }" | Set-Content -LiteralPath (Join-Path $consumer 'module.norm') -Encoding utf8NoBOM
$tests = (Get-Content (Join-Path $root 'windows/tests/platform_tests.norm') -Raw).Replace('package windows', 'package windows.acceptance')
$tests | Set-Content -LiteralPath (Join-Path $consumer 'tests/platform_tests.norm') -Encoding utf8NoBOM
$previous = $env:JDK_JAVA_OPTIONS
try {
    $env:JDK_JAVA_OPTIONS = "$previous --add-modules=jdk.httpserver -Duser.home=`"$(Join-Path $temporary 'home')`""
    & $NormExecutable test $consumer --format json
    if ($LASTEXITCODE -ne 0) { throw 'Published module consumer tests failed.' }
} finally { $env:JDK_JAVA_OPTIONS = $previous }
