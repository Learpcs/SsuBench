package com.rodin.SsuBench.Controller;

import com.rodin.SsuBench.Config.UserDetailsImpl;
import com.rodin.SsuBench.Controller.Request.Task.CreateTaskRequest;
import com.rodin.SsuBench.Controller.Request.Task.UpdateTaskRequest;
import com.rodin.SsuBench.Controller.Response.PageResponse;
import com.rodin.SsuBench.Controller.Response.Task.TaskResponse;
import com.rodin.SsuBench.Entity.TaskStatus;
import com.rodin.SsuBench.Service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        TaskResponse response = taskService.createTask(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id) {
        TaskResponse response = taskService.getTask(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<TaskResponse>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<TaskResponse> response = taskService.getAllTasks(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<PageResponse<TaskResponse>> getTasksByStatus(
            @PathVariable TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<TaskResponse> response = taskService.getTasksByStatus(status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<TaskResponse>> getMyTasks(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<TaskResponse> response = taskService.getTasksByCustomer(userDetails.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<PageResponse<TaskResponse>> getAvailableTasks(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<TaskResponse> response = taskService.getAvailableTasksForExecutor(userDetails.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        TaskResponse response = taskService.updateTask(id, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<TaskResponse> cancelTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        TaskResponse response = taskService.cancelTask(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
