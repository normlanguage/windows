param([Parameter(Mandatory)][string]$NormExecutable, [Parameter(Mandatory)][string]$NormHome)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$temporary = Join-Path $root ('.tmp/source-consumer-' + [Guid]::NewGuid().ToString('N'))
$consumer = Join-Path $temporary 'windows/acceptance'
$dependency = Join-Path $temporary 'dependencies/windows'
New-Item -ItemType Directory -Force (Join-Path $consumer 'tests'), $dependency | Out-Null
$module = Get-Content (Join-Path $root 'windows/module.norm') -Raw
$version = [regex]::Match($module, 'module\(name: "windows", version: (\d+)').Groups[1].Value
Copy-Item -LiteralPath (Join-Path $root 'windows/module.norm') -Destination $dependency
"Module module() { module(name: `"windows.acceptance`", version: 1, dependencies: [dependency(repository: `"github`", name: `"windows`", version: $version)]) }" | Set-Content (Join-Path $consumer 'module.norm') -Encoding utf8NoBOM
@'
package windows.acceptance
import windows.atomicTextStoreDigest
import std.testing.Test
@Test
Void sourceBindingExportsThePlatformContract() {
  require(condition: atomicTextStoreDigest(arg0: "") == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", message: "External source consumer receives the public digest function")
}
'@ | Set-Content (Join-Path $consumer 'tests/contract.norm') -Encoding utf8NoBOM
$previous = $env:JDK_JAVA_OPTIONS
try {
    $env:JDK_JAVA_OPTIONS = "$previous --add-modules=jdk.httpserver -Duser.home=`"$NormHome`""
    & $NormExecutable test $consumer --format json
    if ($LASTEXITCODE -ne 0) { throw 'Public source consumer contract failed' }
} finally { $env:JDK_JAVA_OPTIONS = $previous }
