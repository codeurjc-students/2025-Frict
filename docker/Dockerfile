# ==========================================
# STAGE 1: Frontend Build (Angular)
# ==========================================
FROM node:20-alpine AS frontend-build

WORKDIR /app-ui

# 1. Copy package.json and package-lock.json (if exist)
COPY frontend/package*.json ./

# 2. Clean dependencies installation
RUN npm ci

# 3. Source code copy
COPY /frontend .

# 4. Compile
RUN npm run build -- --configuration production


# ==========================================
# STAGE 2: Backend Build (Spring Boot)
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS backend-build

WORKDIR /app-api

# 1. Maven dependency cache
COPY /backend/pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copy Java source code
COPY /backend/src ./src

# 3. Frontend incrustation
COPY --from=frontend-build /app-ui/dist/frontend/browser/ ./src/main/resources/static/

# 4. JAR packaging
RUN mvn clean package -DskipTests


# ==========================================
# STAGE 3: Final Runtime Image (Production)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Container security (use low-privileged user)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR and certificate
COPY --from=backend-build /app-api/target/*.jar frict.jar
COPY --from=backend-build /app-api/src/main/resources/keystore.p12 /app/keystore.p12

# Permissions
RUN chown spring:spring frict.jar /app/keystore.p12

USER spring:spring

# Expose secure internal port
EXPOSE 8443

# Entrypoint with active SSL
ENTRYPOINT ["java", \
            "-Dserver.port=8443", \
            "-Dserver.ssl.enabled=true", \
            "-Dserver.ssl.key-store=/app/keystore.p12", \
            "-Dserver.ssl.key-store-type=PKCS12", \
            "-jar", "frict.jar"]