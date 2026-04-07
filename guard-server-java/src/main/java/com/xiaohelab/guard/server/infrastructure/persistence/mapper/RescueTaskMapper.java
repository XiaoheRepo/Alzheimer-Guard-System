package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.RescueTaskDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * rescue_task 数据访问层。
 * HC-01 约束：任务域是 lost_status 唯一权威来源；
 * HC-02 约束：所有写操作必须在同一事务中写 sys_outbox_log。
 */
@Mapper
public interface RescueTaskMapper {

    @Select("SELECT id, task_no, patient_id, status, source, remark, " +
            "ai_analysis_summary, poster_url, close_reason, event_version, " +
            "created_by, created_at, closed_at, updated_at " +
            "FROM rescue_task WHERE id = #{id}")
    RescueTaskDO findById(Long id);

    @Select("SELECT id, task_no, patient_id, status, source, remark, " +
            "ai_analysis_summary, poster_url, close_reason, event_version, " +
            "created_by, created_at, closed_at, updated_at " +
            "FROM rescue_task WHERE task_no = #{taskNo}")
    RescueTaskDO findByTaskNo(String taskNo);

    /** 查询患者的当前激活任务（同一患者至多1个ACTIVE） */
    @Select("SELECT id, task_no, patient_id, status, source, remark, " +
            "ai_analysis_summary, poster_url, close_reason, event_version, " +
            "created_by, created_at, closed_at, updated_at " +
            "FROM rescue_task WHERE patient_id = #{patientId} AND status = 'ACTIVE' LIMIT 1")
    RescueTaskDO findActiveByPatientId(Long patientId);

    /** 分页查询患者历史任务（含ACTIVE/CLOSED） */
    @Select("SELECT id, task_no, patient_id, status, source, remark, " +
            "ai_analysis_summary, poster_url, close_reason, event_version, " +
            "created_by, created_at, closed_at, updated_at " +
            "FROM rescue_task WHERE patient_id = #{patientId} " +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<RescueTaskDO> listByPatientId(@Param("patientId") Long patientId,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM rescue_task WHERE patient_id = #{patientId}")
    long countByPatientId(Long patientId);

    /** 创建新任务 */
    @Insert("INSERT INTO rescue_task(task_no, patient_id, source, remark, status, " +
            "event_version, created_by, created_at, updated_at) " +
            "VALUES(#{taskNo}, #{patientId}, #{source}, #{remark}, #{status}, " +
            "#{eventVersion}, #{createdBy}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RescueTaskDO task);

    /**
     * 条件关闭任务（HC-01 关键路径）。
     * WHERE status='ACTIVE' 防止并发重复关闭；
     * event_version 乐观锁防止乱序更新。
     */
    @Update("UPDATE rescue_task SET status=#{status}, close_reason=#{closeReason}, " +
            "remark=#{remark}, closed_at=NOW(), " +
            "event_version=event_version+1, updated_at=NOW() " +
            "WHERE id=#{id} AND status='ACTIVE' AND event_version=#{eventVersion}")
    int closeConditionally(RescueTaskDO task);

    /** 管理员强制关闭（不使用乐观锁，ACTIVE 状态检查仍保留） */
    @Update("UPDATE rescue_task SET status='RESOLVED', close_reason=#{closeReason}, " +
            "remark=#{remark}, closed_at=NOW(), updated_at=NOW() " +
            "WHERE id=#{id} AND status='ACTIVE'")
    int forceClose(@Param("id") Long id,
                   @Param("closeReason") String closeReason,
                   @Param("remark") String remark);

    /** 管理端全量任务列表（支持 status/source 过滤） */
    @Select("<script>" +
            "SELECT id, task_no, patient_id, status, source, remark, " +
            "ai_analysis_summary, poster_url, close_reason, event_version, " +
            "created_by, created_at, closed_at, updated_at " +
            "FROM rescue_task " +
            "<where>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='source != null'>AND source = #{source}</if>" +
            "</where>" +
            "ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<RescueTaskDO> listAll(@Param("status") String status,
                               @Param("source") String source,
                               @Param("limit") int limit,
                               @Param("offset") int offset);

    /** 管理端全量任务计数 */
    @Select("<script>" +
            "SELECT COUNT(*) FROM rescue_task " +
            "<where>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='source != null'>AND source = #{source}</if>" +
            "</where>" +
            "</script>")
    long countAll(@Param("status") String status, @Param("source") String source);

    /** 按状态统计任务数 */
    @Select("<script>" +
            "SELECT COUNT(*) FROM rescue_task " +
            "<where>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='timeFrom != null'>AND created_at &gt;= #{timeFrom}::timestamptz</if>" +
            "<if test='timeTo != null'>AND created_at &lt;= #{timeTo}::timestamptz</if>" +
            "</where>" +
            "</script>")
    long countByStatus(@Param("status") String status,
                       @Param("timeFrom") String timeFrom,
                       @Param("timeTo") String timeTo);
}
