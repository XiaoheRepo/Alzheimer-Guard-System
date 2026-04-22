package com.xiaohelab.guard.server.material.repository;

import com.xiaohelab.guard.server.material.entity.TagAssetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagAssetRepository extends JpaRepository<TagAssetEntity, Long> {

    Optional<TagAssetEntity> findByTagCode(String tagCode);

    Optional<TagAssetEntity> findByResourceToken(String resourceToken);

    Page<TagAssetEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    List<TagAssetEntity> findByOrderId(Long orderId);

    @Query(value = "SELECT * FROM tag_asset WHERE status = 'UNBOUND' AND tag_type = :tagType " +
            "ORDER BY id ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<TagAssetEntity> claimUnbound(@Param("tagType") String tagType, @Param("limit") int limit);

    /** 库存聚合：返回 [tag_type, status, count] 行。 */
    @Query("SELECT t.tagType, t.status, COUNT(t) FROM TagAssetEntity t GROUP BY t.tagType, t.status")
    List<Object[]> aggInventoryByTypeAndStatus();
}
