# windows Skill

Use this skill to work with Norm module `windows` version 5. Use only declarations listed below.

## `windows`

Source: `ApplicationFiles.norm`

### `ApplicationFiles`

```norm
class ApplicationFiles
```

#### `ApplicationFiles`

```norm
ApplicationFiles(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

### `applicationFilesExecutableOnPath`

```norm
String? applicationFilesExecutableOnPath(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

### `applicationFilesFindExecutable`

```norm
String? applicationFilesFindExecutable(String? arg0, String? arg1, Integer arg2)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`
- `arg2`: `Integer`

Returns: `String?`

### `applicationFilesMaterialize`

```norm
String? applicationFilesMaterialize(String? arg0, String? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`

Returns: `String?`

## `windows`

Source: `AtomicTextStore.norm`

### `AtomicTextStore`

```norm
class AtomicTextStore
```

#### `AtomicTextStore`

```norm
AtomicTextStore(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `acquire`

```norm
StoreLease? acquire(String? arg0, Integer arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Integer`

Returns: `StoreLease?`

#### `keys`

```norm
JavaList<String>? keys(String? arg0, Integer arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Integer`

Returns: `JavaList<String>?`

#### `read`

```norm
String? read(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

#### `write`

```norm
Void write(String? arg0, String? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`

Returns: `Void`

### `atomicTextStoreNew`

```norm
AtomicTextStore atomicTextStoreNew(String? arg0, Integer arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Integer`

Returns: `AtomicTextStore`

### `atomicTextStoreDigest`

```norm
String? atomicTextStoreDigest(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

## `windows`

Source: `DesktopActions.norm`

### `DesktopActions`

```norm
class DesktopActions
```

#### `DesktopActions`

```norm
DesktopActions(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

### `desktopActionsBrowse`

```norm
Void desktopActionsBrowse(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Void`

### `desktopActionsCopyText`

```norm
Void desktopActionsCopyText(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Void`

### `desktopActionsOpenDirectory`

```norm
Void desktopActionsOpenDirectory(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Void`

## `windows`

Source: `DesktopTray.norm`

### `DesktopTray`

```norm
class DesktopTray implements AutoCloseable
```

#### `DesktopTray`

```norm
DesktopTray(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `available`

```norm
Boolean available()
```

Returns: `Boolean`

#### `close`

```norm
Void close()
```

Returns: `Void`

#### `install`

```norm
Boolean install(String? arg0, String? arg1, String? arg2, Function<Void()>? arg3, Function<Void()>? arg4)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`
- `arg2`: `String?`
- `arg3`: `Function<Void()>?`
- `arg4`: `Function<Void()>?`

Returns: `Boolean`

#### `message`

```norm
Void message(String? arg0, String? arg1, Boolean arg2)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`
- `arg2`: `Boolean`

Returns: `Void`

#### `tooltip`

```norm
Void tooltip(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Void`

### `desktopTrayNew`

```norm
DesktopTray desktopTrayNew()
```

Returns: `DesktopTray`

## `windows`

Source: `EndpointProbe.norm`

### `EndpointProbe`

```norm
class EndpointProbe
```

#### `EndpointProbe`

```norm
EndpointProbe(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

### `endpointProbeReadinessFile`

```norm
Integer endpointProbeReadinessFile(String? arg0, Integer arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Integer`

Returns: `Integer`

### `endpointProbeTcpOpen`

```norm
Boolean endpointProbeTcpOpen(Integer arg0, Integer arg1)
```

Parameters:

- `arg0`: `Integer`
- `arg1`: `Integer`

Returns: `Boolean`

## `windows`

Source: `HttpExchangeHandle.norm`

### `HttpExchangeHandle`

```norm
class HttpExchangeHandle implements AutoCloseable
```

#### `HttpExchangeHandle`

```norm
HttpExchangeHandle(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `begin`

```norm
Void begin(Integer arg0, String? arg1)
```

Parameters:

- `arg0`: `Integer`
- `arg1`: `String?`

Returns: `Void`

#### `body`

```norm
String? body()
```

Returns: `String?`

#### `close`

```norm
Void close()
```

Returns: `Void`

#### `header`

```norm
String? header(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

#### `method`

```norm
String? method()
```

Returns: `String?`

#### `path`

```norm
String? path()
```

Returns: `String?`

#### `respond`

```norm
Void respond(Integer arg0, String? arg1, String? arg2)
```

Parameters:

- `arg0`: `Integer`
- `arg1`: `String?`
- `arg2`: `String?`

Returns: `Void`

#### `responseHeader`

```norm
Void responseHeader(String? arg0, String? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`

Returns: `Void`

#### `started`

```norm
Boolean started()
```

Returns: `Boolean`

#### `write`

```norm
Void write(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Void`

## `windows`

Source: `HttpResult.norm`

### `HttpResult`

```norm
class HttpResult implements AutoCloseable
```

#### `HttpResult`

```norm
HttpResult(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `close`

```norm
Void close()
```

Returns: `Void`

#### `event`

```norm
String? event(Integer arg0)
```

Parameters:

- `arg0`: `Integer`

Returns: `String?`

#### `header`

```norm
String? header(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

#### `status`

```norm
Integer status()
```

Returns: `Integer`

## `windows`

Source: `LocalHttpSession.norm`

### `LocalHttpSession`

```norm
class LocalHttpSession implements AutoCloseable
```

#### `LocalHttpSession`

```norm
LocalHttpSession(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `close`

```norm
Void close()
```

Returns: `Void`

#### `request`

```norm
HttpResult? request(String? arg0, String? arg1, JavaMap<String, String>? arg2, Integer arg3)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`
- `arg2`: `JavaMap<String, String>?`
- `arg3`: `Integer`

Returns: `HttpResult?`

### `localHttpSessionNew`

```norm
LocalHttpSession localHttpSessionNew(String? arg0, Integer arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Integer`

Returns: `LocalHttpSession`

## `windows`

Source: `LoopbackServer.norm`

### `LoopbackServer`

```norm
class LoopbackServer implements AutoCloseable
```

#### `LoopbackServer`

```norm
LoopbackServer(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `close`

```norm
Void close()
```

Returns: `Void`

#### `port`

```norm
Integer port()
```

Returns: `Integer`

#### `start`

```norm
Void start(Function<Void(HttpExchangeHandle?)>? arg0)
```

Parameters:

- `arg0`: `Function<Void(HttpExchangeHandle?)>?`

Returns: `Void`

### `loopbackServerNew`

```norm
LoopbackServer loopbackServerNew(Integer arg0, Integer arg1, Integer arg2, Integer arg3)
```

Parameters:

- `arg0`: `Integer`
- `arg1`: `Integer`
- `arg2`: `Integer`
- `arg3`: `Integer`

Returns: `LoopbackServer`

### `loopbackServerIdentifier`

```norm
String? loopbackServerIdentifier()
```

Returns: `String?`

### `loopbackServerNowMillis`

```norm
Long loopbackServerNowMillis()
```

Returns: `Long`

### `loopbackServerRandomToken`

```norm
String? loopbackServerRandomToken()
```

Returns: `String?`

### `loopbackServerSecureEquals`

```norm
Boolean loopbackServerSecureEquals(String? arg0, String? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`

Returns: `Boolean`

## `windows`

Source: `NativeCredentials.norm`

### `NativeCredentials`

```norm
class NativeCredentials
```

#### `NativeCredentials`

```norm
NativeCredentials(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

### `nativeCredentialsLoadOrCreateToken`

```norm
String? nativeCredentialsLoadOrCreateToken(String? arg0, String? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`

Returns: `String?`

### `nativeCredentialsProtect`

```norm
String? nativeCredentialsProtect(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

### `nativeCredentialsUnprotect`

```norm
String? nativeCredentialsUnprotect(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

## `windows`

Source: `ProcessSession.norm`

### `ProcessSession`

```norm
class ProcessSession implements AutoCloseable
```

#### `ProcessSession`

```norm
ProcessSession(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `alive`

```norm
Boolean alive()
```

Returns: `Boolean`

#### `argument`

```norm
ProcessSession? argument(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `ProcessSession?`

#### `captureErrors`

```norm
ProcessSession? captureErrors(Integer arg0)
```

Parameters:

- `arg0`: `Integer`

Returns: `ProcessSession?`

#### `close`

```norm
Void close()
```

Returns: `Void`

#### `environment`

```norm
ProcessSession? environment(String? arg0, String? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`

Returns: `ProcessSession?`

#### `exitCode`

```norm
Integer exitCode()
```

Returns: `Integer`

#### `logMode`

```norm
ProcessSession? logMode(Integer arg0)
```

Parameters:

- `arg0`: `Integer`

Returns: `ProcessSession?`

#### `logs`

```norm
JavaList<String>? logs()
```

Returns: `JavaList<String>?`

#### `outputEnded`

```norm
Boolean outputEnded()
```

Returns: `Boolean`

#### `ownJob`

```norm
ProcessSession? ownJob()
```

Returns: `ProcessSession?`

#### `ownership`

```norm
String? ownership()
```

Returns: `String?`

#### `pid`

```norm
Long pid()
```

Returns: `Long`

#### `receiveLine`

```norm
String? receiveLine(Integer arg0)
```

Parameters:

- `arg0`: `Integer`

Returns: `String?`

#### `redact`

```norm
ProcessSession? redact(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `ProcessSession?`

#### `removeEnvironment`

```norm
ProcessSession? removeEnvironment(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `ProcessSession?`

#### `sendLine`

```norm
Void sendLine(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Void`

#### `start`

```norm
Void start()
```

Returns: `Void`

### `processSessionNew`

```norm
ProcessSession processSessionNew(String? arg0, String? arg1, Integer arg2)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`
- `arg2`: `Integer`

Returns: `ProcessSession`

## `windows`

Source: `RuntimeGate.norm`

### `RuntimeGate`

```norm
class RuntimeGate
```

#### `RuntimeGate`

```norm
RuntimeGate(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `enter`

```norm
Void enter()
```

Returns: `Void`

#### `exit`

```norm
Void exit()
```

Returns: `Void`

### `runtimeGateNew`

```norm
RuntimeGate runtimeGateNew()
```

Returns: `RuntimeGate`

## `windows`

Source: `SafeTextFiles.norm`

### `SafeTextFiles`

```norm
class SafeTextFiles
```

#### `SafeTextFiles`

```norm
SafeTextFiles(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

### `safeTextFilesCanonical`

```norm
String? safeTextFilesCanonical(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

### `safeTextFilesChild`

```norm
String? safeTextFilesChild(String? arg0, String? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`

Returns: `String?`

### `safeTextFilesChildren`

```norm
JavaList<String>? safeTextFilesChildren(String? arg0, Integer arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Integer`

Returns: `JavaList<String>?`

### `safeTextFilesCompareAndWrite`

```norm
Boolean safeTextFilesCompareAndWrite(String? arg0, String? arg1, String? arg2, Integer arg3)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`
- `arg2`: `String?`
- `arg3`: `Integer`

Returns: `Boolean`

### `safeTextFilesDirectory`

```norm
Boolean safeTextFilesDirectory(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Boolean`

### `safeTextFilesName`

```norm
String? safeTextFilesName(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

### `safeTextFilesParent`

```norm
String? safeTextFilesParent(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

### `safeTextFilesRead`

```norm
String? safeTextFilesRead(String? arg0, Integer arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Integer`

Returns: `String?`

## `windows`

Source: `SessionRegistry.norm`

### `SessionRegistry`

```norm
class SessionRegistry implements AutoCloseable
```

#### `SessionRegistry`

```norm
SessionRegistry(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `authenticate`

```norm
Boolean authenticate(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Boolean`

#### `authenticated`

```norm
Boolean authenticated(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Boolean`

#### `close`

```norm
Void close()
```

Returns: `Void`

#### `register`

```norm
Boolean register(String? arg0, Function<Void()>? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Function<Void()>?`

Returns: `Boolean`

#### `remove`

```norm
Void remove(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Void`

#### `submit`

```norm
Boolean submit(String? arg0, Function<Void()>? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Function<Void()>?`

Returns: `Boolean`

### `sessionRegistryNew`

```norm
SessionRegistry sessionRegistryNew(Integer arg0, Integer arg1, Integer arg2, Integer arg3)
```

Parameters:

- `arg0`: `Integer`
- `arg1`: `Integer`
- `arg2`: `Integer`
- `arg3`: `Integer`

Returns: `SessionRegistry`

## `windows`

Source: `StoreLease.norm`

### `StoreLease`

```norm
class StoreLease implements AutoCloseable
```

#### `StoreLease`

```norm
StoreLease(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `close`

```norm
Void close()
```

Returns: `Void`

## `windows`

Source: `StructuredText.norm`

### `StructuredText`

```norm
class StructuredText
```

#### `StructuredText`

```norm
StructuredText(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

### `structuredTextMatches`

```norm
Boolean structuredTextMatches(String? arg0, String? arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`

Returns: `Boolean`

### `structuredTextPathSegments`

```norm
JavaList<String>? structuredTextPathSegments(String? arg0, String? arg1, String? arg2)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`
- `arg2`: `String?`

Returns: `JavaList<String>?`

## `windows`

Source: `SystemSupport.norm`

### `SystemSupport`

```norm
class SystemSupport
```

#### `SystemSupport`

```norm
SystemSupport(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

### `systemSupportFormatTime`

```norm
String? systemSupportFormatTime(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

### `systemSupportProbeProxy`

```norm
Long systemSupportProbeProxy(String? arg0, Integer arg1)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `Integer`

Returns: `Long`

### `systemSupportUtf8Size`

```norm
Integer systemSupportUtf8Size(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `Integer`

### `systemSupportValidateEndpoint`

```norm
String? systemSupportValidateEndpoint(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

### `systemSupportValidateProxy`

```norm
String? systemSupportValidateProxy(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

## `windows`

Source: `VerifiedArchive.norm`

### `VerifiedArchive`

```norm
class VerifiedArchive
```

#### `VerifiedArchive`

```norm
VerifiedArchive(__JarBindingToken token)
```

Parameters:

- `token`: `__JarBindingToken`

#### `cancel`

```norm
Void cancel()
```

Returns: `Void`

#### `install`

```norm
String? install(String? arg0, String? arg1, String? arg2, String? arg3, String? arg4)
```

Parameters:

- `arg0`: `String?`
- `arg1`: `String?`
- `arg2`: `String?`
- `arg3`: `String?`
- `arg4`: `String?`

Returns: `String?`

#### `progress`

```norm
Integer progress()
```

Returns: `Integer`

### `verifiedArchiveNew`

```norm
VerifiedArchive verifiedArchiveNew()
```

Returns: `VerifiedArchive`

### `verifiedArchiveHash`

```norm
String? verifiedArchiveHash(String? arg0)
```

Parameters:

- `arg0`: `String?`

Returns: `String?`

