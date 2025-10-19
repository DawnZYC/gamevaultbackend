package com.sg.nusiss.gamevaultbackend.repository.file;

import com.sg.nusiss.gamevaultbackend.entity.file.FileChunkInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName FileChunkInfoRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Repository
public interface FileChunkInfoRepository extends JpaRepository<FileChunkInfo, Long> {

    /**
     * 根据任务ID和分片序号查询
     */
    Optional<FileChunkInfo> findByTaskIdAndChunkNumber(String taskId, Integer chunkNumber);

    /**
     * 根据任务ID查询所有分片
     */
    List<FileChunkInfo> findByTaskIdOrderByChunkNumber(String taskId);

    /**
     * 根据任务ID和状态查询分片
     */
    List<FileChunkInfo> findByTaskIdAndStatusOrderByChunkNumber(String taskId, Integer status);

    /**
     * 统计任务的已完成分片数
     */
    Long countByTaskIdAndStatus(String taskId, Integer status);

    /**
     * 查询过期的分片上传URL
     */
    @Query("SELECT c FROM FileChunkInfo c WHERE c.urlExpiresAt < :now AND c.status IN (1, 2)")
    List<FileChunkInfo> findExpiredUploadUrls(@Param("now") LocalDateTime now);

    /**
     * 更新分片状态和ETag
     */
    @Modifying
    @Query("UPDATE FileChunkInfo c SET c.status = :status, c.etag = :etag, c.updatedAt = :updatedAt " +
            "WHERE c.taskId = :taskId AND c.chunkNumber = :chunkNumber")
    int updateChunkStatus(@Param("taskId") String taskId,
                          @Param("chunkNumber") Integer chunkNumber,
                          @Param("status") Integer status,
                          @Param("etag") String etag,
                          @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 批量更新分片状态
     */
    @Modifying
    @Query("UPDATE FileChunkInfo c SET c.status = :status, c.updatedAt = :updatedAt WHERE c.taskId = :taskId")
    int updateAllChunkStatusByTaskId(@Param("taskId") String taskId,
                                     @Param("status") Integer status,
                                     @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 根据任务ID删除所有分片
     */
    void deleteByTaskId(String taskId);

    /**
     * 批量删除任务的所有分片
     */
    void deleteByTaskIdIn(List<String> taskIds);

    /**
     * 查询任务的所有已完成分片（按序号排序，用于合并）
     */
    @Query("SELECT c FROM FileChunkInfo c WHERE c.taskId = :taskId AND c.status = 3 ORDER BY c.chunkNumber")
    List<FileChunkInfo> findCompletedChunksByTaskId(@Param("taskId") String taskId);
}
