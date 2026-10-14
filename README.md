# Moneta 💰

A personal finance manager I built to track income, expenses, savings goals, and generate monthly/yearly reports. Built with Spring Boot on the backend and React on the frontend — everything served from a single port.

---

## What it does

- **Track transactions** — log income and expenses with categories, filter by date/type
- **Savings goals** — set a target and it auto-calculates your progress from your income minus expenses
- **Reports** — monthly and yearly breakdowns by category
- **Custom categories** — add your own on top of the 7 built-in ones (Salary, Food, Rent, etc.)
- **Multi-user** — each account is fully isolated, your data stays yours

---

## Stack

**Backend** — Java 17, Spring Boot 3.2.5, Spring Security (session cookies), Spring Data JPA, PostgreSQL / H2

**Frontend** — React 18, Vite, Recharts, Lucide icons, plain CSS

---

## Running locally

You need Java 17+ and Node 18+.

```bash
# Build the frontend first
cd frontend
npm install
npm run build

# Then run the backend (uses local H2 database by default)
cd ../backend
./mvnw spring-boot:run        # macOS / Linux
mvnw.cmd spring-boot:run      # Windows
```

Open **http://localhost:8081** — that's it.

To use your own PostgreSQL instead of H2:

```bash
export DB_URL="postgresql://user:password@host:5432/dbname"
./mvnw spring-boot:run
```

---

## Tests

```bash
cd backend
./mvnw test
```

58 unit + integration tests, 84%+ line coverage via JaCoCo.

There's also a full end-to-end API test suite:

```bash
node run_tests.js http://localhost:8081/api
# 87/87 passing ✓
```

---

## Deploying to Render

1. Push this repo to GitHub
2. Create a PostgreSQL database on Render, copy the external URL
3. Create a Web Service:
   - Root directory: `backend`
   - Build: `./mvnw clean package -DskipTests`
   - Start: `java -jar target/moneta-backend-0.0.1-SNAPSHOT.jar`
   - Env var: `DATABASE_URL` = your Render PostgreSQL URL

---

## License

MIT
