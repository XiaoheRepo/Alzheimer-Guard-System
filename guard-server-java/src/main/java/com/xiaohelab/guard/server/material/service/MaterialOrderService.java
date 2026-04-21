package com.xiaohelab.guard.server.material.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.material.dto.OrderCreateRequest;
import com.xiaohelab.guard.server.material.dto.OrderReviewRequest;
import com.xiaohelab.guard.server.material.dto.OrderShipRequest;
import com.xiaohelab.guard.server.material.entity.TagApplyRecordEntity;
import com.xiaohelab.guard.server.material.entity.TagAssetEntity;
import com.xiaohelab.guard.server.material.repository.TagApplyRecordRepository;
import com.xiaohelab.guard.server.material.repository.TagAssetRepository;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(MaterialOrderService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TagApplyRecordRepository orderRepository;
    private final TagAssetRepository tagRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

    public MaterialOrderService(TagApplyRecordRepository orderRepository, TagAssetRepository tagRepository,
                                GuardianAuthorizationService authorizationService, OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.tagRepository = tagRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
    }

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

    public TagApplyRecordEntity get(Long orderId) {
        AuthUser user = SecurityUtil.current();
        TagApplyRecordEntity o = orderRepository.findById(orderId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_MAT_4041));
        if (!user.isAdmin() && !o.getApplicantUserId().equals(user.getUserId())) {
            authorizationService.assertGuardian(user, o.getPatientId());
        }
        return o;
    }

    public Page<TagApplyRecordEntity> listMine(int page, int size) {
        AuthUser user = SecurityUtil.current();
        return orderRepository.findByApplicantUserIdOrderByCreatedAtDesc(user.getUserId(), PageRequest.of(page, size));
    }
}
