package com.xiaohelab.guard.server.auth.service;

import com.xiaohelab.guard.server.auth.dto.AdminCreateRequest;
import com.xiaohelab.guard.server.auth.dto.AdminUserActionRequest;
import com.xiaohelab.guard.server.auth.dto.AdminUserDetailResponse;
import com.xiaohelab.guard.server.auth.dto.AdminUserListItem;
import com.xiaohelab.guard.server.auth.dto.AdminUserUpdateRequest;
import com.xiaohelab.guard.server.common.dto.CursorResponse;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.JwtRevocationService;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.CursorUtil;
import com.xiaohelab.guard.server.common.util.CryptoUtil;
import com.xiaohelab.guard.server.common.util.DesensitizeUtil;
import com.xiaohelab.guard.server.common.util.TraceIdUtil;
import com.xiaohelab.guard.server.gov.service.AuditLogger;
import com.xiaohelab.guard.server.material.repository.TagApplyRecordRepository;
import com.xiaohelab.guard.server.notification.entity.NotificationInboxEntity;
import com.xiaohelab.guard.server.notification.repository.NotificationInboxRepository;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.entity.GuardianRelationEntity;
import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import com.xiaohelab.guard.server.patient.repository.GuardianRelationRepository;
import com.xiaohelab.guard.server.patient.repository.PatientProfileRepository;
import com.xiaohelab.guard.server.rescue.repository.RescueTaskRepository;
import com.xiaohelab.guard.server.user.entity.UserEntity;
import com.xiaohelab.guard.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员治理 - 用户管理服务（V2.1 增量）。
 * <p>对应：API §3.6.15~3.6.20、LLD §8.3.8~8.3.13、FR-GOV-011~014、BDD §24。</p>
 *
 * <p>核心授权矩阵（handbook §24.3 10 条铁律）：</p>
 * <ol>
 *   <li>仅 ADMIN / SUPER_ADMIN 可访问本类全部接口</li>
 *   <li>ADMIN 的可视 / 操作对象仅 FAMILY；SUPER_ADMIN 可操作 FAMILY / ADMIN</li>
 *   <li>SUPER_ADMIN 账号不可被禁用 / 注销 / 角色降级（含自身）</li>
 *   <li>任何管理员不可对自身执行状态/角色变更</li>
 *   <li>role 字段仅 SUPER_ADMIN 可修改</li>
 *   <li>注销需 CONFIRM_3；禁用 / 改角色需 CONFIRM_2；启用需 CONFIRM_1</li>
 *   <li>写操作必须 CAS + 幂等 + Outbox + 审计，四件套缺一不可</li>
 *   <li>禁用 / 改角色 / 注销后必须立即吊销目标在途 JWT</li>
 *   <li>注销存在前置校验（仍为主监护 / 有未终态物资工单 / 有未终态寻回任务）</li>
 *   <li>所有高危写操作必须记录行政理由（reason ≥ 10）</li>
 * </ol>
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    /** 物资工单未终态集合（见 BDD §10.3 物资工单状态机）。 */
    private static final List<String> MATERIAL_ORDER_OPEN = List.of(
            "PENDING_AUDIT", "PENDING_PAYMENT", "PENDING_SHIP", "SHIPPED");

    private final UserRepository userRepository;
    private final GuardianRelationRepository guardianRelationRepository;
    private final TagApplyRecordRepository tagApplyRecordRepository;
    private final RescueTaskRepository rescueTaskRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final NotificationInboxRepository notificationInboxRepository;
    private final OutboxService outboxService;
    private final AuditLogger auditLogger;
    private final JwtRevocationService jwtRevocationService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository,
                            GuardianRelationRepository guardianRelationRepository,
                            TagApplyRecordRepository tagApplyRecordRepository,
                            RescueTaskRepository rescueTaskRepository,
                            PatientProfileRepository patientProfileRepository,
                            NotificationInboxRepository notificationInboxRepository,
                            OutboxService outboxService,
                            AuditLogger auditLogger,
                            JwtRevocationService jwtRevocationService,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.guardianRelationRepository = guardianRelationRepository;
        this.tagApplyRecordRepository = tagApplyRecordRepository;
        this.rescueTaskRepository = rescueTaskRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.notificationInboxRepository = notificationInboxRepository;
        this.outboxService = outboxService;
        this.auditLogger = auditLogger;
        this.jwtRevocationService = jwtRevocationService;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================
    // 0. 创建管理员（POST /api/v1/admin/users）—— CRITICAL + CONFIRM_2
    //    仅 SUPER_ADMIN 可调用；初始密码自动生成，一次性明文返回
    // =========================================================
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createAdmin(AdminCreateRequest req, String confirmLevel) {
        AuthUser me = SecurityUtil.current();
        // 仅 SUPER_ADMIN 可创建管理员
        if (!me.isSuperAdmin()) throw BizException.of(ErrorCode.E_AUTH_4031);
        requireConfirmLevel(confirmLevel, "CONFIRM_2");

        // 1. 唯一性校验
        if (userRepository.existsByUsername(req.getUsername())) {
            throw BizException.of(ErrorCode.E_GOV_4091);
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw BizException.of(ErrorCode.E_GOV_4092);
        }

        // 2. 自动生成 16 位初始密码（字母+数字随机）
        String tempPassword = CryptoUtil.randomToken(8); // 16 hex chars

        // 3. 落库
        UserEntity u = new UserEntity();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setEmailVerified(false);
        u.setPasswordHash(passwordEncoder.encode(tempPassword));
        u.setNickname(req.getNickname() != null && !req.getNickname().isBlank()
                ? req.getNickname() : req.getUsername());
        u.setRole("ADMIN");
        u.setStatus("ACTIVE");
        userRepository.save(u);

        // 4. Outbox → 下游邮件服务发送欢迎邮件 + 初始密码（邮件内含密码，不持久化到 DB）
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", String.valueOf(u.getId()));
        payload.put("username", u.getUsername());
        payload.put("email", u.getEmail());
        payload.put("nickname", u.getNickname());
        payload.put("temp_password", tempPassword);   // 消费方用于组装欢迎邮件
        payload.put("operator_user_id", me.getUserId());
        payload.put("reason", req.getReason());
        payload.put("occurred_at", OffsetDateTime.now().toString());
        outboxService.publish(OutboxTopics.ADMIN_CREATED,
                String.valueOf(u.getId()), String.valueOf(u.getId()), payload);

        // 5. 审计
        auditLogger.logSuccess("GOV", "admin.user.create", String.valueOf(u.getId()),
                "CRITICAL", "CONFIRM_2",
                Map.of("new_user_id", u.getId(), "username", u.getUsername(),
                        "email", DesensitizeUtil.email(u.getEmail()), "reason", req.getReason()));

        log.info("[Admin] SUPER_ADMIN {} 创建管理员账号 userId={} username={}",
                me.getUserId(), u.getId(), u.getUsername());

        // 6. 响应：一次性返回临时密码（前端弹窗展示，不可二次获取）
        Map<String, Object> result = new HashMap<>();
        result.put("user_id", String.valueOf(u.getId()));
        result.put("username", u.getUsername());
        result.put("email", u.getEmail());
        result.put("nickname", u.getNickname());
        result.put("role", "ADMIN");
        result.put("status", "ACTIVE");
        result.put("temp_password", tempPassword);
        result.put("temp_password_note", "此密码仅展示一次，请立即通知管理员本人修改密码");
        result.put("created_at", u.getCreatedAt());
        return result;
    }

    // =========================================================
    // 1. 列表（GET /api/v1/admin/users）—— LOW 风险，只读
    // =========================================================
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public CursorResponse<AdminUserListItem> list(String keyword,
                                                  String role,
                                                  String status,
                                                  String cursor,
                                                  int pageSize) {
        AuthUser me = SecurityUtil.current();
        assertAdmin(me);
        int size = normalizePageSize(pageSize);

        // 1. 角色可视范围（规则 2）
        Set<String> visibleRoles = me.isSuperAdmin()
                ? Set.of("FAMILY", "ADMIN", "SUPER_ADMIN")
                : Set.of("FAMILY");
        if (role != null && !role.isBlank()) {
            if (!visibleRoles.contains(role)) {
                throw BizException.of(ErrorCode.E_AUTH_4031);
            }
            visibleRoles = Set.of(role);
        }

        // 2. 状态白名单；null 表示不过滤，传全量值避免 SQL 集合 IS NULL 类型推断失败
        List<String> statuses = (status == null || status.isBlank())
                ? List.of("ACTIVE", "DISABLED", "DEACTIVATED")
                : List.of(status);

        // 3. 游标翻页
        Long cursorId = CursorUtil.decodeId(cursor);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();

        // 4. 多取 1 条以判断 has_next
        List<UserEntity> raw = userRepository.findForAdmin(
                kw, visibleRoles, statuses, cursorId, size + 1);
        boolean hasNext = raw.size() > size;
        if (hasNext) raw = raw.subList(0, size);

        List<AdminUserListItem> items = raw.stream().map(this::toListItem).collect(Collectors.toList());
        String nextCursor = hasNext && !items.isEmpty()
                ? CursorUtil.encode(raw.get(raw.size() - 1).getId())
                : null;

        // 5. 审计（LOW + 条件摘要）
        auditLogger.logSuccess("GOV", "admin.user.list", null, "LOW", null,
                Map.of("keyword", kw == null ? "" : kw,
                        "role", role == null ? "" : role,
                        "status", status == null ? "" : status,
                        "page_size", size,
                        "result_size", items.size()));
        return CursorResponse.of(items, size, nextCursor, hasNext);
    }

    // =========================================================
    // 2. 详情（GET /api/v1/admin/users/{id}）—— LOW 只读
    // =========================================================
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AdminUserDetailResponse detail(Long userId) {
        AuthUser me = SecurityUtil.current();
        assertAdmin(me);
        UserEntity u = loadAndAssertVisible(userId, me);

        AdminUserDetailResponse r = new AdminUserDetailResponse();
        r.setUserId(String.valueOf(u.getId()));
        r.setUsername(u.getUsername());
        r.setNickname(u.getNickname());
        r.setEmail(DesensitizeUtil.email(u.getEmail()));
        r.setPhone(DesensitizeUtil.phone(u.getPhone()));
        r.setRole(u.getRole());
        r.setStatus(u.getStatus());
        r.setEmailVerified(u.getEmailVerified());
        r.setLastLoginAt(u.getLastLoginAt());
        r.setLastLoginIp(u.getLastLoginIp());
        r.setDeactivatedAt(u.getDeactivatedAt());
        r.setCreatedAt(u.getCreatedAt());
        r.setUpdatedAt(u.getUpdatedAt());

        // 统计：用于前端展示注销前置不满足的提示
        AdminUserDetailResponse.Stats stats = new AdminUserDetailResponse.Stats();
        stats.setPrimaryGuardianPatientCount(guardianRelationRepository.findPrimaryActivePatientIds(userId).size());
        stats.setGuardianPatientCount(guardianRelationRepository.findByUserIdAndRelationStatus(userId, "ACTIVE").size());
        stats.setPendingMaterialOrderCount(
                tagApplyRecordRepository.countByApplicantUserIdAndStatusIn(userId, MATERIAL_ORDER_OPEN));
        r.setStats(stats);

        auditLogger.logSuccess("GOV", "admin.user.read", String.valueOf(userId), "LOW", null,
                Map.of("target_user_id", userId));
        return r;
    }

    // =========================================================
    // 3. 修改（PUT /api/v1/admin/users/{id}）—— HIGH / CRITICAL
    // =========================================================
    @Transactional(rollbackFor = Exception.class)
    public AdminUserDetailResponse update(Long userId,
                                          AdminUserUpdateRequest req,
                                          String confirmLevel) {
        AuthUser me = SecurityUtil.current();
        assertAdmin(me);
        // 规则 4：仅在角色变更时禁止操作自身，纯资料修改（昵称/邮箱/手机）允许自编辑
        UserEntity u = loadAndAssertVisible(userId, me);

        boolean hasRoleChange = req.getRole() != null && !req.getRole().equals(u.getRole());
        // 规则 4：角色变更不可作用于自身
        if (hasRoleChange) {
            assertNotSelf(me, userId);
        }
        // 规则 5：role 仅 SUPER_ADMIN 可改
        if (hasRoleChange && !me.isSuperAdmin()) {
            throw BizException.of(ErrorCode.E_USR_4035);
        }
        // 规则 3：SUPER_ADMIN 不可降级
        if (hasRoleChange && "SUPER_ADMIN".equals(u.getRole())) {
            throw BizException.of(ErrorCode.E_USR_4033);
        }
        // 规则 6：改角色需 CONFIRM_2
        if (hasRoleChange) {
            requireConfirmLevel(confirmLevel, "CONFIRM_2");
        }

        // 变更前快照
        Map<String, Object> before = snapshot(u);

        // 字段变更
        boolean changed = false;
        if (req.getNickname() != null && !req.getNickname().equals(u.getNickname())) {
            u.setNickname(req.getNickname());
            changed = true;
        }
        if (req.getEmail() != null && !req.getEmail().equalsIgnoreCase(u.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw BizException.of(ErrorCode.E_GOV_4092);
            }
            u.setEmail(req.getEmail());
            u.setEmailVerified(false); // API 契约：邮箱变更后必须重新验证
            changed = true;
        }
        if (req.getPhone() != null && !req.getPhone().equals(u.getPhone())) {
            u.setPhone(req.getPhone());
            changed = true;
        }
        if (hasRoleChange) {
            u.setRole(req.getRole());
            changed = true;
        }

        if (!changed) {
            // 无实际变更：直接返回当前视图，不落审计写操作
            return detailView(u);
        }

        userRepository.save(u);

        // 角色变更：吊销 JWT + Outbox 广播 + CRITICAL 审计
        if (hasRoleChange) {
            jwtRevocationService.revokeAllForUser(userId);
            Map<String, Object> payload = new HashMap<>();
            payload.put("user_id", String.valueOf(userId));
            payload.put("from_role", before.get("role"));
            payload.put("to_role", u.getRole());
            payload.put("operator_user_id", me.getUserId());
            payload.put("occurred_at", OffsetDateTime.now().toString());
            outboxService.publish(OutboxTopics.USER_ROLE_CHANGED,
                    String.valueOf(userId), String.valueOf(userId), payload);
            auditLogger.logSuccess("GOV", "admin.user.update", String.valueOf(userId),
                    "CRITICAL", "CONFIRM_2",
                    Map.of("before", before, "after", snapshot(u), "role_changed", true));
        } else {
            auditLogger.logSuccess("GOV", "admin.user.update", String.valueOf(userId),
                    "HIGH", null,
                    Map.of("before", before, "after", snapshot(u), "role_changed", false));
        }
        return detailView(u);
    }

    // =========================================================
    // 4. 禁用（POST /api/v1/admin/users/{id}/disable）—— HIGH + CONFIRM_2
    // =========================================================
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long userId, AdminUserActionRequest req, String confirmLevel) {
        AuthUser me = SecurityUtil.current();
        assertAdmin(me);
        assertNotSelf(me, userId);
        requireConfirmLevel(confirmLevel, "CONFIRM_2");
        UserEntity u = loadAndAssertVisible(userId, me);

        // 规则 3
        if ("SUPER_ADMIN".equals(u.getRole())) throw BizException.of(ErrorCode.E_USR_4033);
        // 状态前置：仅 ACTIVE 可禁用
        if (!"ACTIVE".equals(u.getStatus())) throw BizException.of(ErrorCode.E_USR_4091);

        // 1. 查询该用户作为 PRIMARY_GUARDIAN 的患者列表
        List<Long> primaryPatientIds = guardianRelationRepository.findPrimaryActivePatientIds(userId);

        // 2. 对每个患者检查是否存在进行中的寻回任务，执行顺位继承或阻断
        OffsetDateTime now = OffsetDateTime.now();
        for (Long patientId : primaryPatientIds) {
            rescueTaskRepository.findActiveByPatient(patientId).ifPresent(activeTask -> {
                // 2a. 查找该患者其他在岗监护人（按加入时间升序，取最早的作为继承人）
                List<GuardianRelationEntity> others =
                        guardianRelationRepository.findOtherActiveGuardiansByPatient(patientId, userId);
                if (others.isEmpty()) {
                    // 仅一名监护人且任务进行中 → 阻断禁用，要求管理员先关闭任务
                    PatientProfileEntity patient = patientProfileRepository.findById(patientId)
                            .orElseThrow(() -> BizException.of(ErrorCode.E_USR_4094));
                    log.warn("[disable] 阻断禁用 userId={} patientId={} name={} 唯一监护人且任务进行中",
                            userId, patientId, patient.getName());
                    throw BizException.of(ErrorCode.E_USR_4094);
                }

                // 2b. 顺位继承：将加入时间最早的 GUARDIAN 晋升为 PRIMARY_GUARDIAN
                GuardianRelationEntity successor = others.get(0);
                // 降级原主监护关系（不撤销，保留历史关系，改为普通 GUARDIAN）
                GuardianRelationEntity oldPrimary = guardianRelationRepository
                        .findByPatientIdAndRelationRoleAndRelationStatus(patientId, "PRIMARY_GUARDIAN", "ACTIVE")
                        .orElse(null);
                if (oldPrimary != null) {
                    oldPrimary.setRelationRole("GUARDIAN");
                    guardianRelationRepository.save(oldPrimary);
                }
                // 晋升继承人
                successor.setRelationRole("PRIMARY_GUARDIAN");
                guardianRelationRepository.save(successor);

                // 2c. 站内通知（同步落库，保证事务内可见）
                PatientProfileEntity patient = patientProfileRepository.findById(patientId).orElse(null);
                String patientName = patient != null ? patient.getName() : ("患者#" + patientId);
                UserEntity successorUser = userRepository.findById(successor.getUserId()).orElse(null);
                if (successorUser != null) {
                    NotificationInboxEntity inbox = new NotificationInboxEntity();
                    inbox.setUserId(successor.getUserId());
                    inbox.setType("GUARDIAN_PROMOTED");
                    inbox.setTitle("您已成为主监护人");
                    inbox.setContent(String.format(
                            "原主监护人账号因管理原因被停用。您现在是患者【%s】的主监护人，当前寻回任务（编号：%s）正在进行中，请及时关注进展。",
                            patientName, activeTask.getTaskNo()));
                    inbox.setLevel("WARNING");
                    inbox.setRelatedPatientId(patientId);
                    inbox.setRelatedTaskId(activeTask.getId());
                    inbox.setRelatedObjectId(String.valueOf(patientId));
                    inbox.setTraceId(TraceIdUtil.currentTraceId());
                    notificationInboxRepository.save(inbox);

                    // 2d. Outbox 事件 → 消费方负责发送邮件 + 推送
                    Map<String, Object> promotedPayload = new HashMap<>();
                    promotedPayload.put("successor_user_id", String.valueOf(successor.getUserId()));
                    promotedPayload.put("successor_email", successorUser.getEmail());
                    promotedPayload.put("successor_nickname", successorUser.getNickname());
                    promotedPayload.put("patient_id", String.valueOf(patientId));
                    promotedPayload.put("patient_name", patientName);
                    promotedPayload.put("task_no", activeTask.getTaskNo());
                    promotedPayload.put("disabled_user_id", String.valueOf(userId));
                    promotedPayload.put("occurred_at", now.toString());
                    outboxService.publish(OutboxTopics.GUARDIAN_PROMOTED,
                            String.valueOf(patientId), String.valueOf(successor.getUserId()), promotedPayload);

                    log.info("[disable] 顺位继承完成 patientId={} 新主监护 userId={} email={}",
                            patientId, successor.getUserId(), successorUser.getEmail());
                }
            });
        }

        // 3. 执行 CAS 禁用
        int rows = userRepository.casStatus(userId, "ACTIVE", "DISABLED");
        if (rows == 0) throw BizException.of(ErrorCode.E_USR_4091);

        // 4. 立即吊销 JWT
        jwtRevocationService.revokeAllForUser(userId);

        // 5. Outbox：user.disabled（含 primaryPatientIds 供下游感知）
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", String.valueOf(userId));
        payload.put("operator_user_id", me.getUserId());
        payload.put("reason", req.getReason());
        payload.put("primary_patient_ids", primaryPatientIds.stream().map(String::valueOf).toList());
        payload.put("occurred_at", now.toString());
        outboxService.publish(OutboxTopics.USER_DISABLED,
                String.valueOf(userId), String.valueOf(userId), payload);

        auditLogger.logSuccess("GOV", "admin.user.disable", String.valueOf(userId),
                "HIGH", "CONFIRM_2",
                Map.of("target_user_id", userId, "reason", req.getReason(),
                        "guardian_promoted_patients", primaryPatientIds));
    }

    // =========================================================
    // 5. 启用（POST /api/v1/admin/users/{id}/enable）—— MEDIUM + CONFIRM_1
    // =========================================================
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long userId, AdminUserActionRequest req, String confirmLevel) {
        AuthUser me = SecurityUtil.current();
        assertAdmin(me);
        assertNotSelf(me, userId);
        requireConfirmLevel(confirmLevel, "CONFIRM_1");
        UserEntity u = loadAndAssertVisible(userId, me);

        // 启用仅作用于 DISABLED
        if (!"DISABLED".equals(u.getStatus())) throw BizException.of(ErrorCode.E_USR_4091);
        int rows = userRepository.casStatus(userId, "DISABLED", "ACTIVE");
        if (rows == 0) throw BizException.of(ErrorCode.E_USR_4091);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", String.valueOf(userId));
        payload.put("operator_user_id", me.getUserId());
        payload.put("reason", req.getReason());
        payload.put("occurred_at", OffsetDateTime.now().toString());
        outboxService.publish(OutboxTopics.USER_ENABLED,
                String.valueOf(userId), String.valueOf(userId), payload);

        auditLogger.logSuccess("GOV", "admin.user.enable", String.valueOf(userId),
                "MEDIUM", "CONFIRM_1",
                Map.of("target_user_id", userId, "reason", req.getReason()));
    }

    // =========================================================
    // 6. 注销（DELETE /api/v1/admin/users/{id}）—— CRITICAL + CONFIRM_3
    // =========================================================
    @Transactional(rollbackFor = Exception.class)
    public void deactivate(Long userId, AdminUserActionRequest req, String confirmLevel) {
        AuthUser me = SecurityUtil.current();
        assertAdmin(me);
        assertNotSelf(me, userId);
        requireConfirmLevel(confirmLevel, "CONFIRM_3");
        UserEntity u = loadAndAssertVisible(userId, me);

        // 规则 3：SUPER_ADMIN 不可注销
        if ("SUPER_ADMIN".equals(u.getRole())) throw BizException.of(ErrorCode.E_USR_4033);
        // 幂等：已 DEACTIVATED 视为成功（不重复发事件）
        if ("DEACTIVATED".equals(u.getStatus())) {
            log.info("[Admin] 用户 id={} 已为 DEACTIVATED, 幂等返回", userId);
            return;
        }
        // 前置校验 A：仍为患者主监护 → E_USR_4092
        List<Long> primary = guardianRelationRepository.findPrimaryActivePatientIds(userId);
        if (!primary.isEmpty()) {
            throw BizException.of(ErrorCode.E_USR_4092,
                    "仍为 " + primary.size() + " 位患者主监护人,请先转移或降级主监护关系");
        }
        // 前置校验 B：仍有未终态物资工单
        long openOrders = tagApplyRecordRepository.countByApplicantUserIdAndStatusIn(userId, MATERIAL_ORDER_OPEN);
        if (openOrders > 0) {
            throw BizException.of(ErrorCode.E_USR_4093,
                    "仍有 " + openOrders + " 个未终态物资工单");
        }
        // 前置校验 C：仍有未终态寻回任务
        long activeRescue = rescueTaskRepository.countActiveByCreator(userId);
        if (activeRescue > 0) {
            throw BizException.of(ErrorCode.E_USR_4093,
                    "仍有 " + activeRescue + " 个 ACTIVE/SUSTAINED 寻回任务");
        }

        long epoch = System.currentTimeMillis() / 1000L;
        String suffix = "#DEL_" + epoch;
        int rows = userRepository.casDeactivate(userId, suffix, OffsetDateTime.now());
        if (rows == 0) {
            // 并发兜底：另一路已迁出 ACTIVE/DISABLED
            throw BizException.of(ErrorCode.E_USR_4091);
        }

        // 撤销非主监护活跃关系（按 LLD §8.3.13）
        int revokedRelations = guardianRelationRepository.revokeNonPrimaryForUser(userId);

        jwtRevocationService.revokeAllForUser(userId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", String.valueOf(userId));
        payload.put("operator_user_id", me.getUserId());
        payload.put("reason", req.getReason());
        payload.put("revoked_relations", revokedRelations);
        payload.put("occurred_at", OffsetDateTime.now().toString());
        outboxService.publish(OutboxTopics.USER_DEACTIVATED,
                String.valueOf(userId), String.valueOf(userId), payload);

        auditLogger.logSuccess("GOV", "admin.user.deactivate", String.valueOf(userId),
                "CRITICAL", "CONFIRM_3",
                Map.of("target_user_id", userId,
                        "reason", req.getReason(),
                        "suffix", suffix,
                        "revoked_relations", revokedRelations));
    }

    // ===============================================
    // 内部辅助
    // ===============================================

    /** 规则 1：仅 ADMIN / SUPER_ADMIN 可访问。 */
    private void assertAdmin(AuthUser me) {
        if (!me.isAdmin()) throw BizException.of(ErrorCode.E_AUTH_4031);
    }

    /** 规则 4：不可对自身执行。 */
    private void assertNotSelf(AuthUser me, Long targetUserId) {
        if (me.getUserId().equals(targetUserId)) throw BizException.of(ErrorCode.E_USR_4034);
    }

    /** 规则 2：加载并校验可见性；不可见一律返回 404（不泄露账号存在性）。自身账号始终可见。 */
    private UserEntity loadAndAssertVisible(Long userId, AuthUser me) {
        UserEntity u = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_USR_4041));
        // 自身账号始终允许加载（自编辑场景）
        if (me.getUserId().equals(userId)) return u;
        if (!me.isSuperAdmin() && !"FAMILY".equals(u.getRole())) {
            // ADMIN 不可见 ADMIN/SUPER_ADMIN：按 4041 返回避免枚举
            throw BizException.of(ErrorCode.E_USR_4041);
        }
        return u;
    }

    /** Header CONFIRM 校验。 */
    private void requireConfirmLevel(String actual, String expected) {
        if (actual == null || !actual.equals(expected)) {
            throw BizException.of(ErrorCode.E_AUTH_4031,
                    "该操作需要 X-Confirm-Level=" + expected);
        }
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) return 20;
        return Math.min(pageSize, 100);
    }

    private AdminUserListItem toListItem(UserEntity u) {
        AdminUserListItem i = new AdminUserListItem();
        i.setUserId(String.valueOf(u.getId()));
        i.setUsername(u.getUsername());
        i.setNickname(u.getNickname());
        i.setEmail(DesensitizeUtil.email(u.getEmail()));
        i.setPhone(DesensitizeUtil.phone(u.getPhone()));
        i.setRole(u.getRole());
        i.setStatus(u.getStatus());
        i.setEmailVerified(u.getEmailVerified());
        i.setLastLoginAt(u.getLastLoginAt());
        i.setCreatedAt(u.getCreatedAt());
        return i;
    }

    private Map<String, Object> snapshot(UserEntity u) {
        Map<String, Object> m = new HashMap<>();
        m.put("nickname", u.getNickname());
        m.put("email", u.getEmail()); // 内部审计字段，不对外返回
        m.put("phone", u.getPhone());
        m.put("role", u.getRole());
        m.put("status", u.getStatus());
        return m;
    }

    private AdminUserDetailResponse detailView(UserEntity u) {
        AdminUserDetailResponse r = new AdminUserDetailResponse();
        r.setUserId(String.valueOf(u.getId()));
        r.setUsername(u.getUsername());
        r.setNickname(u.getNickname());
        r.setEmail(DesensitizeUtil.email(u.getEmail()));
        r.setPhone(DesensitizeUtil.phone(u.getPhone()));
        r.setRole(u.getRole());
        r.setStatus(u.getStatus());
        r.setEmailVerified(u.getEmailVerified());
        r.setLastLoginAt(u.getLastLoginAt());
        r.setLastLoginIp(u.getLastLoginIp());
        r.setDeactivatedAt(u.getDeactivatedAt());
        r.setCreatedAt(u.getCreatedAt());
        r.setUpdatedAt(u.getUpdatedAt());
        return r;
    }

    /** 包可见：供测试覆盖数量检验。 */
    static Collection<String> materialOrderOpenStatuses() { return MATERIAL_ORDER_OPEN; }
}
