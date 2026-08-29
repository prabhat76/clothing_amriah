package com.clothing.ai.notification.controller;

import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.common.response.PageResponse;
import com.clothing.ai.notification.dto.NotificationDtos.*;
import com.clothing.ai.notification.service.NotificationService;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * In-app notification inbox.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification inbox: list, mark as read, unread count")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService service;

    @Operation(
            summary = "List my notifications",
            description = "Returns the authenticated user's notification inbox, newest first.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Notification list returned")
    })
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.my(SecurityUtils.currentUserId(), page, size));
    }

    @Operation(
            summary = "Get unread notification count",
            description = "Returns the count of unread notifications (for badge display).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Unread count returned")
    })
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.success(service.unreadCount(SecurityUtils.currentUserId()));
    }

    @Operation(
            summary = "Mark a notification as read",
            description = "Sets `readAt` on the given notification. Idempotent.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Notification marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Notification not found or belongs to another user")
    })
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @Parameter(description = "Notification UUID") @PathVariable UUID id) {
        service.markRead(id, SecurityUtils.currentUserId());
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Bulk-marks all unread notifications for the authenticated user as read.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "All notifications marked as read")
    })
    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        service.markAllRead(SecurityUtils.currentUserId());
        return ApiResponse.success("All notifications marked as read", null);
    }

    @Operation(
            summary = "Delete a notification",
            description = "Permanently removes a single notification from the inbox.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Notification deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Notification not found")
    })
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "Notification UUID") @PathVariable UUID id) {
        service.delete(id, SecurityUtils.currentUserId());
        return ApiResponse.success("Notification deleted", null);
    }
}
