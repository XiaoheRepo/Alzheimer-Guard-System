package com.xiaohelab.guard.server.patient.service;

import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.dto.InvitationCreateRequest;
import com.xiaohelab.guard.server.patient.dto.InvitationResponseRequest;
import com.xiaohelab.guard.server.patient.dto.TransferCreateRequest;
import com.xiaohelab.guard.server.patient.entity.GuardianInvitationEntity;
import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.patient.entity.GuardianTransferRequestEntity;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.*;
import com.xiaohelab.guard.server.user.entity.UserEntity;
import com.xiaohelab.guard.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** 监护成员管理：邀请、转移、移除。 */
@Service
public class GuardianService {

    private static final Logger log = LoggerFactory.getLogger(GuardianService.class);

    private final GuardianInvitationRepository invitationRepository;
    private final GuardianRelationRepository relationRepository;
    private final GuardianTransferRequestRepository transferRepository;
    private final PatientProfileRepository patientRepository;
    private final UserRepository userRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

    public GuardianService(GuardianInvitationRepository invitationRepository,
                           GuardianRelationRepository relationRepository,
                           GuardianTransferRequestRepository transferRepository,
                           PatientProfileRepository patientRepository,
                           UserRepository userRepository,
                           GuardianAuthorizationService authorizationService,
                           OutboxService outboxService) {
        this.invitationRepository = invitationRepository;
        this.relationRepository = relationRepository;
        this.transferRepository = transferRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
    }

    /** 主监护人发起邀请。 */
    @Transactional(rollbackFor = Exception.class)
    public GuardianInvitationEntity invite(Long patientId, InvitationCreateRequest req) {
        AuthUser user = SecurityUtil.current();
        authorizationService.assertPrimary(user, patientId);
        UserEntity invitee = userRepository.findById(req.getInviteeUserId())
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4042));
        if (invitee.getId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_PRO_4094);
        }
        // 已激活 / 待确认都视为冲突
        if (relationRepository.findByUserIdAndPatientIdAndRelationStatus(invitee.getId(), patientId, "ACTIVE").isPresent()) {
            throw BizException.of(ErrorCode.E_PRO_4094);
        }
        if (invitationRepository.findByPatientIdAndInviteeUserIdAndStatus(patientId, invitee.getId(), "PENDING").isPresent()) {
            throw BizException.of(ErrorCode.E_PRO_4094);
        }
        GuardianInvitationEntity inv = new GuardianInvitationEntity();
        inv.setInviteId(BusinessNoUtil.inviteId());
        inv.setPatientId(patientId);
        inv.setInviterUserId(user.getUserId());
        inv.setInviteeUserId(invitee.getId());
        inv.setRelationRole("GUARDIAN");
        inv.setStatus("PENDING");
        inv.setReason(req.getReason());
        inv.setExpireAt(OffsetDateTime.now().plusSeconds(req.getExpireInSeconds()));
        invitationRepository.save(inv);
        outboxService.publish(OutboxTopics.GUARDIAN_INVITED, inv.getInviteId(), String.valueOf(patientId),
                Map.of("invite_id", inv.getInviteId(), "patient_id", patientId,
                        "invitee_user_id", invitee.getId(), "inviter_user_id", user.getUserId()));
        return inv;
    }

    /** 受邀人响应邀请（接受/拒绝）。 */
    @Transactional(rollbackFor = Exception.class)
    public GuardianInvitationEntity respondInvitation(String inviteId, InvitationResponseRequest req) {
        AuthUser user = SecurityUtil.current();
        GuardianInvitationEntity inv = invitationRepository.findByInviteId(inviteId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4043));
        if (!inv.getInviteeUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_PRO_4033);
        }
        if (!"PENDING".equals(inv.getStatus())) {
            throw BizException.of(ErrorCode.E_PRO_4096);
        }
        if (inv.getExpireAt().isBefore(OffsetDateTime.now())) {
            inv.setStatus("EXPIRED");
            invitationRepository.save(inv);
            throw BizException.of(ErrorCode.E_PRO_4096);
        }
        if ("ACCEPT".equals(req.getAction())) {
            inv.setStatus("ACCEPTED");
            inv.setRespondedAt(OffsetDateTime.now());
            invitationRepository.save(inv);
            // 建立 ACTIVE 关系
            GuardianRelationEntity rel = new GuardianRelationEntity();
            rel.setUserId(user.getUserId());
            rel.setPatientId(inv.getPatientId());
            rel.setRelationRole(inv.getRelationRole());
            rel.setRelationStatus("ACTIVE");
            rel.setJoinedAt(OffsetDateTime.now());
            relationRepository.save(rel);
            outboxService.publish(OutboxTopics.GUARDIAN_JOINED, inv.getInviteId(),
                    String.valueOf(inv.getPatientId()),
                    Map.of("invite_id", inv.getInviteId(), "patient_id", inv.getPatientId(), "user_id", user.getUserId()));
        } else if ("REJECT".equals(req.getAction())) {
            inv.setStatus("REJECTED");
            inv.setRespondedAt(OffsetDateTime.now());
            inv.setRejectReason(req.getRejectReason());
            invitationRepository.save(inv);
        } else {
            throw BizException.of(ErrorCode.E_PRO_4008);
        }
        return inv;
    }

    /** 发起主监护权转移。 */
    @Transactional(rollbackFor = Exception.class)
    public GuardianTransferRequestEntity initiateTransfer(Long patientId, TransferCreateRequest req) {
        AuthUser user = SecurityUtil.current();
        PatientProfileEntity p = authorizationService.assertPrimary(user, patientId);
        if (req.getToUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_PRO_4099);
        }
        if (relationRepository.findByUserIdAndPatientIdAndRelationStatus(req.getToUserId(), patientId, "ACTIVE").isEmpty()) {
            throw BizException.of(ErrorCode.E_PRO_4044);
        }
        if (transferRepository.findByPatientIdAndStatus(patientId, "PENDING_CONFIRM").isPresent()) {
            throw BizException.of(ErrorCode.E_PRO_4095);
        }
        GuardianTransferRequestEntity tr = new GuardianTransferRequestEntity();
        tr.setRequestId(BusinessNoUtil.transferId());
        tr.setPatientId(patientId);
        tr.setFromUserId(user.getUserId());
        tr.setToUserId(req.getToUserId());
        tr.setReason(req.getReason());
        tr.setStatus("PENDING_CONFIRM");
        tr.setExpireAt(OffsetDateTime.now().plusSeconds(req.getExpireInSeconds()));
        transferRepository.save(tr);
        outboxService.publish(OutboxTopics.GUARDIAN_TRANSFER_REQ, tr.getRequestId(), String.valueOf(patientId),
                Map.of("request_id", tr.getRequestId(), "patient_id", patientId,
                        "from", user.getUserId(), "to", req.getToUserId()));
        return tr;
    }

    /** 受方确认或拒绝主监护权转移。 */
    @Transactional(rollbackFor = Exception.class)
    public GuardianTransferRequestEntity respondTransfer(String requestId, String action, String rejectReason) {
        AuthUser user = SecurityUtil.current();
        GuardianTransferRequestEntity tr = transferRepository.findByRequestId(requestId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4045));
        if (!tr.getToUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_PRO_4033);
        }
        if (!"PENDING_CONFIRM".equals(tr.getStatus())) {
            throw BizException.of(ErrorCode.E_PRO_4097);
        }
        if (tr.getExpireAt().isBefore(OffsetDateTime.now())) {
            tr.setStatus("EXPIRED");
            transferRepository.save(tr);
            throw BizException.of(ErrorCode.E_PRO_4097);
        }
        if ("ACCEPT".equals(action)) {
            tr.setStatus("COMPLETED");
            tr.setConfirmedAt(OffsetDateTime.now());
            transferRepository.save(tr);
            // 执行主监护权转移
            GuardianRelationEntity oldPrimary = relationRepository.findByPatientIdAndRelationRoleAndRelationStatus(
                    tr.getPatientId(), "PRIMARY_GUARDIAN", "ACTIVE")
                    .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4046));
            oldPrimary.setRelationRole("GUARDIAN");
            relationRepository.save(oldPrimary);
            GuardianRelationEntity newPrimary = relationRepository.findByUserIdAndPatientIdAndRelationStatus(
                    tr.getToUserId(), tr.getPatientId(), "ACTIVE")
                    .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4046));
            newPrimary.setRelationRole("PRIMARY_GUARDIAN");
            relationRepository.save(newPrimary);
            outboxService.publish(OutboxTopics.GUARDIAN_TRANSFER_DONE, tr.getRequestId(),
                    String.valueOf(tr.getPatientId()),
                    Map.of("request_id", tr.getRequestId(), "patient_id", tr.getPatientId(),
                            "from", tr.getFromUserId(), "to", tr.getToUserId()));
        } else if ("REJECT".equals(action)) {
            tr.setStatus("REJECTED");
            tr.setRejectedAt(OffsetDateTime.now());
            tr.setRejectReason(rejectReason);
            transferRepository.save(tr);
        } else {
            throw BizException.of(ErrorCode.E_PRO_4011);
        }
        return tr;
    }

    /** 原发起方撤销转移。 */
    @Transactional(rollbackFor = Exception.class)
    public GuardianTransferRequestEntity cancelTransfer(String requestId, String reason) {
        AuthUser user = SecurityUtil.current();
        GuardianTransferRequestEntity tr = transferRepository.findByRequestId(requestId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4045));
        if (!tr.getFromUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_PRO_4034);
        }
        if (!"PENDING_CONFIRM".equals(tr.getStatus())) {
            throw BizException.of(ErrorCode.E_PRO_4098);
        }
        tr.setStatus("REVOKED");
        tr.setRevokedAt(OffsetDateTime.now());
        tr.setCancelReason(reason);
        transferRepository.save(tr);
        return tr;
    }

    /** 主监护人移除普通监护成员。 */
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long patientId, Long targetUserId) {
        AuthUser user = SecurityUtil.current();
        authorizationService.assertPrimary(user, patientId);
        if (targetUserId.equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_PRO_4099);
        }
        GuardianRelationEntity rel = relationRepository.findByUserIdAndPatientIdAndRelationStatus(
                targetUserId, patientId, "ACTIVE")
                .orElseThrow(() -> BizException.of(ErrorCode.E_PRO_4046));
        if ("PRIMARY_GUARDIAN".equals(rel.getRelationRole())) {
            throw BizException.of(ErrorCode.E_PRO_4035);
        }
        rel.setRelationStatus("REVOKED");
        rel.setRevokedAt(OffsetDateTime.now());
        relationRepository.save(rel);
    }

    public List<GuardianRelationEntity> listMembers(Long patientId) {
        AuthUser user = SecurityUtil.current();
        authorizationService.assertGuardian(user, patientId);
        return relationRepository.findByPatientIdAndRelationStatus(patientId, "ACTIVE");
    }
}
