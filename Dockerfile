# Multi-stage build: Node 22 (SPA) + Maven (JDK 21) → WildFly 34 (JDK 21)

# ---- Stage 1: SPA frontend build ----
FROM node:22-alpine AS ui-build
WORKDIR /ui

COPY periscope-ui/package*.json ./
RUN npm ci

COPY periscope-ui/ ./
RUN npm run build

# ---- Stage 2: Maven backend + embedded SPA ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY periscope-ejb/pom.xml periscope-ejb/
COPY periscope-web/pom.xml periscope-web/

# Resolve dependencies before copying sources so source edits do not bust the cache
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY periscope-ejb periscope-ejb
COPY periscope-web periscope-web
COPY --from=ui-build /ui/dist /app/periscope-web/src/main/webapp/app

RUN mvn -B clean package -DskipTests

# ---- Stage 3: runtime ----
FROM quay.io/wildfly/wildfly:34.0.1.Final-jdk21

ENV MONGODB_URI=mongodb://mongodb:27017 \
    MONGODB_DATABASE=Periscope \
    PERISCOPE_DIR=/opt/periscope

USER root
RUN mkdir -p /opt/periscope \
    && chown -R jboss:jboss /opt/periscope
# Enable MicroProfile OpenAPI via jboss-cli embed-server (no python3).
# Idempotent try/catch: add extension/subsystem only when missing.
# Real :add failures still fail the build (CLI non-zero).
USER jboss
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --commands='\
embed-server --server-config=standalone.xml,\
try,/extension=org.wildfly.extension.microprofile.openapi-smallrye:read-resource,\
catch,/extension=org.wildfly.extension.microprofile.openapi-smallrye:add,\
finally,end-try,\
try,/subsystem=microprofile-openapi-smallrye:read-resource,\
catch,/subsystem=microprofile-openapi-smallrye:add,\
finally,end-try,\
stop-embedded-server'

USER root
COPY --from=build --chown=jboss:jboss \
    /app/periscope-web/target/periscope.war \
    /opt/jboss/wildfly/standalone/deployments/periscope.war
USER jboss

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD curl -sf http://localhost:8080/periscope/rest/health || exit 1

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
