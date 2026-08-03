# Deployment

*Note: As zThread is a library, "deployment" refers to publishing artifacts to Maven Central, not deploying a service to a cloud provider like Kubernetes.*

## Release Process

Artifacts are published to the Sonatype OSSRH (Open Source Software Repository Hosting) and synced to Maven Central.

The project uses the Maven Release Profile and the Sonatype Central Publishing Plugin to automate this process.

### Build and Deploy Command

To release a new version to Maven Central, project maintainers run:

```bash
./mvnw clean deploy -Prelease --batch-mode --no-transfer-progress
```

### The `release` Profile

The `-Prelease` flag activates the `release` Maven profile defined in the root `pom.xml`. This profile attaches the following plugins to the build lifecycle:

1. **`maven-source-plugin`:** Generates `-sources.jar`.
2. **`maven-javadoc-plugin`:** Generates `-javadoc.jar`.
3. **`maven-gpg-plugin`:** Cryptographically signs all artifacts (JARs, POMs) with the maintainer's GPG key.
4. **`central-publishing-maven-plugin`:** Uploads the signed artifacts and automates the release process on Sonatype.

### CI/CD

Release processes are handled manually or via the `Release` GitHub Action (`.github/workflows/release.yml`), which requires the `MAVEN_USERNAME`, `MAVEN_PASSWORD`, and GPG secret keys to be configured as repository secrets.
