package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagApplyRecordDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * tag_apply_record 数据访问层。
 * 申请工单状态机：SUBMITTED → APPROVED（分配标签）→ DELIVERING → CLOSED（终态）
 *   或：SUBMITTED → REJECTED → CLOSED（终态）
 * closed_at 仅在终态写入。
 */
@Mapper
public interface TagApplyRecordMapper {

    @Select("SELECT id, apply_no, applicant_id, patient_id, apply_type, status, " +
            "assigned_tag_id, reviewer_id, reject_reason, closed_at, created_at, updated_at " +
            "FROM tag_apply_record WHERE id = #{id}")
    TagApplyRecordDO findById(Long id);

    @Select("SELECT id, apply_no, applicant_id, patient_id, apply_type, status, " +
            "assigned_tag_id, reviewer_id, reject_reason, closed_at, created_at, updated_at " +
            "FROM tag_apply_record WHERE apply_no = #{applyNo}")
    TagApplyRecordDO findByApplyNo(String applyNo);

    /** 查询患者当前待处理工单（避免重复申请） */
    @Select("SELECT id, apply_no, applicant_id, patient_id, apply_type, status, " +
            "assigned_tag_id, reviewer_id, reject_reason, closed_at, created_at, updated_at " +
            "FROM tag_apply_record WHERE patient_id = #{patientId} " +
            "AND status NOT IN ('CLOSED') LIMIT 1")
    TagApplyRecordDO findOpenByPatientId(Long patientId);

    /** 分页查询申请列表（待审核队列，管理员视图） */
    @Select("SELECT id, apply_no, applicant_id, patient_id, apply_type, status, " +
            "assigned_tag_id, reviewer_id, reject_reason, closed_at, created_at, updated_at " +
            "FROM tag_apply_record WHERE status = #{status} " +
            "ORDER BY created_at LIMIT #{limit} OFFSET #{offset}")
    List<TagApplyRecordDO> listByStatus(@Param("status") String status,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tag_apply_record WHERE status = #{status}")
    long countByStatus(String status);

    /** 用户查看自己的申请记录 */
    @Select("SELECT id, apply_no, applicant_id, patient_id, apply_type, status, " +
            "assigned_tag_id, reviewer_id, reject_reason, closed_at, created_at, updated_at " +
            "FROM tag_apply_record WHERE applicant_id = #{applicantId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<TagApplyRecordDO> listByApplicant(@Param("applicantId") Long applicantId,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    /** 提交申请 */
    @Insert("INSERT INTO tag_apply_record(apply_no, applicant_id, patient_id, apply_type, status, " +
            "created_at, updated_at) " +
            "VALUES(#{applyNo}, #{applicantId}, #{patientId}, #{applyType}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TagApplyRecordDO record);

    /** 审批（分配标签或拒绝） */
    @Update("UPDATE tag_apply_record SET status=#{status}, reviewer_id=#{reviewerId}, " +
            "assigned_tag_id=#{assignedTagId}, reject_reason=#{rejectReason}, updated_at=NOW() " +
            "WHERE id=#{id} AND status='SUBMITTED'")
    int review(TagApplyRecordDO record);

    /** 更新状态（物流流转与终态关闭） */
    @Update("UPDATE tag_apply_record SET status=#{status}, " +
            "closed_at=CASE WHEN #{status}='CLOSED' THEN NOW() ELSE closed_at END, " +
            "updated_at=NOW() WHERE id=#{id}")
    int updateStatus(TagApplyRecordDO record);
}
