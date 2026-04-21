package com.xiaohelab.guard.server.gov.repository;

import com.xiaohelab.guard.server.gov.entity.SysConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysConfigRepository extends JpaRepository<SysConfigEntity, String> {

    List<SysConfigEntity> findByScope(String scope);
}
