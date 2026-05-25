package com.rodin.SsuBench.Service;

import com.rodin.SsuBench.Controller.Request.Bid.CreateBidRequest;
import com.rodin.SsuBench.Controller.Response.Bid.BidResponse;
import com.rodin.SsuBench.Controller.Response.PageResponse;
import com.rodin.SsuBench.Entity.*;
import com.rodin.SsuBench.Exception.BusinessLogicException;
import com.rodin.SsuBench.Exception.ErrorCode;
import com.rodin.SsuBench.Repository.BidRepository;
import com.rodin.SsuBench.Repository.TaskRepository;
import com.rodin.SsuBench.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public BidResponse createBid(CreateBidRequest request, Long executorId, Long taskId) {
        User executor = userRepository.findById(executorId)
                .orElseThrow(() -> BusinessLogicException.of(ErrorCode.USER_ID_NOT_FOUND, executorId));

        if (executor.getRole() != UserRole.EXECUTOR) {
            throw BusinessLogicException.of(ErrorCode.ONLY_EXECUTOR_CAN_BID);
        }

        if (executor.getIsBlocked()) {
            throw BusinessLogicException.of(ErrorCode.ACCOUNT_BLOCKED, executor.getUsername());
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> BusinessLogicException.of(ErrorCode.TASK_ID_NOT_FOUND, taskId));


        if (task.getCustomer().getId().equals(executorId)) {
            throw BusinessLogicException.of(ErrorCode.CANNOT_BID_ON_OWN_TASK, "Нельзя откликнуться на собственную задачу");
        }


        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessLogicException(ErrorCode.CANNOT_MODIFY_TASK_IN_PROGRESS, "Нельзя откликнуться на задачу со статусом " + task.getStatus());
        }


        if (bidRepository.existsByTaskIdAndExecutorIdAndStatus(taskId, executorId, BidStatus.PENDING)) {
            throw new BusinessLogicException(ErrorCode.BID_ALREADY_EXISTS, "Вы уже откликнулись на эту задачу");
        }

        Bid bid = Bid.builder()
                .executor(executor)
                .task(task)
                .description(request.description())
                .status(BidStatus.PENDING)
                .build();

        bidRepository.save(bid);

        return toResponse(bid);
    }

    @Transactional(readOnly = true)
    public BidResponse getBid(Long bidId) {
        Bid bid = bidRepository.findByIdWithExecutor(bidId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.BID_NOT_FOUND, "Отклик не найден"));

        return toResponse(bid);
    }

    @Transactional(readOnly = true)
    public PageResponse<BidResponse> getBidsByTask(Long taskId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Bid> bidPage = bidRepository.findByTaskId(taskId, pageable);

        return toPageResponse(bidPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<BidResponse> getBidsByExecutor(Long executorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Bid> bidPage = bidRepository.findByExecutorId(executorId, pageable);

        return toPageResponse(bidPage);
    }

    @Transactional
    public BidResponse acceptBid(Long bidId, Long customerId) {
        Bid bid = bidRepository.findByIdWithExecutor(bidId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.BID_NOT_FOUND, "Отклик не найден"));

        Task task = bid.getTask();


        if (!task.getCustomer().getId().equals(customerId)) {
            throw new BusinessLogicException(ErrorCode.ONLY_CUSTOMER_CAN_MODIFY_TASK, "Только заказчик может принять отклик");
        }


        if (task.getAcceptedBid() != null) {
            throw new BusinessLogicException(ErrorCode.TASK_ALREADY_HAS_ACCEPTED_BID, "У задачи уже есть принятый исполнитель");
        }


        if (task.getStatus() != TaskStatus.OPEN) {
            throw new BusinessLogicException(ErrorCode.CANNOT_MODIFY_TASK_IN_PROGRESS, "Нельзя принять отклик для задачи со статусом " + task.getStatus());
        }


        bidRepository.findByTaskIdAndStatus(task.getId(), BidStatus.PENDING).stream()
                .filter(b -> !b.getId().equals(bidId))
                .forEach(b -> {
                    b.setStatus(BidStatus.REJECTED);
                    bidRepository.save(b);
                });


        bid.setStatus(BidStatus.ACCEPTED);
        task.setAcceptedBid(bid);
        task.setStatus(TaskStatus.IN_PROGRESS);

        bidRepository.save(bid);
        taskRepository.save(task);

        return toResponse(bid);
    }

    @Transactional
    public BidResponse rejectBid(Long bidId, Long customerId) {
        Bid bid = bidRepository.findByIdWithExecutor(bidId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.BID_NOT_FOUND, "Отклик не найден"));

        Task task = bid.getTask();


        if (!task.getCustomer().getId().equals(customerId)) {
            throw new BusinessLogicException(ErrorCode.ONLY_CUSTOMER_CAN_MODIFY_TASK, "Только заказчик может отклонить отклик");
        }


        if (bid.getStatus() != BidStatus.PENDING) {
            throw new BusinessLogicException(ErrorCode.BID_NOT_ACCEPTED, "Можно отклонить только отклик в статусе PENDING");
        }

        bid.setStatus(BidStatus.REJECTED);
        bidRepository.save(bid);

        return toResponse(bid);
    }

    @Transactional
    public BidResponse cancelBid(Long bidId, Long executorId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BusinessLogicException(ErrorCode.BID_NOT_FOUND, "Отклик не найден"));


        if (!bid.getExecutor().getId().equals(executorId)) {
            throw new BusinessLogicException(ErrorCode.ONLY_EXECUTOR_CAN_BID, "Только автор отклика может его отменить");
        }


        if (bid.getStatus() != BidStatus.PENDING) {
            throw new BusinessLogicException(ErrorCode.BID_NOT_ACCEPTED, "Можно отменить только отклик в статусе PENDING");
        }

        bid.setStatus(BidStatus.CANCELLED);
        bidRepository.save(bid);

        return toResponse(bid);
    }

    private BidResponse toResponse(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getExecutor().getId(),
                bid.getExecutor().getUsername(),
                bid.getTask().getId(),
                bid.getDescription(),
                bid.getStatus(),
                bid.getCreatedAt()
        );
    }

    private PageResponse<BidResponse> toPageResponse(Page<Bid> page) {
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
