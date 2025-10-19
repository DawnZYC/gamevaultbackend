package com.sg.nusiss.gamevaultbackend.repository.file;

import com.sg.nusiss.gamevaultbackend.entity.file.FileAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName FileAccessLogRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Repository
public interface FileAccessLogRepository extends JpaRepository<FileAccessLog, Long> {

    /**
     * 根据文件ID查询访问日志
     */
    List<FileAccessLog> findByFileIdOrderByCreatedAtDesc(String fileId);

    /**
     * 根据用户ID查询访问日志
     */
    List<FileAccessLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 根据文件ID和访问类型查询
     */
    List<FileAccessLog> findByFileIdAndAccessTypeOrderByCreatedAtDesc(String fileId, Integer accessType);

    /**
     * 统计文件的访问次数
     */
    Long countByFileId(String fileId);

    /**
     * 统计文件在指定时间段内的访问次数
     */
    Long countByFileIdAndCreatedAtBetween(String fileId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计文件的下载次数
     */
    Long countByFileIdAndAccessType(String fileId, Integer accessType);

    /**
     * 查询最近N天的热门文件（按访问次数排序）
     */
    @Query("SELECT l.fileId, COUNT(l) as accessCount FROM FileAccessLog l " +
            "WHERE l.createdAt >= :startTime " +
            "GROUP BY l.fileId " +
            "ORDER BY accessCount DESC")
    List<Object[]> findPopularFiles(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计用户在指定时间段内的访问次数
     */
    Long countByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据IP地址查询访问日志
     */
    List<FileAccessLog> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    /**
     * 查询指定时间段内的访问日志
     */
    List<FileAccessLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除指定时间之前的访问日志（用于日志清理）
     */
    void deleteByCreatedAtBefore(LocalDateTime before);

    /**
     * 统计指定文件类型的访问次数
     */
    @Query("SELECT l.accessType, COUNT(l) FROM FileAccessLog l " +
            "WHERE l.fileId = :fileId " +
            "GROUP BY l.accessType")
    List<Object[]> countAccessTypesByFileId(@Param("fileId") String fileId);
}
