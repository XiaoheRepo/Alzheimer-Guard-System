package com.xiaohelab.guard.server.ai.controller;

import com.xiaohelab.guard.server.ai.dto.MemoryNoteRequest;
import com.xiaohelab.guard.server.ai.entity.PatientMemoryNoteEntity;
import com.xiaohelab.guard.server.ai.service.PatientMemoryNoteService;
import com.xiaohelab.guard.server.common.annotation.Idempotent;
import com.xiaohelab.guard.server.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI.MemoryNote", description = "患者记忆笔记 (记忆增强 RAG 数据源)")
@RestController
@RequestMapping("/api/v1/ai/memory-notes")
public class PatientMemoryNoteController {

    private final PatientMemoryNoteService noteService;

    public PatientMemoryNoteController(PatientMemoryNoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @Idempotent
    @Operation(summary = "追加患者记忆笔记")
    public Result<PatientMemoryNoteEntity> create(@Valid @RequestBody MemoryNoteRequest req) {
        return Result.ok(noteService.create(req));
    }

    @GetMapping
    @Operation(summary = "分页列出患者记忆笔记")
    public Result<Page<PatientMemoryNoteEntity>> list(@RequestParam Long patientId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return Result.ok(noteService.list(patientId, page, size));
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "获取单条记忆笔记详情")
    public Result<PatientMemoryNoteEntity> get(@PathVariable String noteId) {
        return Result.ok(noteService.get(noteId));
    }

    @DeleteMapping("/{noteId}")
    @Idempotent
    @Operation(summary = "删除记忆笔记")
    public Result<Void> delete(@PathVariable String noteId) {
        noteService.delete(noteId);
        return Result.ok();
    }
}
