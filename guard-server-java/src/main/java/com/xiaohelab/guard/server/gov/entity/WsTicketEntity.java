package com.xiaohelab.guard.server.gov.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ws_ticket")
public class WsTicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket", length = 128, nullable = false, unique = true)
    private String ticket;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expire_at", nullable = false)
    private OffsetDateTime expireAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicket() { return ticket; }
    public void setTicket(String ticket) { this.ticket = ticket; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
