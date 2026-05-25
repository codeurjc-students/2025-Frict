# Command (from root folder): docker build -t frict-app:local -f docker/Dockerfile .
# ==========================================
# STAGE 1: Frontend Build (Angular)
# ==========================================
FROM node:20-alpine AS frontend-build

WORKDIR /app-ui

# 1. Copy package.json and package-lock.json (if exist)
COPY /frontend/package*.json ./

# 2. Clean dependencies installation
RUN npm ci

# 3. Source code copy
COPY /frontend .

# 4. Compile in production mode (minification + tree-shaking + name-mangling)
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

# Copy JAR and entrypoint
COPY --from=backend-build /app-api/target/*.jar frict.jar
COPY docker/entrypoint.sh entrypoint.sh

# Permissions
RUN chown spring:spring frict.jar entrypoint.sh && chmod +x entrypoint.sh

USER spring:spring

EXPOSE 8443

ENTRYPOINT ["/app/entrypoint.sh"]
