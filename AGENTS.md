# Alfred.2017 Contributor Guide

This document explains how to work in this repository. Its scope covers the
entire repo unless a subdirectory introduces a more specific `AGENTS.md`.

## Repository layout
- `app/` – Android application module for Alfred 2017. Kotlin is preferred for
  new Android code, but the existing Java files are still in service.
- `smartfoo/android/smartfoo-android-lib-core/` – the bundled SmartFoo Android
  core library. It is written almost entirely in Java and uses an Allman brace
  style. Treat it like a library module that other projects may consume.
- `marketing/` – marketing graphics that accompany the Play Store listing.
- `JOURNAL.md`, `README.md` – project documentation and history. Keep them in
  sync when user-visible changes ship.

## Toolchain and dependencies
- The project targets Android SDK 35 and requires a **minimum SDK of 34**.
  Emulator/device testing therefore needs Android 14 or newer.
- Java 21 and Kotlin `kotlinOptions.jvmTarget = "21"` are configured. When
  working locally use a matching JDK (Android Studio Giraffe+ or command line
  JDK 21).
- The SmartFoo module has Gradle tasks of its own but shares the root wrapper.

## Build & test commands
Run Gradle tasks from the repository root with the included wrapper:
- `./gradlew assembleDebug` – default smoke check that compiles the app.
- `./gradlew :app:testDebugUnitTest` – JVM unit tests for the app module.
- `./gradlew :smartfoo-android-lib-core:testDebugUnitTest` – JVM tests for the
  SmartFoo core library when present.
- `./gradlew lint` – Android Lint across all modules.
- `./gradlew connectedAndroidTest` – instrumentation tests. Requires an
  attached Android 14+ device or emulator; skip in CI when unavailable.

Always mention which commands you executed in your change summary.

## Kotlin style (Android app module)
- Follow the official Kotlin style (`gradle.properties` enforces
  `kotlin.code.style=official`). Use 4-space indentation, trailing commas where
  they improve diffs, and keep lines readable (< 120 characters when practical).
- Prefer `val` over `var`. Use expression bodies for very small functions but
  favor clarity over terseness.
- Import order: Android/Java SDK, third-party, SmartFoo, then local
  `com.swooby.alfred` types. Keep imports explicit; do not use `*` imports.
- Logging: use `FooLog` for consistency with the rest of the codebase. Define a
  `TAG` in a companion object via `FooLog.TAG(...)` when a new class is added.
- When interacting with Android platform APIs prefer SmartFoo helpers (for
  example `FooRun`, `FooString`, `FooPlatformUtils`) to match existing
  conventions and to centralize error handling.
- Use ViewBinding (`ActivityMainBinding`, etc.) instead of
  `findViewById` unless there is a compelling reason otherwise.

## Java style (app module & SmartFoo library)
- Maintain the Allman brace style seen throughout the project (opening braces on
  their own line) with 4 spaces per indent level.
- Keep methods short and cohesive. Extract helpers instead of creating deeply
  nested logic.
- Null-safety: prefer `FooRun.throwIllegalArgumentExceptionIfNull*` helpers
  rather than manual `if` checks so behavior stays consistent.
- Logging must go through `FooLog` and should include the class TAG.
- For new SmartFoo utilities ensure they remain Android-agnostic unless they
  explicitly live in an Android-specific package.

## Resources and UI assets
- Name resources using Android standards (snake_case, type prefixes where it
  makes sense, e.g. `activity_main`, `ic_notification`).
- Place all end-user strings in `res/values/strings.xml`. Only test/debug text
  should stay inline in code.
- Enable dark theme support by adding night-qualified resources when changing
  colors or drawables.
- When adding layouts prefer ConstraintLayout unless an existing pattern in the
  file suggests otherwise. Ensure layout IDs line up with generated view-binding
  classes.

## Notification parser architecture
- Parsers live in `app/src/main/java/com/swooby/alfred/notification/parsers/` and
  extend `AbstractNotificationParser`.
- To add a new parser:
  1. Implement the parser class, override the relevant callbacks, and use
     `NotificationParserUtils` helpers to inspect `StatusBarNotification`
     content.
  2. Register the parser inside `NotificationParserManager` so it can be
     discovered. Maintain the existing map ordering.
  3. Provide friendly spoken names via `FooTextToSpeechBuilder` so speech output
     is consistent with other parsers.
  4. Add unit tests if logic can be exercised off-device. Otherwise document the
     manual testing steps in the PR description.
- Keep parsers tolerant of unexpected notification payloads—log with `FooLog.v`
  and bail out gracefully instead of throwing.

## Working with SmartFoo
- SmartFoo is treated as a reusable library. Avoid importing Android app code
  from it; flow should go the other direction.
- Any new SmartFoo class should ship with JavaDoc comments explaining platform
  nuances. Keep APIs general-purpose so that other projects can reuse them.
- If you need Android resources in SmartFoo, add them to the library’s `res/`
  directory and reference them through the library R class.

## Documentation & marketing assets
- Update `README.md` or `JOURNAL.md` when you add noteworthy features or user
  flows. The README is the high-level overview; the journal tracks milestones.
- Marketing imagery lives in `marketing/`. Keep the existing filenames intact
  when replacing assets so store listings stay linked.

## Pull requests and reviews
- Keep commits focused and include clear messages.
- PR descriptions should summarize user-visible changes, testing performed, and
  any follow-up work.
- Do not commit generated files, IDE metadata, or local signing artifacts.
- If you touch multiple modules document how each was exercised.

When in doubt, follow existing patterns in the surrounding files. Consistency is
more important than introducing a brand-new style.
