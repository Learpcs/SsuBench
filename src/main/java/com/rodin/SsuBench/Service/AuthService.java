package com.rodin.SsuBench.Service;

import com.rodin.SsuBench.Controller.Request.RegisterRequest;
import com.rodin.SsuBench.Controller.Response.AuthResponse;
import com.rodin.SsuBench.Entity.User;
import com.rodin.SsuBench.Exception.BusinessLogicException;
import com.rodin.SsuBench.Exception.ErrorCode;
import com.rodin.SsuBench.Repository.UserRepository;
import com.rodin.SsuBench.Utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    /**
     * Регистрация нового пользователя.
     * Создает запись в БД и сразу возвращает пару токенов.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw BusinessLogicException.of(ErrorCode.USER_ALREADY_EXISTS, request.username());
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .balance(java.math.BigDecimal.ZERO)
                .isBlocked(false)
                .build();

        userRepository.save(user);

        return generateTokens(user);
    }

    /**
     * Аутентификация по логину и паролю.
     * Использует Spring Security AuthenticationManager для проверки.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(String username, String password) {
        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> BusinessLogicException.of(ErrorCode.USER_ID_NOT_FOUND, username));

            return generateTokens(user);

        } catch (org.springframework.security.core.AuthenticationException e) {
            throw BusinessLogicException.of(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * Обновление пары токенов по Refresh токену.
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshTokens(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw BusinessLogicException.of(ErrorCode.INVALID_CREDENTIALS);
        }

        String username = jwtUtils.getUsernameFromToken(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessLogicException.of(ErrorCode.USER_ID_NOT_FOUND, username));

        if (user.getIsBlocked()) {
            throw BusinessLogicException.of(ErrorCode.USER_BLOCKED, username);
        }

        return generateTokens(user);
    }

    /**
     * Приватный метод для генерации пары токенов.
     */
    private AuthResponse generateTokens(User user) {
        String role = user.getRole().name();

        String accessToken = jwtUtils.generateAccessToken(user.getUsername(), role);
        String refreshToken = jwtUtils.generateRefreshToken(user.getUsername(), role);

        return new AuthResponse(accessToken, refreshToken);
    }
}
