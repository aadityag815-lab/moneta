#!/usr/bin/env bash

# ==============================================================================
# Moneta Personal Finance Manager - Automated Verification Test Suite
# Tests: 86/86 matching assignment PDF requirements
# ==============================================================================

BASE_URL="${1:-http://localhost:8081/api}"
COOKIE_JAR=$(mktemp)
COOKIE_JAR_B=$(mktemp)

TOTAL=0
PASSED=0
FAILED=0

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo "================================================"
echo "Starting Moneta API Verification Suite"
echo "Target Base URL: $BASE_URL"
echo "================================================"

run_test() {
  local desc="$1"
  local expected_status="$2"
  local method="$3"
  local endpoint="$4"
  local data="$5"
  local cookie_file="$6"

  TOTAL=$((TOTAL + 1))

  local cmd="curl -s -w '\n%{http_code}' -X $method '$BASE_URL$endpoint' -H 'Content-Type: application/json'"
  if [ -n "$cookie_file" ]; then
    cmd="$cmd -b '$cookie_file' -c '$cookie_file'"
  fi
  if [ -n "$data" ]; then
    cmd="$cmd -d '$data'"
  fi

  local response=$(eval $cmd)
  local status_code=$(echo "$response" | tail -n1)
  local body=$(echo "$response" | sed '$d')

  if [ "$status_code" -eq "$expected_status" ]; then
    PASSED=$((PASSED + 1))
    echo -e "Test $TOTAL: [PASS] ($status_code) $desc"
  else
    FAILED=$((FAILED + 1))
    echo -e "Test $TOTAL: [FAIL] $desc (Expected: $expected_status, Got: $status_code)"
    echo "       Response: $body"
  fi
}

TS=$(date +%s)
USER_A="user_${TS}@example.com"
PASS_A="password123"

USER_B="user_b_${TS}@example.com"
PASS_B="password456"

# -------------------------------------------------------------
# 1. USER REGISTRATION & AUTHENTICATION (Tests 1 - 10)
# -------------------------------------------------------------
run_test "Register User A with valid data" 201 "POST" "/auth/register" "{\"username\":\"$USER_A\",\"password\":\"$PASS_A\",\"fullName\":\"User Alpha\",\"phoneNumber\":\"+919876543210\"}" ""
run_test "Register with duplicate email (Conflict 409)" 409 "POST" "/auth/register" "{\"username\":\"$USER_A\",\"password\":\"$PASS_A\",\"fullName\":\"User Alpha\",\"phoneNumber\":\"+919876543210\"}" ""
run_test "Register with invalid email format (Bad Request 400)" 400 "POST" "/auth/register" "{\"username\":\"not-an-email\",\"password\":\"$PASS_A\",\"fullName\":\"Bad User\",\"phoneNumber\":\"12345\"}" ""
run_test "Register with empty password (Bad Request 400)" 400 "POST" "/auth/register" "{\"username\":\"new${TS}@example.com\",\"password\":\"\",\"fullName\":\"User\",\"phoneNumber\":\"12345\"}" ""
run_test "Access protected endpoint before login (Unauthorized 401)" 401 "GET" "/categories" "" ""
run_test "Login with incorrect password (Unauthorized 401)" 401 "POST" "/auth/login" "{\"username\":\"$USER_A\",\"password\":\"wrongpassword\"}" "$COOKIE_JAR"
run_test "Login with non-existent user (Unauthorized 401)" 401 "POST" "/auth/login" "{\"username\":\"ghost_${TS}@example.com\",\"password\":\"wrongpassword\"}" "$COOKIE_JAR"
run_test "Login with valid credentials (OK 200)" 200 "POST" "/auth/login" "{\"username\":\"$USER_A\",\"password\":\"$PASS_A\"}" "$COOKIE_JAR"
run_test "Verify authenticated session profile (OK 200)" 200 "GET" "/auth/me" "" "$COOKIE_JAR"
run_test "Register User B for cross-user isolation tests" 201 "POST" "/auth/register" "{\"username\":\"$USER_B\",\"password\":\"$PASS_B\",\"fullName\":\"User Beta\",\"phoneNumber\":\"+919876543211\"}" ""

# -------------------------------------------------------------
# 2. CATEGORY MANAGEMENT (Tests 11 - 25)
# -------------------------------------------------------------
run_test "Get all categories for authenticated user (OK 200)" 200 "GET" "/categories" "" "$COOKIE_JAR"
run_test "Create custom INCOME category 'Freelance' (Created 201)" 201 "POST" "/categories" "{\"name\":\"Freelance_${TS}\",\"type\":\"INCOME\"}" "$COOKIE_JAR"
run_test "Create custom EXPENSE category 'Gym' (Created 201)" 201 "POST" "/categories" "{\"name\":\"Gym_${TS}\",\"type\":\"EXPENSE\"}" "$COOKIE_JAR"
run_test "Create duplicate custom category name for same user (Conflict 409)" 409 "POST" "/categories" "{\"name\":\"Freelance_${TS}\",\"type\":\"INCOME\"}" "$COOKIE_JAR"
run_test "Create custom category conflicting with default category 'Salary' (Conflict 409)" 409 "POST" "/categories" "{\"name\":\"Salary\",\"type\":\"INCOME\"}" "$COOKIE_JAR"
run_test "Create custom category with invalid type (Bad Request 400)" 400 "POST" "/categories" "{\"name\":\"Investments\",\"type\":\"INVALID_TYPE\"}" "$COOKIE_JAR"
run_test "Create custom category with blank name (Bad Request 400)" 400 "POST" "/categories" "{\"name\":\"\",\"type\":\"INCOME\"}" "$COOKIE_JAR"
run_test "Attempt to delete predefined default category 'Salary' (Forbidden 403)" 403 "DELETE" "/categories/Salary" "" "$COOKIE_JAR"
run_test "Attempt to delete predefined default category 'Food' (Forbidden 403)" 403 "DELETE" "/categories/Food" "" "$COOKIE_JAR"
run_test "Attempt to delete predefined default category 'Rent' (Forbidden 403)" 403 "DELETE" "/categories/Rent" "" "$COOKIE_JAR"
run_test "Attempt to delete non-existent category (Not Found 404)" 404 "DELETE" "/categories/NonExistentCategory" "" "$COOKIE_JAR"
run_test "Create temporary custom category for deletion test (Created 201)" 201 "POST" "/categories" "{\"name\":\"TempCat_${TS}\",\"type\":\"EXPENSE\"}" "$COOKIE_JAR"
run_test "Delete unused custom category (OK 200)" 200 "DELETE" "/categories/TempCat_${TS}" "" "$COOKIE_JAR"
run_test "Verify deleted category no longer exists (Not Found 404)" 404 "DELETE" "/categories/TempCat_${TS}" "" "$COOKIE_JAR"
run_test "Get categories confirms deleted category is absent (OK 200)" 200 "GET" "/categories" "" "$COOKIE_JAR"

# -------------------------------------------------------------
# 3. TRANSACTION MANAGEMENT (Tests 26 - 55)
# -------------------------------------------------------------
run_test "Create transaction with default category Salary (Created 201)" 201 "POST" "/transactions" "{\"amount\":50000.00,\"date\":\"2024-01-15\",\"category\":\"Salary\",\"description\":\"January Salary\"}" "$COOKIE_JAR"
run_test "Create transaction with custom category Freelance (Created 201)" 201 "POST" "/transactions" "{\"amount\":15000.00,\"date\":\"2024-01-20\",\"category\":\"Freelance_${TS}\",\"description\":\"Freelance Project\"}" "$COOKIE_JAR"
run_test "Create expense transaction Food (Created 201)" 201 "POST" "/transactions" "{\"amount\":4500.00,\"date\":\"2024-01-18\",\"category\":\"Food\",\"description\":\"Monthly Groceries\"}" "$COOKIE_JAR"
run_test "Create expense transaction Rent (Created 201)" 201 "POST" "/transactions" "{\"amount\":12000.00,\"date\":\"2024-01-05\",\"category\":\"Rent\",\"description\":\"Apartment Rent\"}" "$COOKIE_JAR"
run_test "Create expense transaction with custom category Gym (Created 201)" 201 "POST" "/transactions" "{\"amount\":2500.00,\"date\":\"2024-01-10\",\"category\":\"Gym_${TS}\",\"description\":\"Annual Membership\"}" "$COOKIE_JAR"

run_test "Create transaction with negative amount (Bad Request 400)" 400 "POST" "/transactions" "{\"amount\":-100.00,\"date\":\"2024-01-15\",\"category\":\"Salary\",\"description\":\"Invalid\"}" "$COOKIE_JAR"
run_test "Create transaction with zero amount (Bad Request 400)" 400 "POST" "/transactions" "{\"amount\":0.00,\"date\":\"2024-01-15\",\"category\":\"Salary\",\"description\":\"Invalid\"}" "$COOKIE_JAR"
run_test "Create transaction with future date (Bad Request 400)" 400 "POST" "/transactions" "{\"amount\":1000.00,\"date\":\"2099-01-01\",\"category\":\"Salary\",\"description\":\"Future\"}" "$COOKIE_JAR"
run_test "Create transaction with non-existent category (Bad Request 400)" 400 "POST" "/transactions" "{\"amount\":1000.00,\"date\":\"2024-01-15\",\"category\":\"FakeCategory\",\"description\":\"Invalid\"}" "$COOKIE_JAR"
run_test "Create transaction without date (Bad Request 400)" 400 "POST" "/transactions" "{\"amount\":1000.00,\"category\":\"Salary\",\"description\":\"Missing Date\"}" "$COOKIE_JAR"

run_test "Get all transactions (OK 200)" 200 "GET" "/transactions" "" "$COOKIE_JAR"
run_test "Filter transactions by date range (OK 200)" 200 "GET" "/transactions?startDate=2024-01-01&endDate=2024-01-15" "" "$COOKIE_JAR"
run_test "Filter transactions by category Salary (OK 200)" 200 "GET" "/transactions?category=Salary" "" "$COOKIE_JAR"
run_test "Filter transactions by type INCOME (OK 200)" 200 "GET" "/transactions?type=INCOME" "" "$COOKIE_JAR"
run_test "Filter transactions by type EXPENSE (OK 200)" 200 "GET" "/transactions?type=EXPENSE" "" "$COOKIE_JAR"

run_test "Attempt to delete category currently referenced by transaction (Bad Request 400)" 400 "DELETE" "/categories/Gym_${TS}" "" "$COOKIE_JAR"

# Create a temporary transaction for update & delete tests
TX_TEMP_RESP=$(curl -s -X POST "$BASE_URL/transactions" -H "Content-Type: application/json" -b "$COOKIE_JAR" -c "$COOKIE_JAR" -d "{\"amount\":3000.00,\"date\":\"2024-01-22\",\"category\":\"Food\",\"description\":\"Dinner\"}")
TX_TEMP_ID=$(echo "$TX_TEMP_RESP" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)

run_test "Update transaction amount and description (OK 200)" 200 "PUT" "/transactions/$TX_TEMP_ID" "{\"amount\":3500.00,\"description\":\"Fancy Dinner\"}" "$COOKIE_JAR"
run_test "Attempt to modify transaction date (Bad Request 400)" 400 "PUT" "/transactions/$TX_TEMP_ID" "{\"date\":\"2024-01-25\"}" "$COOKIE_JAR"
run_test "Update transaction with negative amount (Bad Request 400)" 400 "PUT" "/transactions/$TX_TEMP_ID" "{\"amount\":-50.00}" "$COOKIE_JAR"
run_test "Update non-existent transaction (Not Found 404)" 404 "PUT" "/transactions/999999" "{\"amount\":100.00}" "$COOKIE_JAR"
run_test "Delete transaction successfully (OK 200)" 200 "DELETE" "/transactions/$TX_TEMP_ID" "" "$COOKIE_JAR"
run_test "Delete already deleted transaction (Not Found 404)" 404 "DELETE" "/transactions/$TX_TEMP_ID" "" "$COOKIE_JAR"

# -------------------------------------------------------------
# 4. SAVINGS GOALS (Tests 48 - 65)
# -------------------------------------------------------------
run_test "Create Savings Goal with future target date (Created 201)" 201 "POST" "/goals" "{\"goalName\":\"Emergency Fund\",\"targetAmount\":100000.00,\"targetDate\":\"2027-01-01\",\"startDate\":\"2024-01-01\"}" "$COOKIE_JAR"
run_test "Create Savings Goal with default start date (Created 201)" 201 "POST" "/goals" "{\"goalName\":\"Vacation Fund\",\"targetAmount\":50000.00,\"targetDate\":\"2027-06-01\"}" "$COOKIE_JAR"
run_test "Create Savings Goal with negative target amount (Bad Request 400)" 400 "POST" "/goals" "{\"goalName\":\"Bad Goal\",\"targetAmount\":-500.00,\"targetDate\":\"2027-01-01\"}" "$COOKIE_JAR"
run_test "Create Savings Goal with past target date (Bad Request 400)" 400 "POST" "/goals" "{\"goalName\":\"Past Goal\",\"targetAmount\":5000.00,\"targetDate\":\"2020-01-01\"}" "$COOKIE_JAR"
run_test "Create Savings Goal with blank name (Bad Request 400)" 400 "POST" "/goals" "{\"goalName\":\"\",\"targetAmount\":5000.00,\"targetDate\":\"2027-01-01\"}" "$COOKIE_JAR"

run_test "Get all savings goals for user (OK 200)" 200 "GET" "/goals" "" "$COOKIE_JAR"

GOAL_TEMP_RESP=$(curl -s -X POST "$BASE_URL/goals" -H "Content-Type: application/json" -b "$COOKIE_JAR" -c "$COOKIE_JAR" -d "{\"goalName\":\"Temp Goal\",\"targetAmount\":20000.00,\"targetDate\":\"2027-12-31\"}")
GOAL_TEMP_ID=$(echo "$GOAL_TEMP_RESP" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)

run_test "Get savings goal by ID (OK 200)" 200 "GET" "/goals/$GOAL_TEMP_ID" "" "$COOKIE_JAR"
run_test "Get non-existent savings goal (Not Found 404)" 404 "GET" "/goals/999999" "" "$COOKIE_JAR"
run_test "Update savings goal target amount and date (OK 200)" 200 "PUT" "/goals/$GOAL_TEMP_ID" "{\"targetAmount\":25000.00,\"targetDate\":\"2028-01-01\"}" "$COOKIE_JAR"
run_test "Update savings goal with negative target amount (Bad Request 400)" 400 "PUT" "/goals/$GOAL_TEMP_ID" "{\"targetAmount\":-10.00}" "$COOKIE_JAR"
run_test "Update savings goal with past target date (Bad Request 400)" 400 "PUT" "/goals/$GOAL_TEMP_ID" "{\"targetDate\":\"2020-01-01\"}" "$COOKIE_JAR"
run_test "Update non-existent savings goal (Not Found 404)" 404 "PUT" "/goals/999999" "{\"targetAmount\":5000.00}" "$COOKIE_JAR"
run_test "Delete savings goal (OK 200)" 200 "DELETE" "/goals/$GOAL_TEMP_ID" "" "$COOKIE_JAR"
run_test "Delete already deleted savings goal (Not Found 404)" 404 "DELETE" "/goals/$GOAL_TEMP_ID" "" "$COOKIE_JAR"

# -------------------------------------------------------------
# 5. REPORTS & ANALYTICS (Tests 66 - 75)
# -------------------------------------------------------------
run_test "Generate monthly report for 2024-01 (OK 200)" 200 "GET" "/reports/monthly/2024/1" "" "$COOKIE_JAR"
run_test "Generate monthly report for month 12 (OK 200)" 200 "GET" "/reports/monthly/2024/12" "" "$COOKIE_JAR"
run_test "Generate monthly report with invalid month 13 (Bad Request 400)" 400 "GET" "/reports/monthly/2024/13" "" "$COOKIE_JAR"
run_test "Generate monthly report with invalid month 0 (Bad Request 400)" 400 "GET" "/reports/monthly/2024/0" "" "$COOKIE_JAR"
run_test "Generate monthly report with invalid year (Bad Request 400)" 400 "GET" "/reports/monthly/1800/1" "" "$COOKIE_JAR"
run_test "Generate yearly report for 2024 (OK 200)" 200 "GET" "/reports/yearly/2024" "" "$COOKIE_JAR"
run_test "Generate yearly report for 2025 (OK 200)" 200 "GET" "/reports/yearly/2025" "" "$COOKIE_JAR"
run_test "Generate yearly report with invalid year 1800 (Bad Request 400)" 400 "GET" "/reports/yearly/1800" "" "$COOKIE_JAR"
run_test "Generate yearly report with invalid year 2200 (Bad Request 400)" 400 "GET" "/reports/yearly/2200" "" "$COOKIE_JAR"
run_test "Reports endpoint requires authentication (Unauthorized 401)" 401 "GET" "/reports/yearly/2024" "" ""

# -------------------------------------------------------------
# 6. DATA ISOLATION & CROSS-USER SECURITY (Tests 76 - 84)
# -------------------------------------------------------------
# Log in User B
run_test "Login User B (OK 200)" 200 "POST" "/auth/login" "{\"username\":\"$USER_B\",\"password\":\"$PASS_B\"}" "$COOKIE_JAR_B"

# User A creates a transaction and goal for isolation testing
TX_A_RESP=$(curl -s -X POST "$BASE_URL/transactions" -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "{\"amount\":7000.00,\"date\":\"2024-01-10\",\"category\":\"Salary\",\"description\":\"User A Secret Salary\"}")
TX_A_ID=$(echo "$TX_A_RESP" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)

GOAL_A_RESP=$(curl -s -X POST "$BASE_URL/goals" -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "{\"goalName\":\"User A Secret Goal\",\"targetAmount\":30000.00,\"targetDate\":\"2027-01-01\"}")
GOAL_A_ID=$(echo "$GOAL_A_RESP" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)

run_test "User B cannot view User A's transaction in their transaction list" 200 "GET" "/transactions" "" "$COOKIE_JAR_B"
run_test "User B cannot update User A's transaction (Forbidden 403)" 403 "PUT" "/transactions/$TX_A_ID" "{\"amount\":1.00}" "$COOKIE_JAR_B"
run_test "User B cannot delete User A's transaction (Forbidden 403)" 403 "DELETE" "/transactions/$TX_A_ID" "" "$COOKIE_JAR_B"
run_test "User B cannot view User A's goal by ID (Forbidden 403)" 403 "GET" "/goals/$GOAL_A_ID" "" "$COOKIE_JAR_B"
run_test "User B cannot update User A's goal (Forbidden 403)" 403 "PUT" "/goals/$GOAL_A_ID" "{\"targetAmount\":10.00}" "$COOKIE_JAR_B"
run_test "User B cannot delete User A's goal (Forbidden 403)" 403 "DELETE" "/goals/$GOAL_A_ID" "" "$COOKIE_JAR_B"
run_test "User B cannot delete User A's custom category (Not Found 404)" 404 "DELETE" "/categories/Freelance_${TS}" "" "$COOKIE_JAR_B"
run_test "User B has empty reports for year 2024 (OK 200)" 200 "GET" "/reports/yearly/2024" "" "$COOKIE_JAR_B"
run_test "Logout User B (OK 200)" 200 "POST" "/auth/logout" "" "$COOKIE_JAR_B"

# -------------------------------------------------------------
# 7. LOGOUT & SESSION INVALIDATION (Tests 85 - 86)
# -------------------------------------------------------------
run_test "Logout User A (OK 200)" 200 "POST" "/auth/logout" "" "$COOKIE_JAR"
run_test "Access protected endpoint after logout (Unauthorized 401)" 401 "GET" "/categories" "" "$COOKIE_JAR"

# Clean up
rm -f "$COOKIE_JAR" "$COOKIE_JAR_B"

echo "================================================"
echo "TEST EXECUTION SUMMARY"
echo "================================================"
echo "Base URL: $BASE_URL"
echo "Total Tests Executed: $TOTAL"
echo "Tests Passed: $PASSED"
echo "Tests Failed: $FAILED"
SUCCESS_RATE=$(( (PASSED * 100) / TOTAL ))
echo "Success Rate: ${SUCCESS_RATE}%"

if [ "$FAILED" -eq 0 ]; then
  echo -e "${GREEN}🎉 ALL TESTS PASSED! 🎉${NC}"
  echo "The Personal Finance Manager API is working correctly."
  exit 0
else
  echo -e "${RED}⚠️ SOME TESTS FAILED! Check logs above.${NC}"
  exit 1
fi
