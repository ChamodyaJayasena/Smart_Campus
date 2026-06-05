# Stage 1: Build React Frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
# Inject relative paths for frontend API and OAuth redirection
ENV VITE_API_BASE_URL=/api
ENV VITE_OAUTH_USER_URL=/api/auth/google/user
ENV VITE_OAUTH_TECH_URL=/api/auth/google/technician
RUN npm run build

# Stage 2: Build Spring Boot Backend
FROM maven:3.9.6-eclipse-temurin-21-alpine AS backend-builder
WORKDIR /app/backend
# Copy maven configurations first to cache dependencies
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B
# Copy source code
COPY backend/src ./src
# Copy static frontend files built in Stage 1 to the static resources folder
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
# Build the Spring Boot Jar
RUN mvn clean package -DskipTests

# Stage 3: Runner
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/backend/target/smart-campus-backend-0.0.1-SNAPSHOT.jar app.jar
# Expose the default port
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
