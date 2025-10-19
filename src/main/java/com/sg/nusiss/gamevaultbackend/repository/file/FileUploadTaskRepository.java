package com.sg.nusiss.gamevaultbackend.repository.file;

import com.sg.nusiss.gamevaultbackend.entity.file.FileUploadTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName FileUploadTaskRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Repository
public interface FileUploadTaskRepository extends JpaRepository<FileUploadTask, Long> {

    /**
     * 根据任务ID查询
     */
    Optional<FileUploadTask> findByTaskId(String taskId);

    /**
     * 根据文件MD5查询（检查是否有相同文件正在上传）
     */
    Optional<FileUploadTask> findByFileMd5AndStatus(String fileMd5, Integer status);

    /**
     * 根据用户ID查询上传中的任务
     */
    List<FileUploadTask> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status);

    /**
     * 统计用户正在上传的任务数量（用于并发控制）
     */
    Long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 查询过期的上传任务
     */
    @Query("SELECT t FROM FileUploadTask t WHERE t.expiresAt < :now AND t.status = 1")
    List<FileUploadTask> findExpiredTasks(@Param("now") LocalDateTime now);

    /**
     * 更新任务状态
     */
    @Modifying
    @Query("UPDATE FileUploadTask t SET t.status = :status, t.updatedAt = :updatedAt WHERE t.taskId = :taskId")
    int updateStatusByTaskId(@Param("taskId") String taskId,
                             @Param("status") Integer status,
                             @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 增加已上传分片数
     */
    @Modifying
    @Query("UPDATE FileUploadTask t SET t.uploadedChunks = t.uploadedChunks + 1, t.updatedAt = :updatedAt WHERE t.taskId = :taskId")
    int incrementUploadedChunks(@Param("taskId") String taskId,
                                @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * 根据用户ID和状态删除任务
     */
    void deleteByUserIdAndStatus(Long userId, Integer status);

    /**
     * 删除过期的失败/取消任务（物理删除）
     */
    void deleteByStatusInAndUpdatedAtBefore(List<Integer> statuses, LocalDateTime before);

    /**
     * 查询指定时间段内的任务
     */
    List<FileUploadTask> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}
