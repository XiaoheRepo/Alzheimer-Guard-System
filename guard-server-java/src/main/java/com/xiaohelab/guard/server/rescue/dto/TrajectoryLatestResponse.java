package com.xiaohelab.guard.server.rescue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 任务轨迹最新切片响应（API V2.0 §3.2.7）。
 * <p>Cursor 分页，支持 since / after_version 增量拉取。</p>
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class TrajectoryLatestResponse {

    private List<Point> items;
    private int pageSize;
    private String nextCursor;
    private boolean hasNext;

    public TrajectoryLatestResponse() {}

    public TrajectoryLatestResponse(List<Point> items, int pageSize, String nextCursor, boolean hasNext) {
        this.items = items;
        this.pageSize = pageSize;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public static class Point {
        private Long trajectoryId;
        private Long patientId;
        private Long taskId;
        private Long clueId;
        private Double latitude;
        private Double longitude;
        private String coordSystem;
        private OffsetDateTime recordedAt;
        private String sourceType;
        private Long version;

        public Long getTrajectoryId() { return trajectoryId; }
        public void setTrajectoryId(Long v) { this.trajectoryId = v; }
        public Long getPatientId() { return patientId; }
        public void setPatientId(Long v) { this.patientId = v; }
        public Long getTaskId() { return taskId; }
        public void setTaskId(Long v) { this.taskId = v; }
        public Long getClueId() { return clueId; }
        public void setClueId(Long v) { this.clueId = v; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double v) { this.latitude = v; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double v) { this.longitude = v; }
        public String getCoordSystem() { return coordSystem; }
        public void setCoordSystem(String v) { this.coordSystem = v; }
        public OffsetDateTime getRecordedAt() { return recordedAt; }
        public void setRecordedAt(OffsetDateTime v) { this.recordedAt = v; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String v) { this.sourceType = v; }
        public Long getVersion() { return version; }
        public void setVersion(Long v) { this.version = v; }
    }

    public List<Point> getItems() { return items; }
    public void setItems(List<Point> v) { this.items = v; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int v) { this.pageSize = v; }
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String v) { this.nextCursor = v; }
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean v) { this.hasNext = v; }
}
