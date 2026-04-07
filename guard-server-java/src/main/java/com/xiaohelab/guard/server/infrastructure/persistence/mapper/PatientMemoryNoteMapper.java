package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientMemoryNoteDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * patient_memory_note 数据访问层。
 */
@Mapper
public interface PatientMemoryNoteMapper {

    String COLS = "id, note_id, patient_id, created_by, kind, content, " +
            "tags::text AS tags, source_event_id, created_at, updated_at";

    @Select("SELECT " + COLS + " FROM patient_memory_note WHERE note_id = #{noteId}")
    @Results({@Result(property = "tags", column = "tags")})
    PatientMemoryNoteDO findByNoteId(String noteId);

    /** 分页读取患者记忆条目（支持按 kind 过滤），最新在前 */
    @Select("<script>" +
            "SELECT " + COLS + " FROM patient_memory_note " +
            "WHERE patient_id = #{patientId} " +
            "<if test='kind != null'>AND kind = #{kind} </if>" +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results({@Result(property = "tags", column = "tags")})
    List<PatientMemoryNoteDO> listByPatientId(@Param("patientId") Long patientId,
                                               @Param("kind") String kind,
                                               @Param("limit") int limit,
                                               @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM patient_memory_note WHERE patient_id = #{patientId} " +
            "<if test='kind != null'>AND kind = #{kind} </if>" +
            "</script>")
    long countByPatientId(@Param("patientId") Long patientId, @Param("kind") String kind);

    @Insert("INSERT INTO patient_memory_note(note_id, patient_id, created_by, kind, content, " +
            "tags, source_event_id, created_at, updated_at) " +
            "VALUES(#{noteId}, #{patientId}, #{createdBy}, #{kind}, #{content}, " +
            "#{tags}::jsonb, #{sourceEventId}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PatientMemoryNoteDO note);
}
