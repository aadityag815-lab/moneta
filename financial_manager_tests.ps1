param (
    [string]$BaseUrl = "http://localhost:8081/api"
)

$Total = 0
$Passed = 0
$Failed = 0

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Starting Moneta API Verification Suite (curl.exe)" -ForegroundColor Cyan
Write-Host "Target Base URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

$cookieA = [System.IO.Path]::GetTempFileName()
$cookieB = [System.IO.Path]::GetTempFileName()

function Run-CurlTest {
    param (
        [string]$Desc,
        [int]$ExpectedStatus,
        [string]$Method,
        [string]$Endpoint,
        [string]$Body = "",
        [string]$CookieFile = ""
    )

    $script:Total++
    $url = "$BaseUrl$Endpoint"

    $argsList = @("-s", "-w", "`n%{http_code}", "-X", $Method, $url, "-H", "Content-Type: application/json")
    if ($CookieFile) {
        $argsList += @("-b", $CookieFile, "-c", $CookieFile)
    }
    if ($Body) {
        $argsList += @("-d", $Body)
    }

    $raw = & curl.exe @argsList
    $lines = $raw -split "`n"
    $statusCode = [int]($lines[-1].Trim())

    if ($statusCode -eq $ExpectedStatus) {
        $script:Passed++
        Write-Host "Test $script:Total: [PASS] ($statusCode) $Desc" -ForegroundColor Green
    } else {
        $script:Failed++
        Write-Host "Test $script:Total: [FAIL] $Desc (Expected: $ExpectedStatus, Got: $statusCode)" -ForegroundColor Red
    }
}

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$userA = "user_$ts@example.com"
$passA = "password123"

$userB = "user_b_$ts@example.com"
$passB = "password456"

# 1. USER REGISTRATION & AUTHENTICATION (Tests 1 - 10)
Run-CurlTest "Register User A with valid data" 201 "POST" "/auth/register" "{`"username`":`"$userA`",`"password`":`"$passA`",`"fullName`":`"User Alpha`",`"phoneNumber`":`"+919876543210`"}"
Run-CurlTest "Register with duplicate email (Conflict 409)" 409 "POST" "/auth/register" "{`"username`":`"$userA`",`"password`":`"$passA`",`"fullName`":`"User Alpha`",`"phoneNumber`":`"+919876543210`"}"
Run-CurlTest "Register with invalid email format (Bad Request 400)" 400 "POST" "/auth/register" "{`"username`":`"not-an-email`",`"password`":`"$passA`",`"fullName`":`"Bad`",`"phoneNumber`":`"12345`"}"
Run-CurlTest "Register with empty password (Bad Request 400)" 400 "POST" "/auth/register" "{`"username`":`"new_$ts@example.com`",`"password`":`"`",`"fullName`":`"User`",`"phoneNumber`":`"12345`"}"
Run-CurlTest "Access protected endpoint before login (Unauthorized 401)" 401 "GET" "/categories"
Run-CurlTest "Login with incorrect password (Unauthorized 401)" 401 "POST" "/auth/login" "{`"username`":`"$userA`",`"password`":`"wrongpassword`"}" "" $cookieA
Run-CurlTest "Login with non-existent user (Unauthorized 401)" 401 "POST" "/auth/login" "{`"username`":`"ghost_$ts@example.com`",`"password`":`"wrong`"}" "" $cookieA
Run-CurlTest "Login with valid credentials (OK 200)" 200 "POST" "/auth/login" "{`"username`":`"$userA`",`"password`":`"$passA`"}" "" $cookieA
Run-CurlTest "Verify authenticated session profile (OK 200)" 200 "GET" "/auth/me" "" $cookieA
Run-CurlTest "Register User B for cross-user isolation tests" 201 "POST" "/auth/register" "{`"username`":`"$userB`",`"password`":`"$passB`",`"fullName`":`"User Beta`",`"phoneNumber`":`"+919876543211`"}"

# 2. CATEGORY MANAGEMENT (Tests 11 - 25)
Run-CurlTest "Get all categories for authenticated user (OK 200)" 200 "GET" "/categories" "" $cookieA
Run-CurlTest "Create custom INCOME category 'Freelance' (Created 201)" 201 "POST" "/categories" "{`"name`":`"Freelance_$ts`",`"type`":`"INCOME`"}" $cookieA
Run-CurlTest "Create custom EXPENSE category 'Gym' (Created 201)" 201 "POST" "/categories" "{`"name`":`"Gym_$ts`",`"type`":`"EXPENSE`"}" $cookieA
Run-CurlTest "Create duplicate custom category name for same user (Conflict 409)" 409 "POST" "/categories" "{`"name`":`"Freelance_$ts`",`"type`":`"INCOME`"}" $cookieA
Run-CurlTest "Create custom category conflicting with default category 'Salary' (Conflict 409)" 409 "POST" "/categories" "{`"name`":`"Salary`",`"type`":`"INCOME`"}" $cookieA
Run-CurlTest "Create custom category with invalid type (Bad Request 400)" 400 "POST" "/categories" "{`"name`":`"Investments`",`"type`":`"INVALID`"}" $cookieA
Run-CurlTest "Create custom category with blank name (Bad Request 400)" 400 "POST" "/categories" "{`"name`":`"`",`"type`":`"INCOME`"}" $cookieA
Run-CurlTest "Attempt to delete predefined default category 'Salary' (Forbidden 403)" 403 "DELETE" "/categories/Salary" "" $cookieA
Run-CurlTest "Attempt to delete predefined default category 'Food' (Forbidden 403)" 403 "DELETE" "/categories/Food" "" $cookieA
Run-CurlTest "Attempt to delete predefined default category 'Rent' (Forbidden 403)" 403 "DELETE" "/categories/Rent" "" $cookieA
Run-CurlTest "Attempt to delete non-existent category (Not Found 404)" 404 "DELETE" "/categories/NonExistentCategory" "" $cookieA
Run-CurlTest "Create temporary custom category for deletion test (Created 201)" 201 "POST" "/categories" "{`"name`":`"TempCat_$ts`",`"type`":`"EXPENSE`"}" $cookieA
Run-CurlTest "Delete unused custom category (OK 200)" 200 "DELETE" "/categories/TempCat_$ts" "" $cookieA
Run-CurlTest "Verify deleted category no longer exists (Not Found 404)" 404 "DELETE" "/categories/TempCat_$ts" "" $cookieA
Run-CurlTest "Get categories confirms deleted category is absent (OK 200)" 200 "GET" "/categories" "" $cookieA

# 3. TRANSACTION MANAGEMENT (Tests 26 - 47)
Run-CurlTest "Create transaction with default category Salary (Created 201)" 201 "POST" "/transactions" "{`"amount`":50000.00,`"date`":`"2024-01-15`",`"category`":`"Salary`",`"description`":`"January Salary`"}" $cookieA
Run-CurlTest "Create transaction with custom category Freelance (Created 201)" 201 "POST" "/transactions" "{`"amount`":15000.00,`"date`":`"2024-01-20`",`"category`":`"Freelance_$ts`",`"description`":`"Freelance Project`"}" $cookieA
Run-CurlTest "Create expense transaction Food (Created 201)" 201 "POST" "/transactions" "{`"amount`":4500.00,`"date`":`"2024-01-18`",`"category`":`"Food`",`"description`":`"Monthly Groceries`"}" $cookieA
Run-CurlTest "Create expense transaction Rent (Created 201)" 201 "POST" "/transactions" "{`"amount`":12000.00,`"date`":`"2024-01-05`",`"category`":`"Rent`",`"description`":`"Apartment Rent`"}" $cookieA
Run-CurlTest "Create expense transaction with custom category Gym (Created 201)" 201 "POST" "/transactions" "{`"amount`":2500.00,`"date`":`"2024-01-10`",`"category`":`"Gym_$ts`",`"description`":`"Gym Membership`"}" $cookieA

Run-CurlTest "Create transaction with negative amount (Bad Request 400)" 400 "POST" "/transactions" "{`"amount`":-100.00,`"date`":`"2024-01-15`",`"category`":`"Salary`"}" $cookieA
Run-CurlTest "Create transaction with zero amount (Bad Request 400)" 400 "POST" "/transactions" "{`"amount`":0.00,`"date`":`"2024-01-15`",`"category`":`"Salary`"}" $cookieA
Run-CurlTest "Create transaction with future date (Bad Request 400)" 400 "POST" "/transactions" "{`"amount`":1000.00,`"date`":`"2099-01-01`",`"category`":`"Salary`"}" $cookieA
Run-CurlTest "Create transaction with non-existent category (Bad Request 400)" 400 "POST" "/transactions" "{`"amount`":1000.00,`"date`":`"2024-01-15`",`"category`":`"FakeCategory`"}" $cookieA
Run-CurlTest "Create transaction without date (Bad Request 400)" 400 "POST" "/transactions" "{`"amount`":1000.00,`"category`":`"Salary`"}" $cookieA

Run-CurlTest "Get all transactions (OK 200)" 200 "GET" "/transactions" "" $cookieA
Run-CurlTest "Filter transactions by date range (OK 200)" 200 "GET" "/transactions?startDate=2024-01-01&endDate=2024-01-15" "" $cookieA
Run-CurlTest "Filter transactions by category Salary (OK 200)" 200 "GET" "/transactions?category=Salary" "" $cookieA
Run-CurlTest "Filter transactions by type INCOME (OK 200)" 200 "GET" "/transactions?type=INCOME" "" $cookieA
Run-CurlTest "Filter transactions by type EXPENSE (OK 200)" 200 "GET" "/transactions?type=EXPENSE" "" $cookieA

Run-CurlTest "Attempt to delete category currently referenced by transaction (Bad Request 400)" 400 "DELETE" "/categories/Gym_$ts" "" $cookieA

$txTempRaw = & curl.exe -s -X POST "$BaseUrl/transactions" -H "Content-Type: application/json" -b $cookieA -c $cookieA -d "{`"amount`":3000.00,`"date`":`"2024-01-22`",`"category`":`"Food`",`"description`":`"Dinner`"}"
$txTempId = ($txTempRaw | ConvertFrom-Json).id

Run-CurlTest "Update transaction amount and description (OK 200)" 200 "PUT" "/transactions/$txTempId" "{`"amount`":3500.00,`"description`":`"Fancy Dinner`"}" $cookieA
Run-CurlTest "Attempt to modify transaction date (Bad Request 400)" 400 "PUT" "/transactions/$txTempId" "{`"date`":`"2024-01-25`"}" $cookieA
Run-CurlTest "Update transaction with negative amount (Bad Request 400)" 400 "PUT" "/transactions/$txTempId" "{`"amount`":-50.00}" $cookieA
Run-CurlTest "Update non-existent transaction (Not Found 404)" 404 "PUT" "/transactions/999999" "{`"amount`":100.00}" $cookieA
Run-CurlTest "Delete transaction successfully (OK 200)" 200 "DELETE" "/transactions/$txTempId" "" $cookieA
Run-CurlTest "Delete already deleted transaction (Not Found 404)" 404 "DELETE" "/transactions/$txTempId" "" $cookieA

# 4. SAVINGS GOALS (Tests 48 - 65)
Run-CurlTest "Create Savings Goal with future target date (Created 201)" 201 "POST" "/goals" "{`"goalName`":`"Emergency Fund`",`"targetAmount`":100000.00,`"targetDate`":`"2027-01-01`",`"startDate`":`"2024-01-01`"}" $cookieA
Run-CurlTest "Create Savings Goal with default start date (Created 201)" 201 "POST" "/goals" "{`"goalName`":`"Vacation Fund`",`"targetAmount`":50000.00,`"targetDate`":`"2027-06-01`"}" $cookieA
Run-CurlTest "Create Savings Goal with negative target amount (Bad Request 400)" 400 "POST" "/goals" "{`"goalName`":`"Bad Goal`",`"targetAmount`":-500.00,`"targetDate`":`"2027-01-01`"}" $cookieA
Run-CurlTest "Create Savings Goal with past target date (Bad Request 400)" 400 "POST" "/goals" "{`"goalName`":`"Past Goal`",`"targetAmount`":5000.00,`"targetDate`":`"2020-01-01`"}" $cookieA
Run-CurlTest "Create Savings Goal with blank name (Bad Request 400)" 400 "POST" "/goals" "{`"goalName`":`"`",`"targetAmount`":5000.00,`"targetDate`":`"2027-01-01`"}" $cookieA

Run-CurlTest "Get all savings goals for user (OK 200)" 200 "GET" "/goals" "" $cookieA

$goalTempRaw = & curl.exe -s -X POST "$BaseUrl/goals" -H "Content-Type: application/json" -b $cookieA -c $cookieA -d "{`"goalName`":`"Temp Goal`",`"targetAmount`":20000.00,`"targetDate`":`"2027-12-31`"}"
$goalTempId = ($goalTempRaw | ConvertFrom-Json).id

Run-CurlTest "Get savings goal by ID (OK 200)" 200 "GET" "/goals/$goalTempId" "" $cookieA
Run-CurlTest "Get non-existent savings goal (Not Found 404)" 404 "GET" "/goals/999999" "" $cookieA
Run-CurlTest "Update savings goal target amount and date (OK 200)" 200 "PUT" "/goals/$goalTempId" "{`"targetAmount`":25000.00,`"targetDate`":`"2028-01-01`"}" $cookieA
Run-CurlTest "Update savings goal with negative target amount (Bad Request 400)" 400 "PUT" "/goals/$goalTempId" "{`"targetAmount`":-10.00}" $cookieA
Run-CurlTest "Update savings goal with past target date (Bad Request 400)" 400 "PUT" "/goals/$goalTempId" "{`"targetDate`":`"2020-01-01`"}" $cookieA
Run-CurlTest "Update non-existent savings goal (Not Found 404)" 404 "PUT" "/goals/999999" "{`"targetAmount`":5000.00}" $cookieA
Run-CurlTest "Delete savings goal (OK 200)" 200 "DELETE" "/goals/$goalTempId" "" $cookieA
Run-CurlTest "Delete already deleted savings goal (Not Found 404)" 404 "DELETE" "/goals/$goalTempId" "" $cookieA

# 5. REPORTS & ANALYTICS (Tests 66 - 75)
Run-CurlTest "Generate monthly report for 2024-01 (OK 200)" 200 "GET" "/reports/monthly/2024/1" "" $cookieA
Run-CurlTest "Generate monthly report for month 12 (OK 200)" 200 "GET" "/reports/monthly/2024/12" "" $cookieA
Run-CurlTest "Generate monthly report with invalid month 13 (Bad Request 400)" 400 "GET" "/reports/monthly/2024/13" "" $cookieA
Run-CurlTest "Generate monthly report with invalid month 0 (Bad Request 400)" 400 "GET" "/reports/monthly/2024/0" "" $cookieA
Run-CurlTest "Generate monthly report with invalid year (Bad Request 400)" 400 "GET" "/reports/monthly/1800/1" "" $cookieA
Run-CurlTest "Generate yearly report for 2024 (OK 200)" 200 "GET" "/reports/yearly/2024" "" $cookieA
Run-CurlTest "Generate yearly report for 2025 (OK 200)" 200 "GET" "/reports/yearly/2025" "" $cookieA
Run-CurlTest "Generate yearly report with invalid year 1800 (Bad Request 400)" 400 "GET" "/reports/yearly/1800" "" $cookieA
Run-CurlTest "Generate yearly report with invalid year 2200 (Bad Request 400)" 400 "GET" "/reports/yearly/2200" "" $cookieA
Run-CurlTest "Reports endpoint requires authentication (Unauthorized 401)" 401 "GET" "/reports/yearly/2024"

# 6. DATA ISOLATION & CROSS-USER SECURITY (Tests 76 - 84)
Run-CurlTest "Login User B (OK 200)" 200 "POST" "/auth/login" "{`"username`":`"$userB`",`"password`":`"$passB`"}" "" $cookieB

$txARaw = & curl.exe -s -X POST "$BaseUrl/transactions" -H "Content-Type: application/json" -b $cookieA -d "{`"amount`":7000.00,`"date`":`"2024-01-10`",`"category`":`"Salary`",`"description`":`"User A Secret Salary`"}"
$txAId = ($txARaw | ConvertFrom-Json).id

$goalARaw = & curl.exe -s -X POST "$BaseUrl/goals" -H "Content-Type: application/json" -b $cookieA -d "{`"goalName`":`"User A Secret Goal`",`"targetAmount`":30000.00,`"targetDate`":`"2027-01-01`"}"
$goalAId = ($goalARaw | ConvertFrom-Json).id

Run-CurlTest "User B cannot view User A's transaction in their list (OK 200)" 200 "GET" "/transactions" "" $cookieB
Run-CurlTest "User B cannot update User A's transaction (Forbidden 403)" 403 "PUT" "/transactions/$txAId" "{`"amount`":1.00}" $cookieB
Run-CurlTest "User B cannot delete User A's transaction (Forbidden 403)" 403 "DELETE" "/transactions/$txAId" "" $cookieB
Run-CurlTest "User B cannot view User A's goal by ID (Forbidden 403)" 403 "GET" "/goals/$goalAId" "" $cookieB
Run-CurlTest "User B cannot update User A's goal (Forbidden 403)" 403 "PUT" "/goals/$goalAId" "{`"targetAmount`":10.00}" $cookieB
Run-CurlTest "User B cannot delete User A's goal (Forbidden 403)" 403 "DELETE" "/goals/$goalAId" "" $cookieB
Run-CurlTest "User B cannot delete User A's custom category (Not Found 404)" 404 "DELETE" "/categories/Freelance_$ts" "" $cookieB
Run-CurlTest "User B has empty reports for year 2024 (OK 200)" 200 "GET" "/reports/yearly/2024" "" $cookieB
Run-CurlTest "Logout User B (OK 200)" 200 "POST" "/auth/logout" "" $cookieB

# 7. LOGOUT & SESSION INVALIDATION (Tests 85 - 86)
Run-CurlTest "Logout User A (OK 200)" 200 "POST" "/auth/logout" "" $cookieA
Run-CurlTest "Access protected endpoint after logout (Unauthorized 401)" 401 "GET" "/categories" "" $cookieA

Remove-Item -Path $cookieA, $cookieB -Force -ErrorAction SilentlyContinue

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "TEST EXECUTION SUMMARY" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Base URL: $BaseUrl"
Write-Host "Total Tests Executed: $script:Total"
Write-Host "Tests Passed: $script:Passed" -ForegroundColor Green
Write-Host "Tests Failed: $script:Failed" -ForegroundColor $(if ($script:Failed -eq 0) { "Green" } else { "Red" })
$rate = [math]::Round(($script:Passed * 100) / $script:Total)
Write-Host "Success Rate: $rate%"

if ($script:Failed -eq 0) {
    Write-Host "🎉 ALL TESTS PASSED! 🎉" -ForegroundColor Green
    Write-Host "The Personal Finance Manager API is working correctly." -ForegroundColor Green
} else {
    Write-Host "⚠️ SOME TESTS FAILED! Check logs above." -ForegroundColor Red
}
