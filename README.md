# Kotlin Composable Architecture

[![Kotlin](https://img.shields.io/badge/kotlin-1.4.21-orange)](https://kotlinlang.org/docs/tutorials/getting-started.html)
[![@wearemakery](https://img.shields.io/badge/contact-@wearemakery-blue)](https://twitter.com/wearemakery)

The Kotlin Composable Architecture is a companion library for the amazing [Swift Composable Architecture](https://github.com/pointfreeco/swift-composable-architecture) created by [Point-Free](https://www.pointfree.co/). This implementation tries to mimic the original version's patterns and techniques, but because Swift and Kotlin have some fundamental differences, there are a few alternative design decisions. Eventually, we are aiming to provide the same ergonomics as the Swift implementation and full feature parity.

⚠️ **Please note that this repository is a work in progress; there is no stable release available.**

## Getting started

Until there is no stable release available, the easiest way to integrate the library into your project is to use [Gradle's `includeBuild()` feature](https://publicobject.com/2021/03/11/includebuild/).

```kotlin
// in build.gradle.kts
implementation("composable-architecture:composable-architecture:0.1.0")
```

```kotlin
// in settings.gradle.kts
includeBuild("<PATH TO kotlin-composable-architecture>") {
    dependencySubstitution {
        substitute(module("composable-architecture:composable-architecture"))
            .with(project(":composable-architecture"))
    }
}
```

## What is the Composable Architecture?

This library provides a few core tools that can be used to build applications of varying purpose and complexity. It provides compelling stories that you can follow to solve many problems you encounter day-to-day when building applications, such as:

* **State management**
  <br> How to manage the state of your application using simple value types, and share state across many screens so that mutations in one screen can be immediately observed in another screen.

* **Composition**
  <br> How to break down large features into smaller components that can be extracted to their own, isolated modules and be easily glued back together to form the feature.

* **Side effects**
  <br> How to let certain parts of the application talk to the outside world in the most testable and understandable way possible.

* **Testing**
  <br> How to not only test a feature built in the architecture, but also write integration tests for features that have been composed of many parts, and write end-to-end tests to understand how side effects influence your application. This allows you to make strong guarantees that your business logic is running in the way you expect.

* **Ergonomics**
  <br> How to accomplish all of the above in a simple API with as few concepts and moving parts as possible.

## Alternative approaches compared to Swift

### Lack of value types

For now, the JVM doesn't have the concept of value types (this might change in the future with [Project Valhalla](https://openjdk.java.net/projects/valhalla/)). Thus, we cannot provide the reducer a mutable state safely, so it is required to return new copies of the state in case of any mutation. Kotlin's `data class` feature comes handy, as a `.copy()` methods get generated with named arguments for all properties.

### Less powerful enums

In Kotlin, enums cannot function as algebraic data types. We get to define a single type wrapped inside an enum, but each case cannot have an individual associated type. Instead, we can model actions with `sealed class` hierarchies.

### Lack of KeyPaths and CasePaths

The Swift compiler has a powerful feature: it generates `KeyPath`s for each struct in our application. Point-Free has implemented a companion library called [swift-case-paths](https://github.com/pointfreeco/swift-case-paths), which provides the same ergonomics for enums. The Swift Composable Architecture uses these two tools to abstract over getters and setters for state and action. In Kotlin, we don't have similar tools, so we rely on code generation through the [Arrow Meta](https://github.com/arrow-kt/arrow) library. Arrow has a module called Optics, which can be utilized to define `Lens`es and `Prism`s to substitute `KeyPath`s and `CasePath`s.

### Combine vs Coroutines

The Swift Composable Architecture is powered by Combine, which comes bundled with iOS SDK 13. The Kotlin Composable Architecture is relying on the Kotlinx Coroutines library. Stores are powered by `MutableStateFlow`, and effects are wrappers for coroutine `Flow`s.

## More info

For more information please visit the [Swift Composable Architecture repository](https://github.com/pointfreeco/swift-composable-architecture).

## License

This library is released under the MIT license. See [LICENSE](LICENSE) for details.
