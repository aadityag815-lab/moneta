# Moneta 💰 — Personal Finance Manager

> A full-stack Personal Finance Manager built with **Spring Boot 3** (Java) and **React** (Vite), backed by **PostgreSQL** on Render.

[![Tests](https://img.shields.io/badge/API%20Tests-87%2F87%20Passing-brightgreen)]()
[![Coverage](https://img.shields.io/badge/Test%20Coverage-84%25-green)]()
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)]()

---

## 🌐 Live Demo

Once deployed to Render, your app is available at:
```
https://<your-service-name>.onrender.com
```

---

## ✨ Features

| Feature | Details |
|---|---|
| **Authentication** | Session-based login/register (JSESSIONID cookie, BCrypt), no JWT |
| **Categories** | 7 system defaults (Salary, Food, Rent, etc.) + custom INCOME/EXPENSE categories |
| **Transactions** | Full CRUD · BigDecimal amounts · date lock · future-date guard · smart filters |
| **Savings Goals** | Progress = income − expenses since goal start date · percentage tracking |
| **Reports** | Monthly & Yearly breakdowns grouped by category with net savings |
| **Security** | 100% cross-user data isolation verified · 403/404 on cross-user access |

---

## 🛠️ Tech Stack

### Backend
- **Java 17** · **Spring Boot 3.2.5**
- **Spring Security** — session cookies, BCrypt
- **Spring Data JPA** + Hibernate + JPA Specifications
- **PostgreSQL** (production) · **H2** (local zero-config fallback)
- **Bean Validation** (`jakarta.validation`)
- **JUnit 5**, **Mockito**, **JaCoCo** — 58 tests, **84%+ coverage**

### Frontend
- **React 18** + **Vite**
- **Plain CSS** with CSS variables (no Tailwind)
- **Recharts** — bar chart & donut chart
- **Lucide React** icons
- Native Fetch API (`credentials: 'include'`)

---

## 🚀 Running Locally

### Prerequisites
- Java 17+
- Node.js 18+
- Maven 3.9+ (or use `./mvnw`)

### Option A — Local H2 database (zero config)

```bash
# 1. Build frontend into backend/src/main/resources/static
cd frontend
npm install
npm run build

# 2. Run backend (auto-uses H2 file DB at ./data/monetadb)
cd ../backend
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

Visit **http://localhost:8081**

### Option B — Connect to your own PostgreSQL

```bash
# Set your database URL, then run:
export DB_URL="postgresql://user:password@host:5432/dbname"
./mvnw spring-boot:run
```

---

## 🧪 Testing

### Unit & Integration Tests (58 tests)
```bash
cd backend
./mvnw test
# JaCoCo report → backend/target/site/jacoco/index.html
```

### End-to-End API Verification (87 tests)
```bash
# Start the server first, then:
node run_tests.js http://localhost:8081/api
```

**Result:**
```
Tests Passed: 87 / 87
Success Rate: 100% 🎉
```

---

## ☁️ Deploy to Render (Free Tier)

### Step 1 — Create PostgreSQL database on Render
1. Go to [render.com](https://render.com) → **New → PostgreSQL**
2. Name it `moneta-db`, region Singapore (or closest)
3. Copy the **External Database URL**

### Step 2 — Deploy the Web Service
1. **New → Web Service** → connect your GitHub repo
2. Set:

| Setting | Value |
|---|---|
| **Root Directory** | `backend` |
| **Environment** | `Java` |
| **Build Command** | `./mvnw clean package -DskipTests` |
| **Start Command** | `java -jar target/moneta-backend-0.0.1-SNAPSHOT.jar` |

3. Add **Environment Variables**:

| Key | Value |
|---|---|
| `DATABASE_URL` | *(paste your Render PostgreSQL External URL)* |
| `PORT` | `8080` |

> ✅ No `DB_USERNAME` / `DB_PASSWORD` needed — the full credentials are embedded in `DATABASE_URL`.

### Step 3 — Verify
```bash
node run_tests.js https://your-service-name.onrender.com/api
```

---

## 📁 Project Structure

```
moneta/
├── backend/                     # Spring Boot application
│   ├── src/main/java/com/moneta/
│   │   ├── config/              # SecurityConfig, DatabaseConfig, DataInitializer, SpaRedirectConfig
│   │   ├── controller/          # Auth, Category, Transaction, Goal, Report
│   │   ├── dto/                 # Request / Response DTOs
│   │   ├── entity/              # User, Category, Transaction, SavingsGoal, CategoryType
│   │   ├── exception/           # GlobalExceptionHandler + custom exceptions
│   │   ├── repository/          # JpaRepositories + JpaSpecificationExecutor
│   │   └── service/             # Business logic, validation, security isolation
│   └── src/main/resources/
│       ├── static/              # Production React build (served by Spring Boot)
│       └── application.properties
├── frontend/                    # React + Vite SPA
│   └── src/
│       ├── components/          # Sidebar, Header, modals
│       └── views/               # Overview, Transactions, Categories, Goals, Reports, Auth
├── run_tests.js                 # 87-test Node.js API verification suite
├── financial_manager_tests.sh   # Bash variant
└── README.md
```

---

## 📋 API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register new user |
| `POST` | `/api/auth/login` | Login (sets JSESSIONID) |
| `POST` | `/api/auth/logout` | Logout + invalidate session |
| `GET` | `/api/auth/profile` | Get current user info |
| `GET` | `/api/categories` | List all categories |
| `POST` | `/api/categories` | Create custom category |
| `DELETE` | `/api/categories/{id}` | Delete custom category |
| `GET` | `/api/transactions` | List (filterable by date/category/type) |
| `POST` | `/api/transactions` | Create transaction |
| `PUT` | `/api/transactions/{id}` | Update transaction |
| `DELETE` | `/api/transactions/{id}` | Delete transaction |
| `GET` | `/api/goals` | List savings goals |
| `POST` | `/api/goals` | Create savings goal |
| `GET` | `/api/goals/{id}` | Get single goal |
| `PUT` | `/api/goals/{id}` | Update goal |
| `DELETE` | `/api/goals/{id}` | Delete goal |
| `GET` | `/api/reports/monthly/{year}/{month}` | Monthly report |
| `GET` | `/api/reports/yearly/{year}` | Yearly report |

---

## 👤 Author

**Aaditya Gupta** · [aadityag815@gmail.com](mailto:aadityag815@gmail.com)
