package com.xiaohelab.guard.server.domain.ai.service;

import com.xiaohelab.guard.server.common.exception.BizException;

/**
 * AI 会话领域服务。
 * 封装会话配额与 HC-01 约束的纯业务规则，无 IO 操作。
 * HC-01：本服务不持有任何对 rescue_task 表的写入路径；
 *        AI 分析结果只能写入 clue_record.risk_score，
 *        任务状态变更必须通过 RescueTaskEntity.close() 方法，由人类操作员触发。
 */
public class AiSessionDomainService {

    /**
     * 断言 token 配额未耗尽。
     *
     * @param usedTokens 今日已累计消耗 token 数
     * @param quotaLimit 每日配额上限
     * @throws BizException E_AI_4291 — 今日配额已耗尽
     */
    public void assertQuotaAvailable(long usedTokens, int quotaLimit) {
        if (usedTokens >= quotaLimit) {
            throw BizException.of("E_AI_4291");
        }
    }

    /**
     * 粗估消息内容消耗的 token 数（4 字符 ≈ 1 token，仅用于预检，不精确）。
     *
     * @param content 消息内容
     * @return 估算 token 数，最小为 1
     */
    public int estimateTokens(String content) {
        if (content == null || content.isEmpty()) return 1;
        return Math.max(1, content.length() / 4);
    }
}
