package com.gametrade.user.controller;

import com.gametrade.common.api.ApiResponse;
import com.gametrade.common.web.GatewayHeaders;
import com.gametrade.user.dto.LoginRequest;
import com.gametrade.user.dto.LoginResponse;
import com.gametrade.user.dto.RegisterRequest;
import com.gametrade.user.dto.UserProfileResponse;
import com.gametrade.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    /**
     * Current user's profile. The gateway injects {@code X-User-Id} after
     * validating the JWT, so no token parsing happens here.
     */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@RequestHeader(GatewayHeaders.USER_ID) Long userId) {
        return ApiResponse.success(userService.getProfile(userId));
    }
}
