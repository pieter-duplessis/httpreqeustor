# HttpRequestor

A simplified and lightweight HTTP client library.

---

## Usage
Simplified usage:

```java
Response<HashMap<String, Object>> response = new HttpRequestor<HashMap<String, Object>>() {}
        .setBaseUrl("https://api.example.com")
        .setPath("")
        .get();

Response<String> response2 = new HttpRequestor<String>() {}
        .setBaseUrl("https://api.example.com")
        .setPath("")
        .get();
```

Published to Maven Central under `page.pieters:httprequestor`.

## Requirements

- Java 11+
- Gradle 8.14.4

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("page.pieters:httprequestor:0.1.1")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'page.pieters:httprequestor:0.1.1'
}
```

### Maven

```xml
<dependency>
    <groupId>page.pieters</groupId>
    <artifactId>httprequestor</artifactId>
    <version>0.1.1</version>
</dependency>
```

---

## Building

Clone the repository and build with the Gradle wrapper:

```bash
./gradlew build
```

This compiles the source, runs all tests, and produces the JAR under `build/libs/`.

To build without running tests:

```bash
./gradlew assemble
```

---

## Testing

Run the full test suite:

```bash
./gradlew test
```

Test reports are written to `build/reports/tests/test/index.html`.

---

## Publishing to Maven Central

Add the following to `~/.gradle/gradle.properties`:

```properties
centralUsername=<portal-token-username>
centralPassword=<portal-token-password>
signingKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----
signingPassword=<gpg-key-passphrase>
```

Then run:

```bash
./gradlew publishToMavenSona
./gradlew publishAllPublicationsToCentralPortal
```

Log in to [central.sonatype.com](https://central.sonatype.com), go to **Deployments**, and click **Release**.

---

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE) for details.
