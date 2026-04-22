package com.xiaohelab.guard.server.patient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 外观特征单独更新请求（API §3.3.3）。
 * <p>用于任务进行中的实时着装/特征更新，触发任务快照投影刷新。</p>
 */
public class AppearanceUpdateRequest {

    @JsonProperty("height_cm")
    @Min(50) @Max(250)
    private Integer heightCm;

    @JsonProperty("weight_kg")
    @Min(10) @Max(300)
    private Integer weightKg;

    @Size(max = 500)
    private String clothing;

    @Size(max = 500)
    private String features;

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer v) { this.heightCm = v; }
    public Integer getWeightKg() { return weightKg; }
    public void setWeightKg(Integer v) { this.weightKg = v; }
    public String getClothing() { return clothing; }
    public void setClothing(String v) { this.clothing = v; }
    public String getFeatures() { return features; }
    public void setFeatures(String v) { this.features = v; }
}
