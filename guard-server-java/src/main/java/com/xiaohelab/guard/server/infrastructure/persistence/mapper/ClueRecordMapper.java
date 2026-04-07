package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.ClueRecordDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/** clue_record 数据访问层。线索坐标系固定为 WGS84，risk_score 范围 [0,1]。 */
@Mapper
public interface ClueRecordMapper {

    @Select("SELECT id, clue_no, task_id, patient_id, tag_code, source_type, " +
            "location_lat, location_lng, coord_system, description, photo_url, " +
            "risk_score, is_valid, suspect_flag, suspect_reason, " +
            "review_status, assignee_user_id, assigned_at, reviewed_at, " +
            "override, override_by, override_reason, rejected_by, reject_reason, " +
            "created_at, updated_at " +
            "FROM clue_record WHERE id = #{id}")
    ClueRecordDO findById(Long id);

    @Select("SELECT id, clue_no, task_id, patient_id, tag_code, source_type, " +
            "location_lat, location_lng, coord_system, description, photo_url, " +
            "risk_score, is_valid, suspect_flag, suspect_reason, " +
            "review_status, assignee_user_id, assigned_at, reviewed_at, " +
            "override, override_by, override_reason, rejected_by, reject_reason, " +
            "created_at, updated_at " +
            "FROM clue_record WHERE id = #{id}")
    ClueRecordDO findByIdFull(Long id);

    /** 分页统计（支持时间范围过滤） */
    @Select("<script>" +
            "SELECT COUNT(*) FROM clue_record " +
            "<where>" +
            "<if test='timeFrom != null'>AND created_at &gt;= #{timeFrom}::timestamptz </if>" +
            "<if test='timeTo != null'>AND created_at &lt;= #{timeTo}::timestamptz </if>" +
            "</where></script>")
    long countAll(@Param("timeFrom") String timeFrom, @Param("timeTo") String timeTo);

    @Select("<script>" +
            "SELECT COUNT(*) FROM clue_record WHERE suspect_flag=TRUE " +
            "<if test='timeFrom != null'>AND created_at &gt;= #{timeFrom}::timestamptz </if>" +
            "<if test='timeTo != null'>AND created_at &lt;= #{timeTo}::timestamptz </if>" +
            "</script>")
    long countSuspected(@Param("timeFrom") String timeFrom, @Param("timeTo") String timeTo);

    @Select("<script>" +
            "SELECT COUNT(*) FROM clue_record WHERE review_status='OVERRIDDEN' " +
            "<if test='timeFrom != null'>AND created_at &gt;= #{timeFrom}::timestamptz </if>" +
            "<if test='timeTo != null'>AND created_at &lt;= #{timeTo}::timestamptz </if>" +
            "</script>")
    long countOverridden(@Param("timeFrom") String timeFrom, @Param("timeTo") String timeTo);

    @Select("<script>" +
            "SELECT COUNT(*) FROM clue_record WHERE review_status='REJECTED' " +
            "<if test='timeFrom != null'>AND created_at &gt;= #{timeFrom}::timestamptz </if>" +
            "<if test='timeTo != null'>AND created_at &lt;= #{timeTo}::timestamptz </if>" +
            "</script>")
    long countRejected(@Param("timeFrom") String timeFrom, @Param("timeTo") String timeTo);

    @Select("SELECT id, clue_no, task_id, patient_id, tag_code, source_type, " +
            "location_lat, location_lng, coord_system, description, photo_url, " +
            "risk_score, is_valid, suspect_flag, suspect_reason, " +
            "review_status, assignee_user_id, assigned_at, reviewed_at, " +
            "created_at, updated_at " +
            "FROM clue_record WHERE clue_no = #{clueNo}")
    ClueRecordDO findByClueNo(String clueNo);

    @Select("SELECT id, clue_no, task_id, patient_id, tag_code, source_type, " +
            "location_lat, location_lng, coord_system, description, photo_url, " +
            "risk_score, is_valid, suspect_flag, suspect_reason, " +
            "review_status, assignee_user_id, assigned_at, reviewed_at, " +
            "created_at, updated_at " +
            "FROM clue_record WHERE task_id = #{taskId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ClueRecordDO> listByTaskId(@Param("taskId") Long taskId,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM clue_record WHERE task_id = #{taskId}")
    long countByTaskId(Long taskId);

    @Select("SELECT id, clue_no, task_id, patient_id, tag_code, source_type, " +
            "location_lat, location_lng, coord_system, description, photo_url, " +
            "risk_score, is_valid, suspect_flag, suspect_reason, " +
            "review_status, assignee_user_id, assigned_at, reviewed_at, " +
            "created_at, updated_at " +
            "FROM clue_record WHERE task_id = #{taskId} " +
            "AND review_status = 'PENDING' ORDER BY risk_score DESC")
    List<ClueRecordDO> listPendingByTaskId(Long taskId);

    @Insert("INSERT INTO clue_record(clue_no, task_id, patient_id, source_type, " +
            "location_lat, location_lng, coord_system, description, photo_url, " +
            "risk_score, review_status, created_at, updated_at) " +
            "VALUES(#{clueNo}, #{taskId}, #{patientId}, #{sourceType}, " +
            "#{locationLat}, #{locationLng}, #{coordSystem}, #{description}, #{photoUrl}, " +
            "#{riskScore}, #{reviewStatus}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ClueRecordDO clue);

    @Update("UPDATE clue_record SET review_status=#{reviewStatus}, assignee_user_id=#{assigneeUserId}, " +
            "reviewed_at=NOW(), updated_at=NOW() " +
            "WHERE id=#{id} AND review_status='PENDING'")
    int review(ClueRecordDO clue);

    /** 管理员 override：强制设为 OVERRIDDEN */
    @Update("UPDATE clue_record SET review_status='OVERRIDDEN', override=TRUE, " +
            "override_by=#{overrideBy}, override_reason=#{overrideReason}, " +
            "reviewed_at=NOW(), updated_at=NOW() WHERE id=#{id}")
    int override(ClueRecordDO clue);

    /** 管理员 reject：设为 REJECTED */
    @Update("UPDATE clue_record SET review_status='REJECTED', rejected_by=#{rejectedBy}, " +
            "reject_reason=#{rejectReason}, reviewed_at=NOW(), updated_at=NOW() WHERE id=#{id}")
    int reject(ClueRecordDO clue);

    /** 管理员分配线索给复核员 */
    @Update("UPDATE clue_record SET assignee_user_id=#{assigneeUserId}, assigned_at=NOW(), " +
            "updated_at=NOW() WHERE id=#{id}")
    int assign(ClueRecordDO clue);

    /** 管理员复核队列：PENDING 状态线索分页 */
    @Select("SELECT id, clue_no, task_id, patient_id, tag_code, source_type, " +
            "location_lat, location_lng, coord_system, description, photo_url, " +
            "risk_score, is_valid, suspect_flag, suspect_reason, " +
            "review_status, assignee_user_id, assigned_at, reviewed_at, " +
            "created_at, updated_at " +
            "FROM clue_record WHERE review_status='PENDING' AND suspect_flag=TRUE " +
            "ORDER BY risk_score DESC LIMIT #{limit} OFFSET #{offset}")
    List<ClueRecordDO> listReviewQueue(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM clue_record WHERE review_status='PENDING' AND suspect_flag=TRUE")
    long countReviewQueue();
}
