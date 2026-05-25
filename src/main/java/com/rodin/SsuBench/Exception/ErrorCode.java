package com.rodin.SsuBench.Exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_CREDENTIALS("AUTH_001", "Неверный логин или пароль", HttpStatus.UNAUTHORIZED),
    ACCOUNT_BLOCKED("AUTH_002", "Аккаунт %s заблокирован", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("AUTH_003", "Токен недействителен или истек", HttpStatus.UNAUTHORIZED),
    USERNAME_NOT_FOUND("AUTH_004", "Пользователь %s не найден", HttpStatus.NOT_FOUND),
    USER_ID_NOT_FOUND("AUTH_005", "Пользователь %s не найден", HttpStatus.NOT_FOUND),
    CANT_BLOCK_OWN_ACCOUNT("AUTH_006", "Нельзя заблокировать свой аккаунт", HttpStatus.BAD_REQUEST),

    USER_ALREADY_EXISTS("USER_001", "Пользователь с таким именем уже существует", HttpStatus.CONFLICT),
    INVALID_ROLE("USER_002", "Недопустимая роль %s", HttpStatus.BAD_REQUEST),
    USER_BLOCKED("USER_003", "Пользователь %s заблокирован", HttpStatus.FORBIDDEN),

    TASK_ID_NOT_FOUND("TASK_001", "Задача %s не найдена", HttpStatus.NOT_FOUND),
    INSUFFICIENT_FUNDS("TASK_002", "Недостаточно средств на балансе", HttpStatus.BAD_REQUEST),
    CANNOT_CANCEL_COMPLETED_TASK("TASK_003", "Нельзя отменить выполненную задачу", HttpStatus.BAD_REQUEST),
    TASK_ALREADY_HAS_ACCEPTED_BID("TASK_004", "У %s задачи уже есть принятый исполнитель", HttpStatus.CONFLICT),
    CANNOT_MODIFY_TASK_IN_PROGRESS("TASK_005", "Нельзя изменить задачу %s со статусом IN_PROGRESS или выше", HttpStatus.BAD_REQUEST),
    ONLY_CUSTOMER_CAN_MODIFY_TASK("TASK_006", "Только заказчик может изменять эту задачу", HttpStatus.FORBIDDEN),

    BID_NOT_FOUND("BID_001", "Отклик %s не найден", HttpStatus.NOT_FOUND),
    BID_ALREADY_EXISTS("BID_002", "Вы уже откликнулись на эту задачу", HttpStatus.CONFLICT),
    CANNOT_BID_ON_OWN_TASK("BID_003", "Нельзя откликнуться на собственную задачу", HttpStatus.BAD_REQUEST),
    ONLY_EXECUTOR_CAN_BID("BID_004", "Только исполнитель может откликаться на задачи", HttpStatus.FORBIDDEN),
    BID_NOT_ACCEPTED("BID_005", "Отклик не был принят", HttpStatus.BAD_REQUEST),
    ONLY_SELECTED_EXECUTOR_CAN_COMPLETE("BID_006", "Только выбранный исполнитель может завершить задачу", HttpStatus.FORBIDDEN),
    TASK_NOT_IN_PROGRESS("BID_007", "Задача не в статусе IN_PROGRESS", HttpStatus.BAD_REQUEST),

    PAYMENT_NOT_FOUND("PAYMENT_001", "Платеж не найден", HttpStatus.NOT_FOUND),
    PAYMENT_ALREADY_EXISTS("PAYMENT_002", "Платеж для этой задачи уже существует", HttpStatus.CONFLICT),

    INTERNAL_SERVER_ERROR("SYS_001", "Внутренняя ошибка сервера", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
