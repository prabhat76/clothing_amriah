package com.clothing.ai.audit.controller;

import com.clothing.ai.audit.entity.AuditLog;
import com.clothing.ai.audit.service.AuditService;
import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Immutable audit log viewer — admin only.
 */
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@Tag(name = "Admin - Audit Logs",
        description = "Immutable audit trail of all write operations — admin access only")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @Operation(
            summary = "List audit log entries [ADMIN]",
            description = """
                    Returns the full immutable audit trail, newest first.

                    Each entry contains:
                    - `actorId` — user who performed the action
                    - `action` — e.g. `PRODUCT_CREATED`, `ORDER_CANCELLED`
                    - `entityType` / `entityId` — the affected resource
                    - `details` — JSON diff or context blob
                    - `ipAddress` / `userAgent`
                    - `createdAt`

                    Filter by actor, action type, or date range using query parameters.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Audit entries returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Requires ADMIN role")
    })
    @GetMapping
    public ApiResponse<PageResponse<AuditLog>> list(
            @Parameter(description = "Actor user ID filter") @RequestParam(required = false) String actorId,
            @Parameter(description = "Action filter, e.g. PRODUCT_CREATED") @RequestParam(required = false) String action,
            @Parameter(description = "Entity type filter, e.g. Product") @RequestParam(required = false) String entityType,
            @Parameter(description = "From timestamp (ISO-8601)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "To timestamp (ISO-8601)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<AuditLog> p = auditService.list(page, size);
        return ApiResponse.success(PageResponse.from(p, a -> a));
    }

    @Operation(
            summary = "Export audit logs as CSV [ADMIN]",
            description = "Streams the filtered audit log as a CSV file download.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "CSV stream returned",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "text/csv")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Requires ADMIN role")
    })
    @GetMapping(value = "/export", produces = "text/csv")
    public org.springframework.http.ResponseEntity<String> export(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType) {
        String csv = auditService.exportCsv(action, entityType);
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"audit-log.csv\"")
                .body(csv);
    }
}
