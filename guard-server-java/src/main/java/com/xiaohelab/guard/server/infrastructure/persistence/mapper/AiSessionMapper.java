package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * ai_session 数据访问层。
 * CAS 更新使用 version 字段实现乐观锁。
 */
@Mapper
public interface AiSessionMapper {

    String COLS = "id, session_id, user_id, patient_id, task_id, messages::text, " +
            "request_tokens, response_tokens, token_usage::text, token_used, model_name, " +
            "status, archived_at, version, created_at, updated_at";

    @Select("SELECT " + COLS + " FROM ai_session WHERE session_id = #{sessionId}")
    @Results({
            @Result(property = "messages", column = "messages"),
            @Result(property = "tokenUsage", column = "token_usage")
    })
    AiSessionDO findBySessionId(String sessionId);

    /** 查询用户会话列表（支持可选患者过滤），最新活跃在前 */
    @Select("<script>" +
            "SELECT " + COLS + " FROM ai_session WHERE user_id = #{userId} " +
            "<if test='patientId != null'>AND patient_id = #{patientId} </if>" +
            "ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results({
            @Result(property = "messages", column = "messages"),
            @Result(property = "tokenUsage", column = "token_usage")
    })
    List<AiSessionDO> listByUserId(@Param("userId") Long userId,
                                    @Param("patientId") Long patientId,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM ai_session WHERE user_id = #{userId} " +
            "<if test='patientId != null'>AND patient_id = #{patientId} </if>" +
            "</script>")
    long countByUserId(@Param("userId") Long userId, @Param("patientId") Long patientId);

    /** 管理端：分页查询所有会话（支持按 userId / patientId 过滤） */
    @Select("<script>" +
            "SELECT " + COLS + " FROM ai_session " +
            "<where>" +
            "<if test='userId != null'>AND user_id = #{userId} </if>" +
            "<if test='patientId != null'>AND patient_id = #{patientId} </if>" +
            "</where>" +
            "ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @Results({
            @Result(property = "messages", column = "messages"),
            @Result(property = "tokenUsage", column = "token_usage")
    })
    List<AiSessionDO> listAll(@Param("userId") Long userId,
                               @Param("patientId") Long patientId,
                               @Param("limit") int limit,
                               @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM ai_session " +
            "<where>" +
            "<if test='userId != null'>AND user_id = #{userId} </if>" +
            "<if test='patientId != null'>AND patient_id = #{patientId} </if>" +
            "</where>" +
            "</script>")
    long countAll(@Param("userId") Long userId, @Param("patientId") Long patientId);

    @Insert("INSERT INTO ai_session(session_id, user_id, patient_id, task_id, messages, " +
            "request_tokens, response_tokens, token_usage, token_used, model_name, " +
            "status, version, created_at, updated_at) " +
            "VALUES(#{sessionId}, #{userId}, #{patientId}, #{taskId}, " +
            "#{messages}::jsonb, #{requestTokens}, #{responseTokens}, #{tokenUsage}::jsonb, " +
            "#{tokenUsed}, #{modelName}, #{status}, #{version}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AiSessionDO session);

    /** 归档会话（version CAS，status 必须为 ACTIVE） */
    @Update("UPDATE ai_session SET status='ARCHIVED', archived_at=NOW(), updated_at=NOW(), version=version+1 " +
            "WHERE session_id=#{sessionId} AND status='ACTIVE'")
    int archiveBySessionId(String sessionId);

    /** CAS 更新 token 消耗（原子累加） */
    @Update("UPDATE ai_session SET token_used = COALESCE(token_used,0) + #{addTokens}, " +
            "updated_at=NOW(), version=version+1 " +
            "WHERE session_id=#{sessionId} AND version=#{version}")
    int casAddTokens(@Param("sessionId") String sessionId,
                      @Param("version") Long version,
                      @Param("addTokens") int addTokens);
}
