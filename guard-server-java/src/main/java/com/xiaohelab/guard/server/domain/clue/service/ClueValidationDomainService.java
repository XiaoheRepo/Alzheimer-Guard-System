package com.xiaohelab.guard.server.domain.clue.service;

import java.math.BigDecimal;

/**
 * 线索有效性校验领域服务。
 * 封装线索 risk_score 阈值、suspect_flag 判断等纯业务规则，无 IO 操作。
 * AI 填入的 risk_score 只影响线索的可疑标记，不触发任务状态变更（HC-01）。
 */
public class ClueValidationDomainService {

    /** 高风险阈值：risk_score ≥ 0.7 时标记为可疑线索 */
    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("0.7");

    /** 线索描述最小有效字符数 */
    private static final int MIN_DESCRIPTION_LENGTH = 10;

    /** 目击相关关键词（规则增强） */
    private static final String[] WITNESS_KEYWORDS = {"找到", "看到", "目击", "发现", "见到"};

    /**
     * 判断线索是否应标记为可疑（suspect_flag = true）。
     * 规则：risk_score ≥ 0.7 或描述包含目击关键词之一。
     */
    public boolean isSuspect(BigDecimal riskScore, String description) {
        if (riskScore != null && riskScore.compareTo(HIGH_RISK_THRESHOLD) >= 0) {
            return true;
        }
        return containsWitnessKeyword(description);
    }

    /**
     * 判断线索描述是否满足最低信息量要求（≥ 10 字符非空白）。
     */
    public boolean isDescriptionSufficient(String description) {
        return description != null
                && description.trim().length() >= MIN_DESCRIPTION_LENGTH;
    }

    /**
     * 判断坐标是否在合法范围内（WGS84 中国 bbox 粗校验）。
     */
    public boolean isCoordInRange(double lat, double lng) {
        return lat >= 3.86 && lat <= 53.55 && lng >= 73.66 && lng <= 135.05;
    }

    private boolean containsWitnessKeyword(String description) {
        if (description == null) return false;
        for (String kw : WITNESS_KEYWORDS) {
            if (description.contains(kw)) return true;
        }
        return false;
    }
}
