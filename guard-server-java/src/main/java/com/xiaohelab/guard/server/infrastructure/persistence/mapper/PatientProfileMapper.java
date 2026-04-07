package com.xiaohelab.guard.server.infrastructure.persistence.mapper;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientProfileDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * patient_profile 数据访问层。
 * short_code 发号通过数据库序列 patient_short_code_seq 保障唯一性。
 */
@Mapper
public interface PatientProfileMapper {

    @Select("SELECT id, profile_no, name, gender, birthday, short_code, pin_code_hash, pin_code_salt, " +
            "photo_url, medical_history::text, fence_enabled, fence_center_lat, fence_center_lng, " +
            "fence_radius_m, lost_status, lost_status_event_time, profile_version, created_at, updated_at " +
            "FROM patient_profile WHERE id = #{id}")
    PatientProfileDO findById(Long id);

    @Select("SELECT id, profile_no, name, gender, birthday, short_code, pin_code_hash, pin_code_salt, " +
            "photo_url, medical_history::text, fence_enabled, fence_center_lat, fence_center_lng, " +
            "fence_radius_m, lost_status, lost_status_event_time, profile_version, created_at, updated_at " +
            "FROM patient_profile WHERE short_code = #{shortCode}")
    PatientProfileDO findByShortCode(String shortCode);

    /** 新建档案（short_code 由调用方传入，已通过序列发号） */
    @Insert("INSERT INTO patient_profile(profile_no, name, gender, birthday, short_code, pin_code_hash, " +
            "pin_code_salt, photo_url, medical_history, fence_enabled, lost_status, lost_status_event_time, " +
            "profile_version, created_at, updated_at) " +
            "VALUES(#{profileNo}, #{name}, #{gender}, #{birthday}, #{shortCode}, #{pinCodeHash}, " +
            "#{pinCodeSalt}, #{photoUrl}, #{medicalHistory}::jsonb, #{fenceEnabled}, #{lostStatus}, " +
            "#{lostStatusEventTime}, #{profileVersion}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PatientProfileDO profile);

    /** 更新基本资料 */
    @Update("UPDATE patient_profile SET name=#{name}, gender=#{gender}, birthday=#{birthday}, " +
            "photo_url=#{photoUrl}, medical_history=#{medicalHistory}::jsonb, " +
            "profile_version=profile_version+1, updated_at=NOW() WHERE id=#{id}")
    int update(PatientProfileDO profile);

    /** 更新围栏配置 */
    @Update("UPDATE patient_profile SET fence_enabled=#{fenceEnabled}, " +
            "fence_center_lat=#{fenceCenterLat}, fence_center_lng=#{fenceCenterLng}, " +
            "fence_radius_m=#{fenceRadiusM}, updated_at=NOW() WHERE id=#{id}")
    int updateFence(PatientProfileDO profile);

    /**
     * 事件驱动更新 lost_status（防乱序：仅当新事件时间晚于当前锚点时更新）。
     * HC-01 约束：profile-service 消费 task 域事件执行此更新。
     */
    @Update("UPDATE patient_profile SET lost_status=#{lostStatus}, " +
            "lost_status_event_time=#{lostStatusEventTime}, updated_at=NOW() " +
            "WHERE id=#{id} AND lost_status_event_time < #{lostStatusEventTime}")
    int updateLostStatusByEvent(@Param("id") Long id,
                                @Param("lostStatus") String lostStatus,
                                @Param("lostStatusEventTime") java.time.Instant eventTime);

    /** 下一个 short_code 序列值 */
    @Select("SELECT nextval('patient_short_code_seq')")
    long nextShortCodeSeq();

    /** 按关联用户查询患者列表（通过 sys_user_patient 关联） */
    @Select("SELECT p.id, p.profile_no, p.name, p.gender, p.birthday, p.short_code, p.pin_code_hash, " +
            "p.pin_code_salt, p.photo_url, p.medical_history::text, p.fence_enabled, p.fence_center_lat, " +
            "p.fence_center_lng, p.fence_radius_m, p.lost_status, p.lost_status_event_time, " +
            "p.profile_version, p.created_at, p.updated_at " +
            "FROM patient_profile p " +
            "INNER JOIN sys_user_patient sup ON sup.patient_id = p.id " +
            "WHERE sup.user_id = #{userId} AND sup.relation_status = 'ACTIVE'")
    List<PatientProfileDO> findByUserId(Long userId);
}
