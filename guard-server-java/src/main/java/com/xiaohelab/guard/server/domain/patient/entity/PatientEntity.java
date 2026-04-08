package com.xiaohelab.guard.server.domain.patient.entity;

import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientProfileDO;
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

    /**
     * 重建聚合根（Repository 使用）。
     */
    public static PatientEntity fromDO(PatientProfileDO d) {
        PatientEntity e = new PatientEntity();
        e.id = d.getId();
        e.profileNo = d.getProfileNo();
        e.name = d.getName();
        e.gender = d.getGender();
        e.birthday = d.getBirthday();
        e.shortCode = d.getShortCode();
        e.pinCodeHash = d.getPinCodeHash();
        e.pinCodeSalt = d.getPinCodeSalt();
        e.photoUrl = d.getPhotoUrl();
        e.medicalHistory = d.getMedicalHistory();
        e.fenceEnabled = d.getFenceEnabled();
        e.fenceCenterLat = d.getFenceCenterLat();
        e.fenceCenterLng = d.getFenceCenterLng();
        e.fenceRadiusM = d.getFenceRadiusM();
        e.lostStatus = d.getLostStatus();
        e.lostStatusEventTime = d.getLostStatusEventTime();
        e.profileVersion = d.getProfileVersion();
        e.createdAt = d.getCreatedAt();
        e.updatedAt = d.getUpdatedAt();
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

    /**
     * 转为 DO（持久化用）。
     */
    public PatientProfileDO toDO() {
        PatientProfileDO d = new PatientProfileDO();
        d.setId(this.id);
        d.setProfileNo(this.profileNo);
        d.setName(this.name);
        d.setGender(this.gender);
        d.setBirthday(this.birthday);
        d.setShortCode(this.shortCode);
        d.setPinCodeHash(this.pinCodeHash);
        d.setPinCodeSalt(this.pinCodeSalt);
        d.setPhotoUrl(this.photoUrl);
        d.setMedicalHistory(this.medicalHistory);
        d.setFenceEnabled(this.fenceEnabled);
        d.setFenceCenterLat(this.fenceCenterLat);
        d.setFenceCenterLng(this.fenceCenterLng);
        d.setFenceRadiusM(this.fenceRadiusM);
        d.setLostStatus(this.lostStatus);
        d.setLostStatusEventTime(this.lostStatusEventTime);
        d.setProfileVersion(this.profileVersion);
        return d;
    }
}
