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
            "created_at, updated_at " +
            "FROM clue_record WHERE id = #{id}")
    ClueRecordDO findById(Long id);

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
}
