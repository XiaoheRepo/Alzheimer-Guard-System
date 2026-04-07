package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.AiSessionMessageDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * ai_session_message 数据访问层。
 * UNIQUE KEY: (session_id, sequence_no)
 */
@Mapper
public interface AiSessionMessageMapper {

    String COLS = "id, session_id, sequence_no, role, content, " +
            "token_usage::text AS token_usage, created_at";

    /** 分页读取消息列表（按 sequence_no 升序） */
    @Select("SELECT " + COLS + " FROM ai_session_message " +
            "WHERE session_id = #{sessionId} " +
            "ORDER BY sequence_no ASC LIMIT #{limit} OFFSET #{offset}")
    @Results({@Result(property = "tokenUsage", column = "token_usage")})
    List<AiSessionMessageDO> listBySessionId(@Param("sessionId") String sessionId,
                                              @Param("limit") int limit,
                                              @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM ai_session_message WHERE session_id = #{sessionId}")
    long countBySessionId(String sessionId);

    @Insert("INSERT INTO ai_session_message(session_id, sequence_no, role, content, token_usage, created_at) " +
            "VALUES(#{sessionId}, #{sequenceNo}, #{role}, #{content}, " +
            "#{tokenUsage}::jsonb, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AiSessionMessageDO msg);

    /** 获取会话当前最大 sequence_no（用于分配下一条消息序号） */
    @Select("SELECT COALESCE(MAX(sequence_no), 0) FROM ai_session_message WHERE session_id = #{sessionId}")
    int maxSequenceNo(String sessionId);
}
