package com.rodin.SsuBench.Service;

import com.rodin.SsuBench.Controller.Response.PaymentResponse;
import com.rodin.SsuBench.Entity.*;
import com.rodin.SsuBench.Exception.BusinessLogicException;
import com.rodin.SsuBench.Exception.ErrorCode;
import com.rodin.SsuBench.Repository.BidRepository;
import com.rodin.SsuBench.Repository.PaymentRepository;
import com.rodin.SsuBench.Repository.TaskRepository;
import com.rodin.SsuBench.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TaskRepository taskRepository;
    private final BidRepository bidRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    /**
     * Исполнитель помечает задачу как выполненную.
     * Может сделать только выбранный исполнитель.
     */
    @Transactional
    public Task completeTask(Long taskId, Long executorId) {
        Task task = taskRepository.findByIdWithBid(taskId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.TASK_ID_NOT_FOUND, "Задача не найдена"));


        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new BusinessLogicException(ErrorCode.TASK_NOT_IN_PROGRESS, "Задача не в статусе IN_PROGRESS");
        }


        Bid acceptedBid = task.getAcceptedBid();
        if (acceptedBid == null || !acceptedBid.getExecutor().getId().equals(executorId)) {
            throw new BusinessLogicException(ErrorCode.ONLY_SELECTED_EXECUTOR_CAN_COMPLETE, "Только выбранный исполнитель может завершить задачу");
        }

        task.setStatus(TaskStatus.COMPLETED);
        return taskRepository.save(task);
    }

    /**
     * Заказчик подтверждает выполнение задачи.
     * Происходит атомарный перевод баллов.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentResponse confirmTaskCompletion(Long taskId, Long customerId) {
        Task task = taskRepository.findByIdWithBid(taskId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.TASK_ID_NOT_FOUND, "Задача не найдена"));


        if (!task.getCustomer().getId().equals(customerId)) {
            throw new BusinessLogicException(ErrorCode.ONLY_CUSTOMER_CAN_MODIFY_TASK, "Только заказчик может подтвердить выполнение");
        }


        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new BusinessLogicException(ErrorCode.TASK_NOT_IN_PROGRESS, "Задача не в статусе COMPLETED");
        }


        if (paymentRepository.existsByTaskId(taskId)) {
            throw new BusinessLogicException(ErrorCode.PAYMENT_ALREADY_EXISTS, "Платеж для этой задачи уже существует");
        }

        Bid acceptedBid = task.getAcceptedBid();
        if (acceptedBid == null) {
            throw new BusinessLogicException(ErrorCode.BID_NOT_FOUND, "У задачи нет принятого отклика");
        }

        User customer = task.getCustomer();
        User executor = acceptedBid.getExecutor();


        if (customer.getBalance().compareTo(task.getReward()) < 0) {
            throw new BusinessLogicException(ErrorCode.INSUFFICIENT_FUNDS, "Недостаточно средств на балансе");
        }


        customer.setBalance(customer.getBalance().subtract(task.getReward()));
        executor.setBalance(executor.getBalance().add(task.getReward()));

        userRepository.save(customer);
        userRepository.save(executor);


        Payment payment = Payment.builder()
                .task(task)
                .bid(acceptedBid)
                .amount(task.getReward())
                .build();

        paymentRepository.save(payment);


        task.setStatus(TaskStatus.CONFIRMED);
        taskRepository.save(task);

        return new PaymentResponse(
                payment.getId(),
                task.getId(),
                acceptedBid.getId(),
                payment.getAmount(),
                payment.getProcessedAt()
        );
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTask(Long taskId) {
        Payment payment = paymentRepository.findByTaskId(taskId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.PAYMENT_NOT_FOUND, "Платеж не найден"));

        return new PaymentResponse(
                payment.getId(),
                payment.getTask().getId(),
                payment.getBid().getId(),
                payment.getAmount(),
                payment.getProcessedAt()
        );
    }
}
