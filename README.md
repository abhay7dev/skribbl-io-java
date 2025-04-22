# [skribbl.io](https://skribbl.io) Clone in Java!

https://skribbl.io clone in java (with a client/server).

Do NOT use the VSCode run button. Always use the following commands or use the command palette and run the gradle task to run the project.

### Gradle Version

* **This project requires JDK 24 to build and JRE 24 to run.** (Required due to crypto libraries)
* As of the creation of this project, Gradle 8.13 is the default, and 8.14 is still in the release candidate phase. Run the following command to get Gradle 8.14 installed.
* `./gradlew wrapper --gradle-version=8.14-rc-2 && ./gradlew wrapper`
* Make sure your default JDK is Java 24
    * On archlinux, if you have multiple JDKs, run: `sudo archlinux-java set java-24-openjdk`

### How to compile and run [WIP]

```bash
./gradlew build         # Build the Project
./gradlew :client:run   # Run client
./gradlew :server:run   # Run server
./gradlew :server:run --args="--headless" # Run server without display. Faster during development
./gradlew clean         # Clean the Build
```

Remove the `./` if on Windows Terminal. Powershell should will work fine with `./`.