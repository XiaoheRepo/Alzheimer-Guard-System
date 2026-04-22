package com.xiaohelab.guard.server.material.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.gov.service.AuditLogger;
import com.xiaohelab.guard.server.material.dto.OrderCreateRequest;
import com.xiaohelab.guard.server.material.dto.OrderResolveExceptionRequest;
import com.xiaohelab.guard.server.material.dto.OrderReviewRequest;
import com.xiaohelab.guard.server.material.dto.OrderShipRequest;
import com.xiaohelab.guard.server.material.entity.TagApplyRecordEntity;
import com.xiaohelab.guard.server.material.entity.TagAssetEntity;
import com.xiaohelab.guard.server.material.repository.TagApplyRecordRepository;
import com.xiaohelab.guard.server.material.repository.TagAssetRepository;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 物资工单服务：申请/审核/发货/收货/取消。标签分配在审核通过时执行。 */
@Service
public class MaterialOrderService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TagApplyRecordRepository orderRepository;
    private final TagAssetRepository tagRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;
    private final AuditLogger auditLogger;

    public MaterialOrderService(TagApplyRecordRepository orderRepository, TagAssetRepository tagRepository,
                                GuardianAuthorizationService authorizationService, OutboxService outboxService,
                                AuditLogger auditLogger) {
        this.orderRepository = orderRepository;
        this.tagRepository = tagRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
        this.auditLogger = auditLogger;
    }

    /**
     * 家属创建物资工单（PENDING_AUDIT）。
     * 落库同时通过 Outbox 发布 MAT_ORDER_CREATED 事件供通知/对账消费。
     *
     * @param req 工单创建请求（收货信息、数量、备注等）
     * @return 已落库的工单实体
     * @throws BizException E_PRO_4033 无监护权
     */
    @Transactional(rollbackFor = Exception.class)
    public TagApplyRecordEntity create(OrderCreateRequest req) {
        AuthUser user = SecurityUtil.current();
        authorizationService.assertGuardian(user, req.getPatientId());
        TagApplyRecordEntity o = new TagApplyRecordEntity();
        o.setOrderNo(BusinessNoUtil.orderNo());
        o.setPatientId(req.getPatientId());
        o.setApplicantUserId(user.getUserId());
        o.setTagType(req.getTagType());
        o.setQuantity(req.getQuantity());
        o.setRemark(req.getRemark());
        o.setShippingProvince(req.getShippingProvince());
        o.setShippingCity(req.getShippingCity());
        o.setShippingDistrict(req.getShippingDistrict());
        o.setShippingDetail(req.getShippingDetail());
        o.setShippingReceiver(req.getShippingReceiver());
        o.setShippingPhone(req.getShippingPhone());
        o.setStatus("PENDING_AUDIT");
        orderRepository.save(o);
        outboxService.publish(OutboxTopics.MAT_ORDER_CREATED, o.getOrderNo(), String.valueOf(o.getPatientId()),
                Map.of("order_id", o.getId(), "order_no", o.getOrderNo(), "patient_id", o.getPatientId(),
                        "applicant", user.getUserId(), "quantity", o.getQuantity(), "tag_type", o.getTagType()));
        return o;
    }

    /**
     * 管理员审核：APPROVE 则从标签池锁定分配 N 枚标签（SKIP LOCKED），REJECT 则置 REJECTED。
     * APPROVE 成功后发布 MAT_ORDER_APPROVED + TAG_ALLOCATED 两条事件。
     *
     * @param orderId 工单主键
     * @param req     审核请求（action=APPROVE/REJECT）
     * @throws BizException E_MAT_4030 非管理员；E_MAT_4041 工单不存在；
     *                      E_MAT_4091 状态非 PENDING_AUDIT；E_MAT_4221 标签库存不足
     */
    @Transactional(rollbackFor = Exception.class)
    public TagApplyRecordEntity review(Long orderId, OrderReviewRequest req) {
        AuthUser user = SecurityUtil.current();
        if (!user.isAdmin()) throw BizException.of(ErrorCode.E_MAT_4030);
        TagApplyRecordEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4041));
        if (!"PENDING_AUDIT".equals(o.getStatus())) {
            throw BizException.of(ErrorCode.E_MAT_4091);
        }
        if ("APPROVE".equals(req.getAction())) {
            // 分配标签（FOR UPDATE SKIP LOCKED）
            List<TagAssetEntity> tags = tagRepository.claimUnbound(o.getTagType(), o.getQuantity());
            if (tags.size() < o.getQuantity()) {
                throw BizException.of(ErrorCode.E_MAT_4221);
            }
            OffsetDateTime now = OffsetDateTime.now();
            for (TagAssetEntity t : tags) {
                t.setStatus("ALLOCATED");
                t.setOrderId(o.getId());
                t.setAllocatedAt(now);
                tagRepository.save(t);
            }
            try {
                List<String> codes = tags.stream().map(TagAssetEntity::getTagCode).collect(Collectors.toList());
                o.setTagCodes(MAPPER.writeValueAsString(codes));
            } catch (JsonProcessingException e) {
                throw BizException.of(ErrorCode.E_SYS_5000, "tag_codes 序列化失败");
            }
            o.setStatus("PENDING_SHIP");
            o.setReviewerUserId(user.getUserId());
            o.setReviewedAt(OffsetDateTime.now());
            orderRepository.save(o);
            outboxService.publish(OutboxTopics.MAT_ORDER_APPROVED, o.getOrderNo(),
                    String.valueOf(o.getPatientId()),
                    Map.of("order_id", o.getId(), "reviewer", user.getUserId()));
            outboxService.publish(OutboxTopics.TAG_ALLOCATED, o.getOrderNo(), String.valueOf(o.getPatientId()),
                    Map.of("order_id", o.getId(), "tag_count", tags.size()));
        } else if ("REJECT".equals(req.getAction())) {
            o.setStatus("REJECTED");
            o.setReviewerUserId(user.getUserId());
            o.setReviewedAt(OffsetDateTime.now());
            o.setRejectReason(req.getReason());
            orderRepository.save(o);
            outboxService.publish(OutboxTopics.MAT_ORDER_REJECTED, o.getOrderNo(),
                    String.valueOf(o.getPatientId()),
                    Map.of("order_id", o.getId(), "reviewer", user.getUserId(), "reason", req.getReason()));
        } else {
            throw BizException.of(ErrorCode.E_REQ_4220);
        }
        return o;
    }

    /**
     * 管理员登记发货（PENDING_SHIP → SHIPPED）。
     * @throws BizException E_MAT_4091 状态非 PENDING_SHIP；E_MAT_4222 标签尚未分配
     */
    @Transactional(rollbackFor = Exception.class)
    public TagApplyRecordEntity ship(Long orderId, OrderShipRequest req) {
        AuthUser user = SecurityUtil.current();
        if (!user.isAdmin()) throw BizException.of(ErrorCode.E_MAT_4030);
        TagApplyRecordEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4041));
        if (!"PENDING_SHIP".equals(o.getStatus())) {
            throw BizException.of(ErrorCode.E_MAT_4091);
        }
        if (o.getTagCodes() == null) throw BizException.of(ErrorCode.E_MAT_4222);
        o.setStatus("SHIPPED");
        o.setLogisticsCompany(req.getLogisticsCompany());
        o.setLogisticsNo(req.getLogisticsNo());
        o.setShippedAt(OffsetDateTime.now());
        orderRepository.save(o);
        outboxService.publish(OutboxTopics.MAT_ORDER_SHIPPED, o.getOrderNo(), String.valueOf(o.getPatientId()),
                Map.of("order_id", o.getId(), "logistics_no", req.getLogisticsNo()));
        return o;
    }

    /**
     * 家属/管理员登记收货（SHIPPED → RECEIVED）。
     * @throws BizException E_MAT_4030 非申请人本人且非管理员；E_MAT_4091 状态非 SHIPPED
     */
    @Transactional(rollbackFor = Exception.class)
    public TagApplyRecordEntity receive(Long orderId) {
        AuthUser user = SecurityUtil.current();
        TagApplyRecordEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4041));
        if (!user.isAdmin() && !o.getApplicantUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_MAT_4030);
        }
        if (!"SHIPPED".equals(o.getStatus())) {
            throw BizException.of(ErrorCode.E_MAT_4091);
        }
        o.setStatus("RECEIVED");
        o.setReceivedAt(OffsetDateTime.now());
        orderRepository.save(o);
        outboxService.publish(OutboxTopics.MAT_ORDER_RECEIVED, o.getOrderNo(), String.valueOf(o.getPatientId()),
                Map.of("order_id", o.getId()));
        return o;
    }

    /**
     * 取消工单：仅允许在 PENDING_AUDIT 状态由申请人或管理员发起。
     * @throws BizException E_MAT_4030 越权；E_MAT_4094 状态不允许取消
     */
    @Transactional(rollbackFor = Exception.class)
    public TagApplyRecordEntity cancel(Long orderId, String reason) {
        AuthUser user = SecurityUtil.current();
        TagApplyRecordEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4041));
        if (!user.isAdmin() && !o.getApplicantUserId().equals(user.getUserId())) {
            throw BizException.of(ErrorCode.E_MAT_4030);
        }
        if (!"PENDING_AUDIT".equals(o.getStatus())) {
            throw BizException.of(ErrorCode.E_MAT_4094);
        }
        o.setStatus("CANCELLED");
        o.setCancelReason(reason);
        o.setCancelledAt(OffsetDateTime.now());
        orderRepository.save(o);
        return o;
    }

    /**
     * 查询单个工单：申请人本人或管理员直通；其它角色需具备对应患者监护权。
     * @throws BizException E_MAT_4041 工单不存在；E_MAT_4030 / E_PRO_4033 越权
     */
    public TagApplyRecordEntity get(Long orderId) {
        AuthUser user = SecurityUtil.current();
        TagApplyRecordEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4041));
        if (!user.isAdmin() && !o.getApplicantUserId().equals(user.getUserId())) {
            authorizationService.assertGuardian(user, o.getPatientId());
        }
        return o;
    }

    /**
     * 分页列出当前用户申请的工单（按创建时间倒序）。
     */
    public Page<TagApplyRecordEntity> listMine(int page, int size) {
        AuthUser user = SecurityUtil.current();
        return orderRepository.findByApplicantUserIdOrderByCreatedAtDesc(user.getUserId(), PageRequest.of(page, size));
    }

    /**
     * 管理员处置 EXCEPTION 工单（API §3.4.12，LLD §6.3.8，FR-MAT-004 / SRS AC-07）。
     *
     * <p>状态机：</p>
     * <ul>
     *   <li>{@code action = RESHIP}：EXCEPTION → SHIPPED（携带新物流单号/承运商）</li>
     *   <li>{@code action = VOID}：EXCEPTION → VOIDED（行政终态，不再流转）</li>
     * </ul>
     *
     * <p>前置：</p>
     * <ol>
     *   <li>仅 ADMIN / SUPER_ADMIN 可执行（E_AUTH_4031）</li>
     *   <li>工单必须存在且 status = EXCEPTION（否则 E_MAT_4091）</li>
     *   <li>RESHIP 时 tracking_no + carrier 必填（Controller 层 @Valid + Service 二次强校验）</li>
     *   <li>RESHIP 时 tracking_no 需全局唯一（否则 E_MAT_4224）</li>
     * </ol>
     */
    @Transactional(rollbackFor = Exception.class)
    public TagApplyRecordEntity resolveException(Long orderId, OrderResolveExceptionRequest req) {
        AuthUser user = SecurityUtil.current();
        // 授权：ADMIN / SUPER_ADMIN（handbook §24.3 rule 1）
        if (!user.isAdmin()) throw BizException.of(ErrorCode.E_AUTH_4031);

        TagApplyRecordEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4041));
        if (!"EXCEPTION".equals(o.getStatus())) {
            throw BizException.of(ErrorCode.E_MAT_4091);
        }

        String action = req.getAction();
        OffsetDateTime now = OffsetDateTime.now();
        String fromStatus = o.getStatus();

        if ("RESHIP".equals(action)) {
            // RESHIP 所需参数强校验（兜底 DTO @Pattern）
            if (req.getTrackingNo() == null || req.getTrackingNo().isBlank()
                    || req.getCarrier() == null || req.getCarrier().isBlank()) {
                throw BizException.of(ErrorCode.E_MAT_4226, "RESHIP 需同时提供 tracking_no 与 carrier");
            }
            // 补发物流单号全局唯一性校验
            orderRepository.findByLogisticsNo(req.getTrackingNo()).ifPresent(dup -> {
                if (!dup.getId().equals(orderId)) {
                    throw BizException.of(ErrorCode.E_MAT_4224,
                            "tracking_no 已被工单 " + dup.getOrderNo() + " 占用");
                }
            });
            o.setLogisticsNo(req.getTrackingNo());
            o.setLogisticsCompany(req.getCarrier());
            o.setShippedAt(now);
            o.setStatus("SHIPPED");
        } else if ("VOID".equals(action)) {
            o.setStatus("VOIDED");
        } else {
            // 理论上 DTO 层已拦截，保留防御
            throw BizException.of(ErrorCode.E_MAT_4226);
        }

        // 共通处置落库
        o.setResolveReason(req.getReason());
        o.setResolvedBy(user.getUserId());
        o.setResolvedAt(now);
        orderRepository.save(o);

        // Outbox：两种动作对应独立 Topic（便于对账 / 财务补偿分路消费）
        String topic = "RESHIP".equals(action)
                ? OutboxTopics.MAT_ORDER_EXCEPTION_RESHIPPED
                : OutboxTopics.MAT_ORDER_EXCEPTION_VOIDED;
        outboxService.publish(topic, o.getOrderNo(), String.valueOf(o.getPatientId()),
                Map.of(
                        "order_id", o.getId(),
                        "order_no", o.getOrderNo(),
                        "patient_id", o.getPatientId(),
                        "from_status", fromStatus,
                        "to_status", o.getStatus(),
                        "operator_user_id", user.getUserId(),
                        "reason", req.getReason(),
                        "tracking_no", req.getTrackingNo() == null ? "" : req.getTrackingNo(),
                        "carrier", req.getCarrier() == null ? "" : req.getCarrier()
                ));

        // 审计（HIGH + CONFIRM_2，见 handbook §12.1 白名单）
        auditLogger.logSuccess("MAT", "admin_resolve_exception_order",
                String.valueOf(o.getId()), "HIGH", "CONFIRM_2",
                Map.of(
                        "order_id", o.getId(),
                        "action", action,
                        "from_status", fromStatus,
                        "to_status", o.getStatus(),
                        "reason", req.getReason(),
                        "tracking_no", req.getTrackingNo() == null ? "" : req.getTrackingNo()
                ));
        return o;
    }
}
