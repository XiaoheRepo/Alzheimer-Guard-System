package com.xiaohelab.guard.server.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohelab.guard.server.ai.dto.MemoryNoteRequest;
import com.xiaohelab.guard.server.ai.entity.PatientMemoryNoteEntity;
import com.xiaohelab.guard.server.ai.repository.PatientMemoryNoteRepository;
import com.xiaohelab.guard.server.common.error.ErrorCode;
import com.xiaohelab.guard.server.common.event.OutboxTopics;
import com.xiaohelab.guard.server.common.exception.BizException;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.common.util.BusinessNoUtil;
import com.xiaohelab.guard.server.outbox.service.OutboxService;
import com.xiaohelab.guard.server.patient.service.GuardianAuthorizationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** 患者记忆笔记 CRUD。 */
@Service
public class PatientMemoryNoteService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PatientMemoryNoteRepository noteRepository;
    private final GuardianAuthorizationService authorizationService;
    private final OutboxService outboxService;

    public PatientMemoryNoteService(PatientMemoryNoteRepository noteRepository,
                                    GuardianAuthorizationService authorizationService,
                                    OutboxService outboxService) {
        this.noteRepository = noteRepository;
        this.authorizationService = authorizationService;
        this.outboxService = outboxService;
    }

    @Transactional(rollbackFor = Exception.class)
    public PatientMemoryNoteEntity create(MemoryNoteRequest req) {
        AuthUser user = SecurityUtil.current();
        authorizationService.assertGuardian(user, req.getPatientId());
        PatientMemoryNoteEntity n = new PatientMemoryNoteEntity();
        n.setNoteId(BusinessNoUtil.noteId());
        n.setPatientId(req.getPatientId());
        n.setCreatedBy(user.getUserId());
        n.setKind(req.getKind());
        n.setContent(req.getContent());
        try {
            n.setTags(req.getTags() != null ? MAPPER.writeValueAsString(req.getTags()) : "{}");
        } catch (JsonProcessingException e) {
            throw BizException.of(ErrorCode.E_SYS_5000, "tags 序列化失败");
        }
        noteRepository.save(n);
        outboxService.publish(OutboxTopics.MEMORY_APPENDED, n.getNoteId(),
                String.valueOf(req.getPatientId()),
                Map.of("note_id", n.getNoteId(), "patient_id", req.getPatientId(), "kind", req.getKind()));
        return n;
    }

    public Page<PatientMemoryNoteEntity> list(Long patientId, int page, int size) {
        AuthUser user = SecurityUtil.current();
        authorizationService.assertGuardian(user, patientId);
        return noteRepository.findByPatientIdOrderByCreatedAtDesc(patientId, PageRequest.of(page, size));
    }

    public PatientMemoryNoteEntity get(String noteId) {
        AuthUser user = SecurityUtil.current();
        PatientMemoryNoteEntity n = noteRepository.findByNoteId(noteId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        authorizationService.assertGuardian(user, n.getPatientId());
        return n;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String noteId) {
        AuthUser user = SecurityUtil.current();
        PatientMemoryNoteEntity n = noteRepository.findByNoteId(noteId)
                .orElseThrow(() -> BizException.of(ErrorCode.E_AI_4041));
        authorizationService.assertGuardian(user, n.getPatientId());
        noteRepository.delete(n);
    }
}
