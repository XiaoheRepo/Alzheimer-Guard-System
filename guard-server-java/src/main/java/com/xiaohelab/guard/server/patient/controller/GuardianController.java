package com.xiaohelab.guard.server.patient.controller;

import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.patient.dto.InvitationCreateRequest;
import com.xiaohelab.guard.server.patient.dto.InvitationResponseRequest;
import com.xiaohelab.guard.server.patient.dto.TransferCreateRequest;
import com.xiaohelab.guard.server.patient.entity.GuardianInvitationEntity;
import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.patient.entity.GuardianTransferRequestEntity;
import com.xiaohelab.guard.server.patient.service.GuardianService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 监护成员管理：邀请 / 响应邀请 / 主监护转移 / 移除成员 / 成员列表。 */
@Tag(name = "Guardian", description = "监护关系与成员管理")
@RestController
@RequestMapping("/api/v1")
public class GuardianController {

    private final GuardianService guardianService;

    public GuardianController(GuardianService guardianService) {
        this.guardianService = guardianService;
    }

    @PostMapping("/patients/{patientId}/invitations")
    @Idempotent
    public Result<GuardianInvitationEntity> invite(@PathVariable Long patientId,
                                                    @Valid @RequestBody InvitationCreateRequest req) {
        return Result.ok(guardianService.invite(patientId, req));
    }

    @PostMapping("/invitations/{inviteId}/response")
    @Idempotent
    public Result<GuardianInvitationEntity> respond(@PathVariable String inviteId,
                                                    @Valid @RequestBody InvitationResponseRequest req) {
        return Result.ok(guardianService.respondInvitation(inviteId, req));
    }

    @GetMapping("/patients/{patientId}/guardians")
    public Result<List<GuardianRelationEntity>> members(@PathVariable Long patientId) {
        return Result.ok(guardianService.listMembers(patientId));
    }

    @DeleteMapping("/patients/{patientId}/guardians/{userId}")
    @Idempotent
    public Result<Void> removeMember(@PathVariable Long patientId, @PathVariable Long userId) {
        guardianService.removeMember(patientId, userId);
        return Result.ok();
    }

    @PostMapping("/patients/{patientId}/transfer-requests")
    @Idempotent
    public Result<GuardianTransferRequestEntity> initiateTransfer(@PathVariable Long patientId,
                                                                  @Valid @RequestBody TransferCreateRequest req) {
        return Result.ok(guardianService.initiateTransfer(patientId, req));
    }

    @PostMapping("/transfer-requests/{requestId}/response")
    @Idempotent
    public Result<GuardianTransferRequestEntity> respondTransfer(@PathVariable String requestId,
                                                                 @RequestBody Map<String, String> body) {
        return Result.ok(guardianService.respondTransfer(requestId,
                body.get("action"), body.get("reject_reason")));
    }

    @PostMapping("/transfer-requests/{requestId}/cancel")
    @Idempotent
    public Result<GuardianTransferRequestEntity> cancelTransfer(@PathVariable String requestId,
                                                                @RequestBody Map<String, String> body) {
        return Result.ok(guardianService.cancelTransfer(requestId, body.get("cancel_reason")));
    }
}
