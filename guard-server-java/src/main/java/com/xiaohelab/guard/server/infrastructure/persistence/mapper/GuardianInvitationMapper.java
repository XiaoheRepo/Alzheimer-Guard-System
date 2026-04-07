package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.GuardianInvitationDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * guardian_invitation 数据访问层。
 * 邀请状态机：PENDING → ACCEPTED | REJECTED | EXPIRED | REVOKED
 * ACCEPTED 后必须在同一事务内激活 sys_user_patient 关联。
 * 列名与 V1__init_schema.sql 保持一致。
 */
@Mapper
public interface GuardianInvitationMapper {

    String COLS = "id, invite_id, patient_id, inviter_user_id, invitee_user_id, " +
            "relation_role, status, reason, reject_reason, expire_at, " +
            "accepted_at, rejected_at, revoked_at, created_at, updated_at";

    @Select("SELECT " + COLS + " FROM guardian_invitation WHERE id = #{id}")
    GuardianInvitationDO findById(Long id);

    @Select("SELECT " + COLS + " FROM guardian_invitation WHERE invite_id = #{inviteId}")
    GuardianInvitationDO findByInviteId(String inviteId);

    /** 查询患者对指定被邀请人的 PENDING 邀请（防重复） */
    @Select("SELECT " + COLS + " FROM guardian_invitation WHERE patient_id = #{patientId} " +
            "AND invitee_user_id = #{inviteeUserId} AND status = 'PENDING' LIMIT 1")
    GuardianInvitationDO findPendingByPatientAndInvitee(@Param("patientId") Long patientId,
                                                        @Param("inviteeUserId") Long inviteeUserId);

    /** 查询患者的邀请列表 */
    @Select("SELECT " + COLS + " FROM guardian_invitation WHERE patient_id = #{patientId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<GuardianInvitationDO> listByPatient(@Param("patientId") Long patientId,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM guardian_invitation WHERE patient_id = #{patientId}")
    long countByPatient(Long patientId);

    /** 查询用户收到的 PENDING 邀请列表 */
    @Select("SELECT " + COLS + " FROM guardian_invitation WHERE invitee_user_id = #{inviteeUserId} " +
            "AND status = 'PENDING' ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<GuardianInvitationDO> listPendingByInvitee(@Param("inviteeUserId") Long inviteeUserId,
                                                     @Param("limit") int limit,
                                                     @Param("offset") int offset);

    /** 创建邀请 */
    @Insert("INSERT INTO guardian_invitation(invite_id, patient_id, inviter_user_id, invitee_user_id, " +
            "relation_role, status, reason, expire_at, created_at, updated_at) " +
            "VALUES(#{inviteId}, #{patientId}, #{inviterUserId}, #{inviteeUserId}, " +
            "#{relationRole}, #{status}, #{reason}, #{expireAt}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GuardianInvitationDO invitation);

    /** 响应邀请（ACCEPT 或 REJECT，仅从 PENDING 迁移） */
    @Update("UPDATE guardian_invitation SET status=#{status}, reject_reason=#{rejectReason}, " +
            "accepted_at=CASE WHEN #{status}='ACCEPTED' THEN NOW() ELSE accepted_at END, " +
            "rejected_at=CASE WHEN #{status}='REJECTED' THEN NOW() ELSE rejected_at END, " +
            "updated_at=NOW() WHERE id=#{id} AND status='PENDING'")
    int respond(GuardianInvitationDO invitation);

    /** 撤销邀请（REVOKE，仅从 PENDING 迁移） */
    @Update("UPDATE guardian_invitation SET status='REVOKED', revoked_at=NOW(), updated_at=NOW() " +
            "WHERE id=#{id} AND status='PENDING'")
    int revoke(Long id);

    /** 批量过期处理（调度任务使用） */
    @Update("UPDATE guardian_invitation SET status='EXPIRED', updated_at=NOW() " +
            "WHERE status='PENDING' AND expire_at < NOW()")
    int expirePending();
}
