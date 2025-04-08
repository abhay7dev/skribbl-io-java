# [skribbl.io](https://skribbl.io) Clone in Java!

https://skribbl.io clone in java (with a client/server).

Do NOT use the VSCode run button. Always use the following commands or use the command palette and run the gradle task to run the project.

### How to compile and run [WIP]

```bash
./gradlew build         # Build the Project
./gradlew :client:run   # Run client
./gradlew :server:run   # Run server
./gradlew :server:run --args="--headless" # Run server without display. Faster during development
./gradlew clean         # Clean the Build
```

Remove the `./` if on Windows Terminal. Powershell should will work fine with `./`.