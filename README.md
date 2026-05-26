# Habit Tracker - Quick Start

This guide contains the basic configuration needed to run the full app locally:
- Backend: Spring Boot API
- Frontend: Expo React Native app

## 1. Prerequisites

Install these tools first:
- Java 21
- Maven 3.9+
- Node.js 18+
- npm 9+
- Docker Desktop (or Docker Engine + Docker Compose)

## 2. Backend setup

Go to the backend folder:

    cd habit-tracker-backend

Create the environment file:

PowerShell:

    Copy-Item .env.example .env

Bash:

    cp .env.example .env

Edit `.env` and set real values (minimum required variables):

    MONGO_INITDB_ROOT_USERNAME=change_me_mongo_user
    MONGO_INITDB_ROOT_PASSWORD=change_me_mongo_password
    MONGO_INITDB_DATABASE=habitdb

    POSTGRES_DB=habit_tracker
    POSTGRES_USER=change_me_pg_user
    POSTGRES_PASSWORD=change_me_pg_password

    ME_CONFIG_MONGODB_ADMINUSERNAME=change_me_mongo_user
    ME_CONFIG_MONGODB_ADMINPASSWORD=change_me_mongo_password
    ME_CONFIG_MONGODB_SERVER=mongo
    ME_CONFIG_MONGODB_AUTH_DATABASE=admin

    SPRING_DATA_MONGODB_URI=mongodb://change_me_mongo_user:change_me_mongo_password@localhost:27018/habitdb?authSource=admin
    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/habit_tracker
    SPRING_DATASOURCE_USERNAME=change_me_pg_user
    SPRING_DATASOURCE_PASSWORD=change_me_pg_password

    APP_JWT_SECRET=change_me_jwt_secret_at_least_32_chars
    APP_JWT_EXPIRATION_MS=86400000
    APP_SEED_ENABLED=true
    APP_OLLAMA_BASE_URL=http://localhost:11434

Start infrastructure services:

    docker compose up -d mongo postgres mongo-express ollama

Run the backend API:

    mvn spring-boot:run

Backend endpoints:
- API: http://localhost:8080
- Mongo Express: http://localhost:8081
- Ollama: http://localhost:11434

## 3. Frontend setup

Open a new terminal and go to frontend:

    cd habit-tracker-frontend

Install dependencies:

    npm install

Create frontend environment file:

PowerShell:

    Copy-Item .env.example .env

Bash:

    cp .env.example .env

Set API URL in `.env`:

    EXPO_PUBLIC_API_URL=http://localhost:8080

Notes:
- Physical device: use your computer LAN IP (example: http://192.168.1.10:8080)
- Android emulator: use http://10.0.2.2:8080

Start Expo:

    npm run start

Optional:

    npm run android
    npm run ios
    npm run web

## 4. Recommended startup order

1. Start Docker services in backend.
2. Start backend API with Maven.
3. Start frontend with Expo.

## 5. Stop services

From backend folder:

    docker compose down

To stop apps, use Ctrl+C in each terminal.
