package com.xiaohelab.guard.server.gov.repository;

import com.xiaohelab.guard.server.gov.entity.WsTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WsTicketRepository extends JpaRepository<WsTicketEntity, Long> {

    Optional<WsTicketEntity> findByTicket(String ticket);
}
