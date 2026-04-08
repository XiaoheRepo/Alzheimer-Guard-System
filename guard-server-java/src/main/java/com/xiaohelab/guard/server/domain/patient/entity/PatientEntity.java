package com.xiaohelab.guard.server.domain.patient.entity;

import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 患者档案聚合根。
 * 状态：lostStatus NORMAL / MISSING（仅由事件驱动，API 不直接写入）。
 * 不变量：
 *  - profileNo、shortCode 创建后不可变
 *  - lostStatus 只能由时间戳更新（anti-disorder）
 */
@Getter
public class PatientEntity {

    private Long id;
    private String profileNo;
    private String name;
    private String gender;
    private LocalDate birthday;
    private String shortCode;
    private String pinCodeHash;
    private String pinCodeSalt;
    private String photoUrl;
    private String medicalHistory;
    private Boolean fenceEnabled;
    private Double fenceCenterLat;
    private Double fenceCenterLng;
    private Integer fenceRadiusM;
    private String lostStatus;
    private Instant lostStatusEventTime;
    private Long profileVersion;
    private Instant createdAt;
    private Instant updatedAt;

    private PatientEntity() {}

    /** 从持久化数据重建（仅 Infrastructure 层 RepositoryImpl 调用）。 */
    public static PatientEntity reconstitute(
            Long id, String profileNo, String name, String gender, LocalDate birthday,
            String shortCode, String pinCodeHash, String pinCodeSalt,
            String photoUrl, String medicalHistory,
            Boolean fenceEnabled, Double fenceCenterLat, Double fenceCenterLng, Integer fenceRadiusM,
            String lostStatus, Instant lostStatusEventTime, Long profileVersion,
            Instant createdAt, Instant updatedAt) {
        PatientEntity e = new PatientEntity();
        e.id = id;
        e.profileNo = profileNo;
        e.name = name;
        e.gender = gender;
        e.birthday = birthday;
        e.shortCode = shortCode;
        e.pinCodeHash = pinCodeHash;
        e.pinCodeSalt = pinCodeSalt;
        e.photoUrl = photoUrl;
        e.medicalHistory = medicalHistory;
        e.fenceEnabled = fenceEnabled;
        e.fenceCenterLat = fenceCenterLat;
        e.fenceCenterLng = fenceCenterLng;
        e.fenceRadiusM = fenceRadiusM;
        e.lostStatus = lostStatus;
        e.lostStatusEventTime = lostStatusEventTime;
        e.profileVersion = profileVersion;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }

    /**
     * 创建新患者档案（工厂方法）。
     * PIN 码哈希由应用层计算后传入，领域不依赖加密实现。
     */
    public static PatientEntity create(String profileNo, String shortCode,
                                       String name, String gender, LocalDate birthday,
                                       String photoUrl, String medicalHistory,
                                       String pinCodeHash, String pinCodeSalt) {
        PatientEntity e = new PatientEntity();
        e.profileNo = profileNo;
        e.shortCode = shortCode;
        e.name = name;
        e.gender = gender;
        e.birthday = birthday;
        e.photoUrl = photoUrl;
        e.medicalHistory = medicalHistory != null ? medicalHistory : "{}";
        e.pinCodeHash = pinCodeHash;
        e.pinCodeSalt = pinCodeSalt;
        e.fenceEnabled = false;
        e.lostStatus = "NORMAL";
        e.profileVersion = 1L;
        return e;
    }

    /**
     * 更新基本资料（仅主监护人或 ADMIN 可调用，权限由应用层校验）。
     */
    public void updateProfile(String name, String gender, LocalDate birthday,
                               String photoUrl, String medicalHistory) {
        this.name = name;
        this.gender = gender;
        this.birthday = birthday;
        this.photoUrl = photoUrl;
        if (medicalHistory != null) this.medicalHistory = medicalHistory;
    }

    /**
     * 更新围栏配置。
     */
    public void updateFence(Boolean fenceEnabled, Double lat, Double lng, Integer radiusM) {
        this.fenceEnabled = fenceEnabled;
        this.fenceCenterLat = lat;
        this.fenceCenterLng = lng;
        this.fenceRadiusM = radiusM;
    }

    /**
     * 事件驱动更新 lostStatus（anti-disorder：新事件时间必须晚于当前锚点）。
     * 返回 true 表示状态已实际变更。
     */
    public boolean applyLostStatusEvent(String newStatus, Instant eventTime) {
        if (this.lostStatusEventTime != null && !eventTime.isAfter(this.lostStatusEventTime)) {
            return false;
        }
        this.lostStatus = newStatus;
        this.lostStatusEventTime = eventTime;
        return true;
    }
}
