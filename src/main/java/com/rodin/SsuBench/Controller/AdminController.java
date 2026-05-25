package com.rodin.SsuBench.Controller;

import com.rodin.SsuBench.Config.UserDetailsImpl;
import com.rodin.SsuBench.Controller.Response.PageResponse;
import com.rodin.SsuBench.Controller.Response.UserResponse;
import com.rodin.SsuBench.Entity.UserRole;
import com.rodin.SsuBench.Service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<UserResponse> response = adminService.getAllUsers(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<PageResponse<UserResponse>> getUsersByRole(
            @PathVariable UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<UserResponse> response = adminService.getUsersByRole(role, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        UserResponse response = adminService.getUser(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/block")
    public ResponseEntity<UserResponse> blockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UserResponse response = adminService.blockUser(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/unblock")
    public ResponseEntity<UserResponse> unblockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        UserResponse response = adminService.unblockUser(id, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
