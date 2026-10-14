package com.moneta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneta.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialManagerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completeUserJourneyIntegrationTest() throws Exception {
        String testEmail = "testuser" + System.currentTimeMillis() + "@example.com";

        // 1. Register User -> 201 Created
        RegisterRequest registerReq = new RegisterRequest(testEmail, "password123", "Test User", "+919876543210");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("User registered successfully")))
                .andExpect(jsonPath("$.userId", notNullValue()));

        // 2. Duplicate registration -> 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict());

        // 3. Invalid email registration -> 400 Bad Request
        RegisterRequest badReq = new RegisterRequest("invalid-email", "password123", "Test", "1234");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());

        // 4. Access protected endpoint before login -> 401 Unauthorized
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());

        // 5. Login with invalid password -> 401 Unauthorized
        LoginRequest badLogin = new LoginRequest(testEmail, "wrongpass");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());

        // 6. Login with valid credentials -> 200 OK (capture session)
        LoginRequest loginReq = new LoginRequest(testEmail, "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Login successful")))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // 7. Get All Categories -> 200 OK (contains default categories)
        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(greaterThanOrEqualTo(7))))
                .andExpect(jsonPath("$.categories[?(@.name == 'Salary')].isCustom", contains(false)))
                .andExpect(jsonPath("$.categories[?(@.name == 'Food')].isCustom", contains(false)));

        // 8. Create Custom Category -> 201 Created
        CategoryRequest customCatReq = new CategoryRequest("FreelanceConsulting", "INCOME");
        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customCatReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("FreelanceConsulting")))
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andExpect(jsonPath("$.isCustom", is(true)));

        // 9. Create Duplicate Custom Category -> 409 Conflict
        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customCatReq)))
                .andExpect(status().isConflict());

        // 10. Attempt to delete default category -> 403 Forbidden
        mockMvc.perform(delete("/api/categories/Salary").session(session))
                .andExpect(status().isForbidden());

        // 11. Create Transaction -> 201 Created
        TransactionRequest tx1 = new TransactionRequest(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "January Salary"
        );
        MvcResult tx1Result = mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.amount", is(50000.00)))
                .andExpect(jsonPath("$.category", is("Salary")))
                .andExpect(jsonPath("$.type", is("INCOME")))
                .andReturn();

        TransactionResponse createdTx1 = objectMapper.readValue(
                tx1Result.getResponse().getContentAsString(),
                TransactionResponse.class
        );

        // 12. Create Expense Transaction with custom category
        CategoryRequest gymCatReq = new CategoryRequest("Gym", "EXPENSE");
        mockMvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(gymCatReq)))
                .andExpect(status().isCreated());

        TransactionRequest tx2 = new TransactionRequest(
                new BigDecimal("2000.00"),
                LocalDate.of(2024, 1, 20),
                "Gym",
                "Monthly Gym"
        );
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tx2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        // 13. Create Transaction with future date -> 400 Bad Request
        TransactionRequest futureTx = new TransactionRequest(
                new BigDecimal("1000.00"),
                LocalDate.now().plusDays(2),
                "Salary",
                "Future"
        );
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(futureTx)))
                .andExpect(status().isBadRequest());

        // 14. Create Transaction with negative amount -> 400 Bad Request
        TransactionRequest negTx = new TransactionRequest(
                new BigDecimal("-500.00"),
                LocalDate.of(2024, 1, 1),
                "Salary",
                "Negative"
        );
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(negTx)))
                .andExpect(status().isBadRequest());

        // 15. Attempt to delete category currently referenced by transaction -> 400 Bad Request
        mockMvc.perform(delete("/api/categories/Gym").session(session))
                .andExpect(status().isBadRequest());

        // 16. Get Transactions with filters -> 200 OK
        mockMvc.perform(get("/api/transactions")
                        .session(session)
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("category", "Salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].category", is("Salary")));

        // 17. Update Transaction -> 200 OK
        UpdateTransactionRequest updateTx = new UpdateTransactionRequest();
        updateTx.setAmount(new BigDecimal("60000.00"));
        updateTx.setDescription("Updated Salary");
        mockMvc.perform(put("/api/transactions/" + createdTx1.getId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTx)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(60000.00)))
                .andExpect(jsonPath("$.description", is("Updated Salary")));

        // 18. Attempt to modify date on transaction -> 400 Bad Request
        UpdateTransactionRequest updateDateTx = new UpdateTransactionRequest();
        updateDateTx.setDate(LocalDate.of(2024, 1, 25));
        mockMvc.perform(put("/api/transactions/" + createdTx1.getId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDateTx)))
                .andExpect(status().isBadRequest());

        // 19. Create Savings Goal -> 201 Created
        GoalRequest goalReq = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("100000.00"),
                LocalDate.now().plusMonths(6),
                LocalDate.of(2024, 1, 1)
        );
        MvcResult goalResult = mockMvc.perform(post("/api/goals")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.goalName", is("Emergency Fund")))
                .andReturn();

        GoalResponse createdGoal = objectMapper.readValue(
                goalResult.getResponse().getContentAsString(),
                GoalResponse.class
        );

        // 20. Get Goal by Id -> 200 OK
        mockMvc.perform(get("/api/goals/" + createdGoal.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProgress", notNullValue()))
                .andExpect(jsonPath("$.progressPercentage", notNullValue()));

        // 21. Update Goal -> 200 OK
        UpdateGoalRequest updateGoalReq = new UpdateGoalRequest(
                new BigDecimal("120000.00"),
                LocalDate.now().plusMonths(8)
        );
        mockMvc.perform(put("/api/goals/" + createdGoal.getId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateGoalReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount", is(120000.00)));

        // 22. Monthly Report -> 200 OK
        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is(1)))
                .andExpect(jsonPath("$.year", is(2024)))
                .andExpect(jsonPath("$.totalIncome.Salary", is(60000.00)))
                .andExpect(jsonPath("$.totalExpenses.Gym", is(2000.00)))
                .andExpect(jsonPath("$.netSavings", is(58000.00)));

        // 23. Yearly Report -> 200 OK
        mockMvc.perform(get("/api/reports/yearly/2024").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(2024)))
                .andExpect(jsonPath("$.netSavings", is(58000.00)));

        // 24. Delete Goal -> 200 OK
        mockMvc.perform(delete("/api/goals/" + createdGoal.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Goal deleted successfully")));

        // 25. Delete Transaction -> 200 OK
        mockMvc.perform(delete("/api/transactions/" + createdTx1.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Transaction deleted successfully")));

        // 26. Logout -> 200 OK
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logout successful")));

        // 27. Access protected endpoint after logout -> 401 Unauthorized
        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isUnauthorized());
    }
}
