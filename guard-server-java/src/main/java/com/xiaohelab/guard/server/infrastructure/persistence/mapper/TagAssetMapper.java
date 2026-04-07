package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagAssetDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * tag_asset 数据访问层。
 * 标签状态机：IDLE → ACTIVE → IDLE（归还）| LOST（设备离线）
 * 扫码路由逻辑：tag_id → patient_id → short_code → 重定向公开档案页。
 */
@Mapper
public interface TagAssetMapper {

    @Select("SELECT id, tag_serial, tag_type, status, bound_patient_id, " +
            "bound_at, unbound_at, created_at, updated_at " +
            "FROM tag_asset WHERE id = #{id}")
    TagAssetDO findById(Long id);

    @Select("SELECT id, tag_serial, tag_type, status, bound_patient_id, " +
            "bound_at, unbound_at, created_at, updated_at " +
            "FROM tag_asset WHERE tag_serial = #{tagSerial}")
    TagAssetDO findByTagSerial(String tagSerial);

    /** 根据患者ID查询当前绑定标签（ACTIVE） */
    @Select("SELECT id, tag_serial, tag_type, status, bound_patient_id, " +
            "bound_at, unbound_at, created_at, updated_at " +
            "FROM tag_asset WHERE bound_patient_id = #{patientId} AND status = 'ACTIVE' LIMIT 1")
    TagAssetDO findActiveByPatientId(Long patientId);

    /** 查询空闲标签列表（供管理员发放） */
    @Select("SELECT id, tag_serial, tag_type, status, bound_patient_id, " +
            "bound_at, unbound_at, created_at, updated_at " +
            "FROM tag_asset WHERE status = 'IDLE' " +
            "ORDER BY created_at LIMIT #{limit} OFFSET #{offset}")
    List<TagAssetDO> listIdle(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tag_asset WHERE status = 'IDLE'")
    long countIdle();

    /** 入库新标签 */
    @Insert("INSERT INTO tag_asset(tag_serial, tag_type, status, created_at, updated_at) " +
            "VALUES(#{tagSerial}, #{tagType}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TagAssetDO tag);

    /** 绑定标签到患者（IDLE → ACTIVE） */
    @Update("UPDATE tag_asset SET status='ACTIVE', bound_patient_id=#{boundPatientId}, " +
            "bound_at=NOW(), unbound_at=NULL, updated_at=NOW() " +
            "WHERE id=#{id} AND status='IDLE'")
    int bindToPatient(@Param("id") Long id, @Param("boundPatientId") Long patientId);

    /** 解绑标签（ACTIVE → IDLE） */
    @Update("UPDATE tag_asset SET status='IDLE', bound_patient_id=NULL, " +
            "unbound_at=NOW(), updated_at=NOW() " +
            "WHERE id=#{id} AND status='ACTIVE'")
    int unbind(Long id);

    /** 更新标签状态（通用，管理员使用） */
    @Update("UPDATE tag_asset SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(TagAssetDO tag);
}
