package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserPatientDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * sys_user_patient 数据访问层。
 * 关联关系状态机：PENDING_CONFIRM → ACTIVE → TRANSFERRED（仅原主监护人）
 * 监护人邀请接受后在同一事务内激活此记录。
 */
@Mapper
public interface SysUserPatientMapper {

    @Select("SELECT id, user_id, patient_id, role, relation_status, is_primary, " +
            "transfer_request_no, transfer_target_user_id, transfer_initiated_at, " +
            "transfer_completed_at, created_at, updated_at " +
            "FROM sys_user_patient WHERE id = #{id}")
    SysUserPatientDO findById(Long id);

    /** 查询用户对指定患者的唯一关联记录 */
    @Select("SELECT id, user_id, patient_id, role, relation_status, is_primary, " +
            "transfer_request_no, transfer_target_user_id, transfer_initiated_at, " +
            "transfer_completed_at, created_at, updated_at " +
            "FROM sys_user_patient WHERE user_id = #{userId} AND patient_id = #{patientId} LIMIT 1")
    SysUserPatientDO findByUserIdAndPatientId(@Param("userId") Long userId,
                                              @Param("patientId") Long patientId);

    /** 查询患者的主监护人（is_primary=true AND status=ACTIVE） */
    @Select("SELECT id, user_id, patient_id, role, relation_status, is_primary, " +
            "transfer_request_no, transfer_target_user_id, transfer_initiated_at, " +
            "transfer_completed_at, created_at, updated_at " +
            "FROM sys_user_patient WHERE patient_id = #{patientId} " +
            "AND is_primary = TRUE AND relation_status = 'ACTIVE' LIMIT 1")
    SysUserPatientDO findPrimaryByPatientId(Long patientId);

    /** 查询患者的全部活跃关联用户（ACTIVE） */
    @Select("SELECT id, user_id, patient_id, role, relation_status, is_primary, " +
            "transfer_request_no, transfer_target_user_id, transfer_initiated_at, " +
            "transfer_completed_at, created_at, updated_at " +
            "FROM sys_user_patient WHERE patient_id = #{patientId} AND relation_status = 'ACTIVE'")
    List<SysUserPatientDO> listActiveByPatientId(Long patientId);

    /** 创建新关联记录 */
    @Insert("INSERT INTO sys_user_patient(user_id, patient_id, role, relation_status, is_primary, " +
            "created_at, updated_at) " +
            "VALUES(#{userId}, #{patientId}, #{role}, #{relationStatus}, #{isPrimary}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SysUserPatientDO record);

    /** 更新关联状态 */
    @Update("UPDATE sys_user_patient SET relation_status=#{relationStatus}, updated_at=NOW() " +
            "WHERE id=#{id}")
    int updateStatus(SysUserPatientDO record);

    /**
     * 主监护人转移——写入转移申请信息（ACTIVE → 转移进行中）。
     * 转移完成在 completeTransfer 方法中执行。
     */
    @Update("UPDATE sys_user_patient SET transfer_request_no=#{transferRequestNo}, " +
            "transfer_target_user_id=#{transferTargetUserId}, " +
            "transfer_initiated_at=NOW(), updated_at=NOW() " +
            "WHERE id=#{id} AND is_primary = TRUE AND relation_status = 'ACTIVE'")
    int initiateTransfer(SysUserPatientDO record);

    /**
     * 完成转移：原主监护人状态改为 TRANSFERRED，新主监护人 is_primary=true。
     * 调用方需在同一事务内完成两条更新 + 写 Outbox。
     */
    @Update("UPDATE sys_user_patient SET relation_status='TRANSFERRED', " +
            "transfer_completed_at=NOW(), is_primary=FALSE, updated_at=NOW() " +
            "WHERE id=#{id}")
    int markTransferred(Long id);

    /** 检查用户是否对患者有 ACTIVE 关联（权限校验用） */
    @Select("SELECT COUNT(*) FROM sys_user_patient " +
            "WHERE user_id=#{userId} AND patient_id=#{patientId} AND relation_status='ACTIVE'")
    long countActiveRelation(@Param("userId") Long userId, @Param("patientId") Long patientId);
}
