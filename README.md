# Personal Finance Manager — Moneta

**Assignment submission for Syfe Backend Engineering Assessment**

---

## Overview

Moneta is a full-stack personal finance management web application. It allows users to track their income and expenses, manage savings goals, and generate financial reports — all through a clean REST API backed by a persistent database.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Authentication | Spring Security (session cookies, BCrypt) |
| Database | PostgreSQL (production) · H2 (local development fallback) |
| ORM | Spring Data JPA + Hibernate |
| Validation | Jakarta Bean Validation |
| Frontend | React 18 + Vite |
| Charts | Recharts |
| Build | Maven 3.9 |
| Testing | JUnit 5, Mockito, JaCoCo |

---

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────┐
│                   Browser                        │
│         React 18 SPA (Vite build)               │
│   Overview · Transactions · Goals · Reports      │
└───────────────────┬─────────────────────────────┘
                    │ HTTP + JSESSIONID cookie
                    ▼
┌─────────────────────────────────────────────────┐
│           Spring Boot 3.2.5 (port 8081)         │
│                                                  │
│  ┌──────────────┐    Serves React static files  │
│  │ Spring MVC   │◄── GET /                      │
│  │ Controllers  │◄── GET /api/**                │
│  └──────┬───────┘                               │
│         │                                        │
│  ┌──────▼───────┐                               │
│  │   Services   │  Business logic, validation,  │
│  │              │  cross-user isolation          │
│  └──────┬───────┘                               │
│         │                                        │
│  ┌──────▼───────┐                               │
│  │ Repositories │  Spring Data JPA +            │
│  │              │  JpaSpecificationExecutor      │
│  └──────┬───────┘                               │
└─────────┼───────────────────────────────────────┘
          │ JDBC / Hibernate
          ▼
┌─────────────────────────────────────────────────┐
│   PostgreSQL (prod)  ·  H2 file DB (local dev)  │
└─────────────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Class(es) | Responsibility |
|---|---|---|
| **Controller** | `AuthController`, `CategoryController`, `TransactionController`, `GoalController`, `ReportController` | Parse HTTP requests, validate input via Bean Validation, delegate to service, return structured JSON responses |
| **Service** | `AuthService`, `CategoryService`, `TransactionService`, `GoalService`, `ReportService` | Business rules, ownership checks, cross-user isolation, domain computations (goal progress, report aggregation) |
| **Repository** | `UserRepository`, `CategoryRepository`, `TransactionRepository`, `SavingsGoalRepository` | Database queries via Spring Data JPA; `TransactionRepository` extends `JpaSpecificationExecutor` for dynamic multi-field filtering |
| **Config** | `SecurityConfig`, `DatabaseConfig`, `DataInitializer`, `SpaRedirectConfig` | Spring Security filter chain, database URL parsing, seeding default categories on startup, SPA routing fallback |

### Data Model

```
User
 ├── id, email, passwordHash, name, createdAt
 │
 ├── Category  (many per user)
 │    ├── id, name, type (INCOME / EXPENSE)
 │    └── system: boolean — system categories cannot be deleted
 │
 ├── Transaction  (many per user)
 │    ├── id, amount (BigDecimal), date, description
 │    ├── type (INCOME / EXPENSE) — derived from category
 │    └── category → Category
 │
 └── SavingsGoal  (many per user)
      ├── id, name, targetAmount (BigDecimal), targetDate, startDate
      └── currentAmount, remainingAmount, progressPercentage (computed on read)
```

### Request Flow — Create Transaction

```
POST /api/transactions
        │
        ▼
TransactionController.createTransaction()
  └── @Valid on TransactionRequest — rejects invalid fields (400)
        │
        ▼
TransactionService.createTransaction()
  ├── Resolves current user from Spring Security context
  ├── Validates: amount > 0, date not in future
  ├── Looks up category — must belong to user or be a system category
  └── Derives transaction type (INCOME/EXPENSE) from category
        │
        ▼
TransactionRepository.save()
        │
        ▼
201 Created + TransactionResponse JSON
```

### Security Design

- **Authentication:** Spring Security session-based login. Passwords hashed with BCrypt.
- **Session:** `JSESSIONID` cookie, `HttpOnly`, `SameSite=Lax`, 24-hour timeout.
- **Authorisation:** Every service method resolves the current user from the security context and enforces ownership — no user can read or mutate another user's data. Cross-user access returns `403 Forbidden` or `404 Not Found`.
- **CORS:** Configured to accept credentials from the frontend origin only.

### Package Structure

```
backend/src/main/java/com/moneta/
├── config/
│   ├── DatabaseConfig.java          # Parses postgres:// and postgresql:// URLs
│   ├── DataInitializer.java         # Seeds 7 default categories on first startup
│   ├── SecurityConfig.java          # Filter chain, CORS, BCrypt, session config
│   └── SpaRedirectConfig.java       # Redirects unknown routes → index.html for SPA
├── controller/
│   ├── AuthController.java
│   ├── CategoryController.java
│   ├── GoalController.java
│   ├── ReportController.java
│   └── TransactionController.java
├── dto/                             # 15 request / response DTOs
├── entity/
│   ├── Category.java
│   ├── CategoryType.java            # Enum: INCOME, EXPENSE
│   ├── SavingsGoal.java
│   ├── Transaction.java
│   └── User.java
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice, maps exceptions → HTTP status
│   └── BadRequestException, ConflictException, ForbiddenException,
│       ResourceNotFoundException, UnauthorizedException
├── repository/
│   ├── CategoryRepository.java
│   ├── SavingsGoalRepository.java
│   ├── TransactionRepository.java   # extends JpaSpecificationExecutor
│   └── UserRepository.java
└── service/
    ├── AuthService.java
    ├── CategoryService.java
    ├── GoalService.java
    ├── ReportService.java
    └── TransactionService.java      # Uses JPA Specifications for dynamic filtering
```

---

## Features Implemented

**Authentication**
- Register with email + password (BCrypt hashed), duplicate email returns `409 Conflict`
- Login sets an `JSESSIONID` session cookie
- Logout invalidates the server-side session
- All API endpoints require authentication (`401 Unauthorized` otherwise)

**Categories**
- 7 predefined system categories: `Salary` (INCOME), `Food`, `Rent`, `Transportation`, `Entertainment`, `Healthcare`, `Utilities` (EXPENSE)
- System categories are protected — cannot be deleted (`403 Forbidden`)
- Users can create custom INCOME or EXPENSE categories
- Category names are unique per user account
- Categories referenced by transactions cannot be deleted (`400 Bad Request`)

**Transactions**
- Full CRUD with amounts stored as `BigDecimal`
- Validates: positive amounts only, no future dates, valid category required
- Transaction type (INCOME/EXPENSE) is derived automatically from the category
- Transaction date is locked after creation — cannot be modified on update
- Filterable by `startDate`, `endDate`, `category`, and `type`, sorted newest-first

**Savings Goals**
- Create goals with a target amount and a future target date
- Progress is computed as: `total income − total expenses` since the goal's start date
- Returns current saved amount, remaining amount, and percentage

**Reports**
- Monthly report (`/api/reports/monthly/{year}/{month}`) — income by category, expenses by category, net savings
- Yearly report (`/api/reports/yearly/{year}`) — same breakdown across a full year

**Security**
- Full cross-user data isolation — users cannot read, update, or delete each other's data

---

## Running Locally

**Prerequisites:** Java 17+, Node.js 18+, Maven 3.9+

```bash
# 1. Build the frontend
cd frontend
npm install
npm run build

# 2. Run the backend (defaults to local H2 database — no setup needed)
cd ../backend
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

The application starts on **http://localhost:8081**

To connect to PostgreSQL instead:

```bash
export DB_URL="postgresql://user:password@host:5432/dbname"
./mvnw spring-boot:run
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get session cookie |
| POST | `/api/auth/logout` | Logout and invalidate session |
| GET | `/api/auth/profile` | Get current user profile |
| GET | `/api/categories` | List all categories |
| POST | `/api/categories` | Create a custom category |
| DELETE | `/api/categories/{id}` | Delete a custom category |
| GET | `/api/transactions` | List transactions (with filters) |
| POST | `/api/transactions` | Create a transaction |
| PUT | `/api/transactions/{id}` | Update a transaction |
| DELETE | `/api/transactions/{id}` | Delete a transaction |
| GET | `/api/goals` | List savings goals |
| POST | `/api/goals` | Create a savings goal |
| GET | `/api/goals/{id}` | Get a savings goal by ID |
| PUT | `/api/goals/{id}` | Update a savings goal |
| DELETE | `/api/goals/{id}` | Delete a savings goal |
| GET | `/api/reports/monthly/{year}/{month}` | Monthly financial report |
| GET | `/api/reports/yearly/{year}` | Yearly financial report |

---

## Testing

### Unit & Integration Tests

```bash
cd backend
./mvnw test
```

- **58 tests** across service and controller layers
- **84%+ line coverage** measured with JaCoCo
- Report generated at `backend/target/site/jacoco/index.html`

### End-to-End API Verification

```bash
# With the server running:
node run_tests.js http://localhost:8081/api
```

```
================================================
TEST EXECUTION SUMMARY
================================================
Total Tests Executed: 87
Tests Passed:         87
Tests Failed:         0
Success Rate:         100%
🎉 ALL TESTS PASSED!
================================================
```

The 87-test suite covers: registration, authentication, category CRUD, transaction CRUD with all validations, savings goal lifecycle, monthly/yearly reports, and cross-user data isolation.
