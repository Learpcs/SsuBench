package com.rodin.SsuBench.Service;

import com.rodin.SsuBench.Controller.Request.Task.CreateTaskRequest;
import com.rodin.SsuBench.Controller.Request.Task.UpdateTaskRequest;
import com.rodin.SsuBench.Controller.Response.PageResponse;
import com.rodin.SsuBench.Controller.Response.Task.TaskResponse;
import com.rodin.SsuBench.Entity.*;
import com.rodin.SsuBench.Exception.BusinessLogicException;
import com.rodin.SsuBench.Exception.ErrorCode;
import com.rodin.SsuBench.Repository.TaskRepository;
import com.rodin.SsuBench.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_ID_NOT_FOUND, "Заказчик не найден"));

        if (customer.getRole() != UserRole.CUSTOMER && customer.getRole() != UserRole.ADMIN) {
            throw new BusinessLogicException(ErrorCode.INVALID_ROLE, "Только заказчик может создавать задачи");
        }

        if (customer.getIsBlocked()) {
            throw new BusinessLogicException(ErrorCode.ACCOUNT_BLOCKED, "Аккаунт заблокирован");
        }

        Task task = Task.builder()
                .description(request.description())
                .customer(customer)
                .reward(request.reward())
                .status(TaskStatus.OPEN)
                .build();

        taskRepository.save(task);

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(Long taskId) {
        Task task = taskRepository.findByIdWithBid(taskId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.TASK_ID_NOT_FOUND, "Задача не найдена"));

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getAllTasks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Task> taskPage = taskRepository.findAll(pageable);

        return toPageResponse(taskPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasksByStatus(TaskStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Task> taskPage = taskRepository.findByStatus(status, pageable);

        return toPageResponse(taskPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getTasksByCustomer(Long customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Task> taskPage = taskRepository.findByCustomerId(customerId, pageable);

        return toPageResponse(taskPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> getAvailableTasksForExecutor(Long excludeCustomerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Task> taskPage = taskRepository.findByStatusAndCustomerIdNot(TaskStatus.OPEN, excludeCustomerId, pageable);

        return toPageResponse(taskPage);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Long customerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.TASK_ID_NOT_FOUND, "Задача не найдена"));


        if (!task.getCustomer().getId().equals(customerId)) {
            throw new BusinessLogicException(ErrorCode.ONLY_CUSTOMER_CAN_MODIFY_TASK, "Только заказчик может изменять эту задачу");
        }


        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessLogicException(ErrorCode.CANNOT_MODIFY_TASK_IN_PROGRESS, "Нельзя изменить задачу со статусом " + task.getStatus());
        }

        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.reward() != null) {
            task.setReward(request.reward());
        }

        taskRepository.save(task);

        return toResponse(task);
    }

    @Transactional
    public TaskResponse cancelTask(Long taskId, Long customerId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.TASK_ID_NOT_FOUND, "Задача не найдена"));


        if (!task.getCustomer().getId().equals(customerId)) {
            throw new BusinessLogicException(ErrorCode.ONLY_CUSTOMER_CAN_MODIFY_TASK, "Только заказчик может отменять задачу");
        }


        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CONFIRMED) {
            throw new BusinessLogicException(ErrorCode.CANNOT_CANCEL_COMPLETED_TASK, "Нельзя отменить выполненную задачу");
        }

        task.setStatus(TaskStatus.CANCELLED);
        taskRepository.save(task);

        return toResponse(task);
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getDescription(),
                task.getCustomer().getId(),
                task.getCustomer().getUsername(),
                task.getStatus(),
                task.getReward(),
                task.getAcceptedBid() != null ? task.getAcceptedBid().getId() : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private PageResponse<TaskResponse> toPageResponse(Page<Task> page) {
        return PageResponse.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
