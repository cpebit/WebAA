# WebAA

Open-source release of WebAA (formerly TeslAA).

Web Implementation for Android Auto tuned for Tesla browser.

> **This project is no longer maintained.** The code is published as-is, for reference and for
> anyone who wants to build or continue it. No support is provided, issues and pull requests
> may not be answered, and there will be no further releases from the original authors.

## Android Auto certificate (required, not included)

To talk to Android Auto on the phone, WebAA acts as a head unit, and the TLS handshake needs a
head unit certificate and private key. **No certificate is shipped in this repository.** You have
to provide your own, otherwise the connection stops with a "No Android Auto certificate found" message.

Where to get one:

- Use your own head unit certificate, if you have one.
- Look at other open-source Android Auto projects that publish one, for example
  [aasdk](https://github.com/opencardev/aasdk) / [OpenAuto](https://github.com/opencardev/openauto)
  (`cert/headunit.crt` and `cert/headunit.key`, also embedded in `src/Messenger/Cryptor.cpp`),
  or other head unit projects.

Make sure you are allowed to use whatever certificate you pick.

### Installing the certificate

WebAA expects a PKCS#12 file named `aa_cert.p12`, protected with the password `webaa`.
Convert a PEM certificate and key with OpenSSL:

```
openssl pkcs12 -export -legacy -in headunit.crt -inkey headunit.key -out aa_cert.p12 -passout pass:webaa
```

(`-legacy` keeps the file readable on older Android versions; drop it if your OpenSSL is 1.x.)

Copy the file to the app's folder on the phone running WebAA:

```
adb push aa_cert.p12 /sdcard/Android/data/it.cpeb.webaa/files/aa_cert.p12
```

Start the app once before copying so the folder exists. If you change the `applicationId`, the
folder name changes with it. The file name and password are defined in
`app/src/main/java/it/cpeb/teslaa/sslhelpers/sslEngineBuilder.java`.

## Building it yourself

### What you need

- **JDK 17** (the JDK bundled with recent Android Studio versions works)
- **Android SDK** with platform **API 36** and its build tools. The easiest way is to install
  [Android Studio](https://developer.android.com/studio), which installs both.
- An internet connection for the first build. Gradle 8.11.1 (via the wrapper), the Android Gradle
  Plugin, `protoc` and all libraries are downloaded automatically from Google, Maven Central and JitPack.

No NDK is needed.

### Build with Android Studio

1. *File → Open* and select the project folder.
2. Let the Gradle sync finish.
3. Pick the `germanstoreDebug` build variant and press *Run*, or use *Build → Build APK(s)*.

### Build from the command line

```
# Linux / macOS
./gradlew assembleGermanstoreDebug

# Windows
gradlew.bat assembleGermanstoreDebug
```

If Gradle can't find the SDK, create `local.properties` in the project root containing
`sdk.dir=/path/to/Android/Sdk`.

The APK ends up in `app/build/outputs/apk/germanstore/debug/` and can be installed with
`adb install -r <apk>`. Then install your certificate as described above.

The installed app is `it.cpeb.webaa`, minimum Android 8.0 (API 26).

### Release builds

`assembleGermanstoreRelease` produces an **unsigned** APK. To sign it, sign the APK yourself with
`apksigner`, or create your own keystore and re-enable the commented-out `signingConfigs` block in
`app/build.gradle.kts`, which reads `keystore.properties` (gitignored) from the project root:

```
storeFile=/path/to/your.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

Never commit your keystore or `keystore.properties`.
