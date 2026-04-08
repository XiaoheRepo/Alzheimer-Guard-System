package com.xiaohelab.guard.server.application.material;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.application.governance.AuditLogService;
import com.xiaohelab.guard.server.domain.guardian.repository.GuardianRepository;
import com.xiaohelab.guard.server.domain.material.repository.MaterialOrderRepository;
import com.xiaohelab.guard.server.domain.tag.entity.TagApplyRecordEntity;
import com.xiaohelab.guard.server.domain.tag.entity.TagAssetEntity;
import com.xiaohelab.guard.server.domain.tag.repository.TagAssetRepository;
import com.xiaohelab.guard.server.domain.governance.entity.SysLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 物资申领与标签管理服务。
 * 申领工单状态机：PENDING → PROCESSING → SHIPPED → COMPLETED
 *                PENDING/PROCESSING → CANCEL_PENDING → CANCELLED
 *                SHIPPED → EXCEPTION → SHIPPED（重发）
 */
@Service
@RequiredArgsConstructor
public class MaterialOrderService {

    private final MaterialOrderRepository orderRepository;
    private final TagAssetRepository tagAssetRepository;
    private final GuardianRepository guardianRepository;
    private final AuditLogService auditLogService;

    // ===== 用户端操作 =====

    /**
     * 创建申领工单（用户发起）。
     */
    @Transactional
    public TagApplyRecordEntity createOrder(Long applicantUserId, Long patientId,
                                            Integer quantity, String applyNote, String deliveryAddress) {
        // 归属权校验
        if (guardianRepository.countActiveRelation(applicantUserId, patientId) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        // 防重复：同一患者只能有一个进行中的工单
        if (orderRepository.findOpenByPatientId(patientId).isPresent()) {
            throw BizException.of("E_ORDER_4091");
        }
        TagApplyRecordEntity order = TagApplyRecordEntity.create(
                IdGenerator.orderNo(), patientId, applicantUserId, quantity, applyNote, deliveryAddress);
        orderRepository.insert(order);
        return order;
    }

    /**
     * 用户申请取消（→ CANCEL_PENDING，等待管理员审核）。
     */
    @Transactional
    public TagApplyRecordEntity cancelOrder(Long orderId, Long userId, String cancelReason) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!order.getApplicantUserId().equals(userId)) throw BizException.of("E_TASK_4030");
        if (!"PENDING".equals(order.getStatus()) && !"PROCESSING".equals(order.getStatus())) {
            throw BizException.of("E_ORDER_4093");
        }
        order.requestCancel(cancelReason);
        orderRepository.update(order);
        return order;
    }

    /**
     * 用户确认收货（SHIPPED → COMPLETED）。
     */
    @Transactional
    public TagApplyRecordEntity confirmReceipt(Long orderId, Long userId) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!order.getApplicantUserId().equals(userId)) throw BizException.of("E_TASK_4030");
        if (!"SHIPPED".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.confirmReceipt();
        orderRepository.update(order);
        return order;
    }

    // ===== 管理员操作 =====

    /**
     * 管理员审批通过（PENDING → PROCESSING）。
     */
    @Transactional
    public TagApplyRecordEntity adminApprove(Long orderId) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!"PENDING".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.approve();
        orderRepository.update(order);
        return order;
    }

    /**
     * 管理员发货（PROCESSING → SHIPPED，锁定标签码）。
     */
    @Transactional
    public TagApplyRecordEntity adminShip(Long orderId, String tagCode,
                                          String trackingNumber, String courierName, String resourceLink) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!"PROCESSING".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");

        // 标签分配：将标签与工单绑定
        TagAssetEntity tag = tagAssetRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of("E_TAG_4041"));
        if (!"UNBOUND".equals(tag.getStatus())) throw BizException.of("E_TAG_4093");
        tagAssetRepository.allocate(tag.getId(), orderId);

        order.ship(tagCode, trackingNumber, courierName, resourceLink);
        orderRepository.update(order);
        return order;
    }

    /**
     * 管理员批准取消申请（CANCEL_PENDING → CANCELLED）。
     */
    @Transactional
    public TagApplyRecordEntity adminApproveCancelRequest(Long orderId) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!"CANCEL_PENDING".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.approveCancelRequest();
        orderRepository.update(order);
        return order;
    }

    /**
     * 管理员拒绝取消申请（CANCEL_PENDING → PROCESSING）。
     */
    @Transactional
    public TagApplyRecordEntity adminRejectCancelRequest(Long orderId) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!"CANCEL_PENDING".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.rejectCancelRequest();
        orderRepository.update(order);
        return order;
    }

    /**
     * 管理员标记物流异常（SHIPPED → EXCEPTION）。
     */
    @Transactional
    public TagApplyRecordEntity adminMarkException(Long orderId, String exceptionDesc) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!"SHIPPED".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.markException(exceptionDesc);
        orderRepository.update(order);
        return order;
    }

    /**
     * 管理员重新发货（EXCEPTION → SHIPPED）。
     */
    @Transactional
    public TagApplyRecordEntity adminReship(Long orderId, String trackingNumber, String courierName) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!"EXCEPTION".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.reship(trackingNumber, courierName);
        orderRepository.update(order);
        return order;
    }

    /**
     * 管理员强制关闭异常工单（EXCEPTION → COMPLETED）。
     */
    @Transactional
    public TagApplyRecordEntity adminCloseException(Long orderId) {
        TagApplyRecordEntity order = requireOrder(orderId);
        if (!"EXCEPTION".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.closeException();
        orderRepository.update(order);
        return order;
    }

    // ===== 标签操作 =====

    /**
     * 用户绑定标签（ALLOCATED/UNBOUND → BOUND）。
     */
    @Transactional
    public TagAssetEntity bindTag(Long patientId, Long userId, String tagCode) {
        if (guardianRepository.countActiveRelation(userId, patientId) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        TagAssetEntity tag = tagAssetRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of("E_TAG_4041"));
        if (!"ALLOCATED".equals(tag.getStatus()) && !"UNBOUND".equals(tag.getStatus())) {
            throw BizException.of("E_TAG_4093");
        }
        tagAssetRepository.bindToPatient(tag.getId(), patientId);
        return tagAssetRepository.findByTagCode(tagCode).orElseThrow(() -> BizException.of("E_TAG_4041"));
    }

    /**
     * 用户上报标签丢失（BOUND → LOST）。
     */
    @Transactional
    public TagAssetEntity reportLost(Long patientId, Long userId, String tagCode) {
        if (guardianRepository.countActiveRelation(userId, patientId) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        TagAssetEntity tag = tagAssetRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of("E_TAG_4041"));
        if (!"BOUND".equals(tag.getStatus()) || !patientId.equals(tag.getPatientId())) {
            throw BizException.of("E_TAG_4093");
        }
        tagAssetRepository.markLost(tag.getId());
        return tagAssetRepository.findByTagCode(tagCode).orElseThrow(() -> BizException.of("E_TAG_4041"));
    }

    // ===== 管理员标签库存操作 =====

    /**
     * 管理员批量导入标签。
     */
    @Transactional
    public void adminImportTags(List<String> tagCodes, String tagType, String batchNo) {
        for (String code : tagCodes) {
            if (tagAssetRepository.findByTagCode(code).isPresent()) continue; // 幂等跳过
            TagAssetEntity tag = TagAssetEntity.create(code, tagType, batchNo);
            tagAssetRepository.insert(tag);
        }
    }

    /**
     * 管理员作废标签。
     */
    @Transactional
    public TagAssetEntity adminVoidTag(String tagCode, String voidReason) {
        TagAssetEntity tag = requireTag(tagCode);
        tagAssetRepository.voidTag(tag.getId(), voidReason);
        return tagAssetRepository.findByTagCode(tagCode).orElseThrow(() -> BizException.of("E_TAG_4041"));
    }

    /**
     * 管理员重置标签（LOST/VOID → UNBOUND）。
     */
    @Transactional
    public TagAssetEntity adminResetTag(String tagCode) {
        TagAssetEntity tag = requireTag(tagCode);
        tagAssetRepository.resetTag(tag.getId());
        return tagAssetRepository.findByTagCode(tagCode).orElseThrow(() -> BizException.of("E_TAG_4041"));
    }

    /**
     * 管理员恢复丢失标签（LOST → BOUND）。
     */
    @Transactional
    public TagAssetEntity adminRecoverTag(String tagCode) {
        TagAssetEntity tag = requireTag(tagCode);
        if (!"LOST".equals(tag.getStatus())) throw BizException.of("E_TAG_4093");
        tagAssetRepository.recover(tag.getId());
        return tagAssetRepository.findByTagCode(tagCode).orElseThrow(() -> BizException.of("E_TAG_4041"));
    }

    // ===== 查询 =====

    public List<TagApplyRecordEntity> listMyOrders(Long userId, int pageNo, int pageSize) {
        return orderRepository.listByApplicant(userId, pageSize, (pageNo - 1) * pageSize);
    }

    public long countMyOrders(Long userId) {
        return orderRepository.countByApplicant(userId);
    }

    public List<TagApplyRecordEntity> adminListOrders(String status, int pageNo, int pageSize) {
        return orderRepository.listByStatus(status, pageSize, (pageNo - 1) * pageSize);
    }

    public long adminCountOrders(String status) {
        return orderRepository.countByStatus(status);
    }

    public TagApplyRecordEntity getOrder(Long orderId) {
        return requireOrder(orderId);
    }

    /** 按标签码查询标签资产（供 PatientController / MaterialController 使用）。 */
    public TagAssetEntity getTagByCode(String tagCode) {
        return tagAssetRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of("E_TAG_4041"));
    }

    /** 按患者 ID + 状态过滤查询标签列表（供 PatientController 使用）。 */
    public List<TagAssetEntity> listPatientTags(Long patientId, String status, int limit, int offset) {
        return tagAssetRepository.listByFilter(status, patientId, limit, offset);
    }

    /** 按资源令牌解析申领工单（供 MaterialController 使用）。 */
    public TagApplyRecordEntity resolveByResourceToken(String token) {
        return orderRepository.findByResourceToken(token)
                .orElseThrow(() -> BizException.of("E_ORDER_4041"));
    }

    /** 查询标签历史审计日志（供 PatientController 使用）。 */
    public List<SysLogEntity> listTagHistory(String tagCode, int limit, int offset) {
        return auditLogService.listByModuleAndObjectId("TAG_ASSET", tagCode, limit, offset);
    }

    public long countTagHistory(String tagCode) {
        return auditLogService.countByModuleAndObjectId("TAG_ASSET", tagCode);
    }

    // ===== 管理员标签查询 =====

    public List<TagAssetEntity> adminListTagsByFilter(String status, Long patientId, int pageSize, int offset) {
        return tagAssetRepository.listByFilter(status, patientId, pageSize, offset);
    }

    public long adminCountTagsByFilter(String status, Long patientId) {
        return tagAssetRepository.countByFilter(status, patientId);
    }

    /**
     * 管理员手工分配标签到工单（UNBOUND → ALLOCATED）。
     * 返回受影响行数，0 表示状态冲突。
     */
    @Transactional
    public int adminAllocateTag(String tagCode, Long orderId) {
        TagAssetEntity tag = requireTag(tagCode);
        return tagAssetRepository.allocate(tag.getId(), orderId);
    }

    /**
     * 管理员释放已分配标签（ALLOCATED → UNBOUND）。
     * 返回受影响行数，0 表示状态冲突。
     */
    @Transactional
    public int adminReleaseTag(String tagCode) {
        return tagAssetRepository.releaseByTagCode(tagCode);
    }

    // ===== 内部工具 =====

    private TagApplyRecordEntity requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> BizException.of("E_ORDER_4041"));
    }

    private TagAssetEntity requireTag(String tagCode) {
        return tagAssetRepository.findByTagCode(tagCode)
                .orElseThrow(() -> BizException.of("E_TAG_4041"));
    }
}
