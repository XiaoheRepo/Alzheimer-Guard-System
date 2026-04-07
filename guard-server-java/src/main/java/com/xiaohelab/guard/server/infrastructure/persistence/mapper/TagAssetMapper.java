package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.TagAssetDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * tag_asset 数据访问层。
 * 状态机：UNBOUND → ALLOCATED → BOUND → LOST / VOID；高危操作需 ADMIN/SUPERADMIN。
 * 列名与 V1__init_schema.sql 保持一致：tag_code / patient_id，无 tag_serial / bound_patient_id。
 */
@Mapper
public interface TagAssetMapper {

    String COLS = "id, tag_code, tag_type, status, patient_id, apply_record_id, " +
            "import_batch_no, void_reason, lost_at, void_at, reset_at, recovered_at, " +
            "created_at, updated_at";

    @Select("SELECT " + COLS + " FROM tag_asset WHERE id = #{id}")
    TagAssetDO findById(Long id);

    @Select("SELECT " + COLS + " FROM tag_asset WHERE tag_code = #{tagCode}")
    TagAssetDO findByTagCode(String tagCode);

    /** 根据患者ID查询当前绑定标签（BOUND） */
    @Select("SELECT " + COLS + " FROM tag_asset WHERE patient_id = #{patientId} AND status = 'BOUND' LIMIT 1")
    TagAssetDO findBoundByPatientId(Long patientId);

    /** 查询 UNBOUND 标签列表（可分配库存） */
    @Select("SELECT " + COLS + " FROM tag_asset WHERE status = 'UNBOUND' " +
            "ORDER BY created_at LIMIT #{limit} OFFSET #{offset}")
    List<TagAssetDO> listUnbound(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM tag_asset WHERE status = 'UNBOUND'")
    long countUnbound();

    /** 批量入库新标签 */
    @Insert("INSERT INTO tag_asset(tag_code, tag_type, status, import_batch_no, created_at, updated_at) " +
            "VALUES(#{tagCode}, #{tagType}, #{status}, #{importBatchNo}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TagAssetDO tag);

    /** 分配标签到申领单（UNBOUND → ALLOCATED） */
    @Update("UPDATE tag_asset SET status='ALLOCATED', apply_record_id=#{applyRecordId}, updated_at=NOW() " +
            "WHERE id=#{id} AND status='UNBOUND'")
    int allocate(@Param("id") Long id, @Param("applyRecordId") Long applyRecordId);

    /** 绑定标签到患者（ALLOCATED → BOUND） */
    @Update("UPDATE tag_asset SET status='BOUND', patient_id=#{patientId}, updated_at=NOW() " +
            "WHERE id=#{id} AND status IN ('UNBOUND','ALLOCATED')")
    int bindToPatient(@Param("id") Long id, @Param("patientId") Long patientId);

    /** 上报标签丢失（BOUND → LOST） */
    @Update("UPDATE tag_asset SET status='LOST', lost_at=NOW(), updated_at=NOW() " +
            "WHERE id=#{id} AND status='BOUND'")
    int markLost(Long id);

    /** 作废标签（任意状态 → VOID，仅 ADMIN）*/
    @Update("UPDATE tag_asset SET status='VOID', void_reason=#{voidReason}, void_at=NOW(), updated_at=NOW() " +
            "WHERE id=#{id}")
    int voidTag(@Param("id") Long id, @Param("voidReason") String voidReason);

    /** 重置标签（LOST/VOID → UNBOUND，仅 ADMIN） */
    @Update("UPDATE tag_asset SET status='UNBOUND', patient_id=NULL, apply_record_id=NULL, " +
            "void_reason=NULL, reset_at=NOW(), updated_at=NOW() WHERE id=#{id}")
    int resetTag(Long id);

    /** 恢复丢失（LOST → BOUND，仅 ADMIN） */
    @Update("UPDATE tag_asset SET status='BOUND', recovered_at=NOW(), updated_at=NOW() " +
            "WHERE id=#{id} AND status='LOST'")
    int recover(Long id);
}
