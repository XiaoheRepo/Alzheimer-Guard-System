package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.GuardianInvitationDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * guardian_invitation 数据访问层。
 * 邀请状态机：PENDING → ACCEPTED | REJECTED | EXPIRED | CANCELLED
 * ACCEPTED 后必须在同一事务内激活 sys_user_patient 关联。
 */
@Mapper
public interface GuardianInvitationMapper {

    @Select("SELECT id, invite_no, inviter_id, patient_id, invitee_phone, invitee_user_id, " +
            "target_role, status, expired_at, created_at, updated_at " +
            "FROM guardian_invitation WHERE id = #{id}")
    GuardianInvitationDO findById(Long id);

    @Select("SELECT id, invite_no, inviter_id, patient_id, invitee_phone, invitee_user_id, " +
            "target_role, status, expired_at, created_at, updated_at " +
            "FROM guardian_invitation WHERE invite_no = #{inviteNo}")
    GuardianInvitationDO findByInviteNo(String inviteNo);

    /** 查询指定患者的待处理邀请（用于判断是否已存在有效邀请） */
    @Select("SELECT id, invite_no, inviter_id, patient_id, invitee_phone, invitee_user_id, " +
            "target_role, status, expired_at, created_at, updated_at " +
            "FROM guardian_invitation WHERE patient_id = #{patientId} " +
            "AND invitee_phone = #{inviteePhone} AND status = 'PENDING' LIMIT 1")
    GuardianInvitationDO findPendingByPatientAndPhone(@Param("patientId") Long patientId,
                                                      @Param("inviteePhone") String inviteePhone);

    /** 查询用户发出的邀请列表 */
    @Select("SELECT id, invite_no, inviter_id, patient_id, invitee_phone, invitee_user_id, " +
            "target_role, status, expired_at, created_at, updated_at " +
            "FROM guardian_invitation WHERE inviter_id = #{inviterId} AND patient_id = #{patientId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<GuardianInvitationDO> listByInviter(@Param("inviterId") Long inviterId,
                                             @Param("patientId") Long patientId,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    /** 查询用户收到的邀请列表 */
    @Select("SELECT id, invite_no, inviter_id, patient_id, invitee_phone, invitee_user_id, " +
            "target_role, status, expired_at, created_at, updated_at " +
            "FROM guardian_invitation WHERE invitee_user_id = #{inviteeUserId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<GuardianInvitationDO> listByInvitee(@Param("inviteeUserId") Long inviteeUserId,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);

    /** 创建邀请 */
    @Insert("INSERT INTO guardian_invitation(invite_no, inviter_id, patient_id, invitee_phone, " +
            "invitee_user_id, target_role, status, expired_at, created_at, updated_at) " +
            "VALUES(#{inviteNo}, #{inviterId}, #{patientId}, #{inviteePhone}, #{inviteeUserId}, " +
            "#{targetRole}, #{status}, #{expiredAt}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GuardianInvitationDO invitation);

    /**
     * 响应邀请（只允许从 PENDING 迁移）。
     * ACCEPTED 后业务层须在同一事务激活 sys_user_patient。
     */
    @Update("UPDATE guardian_invitation SET status=#{status}, invitee_user_id=#{inviteeUserId}, " +
            "updated_at=NOW() " +
            "WHERE id=#{id} AND status='PENDING'")
    int respond(GuardianInvitationDO invitation);

    /** 批量过期处理（调度任务使用） */
    @Update("UPDATE guardian_invitation SET status='EXPIRED', updated_at=NOW() " +
            "WHERE status='PENDING' AND expired_at < NOW()")
    int expirePending();
}
