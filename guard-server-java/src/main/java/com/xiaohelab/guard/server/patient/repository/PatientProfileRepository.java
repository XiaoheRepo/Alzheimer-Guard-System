package com.xiaohelab.guard.server.patient.repository;

import com.xiaohelab.guard.server.patient.entity.PatientProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientProfileRepository extends JpaRepository<PatientProfileEntity, Long> {

    Optional<PatientProfileEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<PatientProfileEntity> findByDeletedAtIsNull(Pageable pageable);

    // ================= V2.1 管理员全局只读（FR-PRO-011）=================

    /**
     * 管理员全局游标分页。通过 primaryGuardianUserId 过滤时使用 EXISTS 子查询。
     *
     * @param keyword 模糊匹配 name / short_code / profile_no（可空）
     * @param status  lost_status 精确过滤（可空）
     * @param gender  gender 精确过滤（可空）
     * @param primaryGuardianUserId 主监护人 user_id（可空）
     * @param cursor  上一页 last id；取 `id < cursor`
     */
    // 使用 Native Query + CAST 规避 Hibernate 6 + PostgreSQL 的 bytea 类型推断 Bug（text ~~ bytea）
    @Query(nativeQuery = true, value =
            "SELECT * FROM patient_profile p " +
            "WHERE p.deleted_at IS NULL " +
            "  AND (CAST(:kw AS text) IS NULL " +
            "       OR lower(p.name) LIKE '%' || CAST(:kw AS text) || '%' " +
            "       OR lower(p.short_code) LIKE '%' || CAST(:kw AS text) || '%' " +
            "       OR lower(COALESCE(p.profile_no,'')) LIKE '%' || CAST(:kw AS text) || '%') " +
            "  AND (CAST(:status AS text) IS NULL OR p.lost_status = CAST(:status AS text)) " +
            "  AND (CAST(:gender AS text) IS NULL OR p.gender = CAST(:gender AS text)) " +
            "  AND (CAST(:primaryUserId AS bigint) IS NULL OR EXISTS (" +
            "       SELECT 1 FROM guardian_relation g " +
            "       WHERE g.patient_id = p.id AND g.user_id = CAST(:primaryUserId AS bigint) " +
            "         AND g.relation_role = 'PRIMARY_GUARDIAN' AND g.relation_status = 'ACTIVE')) " +
            "  AND (CAST(:cursor AS bigint) IS NULL OR p.id < CAST(:cursor AS bigint)) " +
            "ORDER BY p.id DESC " +
            "LIMIT :lim")
    List<PatientProfileEntity> findForAdmin(@Param("kw") String keyword,
                                            @Param("status") String status,
                                            @Param("gender") String gender,
                                            @Param("primaryUserId") Long primaryGuardianUserId,
                                            @Param("cursor") Long cursor,
                                            @Param("lim") int limit);
}
