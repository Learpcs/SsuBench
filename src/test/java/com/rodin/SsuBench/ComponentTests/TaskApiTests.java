package com.rodin.SsuBench.ComponentTests;

import com.rodin.SsuBench.Entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Компонентные тесты для Task API.
 * Тестирует: create, get, list, update, cancel
 */
class TaskApiTests extends ApiTestBase {

    private String customerToken;
    private String executorToken;
    private String adminToken;
    private String customerUsername;
    private String executorUsername;

    @BeforeEach
    void setUp() throws Exception {
        customerUsername = uniqueUsername("customer");
        executorUsername = uniqueUsername("executor");
        customerToken = registerAndGetToken(customerUsername, UserRole.CUSTOMER);
        executorToken = registerAndGetToken(executorUsername, UserRole.EXECUTOR);
        adminToken = registerAndGetToken(uniqueUsername("admin"), UserRole.ADMIN);
    }

    @Test
    @DisplayName("POST /api/tasks - успешное создание задачи заказчиком")
    void createTask_Success() throws Exception {
        String taskJson = objectMapper.writeValueAsString(
                new CreateTaskRequestDto("Разработать API", new BigDecimal("100.00"))
        );

        ResultActions result = mockMvc.perform(post(TASKS_ENDPOINT)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Разработать API"))
                .andExpect(jsonPath("$.reward").value(100.00))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.customerUsername").value(customerUsername));

        String response = result.andReturn().getResponse().getContentAsString();
        assertThat(response).contains("OPEN", customerUsername);
    }

    @Test
    @DisplayName("POST /api/tasks - ошибка: исполнитель не может создавать задачи")
    void createTask_ExecutorForbidden() throws Exception {
        String taskJson = objectMapper.writeValueAsString(
                new CreateTaskRequestDto("Задача", new BigDecimal("50.00"))
        );

        mockMvc.perform(post(TASKS_ENDPOINT)
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks - ошибка: пустое описание")
    void createTask_EmptyDescription() throws Exception {
        String taskJson = objectMapper.writeValueAsString(
                new CreateTaskRequestDto("", new BigDecimal("50.00"))
        );

        mockMvc.perform(post(TASKS_ENDPOINT)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks - ошибка: отрицательная награда")
    void createTask_NegativeReward() throws Exception {
        String taskJson = objectMapper.writeValueAsString(
                new CreateTaskRequestDto("Задача", new BigDecimal("-10.00"))
        );

        mockMvc.perform(post(TASKS_ENDPOINT)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/tasks - ошибка: нулевая награда")
    void createTask_ZeroReward() throws Exception {
        String taskJson = objectMapper.writeValueAsString(
                new CreateTaskRequestDto("Задача", BigDecimal.ZERO)
        );

        mockMvc.perform(post(TASKS_ENDPOINT)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(APPLICATION_JSON)
                        .content(taskJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - успешное получение задачи")
    void getTask_Success() throws Exception {

        Long taskId = createTestTask(customerToken, "Тестовая задача", new BigDecimal("75.00"));

        mockMvc.perform(get(TASKS_ENDPOINT + "/" + taskId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.description").value("Тестовая задача"))
                .andExpect(jsonPath("$.reward").value(75.00));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} - ошибка: задача не найдена")
    void getTask_NotFound() throws Exception {
        mockMvc.perform(get(TASKS_ENDPOINT + "/99999")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tasks - пагинация списка задач")
    void getAllTasks_Pagination() throws Exception {

        for (int i = 0; i < 5; i++) {
            createTestTask(customerToken, "Задача " + i, new BigDecimal("10.00"));
        }

        mockMvc.perform(get(TASKS_ENDPOINT)
                        .header("Authorization", "Bearer " + customerToken)
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("GET /api/tasks/my - получение задач заказчика")
    void getMyTasks_Success() throws Exception {
        Long taskId1 = createTestTask(customerToken, "Моя задача 1", new BigDecimal("100.00"));
        Long taskId2 = createTestTask(customerToken, "Моя задача 2", new BigDecimal("200.00"));

        ResultActions result = mockMvc.perform(get(TASKS_ENDPOINT + "/my")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        String response = result.andReturn().getResponse().getContentAsString();
        assertThat(response).contains("Моя задача 1", "Моя задача 2");
    }

    @Test
    @DisplayName("GET /api/tasks/available - доступные задачи для исполнителя")
    void getAvailableTasks_Success() throws Exception {

        createTestTask(customerToken, "Доступная задача", new BigDecimal("150.00"));

        mockMvc.perform(get(TASKS_ENDPOINT + "/available")
                        .header("Authorization", "Bearer " + executorToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - успешное обновление задачи")
    void updateTask_Success() throws Exception {
        Long taskId = createTestTask(customerToken, "Старое описание", new BigDecimal("50.00"));

        String updateJson = """
                {"description": "Новое описание"}
                """;

        mockMvc.perform(put(TASKS_ENDPOINT + "/" + taskId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Новое описание"));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} - ошибка: не заказчик обновляет")
    void updateTask_NotCustomer() throws Exception {
        Long taskId = createTestTask(customerToken, "Задача", new BigDecimal("50.00"));

        String updateJson = """
                {"description": "Новое описание"}
                """;

        mockMvc.perform(put(TASKS_ENDPOINT + "/" + taskId)
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/cancel - успешная отмена задачи")
    void cancelTask_Success() throws Exception {
        Long taskId = createTestTask(customerToken, "Задача для отмены", new BigDecimal("50.00"));

        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/cancel")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/cancel - ошибка: не заказчик отменяет")
    void cancelTask_NotCustomer() throws Exception {
        Long taskId = createTestTask(customerToken, "Задача", new BigDecimal("50.00"));

        mockMvc.perform(post(TASKS_ENDPOINT + "/" + taskId + "/cancel")
                        .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/tasks/status/{status} - фильтрация по статусу")
    void getTasksByStatus_Success() throws Exception {
        createTestTask(customerToken, "Открытая задача", new BigDecimal("50.00"));
        createTestTask(customerToken, "Другая задача", new BigDecimal("60.00"));

        mockMvc.perform(get(TASKS_ENDPOINT + "/status/OPEN")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    /**
     * Вспомогательный метод для создания тестовой задачи.
     */
    private Long createTestTask(String token, String description, BigDecimal reward) throws Exception {
        String taskJson = objectMapper.writeValueAsString(
                new CreateTaskRequestDto(description, reward)
        );

        ResultActions result = mockMvc.perform(post(TASKS_ENDPOINT)
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(taskJson))
                .andExpect(status().isOk());

        String response = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    /**
     * DTO для создания задачи.
     */
    public record CreateTaskRequestDto(String description, BigDecimal reward) {}

    /**
     * DTO для обновления задачи.
     */
    public record UpdateTaskRequestDto(String description, BigDecimal reward) {}
}
