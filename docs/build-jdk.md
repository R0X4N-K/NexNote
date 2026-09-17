# JDK setup

Use a complete JDK 21 and Android SDK platform 36.1 for local and CI builds.
Both `javac` and `jlink` must be available; a reduced Java runtime can fail during Android's JDK image transform.

## Android Studio

In **Settings > Build, Execution, Deployment > Build Tools > Gradle**, select
a complete JDK 21 as the Gradle JDK. If you use `GRADLE_LOCAL_JAVA_HOME`, set
`java.home` in the local `.gradle/config.properties` file to that installation.
Machine-specific paths belong in ignored local configuration.

## Terminal

Set `JAVA_HOME` to the JDK installation, then run the wrapper:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -version
Test-Path "$env:JAVA_HOME/bin/javac.exe"
Test-Path "$env:JAVA_HOME/bin/jlink.exe"
.\gradlew.bat ci
```

The JDK used to run Gradle is separate from the Java source and target level,
which `app/build.gradle.kts` sets to Java 11.

On Linux or macOS, use `./gradlew ci`. GitHub Actions selects Temurin 21;
other builders need to provide their own JDK and Android SDK.

The project does not set daemon JVM criteria. If Gradle uses a different JDK
from the one you selected, check the IDE's Gradle JDK, `JAVA_HOME`, and local
Gradle properties. After changing the JDK, stop existing daemons with
`./gradlew --stop` and reopen the project if the IDE still uses its old setting.
