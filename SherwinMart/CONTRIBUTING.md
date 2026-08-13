# Contributing to SherwinMart

## Local setup (clone to running instance)

1. **Prerequisites**: JDK 17, Maven 3.9+, Git.
2. **Clone**:
   ```
   git clone <your-repo-url>
   cd SherwinMart
   ```
3. **Configure**: copy `.env.example` to `src/main/resources/config.properties` and adjust
   values if needed. The defaults use an in-memory H2 database, so the app runs with zero
   configuration for local development.
4. **Build & test**:
   ```
   mvn clean verify
   ```
5. **Run locally** (embedded Tomcat via the Maven Cargo/Tomcat plugin, or deploy the WAR to
   a local Tomcat 9 install):
   ```
   mvn clean package
   cp target/sherwinmart.war $CATALINA_HOME/webapps/
   $CATALINA_HOME/bin/catalina.sh run
   ```
6. Visit `http://localhost:8080/sherwinmart/`.

## Branching & PRs

- `main` is always deployable.
- One branch per feature: `feature/<name>`.
- PR description states what changed, why, and how it was tested. Checklist: tests added,
  docs updated, migration included if the schema changed.
- Commit messages follow Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`.

## Definition of Done

See Section 19 of the project specification — Checkstyle/SpotBugs clean, tests passing,
self-reviewed, verified against the deployed URL, README updated if behavior changed.
