# CRRE Academic Data & Support Digitalization Platform

A college academic-data and support digitalization platform built to complement the existing college SIMS by digitizing academic and manually maintained information with modern support tools.

---

## V1 Scope: Authentication & Identity Foundation

V1 intentionally establishes **only** the authentication and identity foundation:
- Flow: **Login → Authentication → Basic Dashboard / Profile → Logout**
- Flexible identifier login (supports Username, Registration Number, Institutional Unique ID, or Email)
- Role-extensible user model (`STUDENT`, `FACULTY`, `COUNSELLOR`, `HOD`, `ADMIN`)
- Government and institutional identity attributes:
  - `registration_number` (VARCHAR(20), ~10 characters)
  - `unique_id` (VARCHAR(20), ~12 characters)
  - `aadhar_number` (BIGINT, 12 digits)
  - `apaar_number` (BIGINT, 12 digits APAAR / Academic Bank ID)
- Stateless JWT authentication via Spring Security (HMAC-SHA256)
- Database schema management via Flyway
- React + TypeScript + Vite frontend with protected routing and Tailwind CSS

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA, JJWT 0.12.6, Flyway, PostgreSQL 16
- **Frontend**: React 19, TypeScript 5.8, Vite 8, React Router 7, Tailwind CSS 4, Lucide React
- **Database**: PostgreSQL 16 (dockerized, dedicated container on port `5433`)

---

## Quick Start Guide

### 1. Start the Dedicated Database
The project includes an isolated PostgreSQL database configuration (`docker-compose.yml`) running on port `5433` to prevent conflicts with any local databases:

```bash
docker compose up -d
```

Database details:
- **Host**: `localhost`
- **Port**: `5433`
- **Database**: `crre_academic_dms`
- **User**: `crre_user`
- **Password**: `crre_password123`

### 2. Run the Backend (Spring Boot)

```bash
cd backend
./mvnw.cmd spring-boot:run
```
*(On Linux/macOS: `./mvnw spring-boot:run`)*

The backend server runs on `http://localhost:8080`. Flyway automatically runs database migrations (`V1` and `V2`) on startup.

### 3. Run the Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`. Requests to `/api` are automatically proxied to `http://localhost:8080`.

---

## Default Seed Credentials

All accounts can be authenticated using **either** their username, registration number, unique ID, or email with the following passwords:

| Role | Username | Registration No | Unique ID | Email | Password |
|---|---|---|---|---|---|
| **Student** | `22b91a0501` | `22B91A0501` | `CRRE20220501` | `student@crre.edu` | `Student@123` |
| **Faculty** | `faculty.cse` | *N/A* | `FAC2020CSE01` | `faculty@crre.edu` | `Faculty@123` |
| **Admin** | `admin` | *N/A* | `ADM20260001` | `admin@crre.edu` | `Admin@123` |

*(Quick-fill buttons are also available on the login page for testing).*

---

## API Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/login` | Public | Authenticates user and returns JWT token + summary |
| `GET` | `/api/v1/auth/me` | Protected (Bearer) | Retrieves the authenticated user profile |
| `POST` | `/api/v1/auth/logout` | Public | Acknowledges session termination |

### Sample Login Request
```json
POST /api/v1/auth/login
Content-Type: application/json

{
  "identifier": "22B91A0501",
  "password": "Student@123"
}
```

### Sample Login Response
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": "00000000-0000-0000-0000-000000000002",
      "username": "22b91a0501",
      "email": "student@crre.edu",
      "fullName": "Kowshik Garapati",
      "role": "STUDENT",
      "registrationNumber": "22B91A0501",
      "uniqueId": "CRRE20220501",
      "aadharNumber": 789012345678,
      "apaarNumber": 987654321012
    }
  },
  "timestamp": "2026-10-01T01:06:02.299Z"
}
```

---

## Frontend Routes

- `/login` — Login screen with flexible identifier input and quick-test presets
- `/` — Authenticated dashboard showing overview and session details (Protected)
- `/profile` — Full user identity profile with registration, unique ID, Aadhaar, and APAAR IDs (Protected)
- `*` — 404 Not Found page

---

## Running Automated Tests

To run the complete test suite (unit and integration tests verifying Flyway migration, Spring context, authentication, and security):

```bash
cd backend
./mvnw.cmd test
```

To run frontend lint and type-checking:

```bash
cd frontend
npm run lint
npm run build
```
