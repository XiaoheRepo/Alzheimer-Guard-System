package com.xiaohelab.guard.server.application.governance;

import com.xiaohelab.guard.server.domain.governance.entity.SysConfigEntity;
import com.xiaohelab.guard.server.domain.governance.repository.SysConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置应用服务。
 * scope: public / ops / security / ai_policy
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigRepository sysConfigRepository;

    public Optional<SysConfigEntity> findByKey(String configKey) {
        return sysConfigRepository.findByKey(configKey);
    }

    public List<SysConfigEntity> listAll() {
        return sysConfigRepository.listAll();
    }

    public List<SysConfigEntity> listByScope(String scope) {
        return sysConfigRepository.listByScope(scope);
    }

    @Transactional
    public void updateConfig(String configKey, String configValue, String scope,
                             Long updatedBy, String reason) {
        SysConfigEntity entity = SysConfigEntity.of(configKey, configValue, scope, updatedBy, reason);
        sysConfigRepository.upsert(entity);
    }
}
