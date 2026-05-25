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
 * Компонентные тесты для Bid API.
 * Тестирует: create, get, list, accept, reject, cancel
 */
class BidApiTests extends ApiTestBase {

    private String customerToken;
    private String executorToken;
    private String executor2Token;
    private Long taskId;
    private String customerUsername;
    private String executorUsername;

    @BeforeEach
    void setUp() throws Exception {
        customerUsername = uniqueUsername("customer");
        executorUsername = uniqueUsername("executor");
        customerToken = registerAndGetToken(customerUsername, UserRole.CUSTOMER);
        executorToken = registerAndGetToken(executorUsername, UserRole.EXECUTOR);
        executor2Token = registerAndGetToken(uniqueUsername("executor2"), UserRole.EXECUTOR);
        

        taskId = createTestTask(customerToken, "Задача для откликов", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("POST /api/bids/task/{id} - успешное создание отклика")
    void createBid_Success() throws Exception {
        String bidJson = objectMapper.writeValueAsString(
                new CreateBidRequestDto("Готов выполнить эту задачу")
        );

        ResultActions result = mockMvc.perform(post(BIDS_ENDPOINT + "/task/" + taskId)
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(APPLICATION_JSON)
                        .content(bidJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Готов выполнить эту задачу"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.executorUsername").value(executorUsername));

        String response = result.andReturn().getResponse().getContentAsString();
        assertThat(response).contains("PENDING", executorUsername);
    }

    @Test
    @DisplayName("POST /api/bids/task/{id} - ошибка: отклик на свою задачу")
    void createBid_OwnTask() throws Exception {

        String bidJson = objectMapper.writeValueAsString(
                new CreateBidRequestDto("Отклик заказчика")
        );

        mockMvc.perform(post(BIDS_ENDPOINT + "/task/" + taskId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(APPLICATION_JSON)
                        .content(bidJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/bids/task/{id} - ошибка: пустое описание")
    void createBid_EmptyDescription() throws Exception {
        String bidJson = objectMapper.writeValueAsString(
                new CreateBidRequestDto("")
        );

        mockMvc.perform(post(BIDS_ENDPOINT + "/task/" + taskId)
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(APPLICATION_JSON)
                        .content(bidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/bids/task/{id} - ошибка: повторный отклик")
    void createBid_DuplicateBid() throws Exception {
        String bidJson = objectMapper.writeValueAsString(
                new CreateBidRequestDto("Первый отклик")
        );


        mockMvc.perform(post(BIDS_ENDPOINT + "/task/" + taskId)
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(APPLICATION_JSON)
                        .content(bidJson))
                .andExpect(status().isOk());


        String bidJson2 = objectMapper.writeValueAsString(
                new CreateBidRequestDto("Второй отклик")
        );

        mockMvc.perform(post(BIDS_ENDPOINT + "/task/" + taskId)
                        .header("Authorization", "Bearer " + executorToken)
                        .contentType(APPLICATION_JSON)
                        .content(bidJson2))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/bids/{id} - успешное получение отклика")
    void getBid_Success() throws Exception {
        Long bidId = createTestBid(executorToken, taskId, "Тестовый отклик");

        mockMvc.perform(get(BIDS_ENDPOINT + "/" + bidId)
                        .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bidId))
                .andExpect(jsonPath("$.description").value("Тестовый отклик"));
    }

    @Test
    @DisplayName("GET /api/bids/task/{id} - список откликов на задачу")
    void getBidsByTask_Success() throws Exception {
        createTestBid(executorToken, taskId, "Отклик 1");
        createTestBid(executor2Token, taskId, "Отклик 2");

        mockMvc.perform(get(BIDS_ENDPOINT + "/task/" + taskId)
                        .header("Authorization", "Bearer " + customerToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/bids/my - мои отклики исполнителя")
    void getMyBids_Success() throws Exception {
        createTestBid(executorToken, taskId, "Мой отклик");

        mockMvc.perform(get(BIDS_ENDPOINT + "/my")
                        .header("Authorization", "Bearer " + executorToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("POST /api/bids/{id}/accept - успешное принятие отклика заказчиком")
    void acceptBid_Success() throws Exception {
        Long bidId = createTestBid(executorToken, taskId, "Отклик для принятия");

        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId + "/accept")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("POST /api/bids/{id}/accept - ошибка: не заказчик принимает")
    void acceptBid_NotCustomer() throws Exception {
        Long bidId = createTestBid(executorToken, taskId, "Отклик");

        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId + "/accept")
                        .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/bids/{id}/accept - ошибка: уже есть принятый отклик")
    void acceptBid_AlreadyAccepted() throws Exception {
        Long bidId1 = createTestBid(executorToken, taskId, "Отклик 1");
        Long bidId2 = createTestBid(executor2Token, taskId, "Отклик 2");


        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId1 + "/accept")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());


        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId2 + "/accept")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/bids/{id}/reject - успешное отклонение отклика")
    void rejectBid_Success() throws Exception {
        Long bidId = createTestBid(executorToken, taskId, "Отклик для отклонения");

        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId + "/reject")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("POST /api/bids/{id}/cancel - успешная отмена отклика исполнителем")
    void cancelBid_Success() throws Exception {
        Long bidId = createTestBid(executorToken, taskId, "Отклик для отмены");

        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId + "/cancel")
                        .header("Authorization", "Bearer " + executorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/bids/{id}/cancel - ошибка: не автор отклика отменяет")
    void cancelBid_NotAuthor() throws Exception {
        Long bidId = createTestBid(executorToken, taskId, "Отклик");

        mockMvc.perform(post(BIDS_ENDPOINT + "/" + bidId + "/cancel")
                        .header("Authorization", "Bearer " + executor2Token))
                .andExpect(status().isForbidden());
    }

    /**
     * Вспомогательный метод для создания тестовой задачи.
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

    /**
     * Вспомогательный метод для создания тестового отклика.
     */
    private Long createTestBid(String token, Long taskId, String description) throws Exception {
        String bidJson = objectMapper.writeValueAsString(
                new CreateBidRequestDto(description)
        );

        ResultActions result = mockMvc.perform(post(BIDS_ENDPOINT + "/task/" + taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(bidJson))
                .andExpect(status().isOk());

        String response = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    /**
     * DTO для создания отклика.
     */
    public record CreateBidRequestDto(String description) {}
}
