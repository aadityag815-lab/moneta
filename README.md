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

Clean 4-tier layered architecture:

```
Controller → Service → Repository → Database
```

```
backend/src/main/java/com/moneta/
├── config/         SecurityConfig, DatabaseConfig, DataInitializer, SpaRedirectConfig
├── controller/     AuthController, CategoryController, TransactionController,
│                   GoalController, ReportController
├── dto/            Request and response transfer objects
├── entity/         User, Category, Transaction, SavingsGoal, CategoryType
├── exception/      GlobalExceptionHandler, custom exception classes
├── repository/     JPA repositories with JpaSpecificationExecutor for dynamic filtering
└── service/        Business logic, validation, security isolation
```

The React frontend is built into `backend/src/main/resources/static/` so the entire application is served from a single port with no separate frontend server needed.

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
