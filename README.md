# ZorluKurt Trading: Unified E-Commerce Platform

This folder contains the merged e-commerce project with Backend, Frontend, AI Analytics, and Database components.

## Prerequisites
- Docker and Docker Compose installed
- Google Gemini API Key (for AI features)

## Setup
1. Clone the project.
2. Navigate to this directory.
3. Create a `.env` file from the example:
   ```bash
   cp .env.example .env
   ```
4. Open `.env` and enter your `OPENAI_API_KEY`.
5. Run the project:
   ```bash
   docker-compose up --build
   ```

## Services (Private)
The following services are **not exposed** to the public and are only accessible through the gateway or within the Docker network:
- **Backend API**: http://backend:8080 (Internal)
- **AI Analytics API**: http://ai:8001 (Internal)
- **Database**: postgres:5432 (Internal)

## Public Access
- **Unified Platform**: http://localhost (Port 80)
  - The Gateway (Nginx) handles all traffic:
    - `/` routes to the Frontend.
    - `/api/` routes to the Backend.
    - `/ai-api/` routes to the AI Analytics service.

## Database Initialization
The database is automatically initialized using the `db-init/init.sql` file (which is a copy of the final database dump).

## Environment Variables
You can customize the following in `.env`:
- `OPENAI_API_KEY`: Your OpenAI API key.
- `JWT_SECRET`: Secret used for token signing (must match between Backend and AI).
- `DB_PASSWORD`: PostgreSQL password.
- `DB_NAME`: Database name.
- `DB_USER`: Database user.
