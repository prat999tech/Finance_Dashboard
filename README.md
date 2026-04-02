# Finance Dashboard API

A role-based finance dashboard backend built with **Java 17 + Spring Boot 3.2**.

> **Live API Docs (when running):** `http://localhost:8080/swagger-ui.html`
> **OpenAPI JSON spec:** `http://localhost:8080/v3/api-docs`

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Getting Started](#getting-started)
4. [Default Users (Dev)](#default-users-dev)
5. [OpenAPI / Swagger Documentation](#openapi--swagger-documentation)
6. [API Reference](#api-reference)
    - [Authentication](#1-authentication)
    - [Transactions](#2-transactions)
    - [Dashboard](#3-dashboard)
    - [User Management](#4-user-management)
7. [Role & Access Control](#role--access-control)
8. [Error Responses](#error-responses)
9. [Architecture Decisions](#architecture-decisions)
10. [Assumptions](#assumptions)
11. [Tradeoffs](#tradeoffs)

---

## Tech Stack

| Layer         | Choice                              |
|---------------|-------------------------------------|
| Language      | Java 17                             |
| Framework     | Spring Boot 3.2                     |
| Security      | Spring Security + JWT (jjwt 0.12)   |
| ORM           | Spring Data JPA + Hibernate         |
| Database      | PostgreSQL (prod) / H2 (dev)        |
| Validation    | Jakarta Bean Validation             |
| API Docs      | Springdoc OpenAPI (Swagger UI)      |
| Build         | Maven                               |
| Testing       | JUnit 5 + Mockito                   |

---

## Project Structure

```
src/main/java/com/finance/dashboard/
├── config/
│   ├── SecurityConfig.java          # Filter chain, RBAC rules, CORS
│   └── OpenApiConfig.java           # Swagger/OpenAPI setup
├── controller/
│   ├── AuthController.java          # POST /api/auth/register, /login
│   ├── UserController.java          # /api/users  (ADMIN only)
│   ├── TransactionController.java   # /api/transactions
│   └── DashboardController.java     # /api/dashboard
├── dto/
│   ├── request/                     # Input DTOs with validation
│   └── response/                    # Output DTOs (no entity leakage)
├── entity/
│   ├── User.java
│   └── Transaction.java             # Has soft-delete column (deleted_at)
├── enums/
│   ├── Role.java                    # VIEWER | ANALYST | ADMIN
│   ├── TransactionType.java         # INCOME | EXPENSE
│   └── UserStatus.java              # ACTIVE | INACTIVE
├── exception/
│   ├── GlobalExceptionHandler.java  # @ControllerAdvice, consistent JSON errors
│   ├── ResourceNotFoundException.java
│   └── DuplicateResourceException.java
├── repository/
│   ├── UserRepository.java
│   └── TransactionRepository.java   # Custom JPQL for filters + analytics
├── security/
│   ├── JwtUtil.java                 # Token generation & validation
│   ├── JwtAuthFilter.java           # OncePerRequestFilter
│   └── CustomUserDetailsService.java
└── service/
    ├── AuthService.java
    ├── UserService.java
    ├── TransactionService.java
    └── DashboardService.java        # All aggregation logic lives here
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (for production profile)

### Run in development mode (H2 in-memory, seed data auto-loaded)

```bash
# Clone the project
git clone <repo-url>
cd finance-dashboard

# Run with dev profile (H2 database, no PostgreSQL needed)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The server starts on **http://localhost:8080**

Swagger UI: **http://localhost:8080/swagger-ui.html**

H2 Console: **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:financedb`
- Username: `sa` / Password: _(empty)_

---

### Run with PostgreSQL (production profile)

1. Create the database:
```sql
CREATE DATABASE finance_dashboard;
```

2. Set environment variables (or update `application.yml`):
```bash
export DB_URL=jdbc:postgresql://localhost:5432/finance_dashboard
export DB_USERNAME=postgres
export DB_PASSWORD=yourpassword
export JWT_SECRET=YourSuperSecretKeyThatIsAtLeast256BitsLong!
```

3. Run without dev profile:
```bash
mvn spring-boot:run
```

---

### Run tests

```bash
mvn test
```

---

## Default Users (Dev)

All dev users have the password: **`password123`**

| Email                    | Role    | Status   |
|--------------------------|---------|----------|
| admin@finance.com        | ADMIN   | ACTIVE   |
| analyst@finance.com      | ANALYST | ACTIVE   |
| viewer@finance.com       | VIEWER  | ACTIVE   |
| inactive@finance.com     | VIEWER  | INACTIVE |

**Quick start flow:**
1. `POST /api/auth/login` with `admin@finance.com` / `password123`
2. Copy the `token` from the response
3. Click **Authorize** in Swagger UI → paste `<token>` (without "Bearer ")
4. All protected endpoints are now accessible

---

## OpenAPI / Swagger Documentation

The application ships with an interactive Swagger UI and a machine-readable OpenAPI 3.0 spec.

| Resource | URL |
|----------|-----|
| Swagger UI (interactive) | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON spec | `http://localhost:8080/v3/api-docs` |
| OpenAPI YAML spec | `http://localhost:8080/v3/api-docs.yaml` |

### How to authorize in Swagger UI

1. Start the app and open `http://localhost:8080/swagger-ui.html`
2. Expand **Authentication → POST /api/auth/login** and execute it with your credentials
3. Copy the `token` string from the response body
4. Click the **Authorize 🔒** button at the top right of the page
5. In the dialog, paste your token (just the token — Swagger prepends `Bearer ` automatically)
6. Click **Authorize** then **Close**
7. All endpoints now send your JWT automatically

### Import into Postman

1. Open Postman → **Import**
2. Select **Link** tab
3. Paste `http://localhost:8080/v3/api-docs`
4. Click **Continue → Import** — a full collection is generated automatically

---

## API Reference

The full spec below mirrors what Swagger UI renders. Each endpoint shows the method, path, required role, request schema, response schema, and possible error codes.

---

### 1. Authentication

> **Base path:** `/api/auth`
> **Security:** None — all endpoints are public

---

#### `POST /api/auth/register` — Register a new user

**Request body** (`application/json`)

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | ✅ | not blank | Full name |
| `email` | string | ✅ | valid email format | Unique email address |
| `password` | string | ✅ | min 6 chars | Plain-text password (stored BCrypt hashed) |
| `role` | enum | ✅ | `VIEWER` \| `ANALYST` \| `ADMIN` | Role to assign |

**Example request**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "VIEWER"
}
```

**Response `201 Created`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIn0...",
  "type": "Bearer",
  "id": 5,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "VIEWER"
}
```

**Error responses**

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Missing or invalid fields |
| `409 Conflict` | Email already registered |

---

#### `POST /api/auth/login` — Login and receive a JWT

**Request body** (`application/json`)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | ✅ | Registered email address |
| `password` | string | ✅ | Account password |

**Example request**
```json
{
  "email": "admin@finance.com",
  "password": "password123"
}
```

**Response `200 OK`**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBmaW5hbmNlLmNvbSJ9...",
  "type": "Bearer",
  "id": 1,
  "name": "Alice Admin",
  "email": "admin@finance.com",
  "role": "ADMIN"
}
```

Use the token in all subsequent requests:
```
Authorization: Bearer <token>
```

**Error responses**

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Missing fields |
| `401 Unauthorized` | Wrong email or password |
| `403 Forbidden` | Account is INACTIVE |

---

### 2. Transactions

> **Base path:** `/api/transactions`
> **Security:** Bearer JWT required for all endpoints

---

#### `POST /api/transactions` — Create a transaction

> 🔒 **Required role:** `ADMIN`

**Request body** (`application/json`)

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `amount` | number | ✅ | > 0, max 2 decimal places | Transaction amount |
| `type` | enum | ✅ | `INCOME` \| `EXPENSE` | Direction of money flow |
| `category` | string | ✅ | max 100 chars | Category label (e.g. Salary, Rent) |
| `date` | string | ✅ | ISO 8601 `YYYY-MM-DD` | Date the transaction occurred |
| `notes` | string | ❌ | max 500 chars | Optional description or memo |

**Example request**
```json
{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-06-05",
  "notes": "June salary payment"
}
```

**Response `201 Created`**
```json
{
  "id": 36,
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "date": "2025-06-05",
  "notes": "June salary payment",
  "userId": 1,
  "userName": "Alice Admin",
  "createdAt": "2025-06-05T09:15:00"
}
```

**Error responses**

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Validation failure (see `fieldErrors`) |
| `401 Unauthorized` | Missing or expired JWT |
| `403 Forbidden` | Role is not ADMIN |

---

#### `GET /api/transactions` — List transactions with filters and pagination

> 🔒 **Required role:** `VIEWER`, `ANALYST`, or `ADMIN`

**Query parameters** (all optional)

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `type` | enum | — | Filter by `INCOME` or `EXPENSE` |
| `category` | string | — | Filter by category (case-insensitive) |
| `from` | date `YYYY-MM-DD` | — | Start of date range (inclusive) |
| `to` | date `YYYY-MM-DD` | — | End of date range (inclusive) |
| `page` | integer | `0` | Page index (zero-based) |
| `size` | integer | `20` | Records per page |

**Example requests**
```
GET /api/transactions
GET /api/transactions?type=EXPENSE
GET /api/transactions?category=Food&from=2025-01-01&to=2025-06-30
GET /api/transactions?type=INCOME&page=1&size=10
```

**Response `200 OK`** — paginated result
```json
{
  "content": [
    {
      "id": 1,
      "amount": 5000.00,
      "type": "INCOME",
      "category": "Salary",
      "date": "2025-06-05",
      "notes": "June salary payment",
      "userId": 1,
      "userName": "Alice Admin",
      "createdAt": "2025-06-05T09:15:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "direction": "DESC", "property": "date" }
  },
  "totalElements": 35,
  "totalPages": 2,
  "last": false,
  "first": true
}
```

**Error responses**

| Status | Reason |
|--------|--------|
| `401 Unauthorized` | Missing or expired JWT |

---

#### `GET /api/transactions/{id}` — Get a single transaction

> 🔒 **Required role:** `VIEWER`, `ANALYST`, or `ADMIN`

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Transaction ID |

**Example request**
```
GET /api/transactions/5
```

**Response `200 OK`**
```json
{
  "id": 5,
  "amount": 1200.00,
  "type": "EXPENSE",
  "category": "Rent",
  "date": "2025-01-01",
  "notes": "January rent",
  "userId": 1,
  "userName": "Alice Admin",
  "createdAt": "2025-01-01T08:00:00"
}
```

**Error responses**

| Status | Reason |
|--------|--------|
| `401 Unauthorized` | Missing or expired JWT |
| `404 Not Found` | No transaction with given ID (or it is soft-deleted) |

---

#### `PUT /api/transactions/{id}` — Update a transaction

> 🔒 **Required role:** `ADMIN`

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Transaction ID to update |

**Request body** — same schema as `POST /api/transactions`

**Example request**
```json
{
  "amount": 300.00,
  "type": "EXPENSE",
  "category": "Dining",
  "date": "2025-06-10",
  "notes": "Team lunch"
}
```

**Response `200 OK`** — updated transaction object (same shape as GET response)

**Error responses**

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Validation failure |
| `401 Unauthorized` | Missing or expired JWT |
| `403 Forbidden` | Role is not ADMIN |
| `404 Not Found` | Transaction does not exist |

---

#### `DELETE /api/transactions/{id}` — Soft-delete a transaction

> 🔒 **Required role:** `ADMIN`

The record is **not physically removed**. The `deleted_at` timestamp is set to the current time. The transaction will no longer appear in any listing, filter, or analytics query.

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | Transaction ID to delete |

**Example request**
```
DELETE /api/transactions/5
```

**Response `204 No Content`** — empty body

**Error responses**

| Status | Reason |
|--------|--------|
| `401 Unauthorized` | Missing or expired JWT |
| `403 Forbidden` | Role is not ADMIN |
| `404 Not Found` | Transaction does not exist |

---

### 3. Dashboard

> **Base path:** `/api/dashboard`
> **Security:** Bearer JWT required
> 🔒 **Required role:** `ANALYST` or `ADMIN` for all endpoints

---

#### `GET /api/dashboard/summary` — Overall financial summary

Returns aggregated totals, net balance, and category-level breakdowns for all non-deleted transactions.

**Example request**
```
GET /api/dashboard/summary
Authorization: Bearer <token>
```

**Response `200 OK`**
```json
{
  "totalIncome": 31900.00,
  "totalExpenses": 10804.00,
  "netBalance": 21096.00,
  "totalTransactions": 35,
  "incomeByCategory": [
    { "category": "Salary",      "total": 30000.00 },
    { "category": "Freelance",   "total": 2000.00  },
    { "category": "Investments", "total": 750.00   },
    { "category": "Other",       "total": 150.00   }
  ],
  "expenseByCategory": [
    { "category": "Rent",          "total": 7200.00 },
    { "category": "Food",          "total": 1040.00 },
    { "category": "Utilities",     "total": 314.00  },
    { "category": "Transport",     "total": 405.00  },
    { "category": "Healthcare",    "total": 575.00  },
    { "category": "Shopping",      "total": 770.00  },
    { "category": "Entertainment", "total": 225.00  }
  ]
}
```

**Response field descriptions**

| Field | Type | Description |
|-------|------|-------------|
| `totalIncome` | number | Sum of all INCOME transactions |
| `totalExpenses` | number | Sum of all EXPENSE transactions |
| `netBalance` | number | `totalIncome - totalExpenses` |
| `totalTransactions` | integer | Count of non-deleted transactions |
| `incomeByCategory` | array | Income subtotals grouped by category, sorted by total descending |
| `expenseByCategory` | array | Expense subtotals grouped by category, sorted by total descending |

**Error responses**

| Status | Reason |
|--------|--------|
| `401 Unauthorized` | Missing or expired JWT |
| `403 Forbidden` | Role is VIEWER |

---

#### `GET /api/dashboard/monthly-trends` — Month-by-month income vs expense

Returns one entry per calendar month for the last N months. Months with no transactions are included with zero values so the frontend always has a complete, gap-free series.

**Query parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `months` | integer | `12` | How many past months to include |

**Example requests**
```
GET /api/dashboard/monthly-trends
GET /api/dashboard/monthly-trends?months=6
GET /api/dashboard/monthly-trends?months=24
```

**Response `200 OK`**
```json
{
  "trends": [
    {
      "year": 2025,
      "month": 1,
      "monthName": "JANUARY",
      "income": 6200.00,
      "expenses": 1689.00,
      "net": 4511.00
    },
    {
      "year": 2025,
      "month": 2,
      "monthName": "FEBRUARY",
      "income": 5300.00,
      "expenses": 1885.00,
      "net": 3415.00
    },
    {
      "year": 2025,
      "month": 3,
      "monthName": "MARCH",
      "income": 5800.00,
      "expenses": 2770.00,
      "net": 3030.00
    }
  ]
}
```

**Response field descriptions**

| Field | Type | Description |
|-------|------|-------------|
| `year` | integer | Calendar year |
| `month` | integer | Month number (1–12) |
| `monthName` | string | Full month name in uppercase (e.g. `JANUARY`) |
| `income` | number | Total INCOME for that month (0 if none) |
| `expenses` | number | Total EXPENSE for that month (0 if none) |
| `net` | number | `income - expenses` for that month |

**Error responses**

| Status | Reason |
|--------|--------|
| `401 Unauthorized` | Missing or expired JWT |
| `403 Forbidden` | Role is VIEWER |

---

#### `GET /api/dashboard/recent` — Ten most recent transactions

Returns the 10 latest transactions ordered by date descending. Useful for a "recent activity" feed on the dashboard.

**Example request**
```
GET /api/dashboard/recent
Authorization: Bearer <token>
```

**Response `200 OK`** — array of transaction objects
```json
[
  {
    "id": 36,
    "amount": 120.00,
    "type": "EXPENSE",
    "category": "Shopping",
    "date": "2025-05-20",
    "notes": "Clothing",
    "userId": 1,
    "userName": "Alice Admin",
    "createdAt": "2025-05-20T11:30:00"
  },
  {
    "id": 35,
    "amount": 45.00,
    "type": "EXPENSE",
    "category": "Entertainment",
    "date": "2025-05-25",
    "notes": "Streaming subscriptions",
    "userId": 1,
    "userName": "Alice Admin",
    "createdAt": "2025-05-25T10:00:00"
  }
]
```

**Error responses**

| Status | Reason |
|--------|--------|
| `401 Unauthorized` | Missing or expired JWT |
| `403 Forbidden` | Role is VIEWER |

---

### 4. User Management

> **Base path:** `/api/users`
> **Security:** Bearer JWT required
> 🔒 **Required role:** `ADMIN` for all endpoints

---

#### `GET /api/users` — List all users

**Example request**
```
GET /api/users
Authorization: Bearer <token>
```

**Response `200 OK`** — array of user objects
```json
[
  {
    "id": 1,
    "name": "Alice Admin",
    "email": "admin@finance.com",
    "role": "ADMIN",
    "status": "ACTIVE",
    "createdAt": "2025-01-01T00:00:00"
  },
  {
    "id": 2,
    "name": "Alan Analyst",
    "email": "analyst@finance.com",
    "role": "ANALYST",
    "status": "ACTIVE",
    "createdAt": "2025-01-01T00:00:00"
  },
  {
    "id": 3,
    "name": "Vera Viewer",
    "email": "viewer@finance.com",
    "role": "VIEWER",
    "status": "ACTIVE",
    "createdAt": "2025-01-01T00:00:00"
  }
]
```

---

#### `GET /api/users/{id}` — Get a user by ID

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | User ID |

**Response `200 OK`** — single user object (same shape as above)

**Error responses**

| Status | Reason |
|--------|--------|
| `404 Not Found` | No user with given ID |

---

#### `PATCH /api/users/{id}/role` — Change a user's role

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | User ID |

**Query parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `role` | enum | ✅ | `VIEWER` \| `ANALYST` \| `ADMIN` |

**Example request**
```
PATCH /api/users/3/role?role=ANALYST
Authorization: Bearer <token>
```

**Response `200 OK`** — updated user object

```json
{
  "id": 3,
  "name": "Vera Viewer",
  "email": "viewer@finance.com",
  "role": "ANALYST",
  "status": "ACTIVE",
  "createdAt": "2025-01-01T00:00:00"
}
```

**Error responses**

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Invalid role value |
| `404 Not Found` | User does not exist |

---

#### `PATCH /api/users/{id}/status` — Activate or deactivate a user

Deactivating a user (`INACTIVE`) prevents them from logging in. Their existing JWT tokens will fail at the `CustomUserDetailsService` check.

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer | User ID |

**Query parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | enum | ✅ | `ACTIVE` \| `INACTIVE` |

**Example request**
```
PATCH /api/users/4/status?status=INACTIVE
Authorization: Bearer <token>
```

**Response `200 OK`** — updated user object

```json
{
  "id": 4,
  "name": "Ivan Inactive",
  "email": "inactive@finance.com",
  "role": "VIEWER",
  "status": "INACTIVE",
  "createdAt": "2025-01-01T00:00:00"
}
```

**Error responses**

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Invalid status value |
| `404 Not Found` | User does not exist |

---

## Role & Access Control

| Endpoint group         | VIEWER | ANALYST | ADMIN |
|------------------------|:------:|:-------:|:-----:|
| POST /auth/**          | ✅ pub  | ✅ pub   | ✅ pub |
| GET /transactions/**   | ✅      | ✅       | ✅     |
| POST /transactions     | ❌      | ❌       | ✅     |
| PUT /transactions/**   | ❌      | ❌       | ✅     |
| DELETE /transactions/**| ❌      | ❌       | ✅     |
| GET /dashboard/**      | ❌      | ✅       | ✅     |
| /users/**              | ❌      | ❌       | ✅     |

Access control is enforced at two levels:
1. **`SecurityConfig`** — HTTP-level rules on method + path patterns
2. **`@PreAuthorize`** — method-level annotation on controllers for fine-grained control

Inactive users receive a `403 Forbidden` with a descriptive message upon login.

---

## Error Responses

All errors return a consistent JSON shape:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields have invalid values",
  "path": "/api/transactions",
  "timestamp": "2025-06-01T10:30:00",
  "fieldErrors": {
    "amount": "Amount must be greater than 0",
    "category": "Category is required"
  }
}
```

| HTTP Status | Scenario                                       |
|-------------|------------------------------------------------|
| 400         | Validation errors (missing/invalid fields)     |
| 401         | No token, expired token, bad credentials       |
| 403         | Valid token but insufficient role / inactive   |
| 404         | Resource not found                             |
| 409         | Duplicate email on registration                |
| 500         | Unexpected server error                        |

---

## Architecture Decisions

### 1. Layered architecture (Controller → Service → Repository)
Keeps business logic out of controllers and database concerns out of services. Each layer has a single responsibility and can be tested independently.

### 2. DTO pattern
Entities are never serialized directly to JSON. DTOs control exactly what goes in and out, preventing accidental field exposure (e.g., password hash) and decoupling the API contract from the database schema.

### 3. JWT stateless authentication
No server-side session state. The role is embedded in the token so the server can authorize requests without a database lookup on every call.

### 4. Soft delete via `@Where` clause
The `@Where(clause = "deleted_at IS NULL")` annotation on `Transaction` makes deleted records automatically invisible to all standard JPA queries. No existing query needs to be updated when soft delete is added.

### 5. Dashboard aggregation in the repository (JPQL)
Aggregations (SUM, GROUP BY, monthly trends) are pushed to the database using JPQL, not computed in Java. This keeps the data transfer minimal and uses the database's optimized aggregation engine.

### 6. Two-profile database setup
`application-dev.yml` uses H2 in-memory for zero-setup local development. `application.yml` targets PostgreSQL for production. Switching profiles is the only change required.

---

## Assumptions

- **One role per user.** A user holds exactly one role at a time. Role elevation is done by an admin via `PATCH /api/users/{id}/role`.
- **All transactions are system-wide, not per-user scoped.** A VIEWER can read all transactions (not just their own). This matches the "finance dashboard" framing where the team shares a view of company finances.
- **ANALYST cannot write transactions.** Only ADMINs can create, update, or delete records. ANALYSTs have read + dashboard access.
- **Soft delete is permanent from the API.** There is no "restore" endpoint. Deleted records remain in the database but are invisible to queries.
- **The JWT secret must be at least 256 bits** (32 characters) for the HS256 algorithm. The dev default key satisfies this; override in production with a strong random value.
- **Monthly trend data fillss zeros for months with no transactions.** This avoids sparse/gapped charts on the frontend.

---

## Tradeoffs

| Decision | Tradeoff |
|----------|----------|
| H2 for dev | Fast setup, but some SQL (e.g., date functions) may differ slightly from PostgreSQL |
| `@Where` for soft delete | Clean API, but requires raw SQL or `@SQLRestriction` bypass to query deleted records if needed |
| Monolithic service layer | Simple and readable now; would split into domain modules if the codebase grew significantly |
| No refresh tokens | Simpler implementation; in production, add a refresh token endpoint with a longer-lived token |
| In-memory H2 on restart | Seed data reloads on every restart in dev — good for testing, means no persistent dev state |
