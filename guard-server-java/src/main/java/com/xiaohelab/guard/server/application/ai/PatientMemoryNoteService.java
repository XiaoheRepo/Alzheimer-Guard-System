package com.xiaohelab.guard.server.application.ai;

import com.xiaohelab.guard.server.infrastructure.persistence.do_.PatientMemoryNoteDO;
import com.xiaohelab.guard.server.infrastructure.persistence.mapper.PatientMemoryNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 患者记忆条目应用服务。
 * 记忆条目与 AI 会话上下文强关联，归属于 AI 域管理。
 */
@Service
@RequiredArgsConstructor
public class PatientMemoryNoteService {

    private final PatientMemoryNoteMapper patientMemoryNoteMapper;

    /** 新增记忆条目并写入数据库，返回已持久化的 DO（含自动生成的 note_id 和时间戳）。 */
    @Transactional
    public PatientMemoryNoteDO addNote(Long patientId, Long createdBy,
                                       String kind, String content, String tagsJson) {
        PatientMemoryNoteDO note = new PatientMemoryNoteDO();
        note.setNoteId(generateNoteId());
        note.setPatientId(patientId);
        note.setCreatedBy(createdBy);
        note.setKind(kind);
        note.setContent(content);
        note.setTags(tagsJson);
        patientMemoryNoteMapper.insert(note);
        return note;
    }

    public List<PatientMemoryNoteDO> listNotes(Long patientId, String kind, int pageSize, int offset) {
        return patientMemoryNoteMapper.listByPatientId(patientId, kind, pageSize, offset);
    }

    public long countNotes(Long patientId, String kind) {
        return patientMemoryNoteMapper.countByPatientId(patientId, kind);
    }

    private String generateNoteId() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(LocalDateTime.now(ZoneOffset.UTC));
        byte[] b = new byte[3];
        new java.util.Random().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte v : b) sb.append(String.format("%02x", v));
        return "mn_" + ts + "_" + sb;
    }
}
