package com.rodin.SsuBench.Controller;

import com.rodin.SsuBench.Config.UserDetailsImpl;
import com.rodin.SsuBench.Controller.Response.PaymentResponse;
import com.rodin.SsuBench.Controller.Response.Task.TaskResponse;
import com.rodin.SsuBench.Entity.Task;
import com.rodin.SsuBench.Service.PaymentService;
import com.rodin.SsuBench.Service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskCompletionController {

    private final PaymentService paymentService;
    private final TaskService taskService;

    /**
     * Исполнитель помечает задачу как выполненную.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> completeTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Task task = paymentService.completeTask(id, userDetails.getId());
        TaskResponse response = taskService.getTask(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Заказчик подтверждает выполнение задачи (перевод баллов).
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirmTaskCompletion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        PaymentResponse response = paymentService.confirmTaskCompletion(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Получить платеж по задаче.
     */
    @GetMapping("/{id}/payment")
    public ResponseEntity<PaymentResponse> getPaymentByTask(@PathVariable Long id) {
        PaymentResponse response = paymentService.getPaymentByTask(id);
        return ResponseEntity.ok(response);
    }
}
