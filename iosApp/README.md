# iOS App

Requires macOS with Xcode 15+ to build. The Kotlin sources in
`app/src/iosMain/` compile into a native framework that Swift calls
through `MainViewControllerKt.MainViewController()`.

## Build steps (on a Mac)

1. Build the XCFramework from the KMP module:
   ```
   ./gradlew :app:assembleXCFramework
   ```
   The output lands in `app/build/XCFrameworks/release/shared.xcframework`.

2. Open `iosApp/iosApp.xcodeproj` in Xcode (generate with `xcodegen` or
   create manually — see the bundle identifier `com.belinze.lifeos`,
   minimum deployment target iOS 16.0, and link the XCFramework produced
   above).

3. Select a simulator or device and press Run.

## Source files

| File | Purpose |
|------|---------|
| `iosApp/ContentView.swift` | SwiftUI wrapper that hosts the KMP Compose view |
| `iosApp/iOSApp.swift` | `@main` entry point |
| `app/src/iosMain/…/MainViewController.kt` | KMP side — initialises Koin and returns a `UIViewController` |
| `app/src/iosMain/…/DatabaseDriverFactory.kt` | SQLDelight `NativeSqliteDriver` actual |
| `app/src/iosMain/…/IosModule.kt` | Koin module for iOS |
