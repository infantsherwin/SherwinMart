# ─────────────────────────────────────────────
# Stage 1: Build the WAR with Maven
# ─────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom first so Docker caches the dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build (skip tests for faster Docker build)
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────
# Stage 2: Run on Tomcat 9 (Servlet 4.0)
# ─────────────────────────────────────────────
FROM tomcat:9.0-jdk17-temurin

# Remove the default Tomcat webapps so ROOT isn't cluttered
RUN rm -rf /usr/local/tomcat/webapps/*

# Deploy our WAR as ROOT so URLs start at / instead of /sherwinmart/
COPY --from=builder /app/target/sherwinmart.war /usr/local/tomcat/webapps/ROOT.war

# Expose the default Tomcat port
EXPOSE 8080

CMD ["catalina.sh", "run"]