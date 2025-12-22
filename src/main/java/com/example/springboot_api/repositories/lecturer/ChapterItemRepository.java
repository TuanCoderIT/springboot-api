package com.example.springboot_api.repositories.lecturer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.ChapterItem;

@Repository
public interface ChapterItemRepository extends JpaRepository<ChapterItem, UUID> {

    @Query("SELECT ci FROM Chapter_Item ci WHERE ci.chapter.id = :chapterId ORDER BY ci.sortOrder ASC")
    List<ChapterItem> findByChapterIdOrderBySortOrderAsc(@Param("chapterId") UUID chapterId);

    @Query("SELECT MAX(ci.sortOrder) FROM Chapter_Item ci WHERE ci.chapter.id = :chapterId")
    Integer findMaxSortOrderByChapterId(@Param("chapterId") UUID chapterId);

    @Query("SELECT COUNT(ci) FROM Chapter_Item ci WHERE ci.chapter.id = :chapterId")
    Long countByChapterId(@Param("chapterId") UUID chapterId);
}
