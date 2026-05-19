package com.attendance.backend.auth.api;

import com.attendance.backend.auth.dto.AuthDtos;
import com.attendance.backend.auth.dto.ChangePasswordRequest;
import com.attendance.backend.auth.dto.ForceChangePasswordRequest;
import com.attendance.backend.auth.dto.RegisterRequest;
import com.attendance.backend.auth.dto.RegisterResponse;
import com.attendance.backend.auth.service.AuthService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String HEADER_USER_AGENT = "User-Agent";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req,
                                        HttpServletRequest request) {
        return authService.login(req, resolveClientIp(request), resolveUserAgent(request));
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request,
                                     HttpServletRequest httpRequest) {
        return authService.register(request, resolveClientIp(httpRequest), resolveUserAgent(httpRequest));
    }


    @PostMapping("/force-change-password")
    public AuthDtos.LoginResponse forceChangePassword(@Valid @RequestBody ForceChangePasswordRequest req,
                                                      HttpServletRequest request) {
        return authService.forceChangePassword(req, resolveClientIp(request), resolveUserAgent(request));
    }

    @PostMapping("/refresh")
    public AuthDtos.LoginResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest req) {
        return authService.refresh(req);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthDtos.LogoutRequest req) {
        authService.logout(req);
    }

    @PostMapping("/forgot-password")
    public AuthDtos.ForgotPasswordResponse forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest req,
                                                          HttpServletRequest request) {
        return authService.forgotPassword(req, resolveClientIp(request), resolveUserAgent(request));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest req) {
        authService.resetPassword(req);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        authService.logoutAll(principal.getUserId());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null || principal.getUserId() == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        authService.changePassword(principal.getUserId(), request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = firstHeaderValue(request.getHeader(HEADER_X_FORWARDED_FOR));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        String realIp = firstHeaderValue(request.getHeader(HEADER_X_REAL_IP));
        if (realIp != null) {
            return realIp;
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return null;
        }
        return remoteAddr.trim();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader(HEADER_USER_AGENT);
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        return userAgent.trim();
    }

    private String firstHeaderValue(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            return null;
        }

        String first = rawHeader.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }
}