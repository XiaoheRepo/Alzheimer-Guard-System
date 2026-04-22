package com.xiaohelab.guard.server.user.repository;

import com.xiaohelab.guard.server.user.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据仓储。
 * <p>V2.1 增量：增加管理员治理查询接口（游标分页 + CAS 状态迁移 + 注销）。</p>
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<UserEntity> findByUsernameContainingIgnoreCase(String keyword);

    // ================= V2.1 管理员治理 =================

    /**
     * 管理员列表游标分页（按 id DESC）。调用方需提前按当前角色限制 roles 集合，服务端再次强制过滤。
     *
     * @param keyword  username / nickname / email / phone 的模糊关键字（可为空）
     * @param roles    当前操作者可见的 role 集合，不得为空
     * @param statuses 过滤 status 集合（可为空或空集）
     * @param cursor   上一页最后一条的 id；取 `id < cursor` 的记录
     * @param pageable 分页参数（仅用 size）
     */
    // 使用 Native Query + CAST 规避 Hibernate 6 + PostgreSQL 的 bytea 类型推断 Bug
    @Query(nativeQuery = true, value =
            "SELECT * FROM sys_user u " +
            "WHERE (CAST(:kw AS text) IS NULL " +
            "       OR lower(u.username) LIKE '%' || CAST(:kw AS text) || '%' " +
            "       OR lower(COALESCE(u.nickname,'')) LIKE '%' || CAST(:kw AS text) || '%' " +
            "       OR lower(u.email) LIKE '%' || CAST(:kw AS text) || '%' " +
            "       OR COALESCE(u.phone,'') LIKE '%' || CAST(:kw AS text) || '%') " +
            "  AND u.role IN (:roles) " +
            "  AND (:statuses IS NULL OR u.status IN (:statuses)) " +
            "  AND (CAST(:cursor AS bigint) IS NULL OR u.id < CAST(:cursor AS bigint)) " +
            "ORDER BY u.id DESC " +
            "LIMIT :lim")
    List<UserEntity> findForAdmin(@Param("kw") String keyword,
                                  @Param("roles") Collection<String> roles,
                                  @Param("statuses") Collection<String> statuses,
                                  @Param("cursor") Long cursor,
                                  @Param("lim") int limit);

    /**
     * 条件性原子状态迁移（CAS）。
     *
     * @param id             用户主键
     * @param expectedStatus 期望的当前状态（例如 ACTIVE）
     * @param newStatus      目标状态（例如 DISABLED）
     * @return 影响行数；0 表示并发冲突或状态已变化
     */
    @Modifying
    @Query("update UserEntity u set u.status = :newStatus " +
            "where u.id = :id and u.status = :expectedStatus")
    int casStatus(@Param("id") Long id,
                  @Param("expectedStatus") String expectedStatus,
                  @Param("newStatus") String newStatus);

    /**
     * 注销 CAS：将 status 置为 DEACTIVATED，并为 username/email/phone 追加后缀以释放唯一约束。
     *
     * @param id     用户主键
     * @param suffix 后缀（调用方传入 `#DEL_{epochSeconds}`）
     * @param now    注销时间戳
     * @return 影响行数；0 表示状态不再为 ACTIVE / DISABLED
     */
    @Modifying
    @Query("update UserEntity u set u.status = 'DEACTIVATED', " +
            "u.deactivatedAt = :now, " +
            "u.username = concat(u.username, :suffix), " +
            "u.email = concat(u.email, :suffix), " +
            "u.phone = case when u.phone is null then null else concat(u.phone, :suffix) end " +
            "where u.id = :id and u.status in ('ACTIVE','DISABLED')")
    int casDeactivate(@Param("id") Long id,
                      @Param("suffix") String suffix,
                      @Param("now") OffsetDateTime now);
}
