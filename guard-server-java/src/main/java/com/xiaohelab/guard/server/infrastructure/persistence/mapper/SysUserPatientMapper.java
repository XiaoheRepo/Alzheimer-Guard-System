package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.SysUserPatientDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * sys_user_patient 数据访问层。
 * relation_role: PRIMARY_GUARDIAN / GUARDIAN
 * relation_status: PENDING / ACTIVE / REVOKED
 * transfer_state: NONE / PENDING_CONFIRM / ACCEPTED / REJECTED / CANCELLED / EXPIRED
 * 列名与 V1__init_schema.sql 保持一致，无 is_primary / role 字段。
 */
@Mapper
public interface SysUserPatientMapper {

    String COLS = "id, user_id, patient_id, relation_role, relation_status, transfer_state, " +
            "transfer_request_id, transfer_target_user_id, transfer_requested_by, " +
            "transfer_requested_at, transfer_reason, transfer_cancelled_by, transfer_cancelled_at, " +
            "transfer_cancel_reason, transfer_expire_at, transfer_confirmed_at, " +
            "transfer_rejected_at, transfer_reject_reason, created_at, updated_at";

    @Select("SELECT " + COLS + " FROM sys_user_patient WHERE id = #{id}")
    SysUserPatientDO findById(Long id);

    /** 查询用户对指定患者的唯一关联记录 */
    @Select("SELECT " + COLS + " FROM sys_user_patient " +
            "WHERE user_id = #{userId} AND patient_id = #{patientId} LIMIT 1")
    SysUserPatientDO findByUserIdAndPatientId(@Param("userId") Long userId,
                                              @Param("patientId") Long patientId);

    /** 查询患者的主监护人（relation_role=PRIMARY_GUARDIAN AND relation_status=ACTIVE） */
    @Select("SELECT " + COLS + " FROM sys_user_patient WHERE patient_id = #{patientId} " +
            "AND relation_role = 'PRIMARY_GUARDIAN' AND relation_status = 'ACTIVE' LIMIT 1")
    SysUserPatientDO findPrimaryByPatientId(Long patientId);

    /** 根据 transfer_request_id 查找转移记录 */
    @Select("SELECT " + COLS + " FROM sys_user_patient WHERE transfer_request_id = #{transferRequestId}")
    SysUserPatientDO findByTransferRequestId(String transferRequestId);

    /** 查询患者的全部活跃关联用户（ACTIVE） */
    @Select("SELECT " + COLS + " FROM sys_user_patient " +
            "WHERE patient_id = #{patientId} AND relation_status = 'ACTIVE'")
    List<SysUserPatientDO> listActiveByPatientId(Long patientId);

    /** 分页查询用户关联的所有患者 */
    @Select("SELECT " + COLS + " FROM sys_user_patient " +
            "WHERE user_id = #{userId} AND relation_status = 'ACTIVE' " +
            "ORDER BY created_at LIMIT #{limit} OFFSET #{offset}")
    List<SysUserPatientDO> listByUserId(@Param("userId") Long userId,
                                         @Param("limit") int limit,
                                         @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM sys_user_patient WHERE user_id=#{userId} AND relation_status='ACTIVE'")
    long countByUserId(Long userId);

    /** 创建新关联记录 */
    @Insert("INSERT INTO sys_user_patient(user_id, patient_id, relation_role, relation_status, " +
            "transfer_state, created_at, updated_at) " +
            "VALUES(#{userId}, #{patientId}, #{relationRole}, #{relationStatus}, " +
            "#{transferState}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SysUserPatientDO record);

    /** 更新关联状态 */
    @Update("UPDATE sys_user_patient SET relation_status=#{relationStatus}, updated_at=NOW() WHERE id=#{id}")
    int updateRelationStatus(@Param("id") Long id, @Param("relationStatus") String relationStatus);

    /** 发起主监护人转移申请 */
    @Update("UPDATE sys_user_patient SET transfer_state='PENDING_CONFIRM', " +
            "transfer_request_id=#{transferRequestId}, transfer_target_user_id=#{transferTargetUserId}, " +
            "transfer_requested_by=#{transferRequestedBy}, transfer_requested_at=NOW(), " +
            "transfer_reason=#{transferReason}, transfer_expire_at=#{transferExpireAt}, " +
            "updated_at=NOW() " +
            "WHERE id=#{id} AND relation_role='PRIMARY_GUARDIAN' AND relation_status='ACTIVE' " +
            "AND transfer_state='NONE'")
    int initiateTransfer(SysUserPatientDO record);

    /** 更新转移状态（确认/拒绝/取消/过期均走此方法） */
    @Update("UPDATE sys_user_patient SET transfer_state=#{transferState}, " +
            "transfer_confirmed_at=#{transferConfirmedAt}, transfer_rejected_at=#{transferRejectedAt}, " +
            "transfer_reject_reason=#{transferRejectReason}, " +
            "transfer_cancelled_by=#{transferCancelledBy}, transfer_cancelled_at=#{transferCancelledAt}, " +
            "transfer_cancel_reason=#{transferCancelReason}, updated_at=NOW() WHERE id=#{id}")
    int updateTransferState(SysUserPatientDO record);

    /** 变更 relation_role（主监护人转移完成后互换角色） */
    @Update("UPDATE sys_user_patient SET relation_role=#{relationRole}, transfer_state='NONE', " +
            "updated_at=NOW() WHERE id=#{id}")
    int updateRole(@Param("id") Long id, @Param("relationRole") String relationRole);

    /** 检查用户是否对患者有 ACTIVE 关联（权限校验用） */
    @Select("SELECT COUNT(*) FROM sys_user_patient " +
            "WHERE user_id=#{userId} AND patient_id=#{patientId} AND relation_status='ACTIVE'")
    long countActiveRelation(@Param("userId") Long userId, @Param("patientId") Long patientId);

    /** 分页查询患者的转移记录（可按 transfer_state 过滤） */
    @Select("<script>SELECT " + COLS + " FROM sys_user_patient " +
            "WHERE patient_id=#{patientId} AND transfer_request_id IS NOT NULL " +
            "<if test='state != null'>AND transfer_state=#{state}</if>" +
            " ORDER BY transfer_requested_at DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<SysUserPatientDO> listTransfersByPatientId(@Param("patientId") Long patientId,
                                                     @Param("state") String state,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset);

    @Select("<script>SELECT COUNT(*) FROM sys_user_patient " +
            "WHERE patient_id=#{patientId} AND transfer_request_id IS NOT NULL " +
            "<if test='state != null'>AND transfer_state=#{state}</if></script>")
    long countTransfersByPatientId(@Param("patientId") Long patientId, @Param("state") String state);
}
