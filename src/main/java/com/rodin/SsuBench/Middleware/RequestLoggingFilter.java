package com.rodin.SsuBench.Middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Фильтр для:
 * - Генерации request_id для каждого запроса
 * - Логгирования входящих запросов и исходящих ответов
 * - Recover от необработанных исключений
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }


        MDC.put(REQUEST_ID_MDC, requestId);


        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, -1);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);


        responseWrapper.setHeader(REQUEST_ID_HEADER, requestId);

        try {

            logRequest(requestWrapper);


            filterChain.doFilter(requestWrapper, responseWrapper);


            logResponse(responseWrapper);

        } catch (Exception e) {

            log.error("Необработанное исключение для запроса {} {}", 
                    request.getMethod(), request.getRequestURI(), e);
            throw e;
        } finally {

            MDC.remove(REQUEST_ID_MDC);

            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String requestBody = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.info("Входящий запрос: {} {} | RequestID: {} | Body: {}",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get(REQUEST_ID_MDC),
                requestBody.isEmpty() ? "-" : requestBody);
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        String responseBody = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.info("Исходящий ответ: {} | Status: {} | Body: {}",
                response.getStatus(),
                MDC.get(REQUEST_ID_MDC),
                responseBody.isEmpty() ? "-" : responseBody);
    }
}
