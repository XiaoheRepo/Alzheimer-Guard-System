package com.xiaohelab.guard.server.interfaces.profile;

import com.xiaohelab.guard.server.application.guardian.GuardianInvitationService;
import com.xiaohelab.guard.server.application.patient.PatientProfileService;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.GuardianInvitationDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientProfileDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysLogDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserPatientDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagAssetDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.GuardianInvitationMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysLogMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.TagAssetMapper;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 患者档案与监护人管理接口。
 * 路由前缀 /api/v1/patients，包含：
 *   - 档案 CRUD + 围栏配置
 *   - 监护人邀请、响应、移除
 *   - 主监护人转移（发起/确认/取消）
 * 附加：GET /api/v1/users/lookup（邀请前置查询）
 */
@RestController
@RequiredArgsConstructor
public class PatientController {

    private final PatientProfileService patientService;
    private final GuardianInvitationService invitationService;
    private final SysUserMapper sysUserMapper;
    private final SysUserPatientMapper sysUserPatientMapper;
    private final GuardianInvitationMapper invitationMapper;
    private final TagAssetMapper tagAssetMapper;
    private final SysLogMapper sysLogMapper;
    private final SecurityContext securityContext;

    // ===== 患者档案 =====

    /** 创建患者档案 */
    @PostMapping("/api/v1/patients")
    public ApiResponse<Map<String, Object>> createPatient(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody CreatePatientRequest req) {

        Long userId = securityContext.currentUserId();
        PatientProfileDO profile = patientService.createPatient(
                userId, req.getName(), req.getGender(),
                req.getBirthday() != null ? LocalDate.parse(req.getBirthday()) : null,
                req.getPhotoUrl(), req.getMedicalHistory(), req.getPinCode());

        return ApiResponse.ok(buildProfileVO(profile), traceId);
    }

    /** 查询患者档案 */
    @GetMapping("/api/v1/patients/{patientId}")
    public ApiResponse<Map<String, Object>> getPatient(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        PatientProfileDO profile = patientService.getPatient(patientId, userId, securityContext.isAdmin());
        return ApiResponse.ok(buildProfileVO(profile), traceId);
    }

    /** 查询当前用户关联的患者列表 */
    @GetMapping("/api/v1/patients")
    public ApiResponse<List<Map<String, Object>>> listMyPatients(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        List<PatientProfileDO> list = patientService.listMyPatients(userId);
        List<Map<String, Object>> vos = list.stream().map(this::buildProfileVO).toList();
        return ApiResponse.ok(vos, traceId);
    }

    /** 更新患者档案 */
    @PutMapping("/api/v1/patients/{patientId}")
    public ApiResponse<Map<String, Object>> updatePatient(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody UpdatePatientRequest req) {

        Long userId = securityContext.currentUserId();
        PatientProfileDO profile = patientService.updatePatient(
                patientId, userId, securityContext.isAdmin(),
                req.getName(), req.getGender(),
                req.getBirthday() != null ? LocalDate.parse(req.getBirthday()) : null,
                req.getPhotoUrl(), req.getMedicalHistory());
        return ApiResponse.ok(buildProfileVO(profile), traceId);
    }

    // ===== 围栏配置 =====

    /** 获取围栏配置 */
    @GetMapping("/api/v1/patients/{patientId}/fence")
    public ApiResponse<Map<String, Object>> getFence(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        PatientProfileDO profile = patientService.getPatient(patientId, userId, securityContext.isAdmin());
        return ApiResponse.ok(Map.of(
                "patient_id", String.valueOf(patientId),
                "fence_enabled", profile.getFenceEnabled() != null && profile.getFenceEnabled(),
                "fence_center_lat", profile.getFenceCenterLat() != null ? profile.getFenceCenterLat() : 0.0,
                "fence_center_lng", profile.getFenceCenterLng() != null ? profile.getFenceCenterLng() : 0.0,
                "fence_radius_m", profile.getFenceRadiusM() != null ? profile.getFenceRadiusM() : 0
        ), traceId);
    }

    /** 更新围栏配置 */
    @PutMapping("/api/v1/patients/{patientId}/fence")
    public ApiResponse<Map<String, Object>> updateFence(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody UpdateFenceRequest req) {

        Long userId = securityContext.currentUserId();
        PatientProfileDO profile = patientService.updateFence(
                patientId, userId, securityContext.isAdmin(),
                req.getFenceEnabled(), req.getFenceCenterLat(),
                req.getFenceCenterLng(), req.getFenceRadiusM());
        return ApiResponse.ok(Map.of(
                "patient_id", String.valueOf(patientId),
                "fence_enabled", profile.getFenceEnabled() != null && profile.getFenceEnabled()
        ), traceId);
    }

    // ===== 监护人邀请 =====

    /** 发起邀请 */
    @PostMapping("/api/v1/patients/{patientId}/guardians/invitations")
    public ApiResponse<Map<String, Object>> createInvitation(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody CreateInvitationRequest req) {

        Long userId = securityContext.currentUserId();
        GuardianInvitationDO inv = invitationService.createInvitation(
                patientId, userId, req.getInviteeUserId(), req.getRelationRole(), req.getReason());
        return ApiResponse.ok(buildInvitationVO(inv), traceId);
    }

    /** 查询患者的邀请列表 */
    @GetMapping("/api/v1/patients/{patientId}/guardians/invitations")
    public ApiResponse<PageResponse<Map<String, Object>>> listInvitations(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        List<GuardianInvitationDO> list = invitationService.listInvitations(patientId, pageNo, pageSize);
        long total = invitationService.countInvitations(patientId);
        List<Map<String, Object>> items = list.stream().map(this::buildInvitationVO).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    /** 响应邀请（被邀请人操作） */
    @PostMapping("/api/v1/patients/{patientId}/guardians/invitations/{inviteId}/accept")
    public ApiResponse<Map<String, Object>> respondInvitation(
            @PathVariable Long patientId,
            @PathVariable String inviteId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody RespondInvitationRequest req) {

        Long userId = securityContext.currentUserId();
        boolean accept = "ACCEPT".equalsIgnoreCase(req.getAction());
        GuardianInvitationDO inv = invitationService.respondInvitation(inviteId, userId, accept, req.getReason());
        return ApiResponse.ok(buildInvitationVO(inv), traceId);
    }

    /** 移除监护人 */
    @DeleteMapping("/api/v1/patients/{patientId}/guardians/{userId}")
    public ApiResponse<Map<String, Object>> removeGuardian(
            @PathVariable Long patientId,
            @PathVariable Long userId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long operatorId = securityContext.currentUserId();
        invitationService.removeGuardian(patientId, userId, operatorId, securityContext.isAdmin());
        return ApiResponse.ok(Map.of("result", "removed"), traceId);
    }

    /** 查询监护人列表 */
    @GetMapping("/api/v1/patients/{patientId}/guardians")
    public ApiResponse<List<Map<String, Object>>> listGuardians(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        List<SysUserPatientDO> list = invitationService.listGuardians(patientId);
        List<Map<String, Object>> vos = list.stream().map(r -> Map.<String, Object>of(
                "user_id", String.valueOf(r.getUserId()),
                "patient_id", String.valueOf(r.getPatientId()),
                "relation_role", r.getRelationRole(),
                "relation_status", r.getRelationStatus()
        )).toList();
        return ApiResponse.ok(vos, traceId);
    }

    // ===== 主监护人转移 =====

    /** 发起主监护人转移 */
    @PostMapping("/api/v1/patients/{patientId}/guardians/primary-transfer")
    public ApiResponse<Map<String, Object>> initiateTransfer(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody InitiateTransferRequest req) {

        Long userId = securityContext.currentUserId();
        SysUserPatientDO rel = invitationService.initiateTransfer(
                patientId, userId, req.getTargetUserId(), req.getReason());
        return ApiResponse.ok(buildTransferVO(rel), traceId);
    }

    /** 确认/拒绝主监护人转移 */
    @PostMapping("/api/v1/patients/{patientId}/guardians/primary-transfer/{transferReqId}/confirm")
    public ApiResponse<Map<String, Object>> confirmTransfer(
            @PathVariable Long patientId,
            @PathVariable String transferReqId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody ConfirmTransferRequest req) {

        Long userId = securityContext.currentUserId();
        boolean accept = "ACCEPT".equalsIgnoreCase(req.getAction());
        SysUserPatientDO rel = invitationService.confirmTransfer(transferReqId, userId, accept, req.getReason());
        return ApiResponse.ok(buildTransferVO(rel), traceId);
    }

    /** 取消主监护人转移 */
    @PostMapping("/api/v1/patients/{patientId}/guardians/primary-transfer/{transferReqId}/cancel")
    public ApiResponse<Map<String, Object>> cancelTransfer(
            @PathVariable Long patientId,
            @PathVariable String transferReqId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody CancelTransferRequest req) {

        Long userId = securityContext.currentUserId();
        invitationService.cancelTransfer(transferReqId, userId, req.getCancelReason());
        return ApiResponse.ok(Map.of("transfer_request_id", transferReqId, "status", "CANCELLED"), traceId);
    }

    // ===== 3.3.13 转移记录查询 =====

    /** 查询患者的主监护人转移记录列表 */
    @GetMapping("/api/v1/patients/{patientId}/guardians/transfers")
    public ApiResponse<PageResponse<Map<String, Object>>> listTransfers(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String transferState,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        if (!securityContext.isAdmin() && sysUserPatientMapper.countActiveRelation(userId, patientId) == 0)
            throw BizException.of("E_PRO_4030");

        List<SysUserPatientDO> list = sysUserPatientMapper.listTransfersByPatientId(
                patientId, transferState, pageSize, (pageNo - 1) * pageSize);
        long total = sysUserPatientMapper.countTransfersByPatientId(patientId, transferState);
        List<Map<String, Object>> items = list.stream().map(r -> Map.<String, Object>of(
                "transfer_request_id", r.getTransferRequestId() != null ? r.getTransferRequestId() : "",
                "from_user_id", String.valueOf(r.getTransferRequestedBy() != null ? r.getTransferRequestedBy() : 0),
                "to_user_id", String.valueOf(r.getTransferTargetUserId() != null ? r.getTransferTargetUserId() : 0),
                "transfer_state", r.getTransferState() != null ? r.getTransferState() : "",
                "requested_at", r.getTransferRequestedAt() != null ? r.getTransferRequestedAt().toString() : "",
                "expire_at", r.getTransferExpireAt() != null ? r.getTransferExpireAt().toString() : ""
        )).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // ===== 3.3.15 邀请详情 =====

    /** 查询单条邀请详情 */
    @GetMapping("/api/v1/patients/{patientId}/guardians/invitations/{inviteId}")
    public ApiResponse<Map<String, Object>> getInvitationDetail(
            @PathVariable Long patientId,
            @PathVariable String inviteId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        if (!securityContext.isAdmin() && sysUserPatientMapper.countActiveRelation(userId, patientId) == 0)
            throw BizException.of("E_PRO_4030");

        GuardianInvitationDO inv = invitationMapper.findByInviteId(inviteId);
        if (inv == null) throw BizException.of("E_PRO_4043");
        return ApiResponse.ok(Map.<String, Object>of(
                "invite_id", inv.getInviteId(),
                "patient_id", String.valueOf(inv.getPatientId()),
                "inviter_user_id", String.valueOf(inv.getInviterUserId() != null ? inv.getInviterUserId() : 0),
                "invitee_user_id", String.valueOf(inv.getInviteeUserId() != null ? inv.getInviteeUserId() : 0),
                "relation_role", inv.getRelationRole() != null ? inv.getRelationRole() : "",
                "status", inv.getStatus() != null ? inv.getStatus() : "",
                "reason", inv.getReason() != null ? inv.getReason() : "",
                "expire_at", inv.getExpireAt() != null ? inv.getExpireAt().toString() : "",
                "created_at", inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : ""
        ), traceId);
    }

    // ===== 3.3.16 转移详情 =====

    /** 查询单条转移记录详情 */
    @GetMapping("/api/v1/patients/{patientId}/guardians/transfers/{transferReqId}")
    public ApiResponse<Map<String, Object>> getTransferDetail(
            @PathVariable Long patientId,
            @PathVariable String transferReqId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        if (!securityContext.isAdmin() && sysUserPatientMapper.countActiveRelation(userId, patientId) == 0)
            throw BizException.of("E_PRO_4030");

        SysUserPatientDO rel = sysUserPatientMapper.findByTransferRequestId(transferReqId);
        if (rel == null) throw BizException.of("E_PRO_4045");
        return ApiResponse.ok(Map.<String, Object>of(
                "transfer_request_id", rel.getTransferRequestId(),
                "patient_id", String.valueOf(rel.getPatientId()),
                "from_user_id", String.valueOf(rel.getTransferRequestedBy() != null ? rel.getTransferRequestedBy() : 0),
                "to_user_id", String.valueOf(rel.getTransferTargetUserId() != null ? rel.getTransferTargetUserId() : 0),
                "transfer_state", rel.getTransferState() != null ? rel.getTransferState() : "",
                "reason", rel.getTransferReason() != null ? rel.getTransferReason() : "",
                "requested_at", rel.getTransferRequestedAt() != null ? rel.getTransferRequestedAt().toString() : "",
                "expire_at", rel.getTransferExpireAt() != null ? rel.getTransferExpireAt().toString() : "",
                "confirmed_at", rel.getTransferConfirmedAt() != null ? rel.getTransferConfirmedAt().toString() : ""
        ), traceId);
    }

    // ===== 3.4.28 患者标签详情 =====

    /** 读取患者维度标签详情 */
    @GetMapping("/api/v1/patients/{patientId}/tags/{tagCode}")
    public ApiResponse<Map<String, Object>> getPatientTag(
            @PathVariable Long patientId,
            @PathVariable String tagCode,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        if (!securityContext.isAdmin() && sysUserPatientMapper.countActiveRelation(userId, patientId) == 0)
            throw BizException.of("E_PRO_4030");

        TagAssetDO tag = tagAssetMapper.findByTagCode(tagCode);
        if (tag == null || !patientId.equals(tag.getPatientId())) throw BizException.of("E_MAT_4044");
        return ApiResponse.ok(Map.<String, Object>of(
                "tag_code", tag.getTagCode(),
                "patient_id", String.valueOf(tag.getPatientId()),
                "status", tag.getStatus(),
                "bind_time", tag.getUpdatedAt() != null ? tag.getUpdatedAt().toString() : "",
                "updated_at", tag.getUpdatedAt() != null ? tag.getUpdatedAt().toString() : ""
        ), traceId);
    }

    // ===== 3.4.29 标签历史 =====

    /** 读取标签历史流转轨迹 */
    @GetMapping("/api/v1/patients/{patientId}/tags/{tagCode}/history")
    public ApiResponse<PageResponse<Map<String, Object>>> getTagHistory(
            @PathVariable Long patientId,
            @PathVariable String tagCode,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        if (!securityContext.isAdmin() && sysUserPatientMapper.countActiveRelation(userId, patientId) == 0)
            throw BizException.of("E_PRO_4030");

        TagAssetDO tag = tagAssetMapper.findByTagCode(tagCode);
        if (tag == null) throw BizException.of("E_MAT_4044");

        List<SysLogDO> logs = sysLogMapper.listByModuleAndObjectId(
                "TAG_ASSET", tagCode, pageSize, (pageNo - 1) * pageSize);
        long total = sysLogMapper.countByModuleAndObjectId("TAG_ASSET", tagCode);
        List<Map<String, Object>> items = logs.stream().map(l -> Map.<String, Object>of(
                "history_id", String.valueOf(l.getId()),
                "from_status", "",
                "to_status", l.getAction() != null ? l.getAction() : "",
                "operator_user_id", String.valueOf(l.getOperatorUserId() != null ? l.getOperatorUserId() : 0),
                "reason", l.getResult() != null ? l.getResult() : "",
                "created_at", l.getCreatedAt() != null ? l.getCreatedAt().toString() : ""
        )).toList();
        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize)
                .total(total).hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // ===== 用户查询（邀请前置） =====

    /**
     * 按手机号查询用户信息（用于邀请前置查询）。
     */
    @GetMapping("/api/v1/users/lookup")
    public ApiResponse<Map<String, Object>> lookupUser(
            @RequestParam String phone,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        SysUserDO user = sysUserMapper.findByPhone(phone);
        if (user == null) throw BizException.of("E_USER_4041");
        return ApiResponse.ok(Map.of(
                "user_id", String.valueOf(user.getId()),
                "display_name", user.getDisplayName() != null ? user.getDisplayName() : "",
                "username", user.getUsername()
        ), traceId);
    }

    // ===== VO 构建 =====

    private Map<String, Object> buildProfileVO(PatientProfileDO p) {
        return Map.of(
                "patient_id", String.valueOf(p.getId()),
                "profile_no", p.getProfileNo(),
                "name", p.getName(),
                "gender", p.getGender() != null ? p.getGender() : "",
                "short_code", p.getShortCode(),
                "photo_url", p.getPhotoUrl() != null ? p.getPhotoUrl() : "",
                "lost_status", p.getLostStatus() != null ? p.getLostStatus() : "NORMAL",
                "profile_version", p.getProfileVersion() != null ? p.getProfileVersion() : 1L
        );
    }

    private Map<String, Object> buildInvitationVO(GuardianInvitationDO inv) {
        return Map.of(
                "invite_id", inv.getInviteId(),
                "patient_id", String.valueOf(inv.getPatientId()),
                "relation_role", inv.getRelationRole(),
                "status", inv.getStatus(),
                "expire_at", inv.getExpireAt() != null ? inv.getExpireAt().toString() : ""
        );
    }

    private Map<String, Object> buildTransferVO(SysUserPatientDO rel) {
        return Map.of(
                "transfer_request_id", rel.getTransferRequestId() != null ? rel.getTransferRequestId() : "",
                "transfer_state", rel.getTransferState() != null ? rel.getTransferState() : "NONE",
                "transfer_target_user_id", String.valueOf(rel.getTransferTargetUserId() != null ? rel.getTransferTargetUserId() : 0)
        );
    }

    // ===== DTO =====

    @Data
    public static class CreatePatientRequest {
        @NotBlank private String name;
        private String gender;
        private String birthday;
        @NotBlank private String photoUrl;
        private String medicalHistory;
        @NotBlank private String pinCode;
    }

    @Data
    public static class UpdatePatientRequest {
        @NotBlank private String name;
        private String gender;
        private String birthday;
        private String photoUrl;
        private String medicalHistory;
    }

    @Data
    public static class UpdateFenceRequest {
        @NotNull private Boolean fenceEnabled;
        private Double fenceCenterLat;
        private Double fenceCenterLng;
        private Integer fenceRadiusM;
    }

    @Data
    public static class CreateInvitationRequest {
        @NotNull private Long inviteeUserId;
        @NotBlank private String relationRole;
        private String reason;
    }

    @Data
    public static class RespondInvitationRequest {
        @NotBlank private String action; // ACCEPT / REJECT
        private String reason;
    }

    @Data
    public static class InitiateTransferRequest {
        @NotNull private Long targetUserId;
        private String reason;
    }

    @Data
    public static class ConfirmTransferRequest {
        @NotBlank private String action; // ACCEPT / REJECT
        private String reason;
    }

    @Data
    public static class CancelTransferRequest {
        private String cancelReason;
    }
}
