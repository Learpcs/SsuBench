package com.rodin.SsuBench.ComponentTests;

import com.rodin.SsuBench.Entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Компонентные тесты для Auth API.
 * Тестирует: register, login, refresh
 */
class AuthApiTests extends ApiTestBase {

    @Test
    @DisplayName("POST /api/auth/register - успешная регистрация заказчика")
    void registerCustomer_Success() throws Exception {
        String registerJson = objectMapper.writeValueAsString(
                new RegisterRequestDto("customer_test", PASSWORD_EXAMPLE, UserRole.CUSTOMER)
        );

        ResultActions result = mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        String response = result.andReturn().getResponse().getContentAsString();
        assertThat(response).contains("accessToken", "refreshToken");
    }

    @Test
    @DisplayName("POST /api/auth/register - успешная регистрация исполнителя")
    void registerExecutor_Success() throws Exception {
        String registerJson = objectMapper.writeValueAsString(
                new RegisterRequestDto("executor_test", PASSWORD_EXAMPLE, UserRole.EXECUTOR)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - успешная регистрация администратора")
    void registerAdmin_Success() throws Exception {
        String registerJson = objectMapper.writeValueAsString(
                new RegisterRequestDto("admin_test", PASSWORD_EXAMPLE, UserRole.ADMIN)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - ошибка: пользователь уже существует")
    void register_UserAlreadyExists() throws Exception {
        String username = "duplicate_user";
        String registerJson = objectMapper.writeValueAsString(
                new RegisterRequestDto(username, PASSWORD_EXAMPLE, UserRole.EXECUTOR)
        );


        mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk());


        mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/register - ошибка: пустое имя пользователя")
    void register_EmptyUsername() throws Exception {
        String registerJson = objectMapper.writeValueAsString(
                new RegisterRequestDto("", PASSWORD_EXAMPLE, UserRole.EXECUTOR)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register - ошибка: короткое имя пользователя")
    void register_ShortUsername() throws Exception {
        String registerJson = objectMapper.writeValueAsString(
                new RegisterRequestDto("abc", PASSWORD_EXAMPLE, UserRole.EXECUTOR)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register - ошибка: null роль")
    void register_NullRole() throws Exception {
        String registerJson = """
                {"username": "test_user", "password": "kfnsadfnasjdkfkjaieowjlasfjsdalkfj123%A", "role": null}
                """;

        mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - успешный вход")
    void login_Success() throws Exception {
        String username = "login_test_user";
        

        registerAndGetToken(username, UserRole.EXECUTOR);


        String loginJson = objectMapper.writeValueAsString(
                new AuthRequestDto(username, PASSWORD_EXAMPLE)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - ошибка: неверный пароль")
    void login_WrongPassword() throws Exception {
        String username = "wrong_pass_user";
        
        registerAndGetToken(username, UserRole.EXECUTOR);

        String loginJson = objectMapper.writeValueAsString(
                new AuthRequestDto(username, "wrongpassword")
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login - ошибка: пользователь не найден")
    void login_UserNotFound() throws Exception {
        String loginJson = objectMapper.writeValueAsString(
                new AuthRequestDto("nonexistent_user", PASSWORD_EXAMPLE)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/refresh - успешное обновление токена")
    void refresh_Success() throws Exception {
        String username = "refresh_test_user";
        String refreshToken = registerAndGetToken(username, UserRole.EXECUTOR);
        

        String loginJson = objectMapper.writeValueAsString(
                new AuthRequestDto(username, PASSWORD_EXAMPLE)
        );
        
        ResultActions loginResult = mockMvc.perform(post(AUTH_ENDPOINT + "/login")
                .contentType(APPLICATION_JSON)
                .content(loginJson));
        
        String loginResponse = loginResult.andReturn().getResponse().getContentAsString();
        refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        String refreshJson = objectMapper.writeValueAsString(
                new RefreshTokenRequestDto(refreshToken)
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("POST /api/auth/refresh - ошибка: невалидный токен")
    void refresh_InvalidToken() throws Exception {
        String refreshJson = objectMapper.writeValueAsString(
                new RefreshTokenRequestDto("invalid-token")
        );

        mockMvc.perform(post(AUTH_ENDPOINT + "/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isUnauthorized());
    }

    /**
     * DTO для refresh токена.
     */
    public record RefreshTokenRequestDto(String refreshToken) {}
}
