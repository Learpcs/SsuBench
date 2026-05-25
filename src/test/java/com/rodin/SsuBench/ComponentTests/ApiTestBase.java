package com.rodin.SsuBench.ComponentTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rodin.SsuBench.Entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Базовый класс для компонентных тестов API.
 * Поднимает полный Spring контекст, PostgreSQL базу данных (Testcontainers) и MockMvc для HTTP запросов.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
public abstract class ApiTestBase extends PostgresTestContainer {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String AUTH_ENDPOINT = "/api/auth";
    protected static final String TASKS_ENDPOINT = "/api/tasks";
    protected static final String BIDS_ENDPOINT = "/api/bids";
    protected static final String ADMIN_ENDPOINT = "/api/admin";
    protected static final String PASSWORD_EXAMPLE = "asdjlkfjdaslkfjA%123";

    /**
     * Генерирует уникальное имя для тестового пользователя.
     */
    protected String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Регистрация пользователя и возврат токена.
     */
    protected String registerAndGetToken(String username, UserRole role) throws Exception {
        String registerJson = objectMapper.writeValueAsString(new RegisterRequestDto(username, PASSWORD_EXAMPLE, role));
        
        mockMvc.perform(post(AUTH_ENDPOINT + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        return loginAndGetToken(username);
    }

    /**
     * Вход и получение access токена.
     */
    protected String loginAndGetToken(String username) throws Exception {
        String loginJson = objectMapper.writeValueAsString(new AuthRequestDto(username, PASSWORD_EXAMPLE));
        
        ResultActions result = mockMvc.perform(post(AUTH_ENDPOINT + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(MockMvcResultMatchers.status().isOk());
        
        String response = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    /**
     * DTO для регистрации (используется только в тестах).
     */
    public record RegisterRequestDto(String username, String password, UserRole role) {}

    /**
     * DTO для входа (используется только в тестах).
     */
    public record AuthRequestDto(String username, String password) {}
}
