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
    @Query("select p from PatientProfileEntity p " +
            "where p.deletedAt is null " +
            "  and (:kw is null or " +
            "       lower(p.name) like concat('%', :kw, '%') or " +
            "       lower(p.shortCode) like concat('%', :kw, '%') or " +
            "       lower(coalesce(p.profileNo, '')) like concat('%', :kw, '%')) " +
            "  and (:status is null or p.lostStatus = :status) " +
            "  and (:gender is null or p.gender = :gender) " +
            "  and (:primaryUserId is null or exists (" +
            "       select 1 from GuardianRelationEntity g " +
            "       where g.patientId = p.id and g.userId = :primaryUserId " +
            "         and g.relationRole = 'PRIMARY_GUARDIAN' and g.relationStatus = 'ACTIVE')) " +
            "  and (:cursor is null or p.id < :cursor) " +
            "order by p.id desc")
    List<PatientProfileEntity> findForAdmin(@Param("kw") String keyword,
                                            @Param("status") String status,
                                            @Param("gender") String gender,
                                            @Param("primaryUserId") Long primaryGuardianUserId,
                                            @Param("cursor") Long cursor,
                                            Pageable pageable);
}
