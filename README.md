# Windows for Norm

`windows` provides Norm APIs for Windows credentials, owned processes, desktop integration, bounded local HTTP/SSE, atomic storage, verified archives, bundled resource materialization and protected token initialization. Application protocols, credentials policy and business logic belong to consumers.

Declare `dependency(repository: "github", name: "windows", version: 2)` in your Norm module. The public API is selected by [module.norm](windows/module.norm); the published NAR bundles its pinned runtime dependencies. Consumers need neither Java source nor a separate Java build.

The supported platform is Windows x64. Platform adapters are implemented internally with JDK APIs and JNA. Norm-facing behavior is exercised by [API tests](windows/tests/platform_tests.norm), including real DPAPI and loopback HTTP. [Platform tests](src/test/java/dev/normlanguage/windows) cover process ownership, bounded streams, cancellation, archive integrity and storage locking.

Run `./scripts/build.ps1 -NormExecutable <path-to-norm.exe>` using PowerShell 7 and JDK 25. The [build entry point](scripts/build.ps1) builds and tests the platform artifact, resolves the declared binding, packages the NAR, and [tests a consumer](tests/package.ps1) with an isolated cache containing only that NAR. Artifact coordinates and content digests are declared in [module.norm](windows/module.norm); build tooling derives the artifact version from it.

Release and verification conditions are defined in [the workflow](.github/workflows/release.yml). Norm module versions use integer `vN` tags. Published versions are immutable.
