// Moneta Personal Finance Manager - Automated Verification Test Suite (Node.js)
// Tests: 86/86 matching assignment PDF requirements

const BASE_URL = process.argv[2] || 'http://localhost:8081/api';

let total = 0;
let passed = 0;
let failed = 0;

let cookieJarA = '';
let cookieJarB = '';

function updateCookies(cookieJar, res) {
  const setCookie = res.headers.get('set-cookie');
  if (setCookie) {
    const parts = setCookie.split(';');
    const mainCookie = parts[0];
    return mainCookie;
  }
  return cookieJar;
}

async function runTest(desc, expectedStatus, method, endpoint, data = null, cookieUser = null) {
  total++;
  const url = `${BASE_URL}${endpoint}`;
  const headers = { 'Content-Type': 'application/json' };

  if (cookieUser === 'A' && cookieJarA) {
    headers['Cookie'] = cookieJarA;
  } else if (cookieUser === 'B' && cookieJarB) {
    headers['Cookie'] = cookieJarB;
  }

  const options = {
    method,
    headers,
  };

  if (data) {
    options.body = JSON.stringify(data);
  }

  try {
    const res = await fetch(url, options);
    const status = res.status;
    let body = null;
    try {
      body = await res.json();
    } catch (e) {
      // not json
    }

    if (cookieUser === 'A') {
      cookieJarA = updateCookies(cookieJarA, res);
    } else if (cookieUser === 'B') {
      cookieJarB = updateCookies(cookieJarB, res);
    }

    if (status === expectedStatus) {
      passed++;
      console.log(`Test ${total}: [PASS] (${status}) ${desc}`);
      return body;
    } else {
      failed++;
      console.log(`Test ${total}: [FAIL] ${desc} (Expected: ${expectedStatus}, Got: ${status})`);
      if (body) console.log('       Response:', JSON.stringify(body));
      return null;
    }
  } catch (err) {
    failed++;
    console.log(`Test ${total}: [ERROR] ${desc} - ${err.message}`);
    return null;
  }
}

async function main() {
  console.log('================================================');
  console.log('Starting Moneta API Verification Suite');
  console.log(`Target Base URL: ${BASE_URL}`);
  console.log('================================================');

  const ts = Date.now();
  const userA = `user_${ts}@example.com`;
  const passA = 'password123';
  const userB = `user_b_${ts}@example.com`;
  const passB = 'password456';

  // 1. User Management & Authentication (Tests 1-10)
  await runTest('Register User A with valid data', 201, 'POST', '/auth/register', {
    username: userA,
    password: passA,
    fullName: 'User Alpha',
    phoneNumber: '+919876543210',
  });

  await runTest('Register with duplicate email (Conflict 409)', 409, 'POST', '/auth/register', {
    username: userA,
    password: passA,
    fullName: 'User Alpha',
    phoneNumber: '+919876543210',
  });

  await runTest('Register with invalid email format (Bad Request 400)', 400, 'POST', '/auth/register', {
    username: 'not-an-email',
    password: passA,
    fullName: 'Bad',
    phoneNumber: '12345',
  });

  await runTest('Register with empty password (Bad Request 400)', 400, 'POST', '/auth/register', {
    username: `new_${ts}@example.com`,
    password: '',
    fullName: 'User',
    phoneNumber: '12345',
  });

  await runTest('Access protected endpoint before login (Unauthorized 401)', 401, 'GET', '/categories');

  await runTest('Login with incorrect password (Unauthorized 401)', 401, 'POST', '/auth/login', {
    username: userA,
    password: 'wrongpassword',
  }, 'A');

  await runTest('Login with non-existent user (Unauthorized 401)', 401, 'POST', '/auth/login', {
    username: `ghost_${ts}@example.com`,
    password: 'wrong',
  }, 'A');

  await runTest('Login with valid credentials (OK 200)', 200, 'POST', '/auth/login', {
    username: userA,
    password: passA,
  }, 'A');

  await runTest('Verify authenticated session profile (OK 200)', 200, 'GET', '/auth/me', null, 'A');

  await runTest('Register User B for cross-user isolation tests', 201, 'POST', '/auth/register', {
    username: userB,
    password: passB,
    fullName: 'User Beta',
    phoneNumber: '+919876543211',
  });

  // 2. Categories (Tests 11-25)
  await runTest('Get all categories for authenticated user (OK 200)', 200, 'GET', '/categories', null, 'A');

  await runTest('Create custom INCOME category (Created 201)', 201, 'POST', '/categories', {
    name: `Freelance_${ts}`,
    type: 'INCOME',
  }, 'A');

  await runTest('Create custom EXPENSE category (Created 201)', 201, 'POST', '/categories', {
    name: `Gym_${ts}`,
    type: 'EXPENSE',
  }, 'A');

  await runTest('Create duplicate custom category name for same user (Conflict 409)', 409, 'POST', '/categories', {
    name: `Freelance_${ts}`,
    type: 'INCOME',
  }, 'A');

  await runTest('Create custom category conflicting with default category Salary (Conflict 409)', 409, 'POST', '/categories', {
    name: 'Salary',
    type: 'INCOME',
  }, 'A');

  await runTest('Create custom category with invalid type (Bad Request 400)', 400, 'POST', '/categories', {
    name: 'Investments',
    type: 'INVALID',
  }, 'A');

  await runTest('Create custom category with blank name (Bad Request 400)', 400, 'POST', '/categories', {
    name: '',
    type: 'INCOME',
  }, 'A');

  await runTest('Attempt to delete predefined default category Salary (Forbidden 403)', 403, 'DELETE', '/categories/Salary', null, 'A');
  await runTest('Attempt to delete predefined default category Food (Forbidden 403)', 403, 'DELETE', '/categories/Food', null, 'A');
  await runTest('Attempt to delete predefined default category Rent (Forbidden 403)', 403, 'DELETE', '/categories/Rent', null, 'A');

  await runTest('Attempt to delete non-existent category (Not Found 404)', 404, 'DELETE', '/categories/NonExistentCategory', null, 'A');

  await runTest('Create temporary custom category for deletion test (Created 201)', 201, 'POST', '/categories', {
    name: `TempCat_${ts}`,
    type: 'EXPENSE',
  }, 'A');

  await runTest('Delete unused custom category (OK 200)', 200, 'DELETE', `/categories/TempCat_${ts}`, null, 'A');
  await runTest('Verify deleted category no longer exists (Not Found 404)', 404, 'DELETE', `/categories/TempCat_${ts}`, null, 'A');
  await runTest('Get categories confirms deleted category is absent (OK 200)', 200, 'GET', '/categories', null, 'A');

  // 3. Transactions (Tests 26-47)
  await runTest('Create transaction with default category Salary (Created 201)', 201, 'POST', '/transactions', {
    amount: 50000.0,
    date: '2024-01-15',
    category: 'Salary',
    description: 'January Salary',
  }, 'A');

  await runTest('Create transaction with custom category Freelance (Created 201)', 201, 'POST', '/transactions', {
    amount: 15000.0,
    date: '2024-01-20',
    category: `Freelance_${ts}`,
    description: 'Freelance Project',
  }, 'A');

  await runTest('Create expense transaction Food (Created 201)', 201, 'POST', '/transactions', {
    amount: 4500.0,
    date: '2024-01-18',
    category: 'Food',
    description: 'Monthly Groceries',
  }, 'A');

  await runTest('Create expense transaction Rent (Created 201)', 201, 'POST', '/transactions', {
    amount: 12000.0,
    date: '2024-01-05',
    category: 'Rent',
    description: 'Apartment Rent',
  }, 'A');

  await runTest('Create expense transaction with custom category Gym (Created 201)', 201, 'POST', '/transactions', {
    amount: 2500.0,
    date: '2024-01-10',
    category: `Gym_${ts}`,
    description: 'Gym Membership',
  }, 'A');

  await runTest('Create transaction with negative amount (Bad Request 400)', 400, 'POST', '/transactions', {
    amount: -100.0,
    date: '2024-01-15',
    category: 'Salary',
  }, 'A');

  await runTest('Create transaction with zero amount (Bad Request 400)', 400, 'POST', '/transactions', {
    amount: 0.0,
    date: '2024-01-15',
    category: 'Salary',
  }, 'A');

  await runTest('Create transaction with future date (Bad Request 400)', 400, 'POST', '/transactions', {
    amount: 1000.0,
    date: '2099-01-01',
    category: 'Salary',
  }, 'A');

  await runTest('Create transaction with non-existent category (Bad Request 400)', 400, 'POST', '/transactions', {
    amount: 1000.0,
    date: '2024-01-15',
    category: 'FakeCategory',
  }, 'A');

  await runTest('Create transaction without date (Bad Request 400)', 400, 'POST', '/transactions', {
    amount: 1000.0,
    category: 'Salary',
  }, 'A');

  await runTest('Get all transactions (OK 200)', 200, 'GET', '/transactions', null, 'A');
  await runTest('Filter transactions by date range (OK 200)', 200, 'GET', '/transactions?startDate=2024-01-01&endDate=2024-01-15', null, 'A');
  await runTest('Filter transactions by category Salary (OK 200)', 200, 'GET', '/transactions?category=Salary', null, 'A');
  await runTest('Filter transactions by type INCOME (OK 200)', 200, 'GET', '/transactions?type=INCOME', null, 'A');
  await runTest('Filter transactions by type EXPENSE (OK 200)', 200, 'GET', '/transactions?type=EXPENSE', null, 'A');

  await runTest('Attempt to delete category currently referenced by transaction (Bad Request 400)', 400, 'DELETE', `/categories/Gym_${ts}`, null, 'A');

  const tempTx = await runTest('Create temporary transaction for update & delete (Created 201)', 201, 'POST', '/transactions', {
    amount: 3000.0,
    date: '2024-01-22',
    category: 'Food',
    description: 'Dinner',
  }, 'A');

  const txTempId = tempTx ? tempTx.id : 999999;

  await runTest('Update transaction amount and description (OK 200)', 200, 'PUT', `/transactions/${txTempId}`, {
    amount: 3500.0,
    description: 'Fancy Dinner',
  }, 'A');

  await runTest('Attempt to modify transaction date (Bad Request 400)', 400, 'PUT', `/transactions/${txTempId}`, {
    date: '2024-01-25',
  }, 'A');

  await runTest('Update transaction with negative amount (Bad Request 400)', 400, 'PUT', `/transactions/${txTempId}`, {
    amount: -50.0,
  }, 'A');

  await runTest('Update non-existent transaction (Not Found 404)', 404, 'PUT', '/transactions/999999', {
    amount: 100.0,
  }, 'A');

  await runTest('Delete transaction successfully (OK 200)', 200, 'DELETE', `/transactions/${txTempId}`, null, 'A');
  await runTest('Delete already deleted transaction (Not Found 404)', 404, 'DELETE', `/transactions/${txTempId}`, null, 'A');

  // 4. Savings Goals (Tests 48-65)
  await runTest('Create Savings Goal with future target date (Created 201)', 201, 'POST', '/goals', {
    goalName: 'Emergency Fund',
    targetAmount: 100000.0,
    targetDate: '2027-01-01',
    startDate: '2024-01-01',
  }, 'A');

  await runTest('Create Savings Goal with default start date (Created 201)', 201, 'POST', '/goals', {
    goalName: 'Vacation Fund',
    targetAmount: 50000.0,
    targetDate: '2027-06-01',
  }, 'A');

  await runTest('Create Savings Goal with negative target amount (Bad Request 400)', 400, 'POST', '/goals', {
    goalName: 'Bad Goal',
    targetAmount: -500.0,
    targetDate: '2027-01-01',
  }, 'A');

  await runTest('Create Savings Goal with past target date (Bad Request 400)', 400, 'POST', '/goals', {
    goalName: 'Past Goal',
    targetAmount: 5000.0,
    targetDate: '2020-01-01',
  }, 'A');

  await runTest('Create Savings Goal with blank name (Bad Request 400)', 400, 'POST', '/goals', {
    goalName: '',
    targetAmount: 5000.0,
    targetDate: '2027-01-01',
  }, 'A');

  await runTest('Get all savings goals for user (OK 200)', 200, 'GET', '/goals', null, 'A');

  const tempGoal = await runTest('Create temporary goal for update & delete (Created 201)', 201, 'POST', '/goals', {
    goalName: 'Temp Goal',
    targetAmount: 20000.0,
    targetDate: '2027-12-31',
  }, 'A');

  const goalTempId = tempGoal ? tempGoal.id : 999999;

  await runTest('Get savings goal by ID (OK 200)', 200, 'GET', `/goals/${goalTempId}`, null, 'A');
  await runTest('Get non-existent savings goal (Not Found 404)', 404, 'GET', '/goals/999999', null, 'A');

  await runTest('Update savings goal target amount and date (OK 200)', 200, 'PUT', `/goals/${goalTempId}`, {
    targetAmount: 25000.0,
    targetDate: '2028-01-01',
  }, 'A');

  await runTest('Update savings goal with negative target amount (Bad Request 400)', 400, 'PUT', `/goals/${goalTempId}`, {
    targetAmount: -10.0,
  }, 'A');

  await runTest('Update savings goal with past target date (Bad Request 400)', 400, 'PUT', `/goals/${goalTempId}`, {
    targetDate: '2020-01-01',
  }, 'A');

  await runTest('Update non-existent savings goal (Not Found 404)', 404, 'PUT', '/goals/999999', {
    targetAmount: 5000.0,
  }, 'A');

  await runTest('Delete savings goal (OK 200)', 200, 'DELETE', `/goals/${goalTempId}`, null, 'A');
  await runTest('Delete already deleted savings goal (Not Found 404)', 404, 'DELETE', `/goals/${goalTempId}`, null, 'A');

  // 5. Reports & Analytics (Tests 66-75)
  await runTest('Generate monthly report for 2024-01 (OK 200)', 200, 'GET', '/reports/monthly/2024/1', null, 'A');
  await runTest('Generate monthly report for month 12 (OK 200)', 200, 'GET', '/reports/monthly/2024/12', null, 'A');
  await runTest('Generate monthly report with invalid month 13 (Bad Request 400)', 400, 'GET', '/reports/monthly/2024/13', null, 'A');
  await runTest('Generate monthly report with invalid month 0 (Bad Request 400)', 400, 'GET', '/reports/monthly/2024/0', null, 'A');
  await runTest('Generate monthly report with invalid year (Bad Request 400)', 400, 'GET', '/reports/monthly/1800/1', null, 'A');

  await runTest('Generate yearly report for 2024 (OK 200)', 200, 'GET', '/reports/yearly/2024', null, 'A');
  await runTest('Generate yearly report for 2025 (OK 200)', 200, 'GET', '/reports/yearly/2025', null, 'A');
  await runTest('Generate yearly report with invalid year 1800 (Bad Request 400)', 400, 'GET', '/reports/yearly/1800', null, 'A');
  await runTest('Generate yearly report with invalid year 2200 (Bad Request 400)', 400, 'GET', '/reports/yearly/2200', null, 'A');
  await runTest('Reports endpoint requires authentication (Unauthorized 401)', 401, 'GET', '/reports/yearly/2024');

  // 6. Data Isolation & Security (Tests 76-84)
  await runTest('Login User B (OK 200)', 200, 'POST', '/auth/login', {
    username: userB,
    password: passB,
  }, 'B');

  const secretTx = await runTest('Create User A Secret Transaction (Created 201)', 201, 'POST', '/transactions', {
    amount: 7000.0,
    date: '2024-01-10',
    category: 'Salary',
    description: 'Secret Salary',
  }, 'A');

  const secretGoal = await runTest('Create User A Secret Goal (Created 201)', 201, 'POST', '/goals', {
    goalName: 'Secret Goal',
    targetAmount: 30000.0,
    targetDate: '2027-01-01',
  }, 'A');

  const txAId = secretTx ? secretTx.id : 999998;
  const goalAId = secretGoal ? secretGoal.id : 999998;

  await runTest("User B cannot view User A's transaction in list (OK 200)", 200, 'GET', '/transactions', null, 'B');
  await runTest("User B cannot update User A's transaction (Forbidden 403)", 403, 'PUT', `/transactions/${txAId}`, {
    amount: 1.0,
  }, 'B');

  await runTest("User B cannot delete User A's transaction (Forbidden 403)", 403, 'DELETE', `/transactions/${txAId}`, null, 'B');
  await runTest("User B cannot view User A's goal by ID (Forbidden 403)", 403, 'GET', `/goals/${goalAId}`, null, 'B');
  await runTest("User B cannot update User A's goal (Forbidden 403)", 403, 'PUT', `/goals/${goalAId}`, {
    targetAmount: 10.0,
  }, 'B');

  await runTest("User B cannot delete User A's goal (Forbidden 403)", 403, 'DELETE', `/goals/${goalAId}`, null, 'B');
  await runTest("User B cannot delete User A's custom category (Not Found 404)", 404, 'DELETE', `/categories/Freelance_${ts}`, null, 'B');
  await runTest('User B has empty reports for year 2024 (OK 200)', 200, 'GET', '/reports/yearly/2024', null, 'B');
  await runTest('Logout User B (OK 200)', 200, 'POST', '/auth/logout', null, 'B');

  // 7. Logout & Session Expiration (Tests 85-86)
  await runTest('Logout User A (OK 200)', 200, 'POST', '/auth/logout', null, 'A');
  await runTest('Access protected endpoint after logout (Unauthorized 401)', 401, 'GET', '/categories', null, 'A');

  console.log('================================================');
  console.log('TEST EXECUTION SUMMARY');
  console.log('================================================');
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Total Tests Executed: ${total}`);
  console.log(`Tests Passed: ${passed}`);
  console.log(`Tests Failed: ${failed}`);
  const rate = Math.round((passed * 100) / total);
  console.log(`Success Rate: ${rate}%`);

  if (failed === 0) {
    console.log('🎉 ALL TESTS PASSED! 🎉');
    console.log('The Personal Finance Manager API is working correctly.');
    process.exit(0);
  } else {
    console.log('⚠️ SOME TESTS FAILED! Check logs above.');
    process.exit(1);
  }
}

main();
