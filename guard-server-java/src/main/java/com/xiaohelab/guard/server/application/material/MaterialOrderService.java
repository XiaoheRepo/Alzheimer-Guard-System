package com.xiaohelab.guard.server.application.material;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.util.IdGenerator;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagApplyRecordDO;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagAssetDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.SysUserPatientMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.TagApplyRecordMapper;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.TagAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    private final TagApplyRecordMapper orderMapper;
    private final TagAssetMapper tagAssetMapper;
    private final SysUserPatientMapper sysUserPatientMapper;

    // ===== 用户端操作 =====

    /**
     * 创建申领工单（用户发起）。
     */
    @Transactional
    public TagApplyRecordDO createOrder(Long applicantUserId, Long patientId,
                                         Integer quantity, String applyNote, String deliveryAddress) {
        // 归属权校验
        if (sysUserPatientMapper.countActiveRelation(applicantUserId, patientId) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        // 防重复：同一患者只能有一个进行中的工单
        if (orderMapper.findOpenByPatientId(patientId) != null) {
            throw BizException.of("E_ORDER_4091");
        }
        TagApplyRecordDO order = new TagApplyRecordDO();
        order.setOrderNo(IdGenerator.orderNo());
        order.setPatientId(patientId);
        order.setApplicantUserId(applicantUserId);
        order.setQuantity(quantity);
        order.setApplyNote(applyNote);
        order.setStatus("PENDING");
        order.setDeliveryAddress(deliveryAddress);
        orderMapper.insert(order);
        return order;
    }

    /**
     * 用户申请取消（→ CANCEL_PENDING，等待管理员审核）。
     */
    @Transactional
    public TagApplyRecordDO cancelOrder(Long orderId, Long userId, String cancelReason) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!order.getApplicantUserId().equals(userId)) throw BizException.of("E_TASK_4030");
        if (!"PENDING".equals(order.getStatus()) && !"PROCESSING".equals(order.getStatus())) {
            throw BizException.of("E_ORDER_4093");
        }
        order.setStatus("CANCEL_PENDING");
        order.setCancelReason(cancelReason);
        orderMapper.update(order);
        return order;
    }

    /**
     * 用户确认收货（SHIPPED → COMPLETED）。
     */
    @Transactional
    public TagApplyRecordDO confirmReceipt(Long orderId, Long userId) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!order.getApplicantUserId().equals(userId)) throw BizException.of("E_TASK_4030");
        if (!"SHIPPED".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.setStatus("COMPLETED");
        order.setClosedAt(Instant.now());
        orderMapper.update(order);
        return order;
    }

    // ===== 管理员操作 =====

    /**
     * 管理员审批通过（PENDING → PROCESSING）。
     */
    @Transactional
    public TagApplyRecordDO adminApprove(Long orderId) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!"PENDING".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.setStatus("PROCESSING");
        order.setApprovedAt(Instant.now());
        orderMapper.update(order);
        return order;
    }

    /**
     * 管理员发货（PROCESSING → SHIPPED，锁定标签码）。
     */
    @Transactional
    public TagApplyRecordDO adminShip(Long orderId, String tagCode,
                                       String trackingNumber, String courierName, String resourceLink) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!"PROCESSING".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");

        // 标签分配：将标签与工单绑定
        TagAssetDO tag = tagAssetMapper.findByTagCode(tagCode);
        if (tag == null) throw BizException.of("E_TAG_4041");
        if (!"UNBOUND".equals(tag.getStatus())) throw BizException.of("E_TAG_4093");
        tagAssetMapper.allocate(tag.getId(), orderId);

        order.setStatus("SHIPPED");
        order.setTagCode(tagCode);
        order.setTrackingNumber(trackingNumber);
        order.setCourierName(courierName);
        order.setResourceLink(resourceLink);
        orderMapper.update(order);
        return order;
    }

    /**
     * 管理员批准取消申请（CANCEL_PENDING → CANCELLED）。
     */
    @Transactional
    public TagApplyRecordDO adminApproveCancelRequest(Long orderId) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!"CANCEL_PENDING".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.setStatus("CANCELLED");
        order.setClosedAt(Instant.now());
        orderMapper.update(order);
        return order;
    }

    /**
     * 管理员拒绝取消申请（CANCEL_PENDING → PROCESSING）。
     */
    @Transactional
    public TagApplyRecordDO adminRejectCancelRequest(Long orderId) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!"CANCEL_PENDING".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.setStatus("PROCESSING");
        orderMapper.update(order);
        return order;
    }

    /**
     * 管理员标记物流异常（SHIPPED → EXCEPTION）。
     */
    @Transactional
    public TagApplyRecordDO adminMarkException(Long orderId, String exceptionDesc) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!"SHIPPED".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.setStatus("EXCEPTION");
        order.setExceptionDesc(exceptionDesc);
        orderMapper.update(order);
        return order;
    }

    /**
     * 管理员重新发货（EXCEPTION → SHIPPED）。
     */
    @Transactional
    public TagApplyRecordDO adminReship(Long orderId, String trackingNumber, String courierName) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!"EXCEPTION".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.setStatus("SHIPPED");
        order.setTrackingNumber(trackingNumber);
        order.setCourierName(courierName);
        orderMapper.update(order);
        return order;
    }

    /**
     * 管理员强制关闭异常工单（EXCEPTION → COMPLETED）。
     */
    @Transactional
    public TagApplyRecordDO adminCloseException(Long orderId) {
        TagApplyRecordDO order = requireOrder(orderId);
        if (!"EXCEPTION".equals(order.getStatus())) throw BizException.of("E_ORDER_4093");
        order.setStatus("COMPLETED");
        order.setClosedAt(Instant.now());
        orderMapper.update(order);
        return order;
    }

    // ===== 标签操作 =====

    /**
     * 用户绑定标签（ALLOCATED/UNBOUND → BOUND）。
     */
    @Transactional
    public TagAssetDO bindTag(Long patientId, Long userId, String tagCode) {
        if (sysUserPatientMapper.countActiveRelation(userId, patientId) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        TagAssetDO tag = tagAssetMapper.findByTagCode(tagCode);
        if (tag == null) throw BizException.of("E_TAG_4041");
        if (!"ALLOCATED".equals(tag.getStatus()) && !"UNBOUND".equals(tag.getStatus())) {
            throw BizException.of("E_TAG_4093");
        }
        tagAssetMapper.bindToPatient(tag.getId(), patientId);
        return tagAssetMapper.findByTagCode(tagCode);
    }

    /**
     * 用户上报标签丢失（BOUND → LOST）。
     */
    @Transactional
    public TagAssetDO reportLost(Long patientId, Long userId, String tagCode) {
        if (sysUserPatientMapper.countActiveRelation(userId, patientId) == 0) {
            throw BizException.of("E_TASK_4030");
        }
        TagAssetDO tag = tagAssetMapper.findByTagCode(tagCode);
        if (tag == null) throw BizException.of("E_TAG_4041");
        if (!"BOUND".equals(tag.getStatus()) || !patientId.equals(tag.getPatientId())) {
            throw BizException.of("E_TAG_4093");
        }
        tagAssetMapper.markLost(tag.getId());
        return tagAssetMapper.findByTagCode(tagCode);
    }

    // ===== 管理员标签库存操作 =====

    /**
     * 管理员批量导入标签。
     */
    @Transactional
    public void adminImportTags(List<String> tagCodes, String tagType, String batchNo) {
        for (String code : tagCodes) {
            if (tagAssetMapper.findByTagCode(code) != null) continue; // 幂等跳过
            TagAssetDO tag = new TagAssetDO();
            tag.setTagCode(code);
            tag.setTagType(tagType);
            tag.setStatus("UNBOUND");
            tag.setImportBatchNo(batchNo);
            tagAssetMapper.insert(tag);
        }
    }

    /**
     * 管理员作废标签。
     */
    @Transactional
    public TagAssetDO adminVoidTag(String tagCode, String voidReason) {
        TagAssetDO tag = requireTag(tagCode);
        tagAssetMapper.voidTag(tag.getId(), voidReason);
        return tagAssetMapper.findByTagCode(tagCode);
    }

    /**
     * 管理员重置标签（LOST/VOID → UNBOUND）。
     */
    @Transactional
    public TagAssetDO adminResetTag(String tagCode) {
        TagAssetDO tag = requireTag(tagCode);
        tagAssetMapper.resetTag(tag.getId());
        return tagAssetMapper.findByTagCode(tagCode);
    }

    /**
     * 管理员恢复丢失标签（LOST → BOUND）。
     */
    @Transactional
    public TagAssetDO adminRecoverTag(String tagCode) {
        TagAssetDO tag = requireTag(tagCode);
        if (!"LOST".equals(tag.getStatus())) throw BizException.of("E_TAG_4093");
        tagAssetMapper.recover(tag.getId());
        return tagAssetMapper.findByTagCode(tagCode);
    }

    // ===== 查询 =====

    public List<TagApplyRecordDO> listMyOrders(Long userId, int pageNo, int pageSize) {
        return orderMapper.listByApplicant(userId, pageSize, (pageNo - 1) * pageSize);
    }

    public long countMyOrders(Long userId) {
        return orderMapper.countByApplicant(userId);
    }

    public List<TagApplyRecordDO> adminListOrders(String status, int pageNo, int pageSize) {
        return orderMapper.listByStatus(status, pageSize, (pageNo - 1) * pageSize);
    }

    public long adminCountOrders(String status) {
        return orderMapper.countByStatus(status);
    }

    public TagApplyRecordDO getOrder(Long orderId) {
        return requireOrder(orderId);
    }

    // ===== 内部工具 =====

    private TagApplyRecordDO requireOrder(Long orderId) {
        TagApplyRecordDO o = orderMapper.findById(orderId);
        if (o == null) throw BizException.of("E_ORDER_4041");
        return o;
    }

    private TagAssetDO requireTag(String tagCode) {
        TagAssetDO tag = tagAssetMapper.findByTagCode(tagCode);
        if (tag == null) throw BizException.of("E_TAG_4041");
        return tag;
    }
}
