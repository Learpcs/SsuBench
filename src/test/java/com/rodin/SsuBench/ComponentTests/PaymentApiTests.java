package com.rodin.SsuBench.ComponentTests;

import com.rodin.SsuBench.Entity.UserRole;
import com.rodin.SsuBench.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Компонентные тесты для Payment/Completion API.
 * Тестирует полный цикл: complete task -> confirm payment -> balance transfer
 */
class PaymentApiTests extends ApiTestBase {

    private String customerToken;
    private String executorToken;
    private String adminToken;
    private Long taskId;
    private Long bidId;
    private String customerUsername;
    private String executorUsername;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() throws Exception {
        customerUsername = uniqueUsername("customer");
        executorUsername = uniqueUsername("executor");
        customerToken = registerAndGetToken(customerUsername, UserRole.CUSTOMER);
        executorToken = registerAndGetToken(executorUsername, UserRole.EXECUTOR);
        adminToken = registerAndGetToken(uniqueUsername("admin"), UserRole.ADMIN);


        var customer = userRepository.findByUsername(customerUsername).orElseThrow();
        customer.setBalance(new BigDecimal("1000.00"));
        userRepository.save(customer);


        taskId = createTestTask(customerToken, "Задача для оплаты", new BigDecimal("100.00"));


        bidId = createTestBid(executorToken, taskId, "Отклик на задачу");


        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId + "/accept")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/complete - исполнитель завершает задачу")
    void completeTask_Success() throws Exception {
        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/complete")
                        .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/complete - ошибка: не выбранный исполнитель завершает")
    void completeTask_WrongExecutor() throws Exception {

        String executor2Token = registerAndGetToken(uniqueUsername("executor2"), UserRole.EXECUTOR);
        Long taskId2 = createTestTask(customerToken, "Другая задача", new BigDecimal("50.00"));
        Long bidId2 = createTestBid(executor2Token, taskId2, "Отклик");


        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId2 + "/accept")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());


        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId2 + "/complete")
                        .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/complete - ошибка: задача не в статусе IN_PROGRESS")
    void completeTask_WrongStatus() throws Exception {

        Long newTaskId = createTestTask(customerToken, "Задача без отклика", new BigDecimal("50.00"));

        mockMvc.perform(post(TASKS_ENDPOINT + "/" + newTaskId + "/complete")
                        .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/confirm - успешное подтверждение и перевод баллов")
    void confirmPayment_Success() throws Exception {

        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/complete")
                .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk());


        Long executorId = getUserIdByUsername(executorUsername);
        String executorBeforeJson = mockMvc.perform(get(ADMIN_ENDPOINT + "/users/" + executorId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BigDecimal balanceBefore = objectMapper.readTree(executorBeforeJson)
                .get("balance")
                .decimalValue();


        ResultActions result = mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/confirm")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.bidId").value(bidId))
                .andExpect(jsonPath("$.amount").exists());

        String response = result.andReturn().getResponse().getContentAsString();
        assertThat(response).contains("amount");


        String executorAfterJson = mockMvc.perform(get(ADMIN_ENDPOINT + "/users/" + executorId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BigDecimal balanceAfter = objectMapper.readTree(executorAfterJson)
                .get("balance")
                .decimalValue();

        assertThat(balanceAfter).isGreaterThan(balanceBefore);
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/confirm - ошибка: не заказчик подтверждает")
    void confirmPayment_NotCustomer() throws Exception {

        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/complete")
                .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk());


        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/confirm")
                        .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/confirm - ошибка: задача не завершена")
    void confirmPayment_TaskNotCompleted() throws Exception {

        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/confirm")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/confirm - ошибка: недостаточно средств")
    void confirmPayment_InsufficientFunds() throws Exception {

        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/complete")
                .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk());

        // Создаем новую задачу с большой наградой
        Long expensiveTaskId = createTestTask(customerToken, "Дорогая задача", new BigDecimal("1000000.00"));
        Long expensiveBidId = createTestBid(executorToken, expensiveTaskId, "Отклик");

        // Принимаем отклик
        mockMvc.perform(post(BIDS_ENDPOINT + "/" + expensiveBidId + "/accept")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Завершаем задачу
        mockMvc.perform(post(TASKS_ENDPOINT + "/" + expensiveTaskId + "/complete")
                .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk());

        // Подтверждаем - должно быть недостаточно средств (баланс 0)
        mockMvc.perform(post(TASKS_ENDPOINT + "/" + expensiveTaskId + "/confirm")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/confirm - ошибка: повторное подтверждение")
    void confirmPayment_AlreadyConfirmed() throws Exception {
        // Завершаем задачу
        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/complete")
                .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk());

        // Первое подтверждение успешно
        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/confirm")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Второе подтверждение должно вернуть ошибку (4xx)
        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/confirm")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/tasks/{id}/payment - успешное получение платежа")
    void getPayment_Success() throws Exception {
        // Завершаем и подтверждаем задачу
        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/complete")
                .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk());

        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/confirm")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Получаем платеж
        mockMvc.perform(get(TASKS_ENDPOINT + "/" + taskId + "/payment")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.bidId").exists())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    @DisplayName("GET /api/tasks/{id}/payment - ошибка: платеж не найден")
    void getPayment_NotFound() throws Exception {
        // Задача создана, но еще не подтверждена
        mockMvc.perform(get(TASKS_ENDPOINT + "/" + taskId + "/payment")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Вспомогательные методы.
     */
    private Long createTestTask(String token, String description, BigDecimal reward) throws Exception {
        String taskJson = objectMapper.writeValueAsString(
                new TaskApiTests.CreateTaskRequestDto(description, reward)
        );

        ResultActions result = mockMvc.perform(post(TASKS_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(taskJson))
                .andExpect(status().isOk());

        String response = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createTestBid(String token, Long taskId, String description) throws Exception {
        String bidJson = objectMapper.writeValueAsString(
                new BidApiTests.CreateBidRequestDto(description)
        );

        ResultActions result = mockMvc.perform(post(BIDS_ENDPOINT + "/task/" + taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(bidJson))
                .andExpect(status().isOk());

        String response = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long getUserIdByUsername(String username) throws Exception {
        ResultActions result = mockMvc.perform(get(ADMIN_ENDPOINT + "/users")
                .header("Authorization", "Bearer " + adminToken)
                .param("page", "0")
                .param("size", "100"));

        String response = result.andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode json = objectMapper.readTree(response);
        com.fasterxml.jackson.databind.JsonNode items = json.get("items");

        for (com.fasterxml.jackson.databind.JsonNode item : items) {
            if (username.equals(item.get("username").asText())) {
                return item.get("id").asLong();
            }
        }

        throw new IllegalStateException("User not found: " + username);
    }
}
