package com.rodin.SsuBench.ComponentTests;

import com.rodin.SsuBench.Entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Компонентные тесты для Admin API.
 * Тестирует: list users, get user, block, unblock
 */
public class AdminApiTests extends ApiTestBase {

    private String adminToken;
    private String customerToken;
    private String executorToken;

    private Long customerId;
    private Long executorId;
    private Long adminId;

    private String adminUsername;
    private String customerName;
    private String executorName;

    @BeforeEach
    void setUp() throws Exception {
        adminUsername = "admin_test" + UUID.randomUUID().toString().substring(0, 8);
        customerName = "customer_test" + UUID.randomUUID().toString().substring(0, 8);
        executorName = "executor_test" + UUID.randomUUID().toString().substring(0, 8);

        adminToken = registerAndGetToken(adminUsername, UserRole.ADMIN);
        customerToken = registerAndGetToken(customerName, UserRole.CUSTOMER);
        executorToken = registerAndGetToken(executorName, UserRole.EXECUTOR);

        adminId = getUserIdByUsername(adminUsername);
        customerId = getUserIdByUsername(customerName);
        executorId = getUserIdByUsername(executorName);
    }

    @Test
    @DisplayName("GET /api/admin/users - успешное получение списка пользователей")
    void getAllUsers_Success() throws Exception {
        ResultActions result = mockMvc.perform(get(ADMIN_ENDPOINT + "/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        String response = result.andReturn().getResponse().getContentAsString();
        assertThat(response).contains("admin_test", "customer_test", "executor_test");
    }

    @Test
    @DisplayName("GET /api/admin/users - ошибка: только администратор")
    void getAllUsers_Forbidden() throws Exception {
        mockMvc.perform(get(ADMIN_ENDPOINT + "/users")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users/role/{role} - фильтрация по роли")
    void getUsersByRole_Success() throws Exception {
        mockMvc.perform(get(ADMIN_ENDPOINT + "/users/role/EXECUTOR")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} - успешное получение пользователя")
    void getUser_Success() throws Exception {
        mockMvc.perform(get(ADMIN_ENDPOINT + "/users/" + customerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.username").value(customerName))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.isBlocked").value(false));
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/block - успешная блокировка пользователя")
    void blockUser_Success() throws Exception {
        mockMvc.perform(post(ADMIN_ENDPOINT + "/users/" + customerId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.isBlocked").value(true));


        String loginJson = objectMapper.writeValueAsString(
                new AuthRequestDto(customerName, PASSWORD_EXAMPLE)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/unblock - успешная разблокировка пользователя")
    void unblockUser_Success() throws Exception {

        mockMvc.perform(post(ADMIN_ENDPOINT + "/users/" + customerId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());


        mockMvc.perform(post(ADMIN_ENDPOINT + "/users/" + customerId + "/unblock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBlocked").value(false));


        String loginJson = objectMapper.writeValueAsString(
                new AuthRequestDto(customerName, PASSWORD_EXAMPLE)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/block - ошибка: админ не может заблокировать себя")
    void blockUser_SelfBlock() throws Exception {
        mockMvc.perform(post(ADMIN_ENDPOINT + "/users/" + adminId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/block - ошибка: пользователь не найден")
    void blockUser_NotFound() throws Exception {
        mockMvc.perform(post(ADMIN_ENDPOINT + "/users/99999/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Вспомогательный метод для получения ID пользователя по имени.
     */
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
