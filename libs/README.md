# libs/

The mod compiles against these jars (`compileOnly` + `runtimeOnly` fileTree). They are other
people's mods, so they are **not** committed to this repository - download the matching versions and
drop them here before building:

| File | Where to get it |
|---|---|
| `create-1.21.1-6.0.10.jar` | Create for NeoForge 1.21.1 (CurseForge / Modrinth) |
| `flywheel-neoforge-1.21.1-1.0.6.jar` | ships inside Create's jar (`META-INF/jarjar`), also on Modrinth |
| `ponder-neoforge-1.0.82+mc1.21.1.jar` | ships inside Create's jar (`META-INF/jarjar`) |
| `Registrate-MC1.21-1.3.0+67.jar` | CMM's dependency, on Modrinth/Maven |
| `createmoremachines-1.21.1-2.7.jar` | Create: More Machines for NeoForge 1.21.1 |

```
libs/
  create-1.21.1-6.0.10.jar
  flywheel-neoforge-1.21.1-1.0.6.jar
  ponder-neoforge-1.0.82+mc1.21.1.jar
  Registrate-MC1.21-1.3.0+67.jar
  createmoremachines-1.21.1-2.7.jar
```

Then `gradlew build`. (If you want CI builds on GitHub, the dependencies have to come from a Maven
repository instead - Create publishes dev artifacts, so `build.gradle` can be switched from the
`fileTree` to real coordinates once CMM is available that way too.)
