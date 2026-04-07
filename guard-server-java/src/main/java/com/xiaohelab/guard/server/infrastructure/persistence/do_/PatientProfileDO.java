package com.xiaohelab.guard.server.infrastructure.persistence.do_;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * patient_profile 持久化对象。
 * medical_history JSONB 以字符串形式映射，Service 层负责序列化/反序列化。
 * 围栏中心点拆分为 fenceCenterLat / fenceCenterLng，避免引入 PostGIS 依赖。
 */
@Data
public class PatientProfileDO {

    private Long id;
    private String profileNo;
    private String name;
    /** MALE / FEMALE / UNKNOWN */
    private String gender;
    private LocalDate birthday;
    /** 6位短码，用于匿名扫码路由 */
    private String shortCode;
    private String pinCodeHash;
    private String pinCodeSalt;
    /** 患者近期正面免冠照片 URL，必填 */
    private String photoUrl;
    /** 医疗扩展信息 JSON 字符串：blood_type/chronic_diseases/allergy_notes */
    private String medicalHistory;
    private Boolean fenceEnabled;
    private Double fenceCenterLat;
    private Double fenceCenterLng;
    /** 围栏半径（米），有效范围 50-5000 */
    private Integer fenceRadiusM;
    /** NORMAL / MISSING，由事件驱动，不提供独立写入 API */
    private String lostStatus;
    /** 防乱序用：事件时间戳锚点 */
    private Instant lostStatusEventTime;
    private Long profileVersion;
    private Instant createdAt;
    private Instant updatedAt;
}
