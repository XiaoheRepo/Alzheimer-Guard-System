package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagApplyRecordDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * tag_apply_record 数据访问层。
 * 状态机：PENDING → PROCESSING → SHIPPED → COMPLETED（终态）
 *        PENDING / PROCESSING → CANCEL_PENDING → CANCELLED（终态）
 *        SHIPPED → EXCEPTION → SHIPPED（重发）
 * 列名与 V1__init_schema.sql 保持一致：order_no / applicant_user_id，无 apply_no / applicant_id。
 */
@Mapper
public interface TagApplyRecordMapper {

    String COLS = "id, order_no, patient_id, applicant_user_id, quantity, apply_note, " +
            "tag_code, status, delivery_address, tracking_number, courier_name, resource_link, " +
            "cancel_reason, approved_at, reject_reason, rejected_at, exception_desc, " +
            "closed_at, created_at, updated_at";

    @Select("SELECT " + COLS + " FROM tag_apply_record WHERE id = #{id}")
    TagApplyRecordDO findById(Long id);

    @Select("SELECT " + COLS + " FROM tag_apply_record WHERE order_no = #{orderNo}")
    TagApplyRecordDO findByOrderNo(String orderNo);

    /** 查询患者当前未关闭工单（防重复提交） */
    @Select("SELECT " + COLS + " FROM tag_apply_record WHERE patient_id = #{patientId} " +
            "AND status NOT IN ('CANCELLED','COMPLETED') LIMIT 1")
    TagApplyRecordDO findOpenByPatientId(Long patientId);

    /** 管理员视图——按状态分页查询 */
    @Select("SELECT " + COLS + " FROM tag_apply_record WHERE status = #{status} " +
            "ORDER BY created_at LIMIT #{limit} OFFSET #{offset}")
    List<TagApplyRecordDO> listByStatus(@Param("status") String status,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tag_apply_record WHERE status = #{status}")
    long countByStatus(String status);

    /** 用户查看自己的申请记录 */
    @Select("SELECT " + COLS + " FROM tag_apply_record WHERE applicant_user_id = #{applicantUserId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<TagApplyRecordDO> listByApplicant(@Param("applicantUserId") Long applicantUserId,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tag_apply_record WHERE applicant_user_id = #{applicantUserId}")
    long countByApplicant(Long applicantUserId);

    /** 提交申请 */
    @Insert("INSERT INTO tag_apply_record(order_no, patient_id, applicant_user_id, quantity, " +
            "apply_note, status, delivery_address, created_at, updated_at) " +
            "VALUES(#{orderNo}, #{patientId}, #{applicantUserId}, #{quantity}, " +
            "#{applyNote}, #{status}, #{deliveryAddress}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TagApplyRecordDO record);

    /** 批量更新字段（审批/发货/关闭等均复用此方法） */
    @Update("UPDATE tag_apply_record SET status=#{status}, tag_code=#{tagCode}, " +
            "tracking_number=#{trackingNumber}, courier_name=#{courierName}, " +
            "resource_link=#{resourceLink}, cancel_reason=#{cancelReason}, " +
            "reject_reason=#{rejectReason}, rejected_at=#{rejectedAt}, " +
            "approved_at=#{approvedAt}, exception_desc=#{exceptionDesc}, " +
            "closed_at=#{closedAt}, updated_at=NOW() WHERE id=#{id}")
    int update(TagApplyRecordDO record);
}
