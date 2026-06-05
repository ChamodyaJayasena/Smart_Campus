#!/usr/bin/env bash
# Exit immediately if a command exits with a non-zero status
set -e

echo "=== START UNIFIED BUILD ==="

# 1. Build the Frontend
echo ">>> Building React frontend..."
cd frontend
npm install
VITE_API_BASE_URL=/api VITE_OAUTH_USER_URL=/api/auth/google/user VITE_OAUTH_TECH_URL=/api/auth/google/technician npm run build
cd ..

# 2. Copy static files into Spring Boot resources
echo ">>> Copying frontend assets to Spring Boot..."
mkdir -p backend/src/main/resources/static
cp -r frontend/dist/* backend/src/main/resources/static/

# 3. Build Spring Boot Jar
echo ">>> Building Spring Boot backend..."
cd backend
mvn clean package -DskipTests
cd ..

echo "=== UNIFIED BUILD COMPLETED ==="
