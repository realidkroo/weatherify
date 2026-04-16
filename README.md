# weatherify
open source android weather app, but with style i guess
## THIS thing is not done yet please wait
# Known annoying bugs - PULL TO REFRESH ITS WORKINGG but its refreshing unlimitedly.... Infinitely 
To-do list | roadmap
- Making layout perfectly suitable for all device
- Making widget
- Moon system
- Multi-Api support
- Rain effect
- Cloud effect
- optimisatiion at cloud engine
- Multiple theme support
- Google material 3 expressiive support
- layout support

Try it on release if there any, or clone this project and compile itt
To compile this project, you will need:

### 1. Android Development Environment
- [Android Studio Iguana](https://developer.android.com/studio) or newer.
- Android SDK 34.
- Android NDK (installed via Android Studio SDK Manager).

### 2. Rust Toolchain
- [Rustup](https://rustup.rs/) (latest stable).
- Android targets for Rust:
  ```bash
  rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
  ```
- [cargo-ndk](https://github.com/bbqsrc/cargo-ndk):
  ```bash
  cargo install cargo-ndk
  ```

## how to compile

### 1 | Build the Rust Engine
go to the `rust_engine` directory and build the library for the desired Android architecture (usually `aarch64` for physical devices):

```bash
cd rust_engine
cargo ndk -t arm64-v8a -o ../app/src/main/jniLibs build --release
```

*note: This command compiles the Rust code into a `.so` library and places it in the correct android project directory.*

### 2 | build the Android App
You can now build the app using Android Studio or the command line:

```bash
# From the project root
./gradlew assembleDebug
```

### 3 | Enter api keys
copy `secrets.properties.example` to `secrets.properties` and enter your real keys. especoiallly openweather.

### 4 | run the app
- Connect an android device or start an emulator.
- Use Android Studio's **run** button or:
  ```bash
  ./gradlew installDebug
  ```

## project structure
- `app/`: Android application code (Kotlin/Compose).
- `rust_engine/`: Native rendering engine source code (Rust).
- `rust_engine/src/rendering/shaders/`: WGSL shaders for background effects.unused for now its so heavy
