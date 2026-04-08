package com.xiaohelab.guard.server.interfaces.ai;

import com.xiaohelab.guard.server.application.ai.AiSessionService;
import com.xiaohelab.guard.server.application.ai.PatientMemoryNoteService;
import com.xiaohelab.guard.server.application.guardian.GuardianInvitationService;
import com.xiaohelab.guard.server.application.patient.PatientProfileService;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.response.ApiResponse;
import com.xiaohelab.guard.server.common.response.PageResponse;
import com.xiaohelab.guard.server.domain.ai.entity.AiSessionEntity;
import com.xiaohelab.guard.server.domain.ai.entity.AiSessionMessageEntity;
import com.xiaohelab.guard.server.domain.ai.entity.PatientMemoryNoteEntity;
import com.xiaohelab.guard.server.security.config.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 域 REST 接口。
 * 用户侧：会话创建/列表/消息收发/归档/配额（3.5.1~3.5.11）。
 * 管理侧：会话审计（3.5.4、3.5.8、3.5.9）。
 * 患者记忆条目：3.5.5、3.5.6。
 */
@RestController
@RequiredArgsConstructor
public class AiController {

    private static final Set<String> VALID_KIND = Set.of("HABIT", "PLACE", "PREFERENCE", "SAFETY_CUE");

    private final AiSessionService aiSessionService;
    private final PatientProfileService patientService;
    private final GuardianInvitationService guardianInvitationService;
    private final PatientMemoryNoteService patientMemoryNoteService;
    private final SecurityContext securityContext;

    // =========================================================================
    // 3.5.1 POST /api/v1/ai/sessions
    // =========================================================================

    @PostMapping("/api/v1/ai/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<Map<String, Object>> createSession(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody CreateSessionRequest req) {

        Long patientId = parseLong(req.getPatientId(), "patient_id");
        Long taskId = req.getTaskId() == null ? null : parseLong(req.getTaskId(), "task_id");

        AiSessionEntity session = aiSessionService.createSession(
                securityContext.currentUserId(), patientId, taskId);

        return ApiResponse.ok(Map.of("session_id", session.getSessionId()), traceId);
    }

    // =========================================================================
    // 3.5.2 GET /api/v1/ai/sessions
    // =========================================================================

    @GetMapping("/api/v1/ai/sessions")
    public ApiResponse<PageResponse<Map<String, Object>>> listSessions(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(name = "patient_id", required = false) String patientIdStr,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        Long patientId = patientIdStr == null ? null : parseLong(patientIdStr, "patient_id");
        List<AiSessionEntity> list = aiSessionService.listSessions(userId, patientId, pageNo, pageSize);
        long total = aiSessionService.countSessions(userId, patientId);

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(list.stream().map(s -> sessionSummaryVO(s, true)).toList())
                .pageNo(pageNo).pageSize(pageSize).total(total)
                .hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // =========================================================================
    // 3.6.4 GET /api/v1/ai/sessions/{sessionId}
    // =========================================================================

    @GetMapping("/api/v1/ai/sessions/{sessionId}")
    public ApiResponse<Map<String, Object>> getSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        AiSessionEntity session = aiSessionService.getSession(sessionId, securityContext.currentUserId());
        long roundCount = aiSessionService.countMessages(sessionId) / 2;
        return ApiResponse.ok(sessionDetailVO(session, roundCount), traceId);
    }

    // =========================================================================
    // 3.5.3 POST /api/v1/ai/sessions/{sessionId}/messages
    //   Accept: application/json  → 202 JSON ACK
    //   Accept: text/event-stream → 200 SSE stream
    // =========================================================================

    @PostMapping(value = "/api/v1/ai/sessions/{sessionId}/messages",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object sendMessage(
            @PathVariable String sessionId,
            @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) String accept,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody SendMessageRequest req,
            HttpServletResponse response) {

        Long userId = securityContext.currentUserId();

        if (MediaType.TEXT_EVENT_STREAM_VALUE.equals(accept)) {
            response.setHeader("X-Accel-Buffering", "no");
            return aiSessionService.sendMessageStream(sessionId, userId, req.getPrompt(), traceId);
        } else {
            // JSON ACK mode – return 202
            response.setStatus(HttpStatus.ACCEPTED.value());
            Map<String, Object> ack = aiSessionService.sendMessageAck(
                    sessionId, userId, req.getPrompt(), traceId);
            return ApiResponse.ok(ack, traceId);
        }
    }

    // =========================================================================
    // 3.5.7 GET /api/v1/ai/sessions/{sessionId}/messages
    // =========================================================================

    @GetMapping("/api/v1/ai/sessions/{sessionId}/messages")
    public ApiResponse<PageResponse<Map<String, Object>>> getMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Long userId = securityContext.currentUserId();
        List<AiSessionMessageEntity> msgs = aiSessionService.getMessages(sessionId, userId, pageNo, pageSize);
        long total = aiSessionService.countMessages(sessionId);

        List<Map<String, Object>> items = msgs.stream()
                .map(m -> Map.<String, Object>of(
                        "message_id", "aim_" + m.getId(),
                        "role", m.getRole(),
                        "content", m.getContent(),
                        "timestamp", m.getCreatedAt() == null ? "" : m.getCreatedAt().toString(),
                        "token_used", 0))
                .toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize).total(total)
                .hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // =========================================================================
    // 3.5.10 GET /api/v1/ai/sessions/{sessionId}/quota
    // =========================================================================

    @GetMapping("/api/v1/ai/sessions/{sessionId}/quota")
    public ApiResponse<Map<String, Object>> getQuota(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        Map<String, Object> quota = aiSessionService.getQuota(sessionId, securityContext.currentUserId());
        return ApiResponse.ok(quota, traceId);
    }

    // =========================================================================
    // 3.5.11 POST /api/v1/ai/sessions/{sessionId}/archive
    // =========================================================================

    @PostMapping("/api/v1/ai/sessions/{sessionId}/archive")
    @Transactional
    public ApiResponse<Map<String, Object>> archiveSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody(required = false) ArchiveRequest req) {

        AiSessionEntity session = aiSessionService.archiveSession(sessionId, securityContext.currentUserId());
        return ApiResponse.ok(Map.of(
                "session_id", session.getSessionId(),
                "status", session.getStatus(),
                "archived_at", session.getArchivedAt() == null ? "" : session.getArchivedAt().toString()),
                traceId);
    }

    // =========================================================================
    // 3.5.4 GET /api/v1/admin/ai/sessions  (ADMIN)
    // =========================================================================

    @GetMapping("/api/v1/admin/ai/sessions")
    public ApiResponse<PageResponse<Map<String, Object>>> adminListSessions(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(name = "patient_id", required = false) String patientIdStr,
            @RequestParam(name = "user_id", required = false) String userIdStr,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        Long patientId = patientIdStr == null ? null : parseLong(patientIdStr, "patient_id");
        Long filterUserId = userIdStr == null ? null : parseLong(userIdStr, "user_id");

        List<AiSessionEntity> list = aiSessionService.listAllSessions(filterUserId, patientId, pageNo, pageSize);
        long total = aiSessionService.countAllSessions(filterUserId, patientId);

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(list.stream().map(s -> sessionSummaryVO(s, false)).toList())
                .pageNo(pageNo).pageSize(pageSize).total(total)
                .hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // =========================================================================
    // 3.5.8 GET /api/v1/admin/ai/sessions/{sessionId}  (ADMIN detail)
    // =========================================================================

    @GetMapping("/api/v1/admin/ai/sessions/{sessionId}")
    public ApiResponse<Map<String, Object>> adminGetSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        AiSessionEntity session = aiSessionService.adminGetSession(sessionId);
        long roundCount = aiSessionService.countMessages(sessionId) / 2;
        return ApiResponse.ok(sessionDetailVO(session, roundCount), traceId);
    }

    // =========================================================================
    // 3.5.9 GET /api/v1/admin/ai/sessions/{sessionId}/messages  (ADMIN)
    // =========================================================================

    @GetMapping("/api/v1/admin/ai/sessions/{sessionId}/messages")
    public ApiResponse<PageResponse<Map<String, Object>>> adminGetMessages(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        requireAdmin();
        long total = aiSessionService.countMessages(sessionId);
        List<AiSessionMessageEntity> msgs = aiSessionService
                .adminGetMessages(sessionId, pageNo, pageSize);

        List<Map<String, Object>> items = msgs.stream()
                .map(m -> Map.<String, Object>of(
                        "message_id", "aim_" + m.getId(),
                        "role", m.getRole(),
                        "content", m.getContent(),
                        "timestamp", m.getCreatedAt() == null ? "" : m.getCreatedAt().toString(),
                        "is_deleted", false,
                        "deleted_at", (Object) null))
                .toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize).total(total)
                .hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // =========================================================================
    // 3.5.5 POST /api/v1/patients/{patientId}/memory-notes
    // =========================================================================

    @PostMapping("/api/v1/patients/{patientId}/memory-notes")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<Map<String, Object>> addMemoryNote(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @Valid @RequestBody MemoryNoteRequest req) {

        // 验证患者存在
        patientService.getPatientById(patientId);
        // 验证监护权限（admin 可跳过）
        if (!securityContext.isAdmin()) {
            if (!guardianInvitationService.hasActiveRelation(securityContext.currentUserId(), patientId)) {
                throw BizException.of("E_PRO_4030");
            }
        }
        // 校验 kind
        if (!VALID_KIND.contains(req.getKind())) throw BizException.of("E_AI_4003");

        // 构建 tags JSON
        String tagsJson = "[]";
        if (req.getTags() != null && !req.getTags().isEmpty()) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < req.getTags().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(req.getTags().get(i).replace("\"", "\\\"")).append("\"");
            }
            sb.append("]");
            tagsJson = sb.toString();
        }

        PatientMemoryNoteEntity note = patientMemoryNoteService.addNote(
                patientId, securityContext.currentUserId(), req.getKind(), req.getContent(), tagsJson);

        return ApiResponse.ok(Map.of(
                "note_id", note.getNoteId(),
                "patient_id", String.valueOf(patientId),
                "kind", note.getKind(),
                "created_at", note.getCreatedAt() == null
                        ? java.time.Instant.now().toString() : note.getCreatedAt().toString()),
                traceId);
    }

    // =========================================================================
    // 3.5.6 GET /api/v1/patients/{patientId}/memory-notes
    // =========================================================================

    @GetMapping("/api/v1/patients/{patientId}/memory-notes")
    public ApiResponse<PageResponse<Map<String, Object>>> listMemoryNotes(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String kind,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        patientService.getPatientById(patientId);
        if (!securityContext.isAdmin()) {
            if (!guardianInvitationService.hasActiveRelation(securityContext.currentUserId(), patientId)) {
                throw BizException.of("E_PRO_4030");
            }
        }
        if (kind != null && !VALID_KIND.contains(kind)) throw BizException.of("E_REQ_4005");

        int offset = (pageNo - 1) * pageSize;
        List<PatientMemoryNoteEntity> notes = patientMemoryNoteService.listNotes(patientId, kind, pageSize, offset);
        long total = patientMemoryNoteService.countNotes(patientId, kind);

        List<Map<String, Object>> items = notes.stream()
                .map(n -> Map.<String, Object>of(
                        "note_id", n.getNoteId(),
                        "kind", n.getKind(),
                        "content", n.getContent(),
                        "tags", parseTagsToList(n.getTags()),
                        "created_at", n.getCreatedAt() == null ? "" : n.getCreatedAt().toString()))
                .toList();

        return ApiResponse.ok(PageResponse.<Map<String, Object>>builder()
                .items(items).pageNo(pageNo).pageSize(pageSize).total(total)
                .hasNext(total > (long) pageNo * pageSize)
                .build(), traceId);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void requireAdmin() {
        if (!securityContext.isAdmin()) throw BizException.of("E_GOV_4030");
    }

    private Long parseLong(String s, String field) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw BizException.of("E_REQ_4005");
        }
    }

    private Map<String, Object> sessionSummaryVO(AiSessionEntity s, boolean includeTaskId) {
        java.util.LinkedHashMap<String, Object> vo = new java.util.LinkedHashMap<>();
        vo.put("session_id", s.getSessionId());
        if (!includeTaskId) vo.put("user_id", String.valueOf(s.getUserId()));
        vo.put("patient_id", String.valueOf(s.getPatientId()));
        if (includeTaskId) vo.put("task_id", s.getTaskId() == null ? null : String.valueOf(s.getTaskId()));
        vo.put("model_name", s.getModelName() == null ? "" : s.getModelName());
        vo.put("token_used_total", String.valueOf(s.getTokenUsed() == null ? 0 : s.getTokenUsed()));
        vo.put("round_count", 0); // lightweight summary, skip count query
        vo.put("status", s.getStatus());
        vo.put("updated_at", s.getUpdatedAt() == null ? "" : s.getUpdatedAt().toString());
        return vo;
    }

    private Map<String, Object> sessionDetailVO(AiSessionEntity s, long roundCount) {
        java.util.LinkedHashMap<String, Object> vo = new java.util.LinkedHashMap<>();
        vo.put("session_id", s.getSessionId());
        vo.put("user_id", String.valueOf(s.getUserId()));
        vo.put("patient_id", String.valueOf(s.getPatientId()));
        vo.put("task_id", s.getTaskId() == null ? null : String.valueOf(s.getTaskId()));
        vo.put("model_name", s.getModelName() == null ? "" : s.getModelName());
        vo.put("status", s.getStatus());
        vo.put("round_count", roundCount);
        vo.put("token_used_total", String.valueOf(s.getTokenUsed() == null ? 0 : s.getTokenUsed()));
        vo.put("created_at", s.getCreatedAt() == null ? null : s.getCreatedAt().toString());
        vo.put("updated_at", s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString());
        return vo;
    }

    private List<String> parseTagsToList(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank() || "[]".equals(tagsJson.trim())) {
            return List.of();
        }
        String stripped = tagsJson.trim().replaceAll("^\\[|]$", "").trim();
        if (stripped.isEmpty()) return List.of();
        return Arrays.stream(stripped.split(","))
                .map(t -> t.trim().replaceAll("^\"|\"$", ""))
                .filter(t -> !t.isEmpty())
                .toList();
    }

    // =========================================================================
    // REQUEST CLASSES
    // =========================================================================

    @Data
    public static class CreateSessionRequest {
        @NotBlank
        private String patientId;
        private String taskId;
    }

    @Data
    public static class SendMessageRequest {
        @NotBlank
        @Size(min = 1, max = 4000)
        private String prompt;
    }

    @Data
    public static class MemoryNoteRequest {
        @NotBlank
        private String kind;
        @NotBlank
        @Size(min = 5, max = 2000)
        private String content;
        private List<String> tags;
    }

    @Data
    public static class ArchiveRequest {
        @Size(max = 256)
        private String reason;
    }
}
