package com.rodin.SsuBench.Service;

import com.rodin.SsuBench.Controller.Response.PageResponse;
import com.rodin.SsuBench.Controller.Response.UserResponse;
import com.rodin.SsuBench.Entity.User;
import com.rodin.SsuBench.Entity.UserRole;
import com.rodin.SsuBench.Exception.BusinessLogicException;
import com.rodin.SsuBench.Exception.ErrorCode;
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
public class AdminService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findAll(pageable);

        return toPageResponse(userPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersByRole(UserRole role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findByRole(role, pageable);

        return toPageResponse(userPage);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessLogicException.of(ErrorCode.USER_ID_NOT_FOUND,  userId));

        return toResponse(user);
    }

    @Transactional
    public UserResponse blockUser(Long userId, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessLogicException.of(ErrorCode.USER_ID_NOT_FOUND, userId));

        if (user.getId().equals(adminId)) {
            throw BusinessLogicException.of(ErrorCode.CANT_BLOCK_OWN_ACCOUNT);
        }

        user.setIsBlocked(true);
        userRepository.save(user);

        return toResponse(user);
    }

    @Transactional
    public UserResponse unblockUser(Long userId, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessLogicException.of(ErrorCode.USER_ID_NOT_FOUND, userId));

        user.setIsBlocked(false);
        userRepository.save(user);

        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getBalance(),
                user.getIsBlocked(),
                user.getCreatedAt()
        );
    }

    private PageResponse<UserResponse> toPageResponse(Page<User> page) {
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
