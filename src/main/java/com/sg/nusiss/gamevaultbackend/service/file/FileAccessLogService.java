package com.sg.nusiss.gamevaultbackend.service.file;

import com.sg.nusiss.gamevaultbackend.entity.file.FileAccessLog;
import com.sg.nusiss.gamevaultbackend.repository.file.FileAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName FileAccessLogService
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAccessLogService {

    private final FileAccessLogRepository accessLogRepository;

    /**
     * 异步记录访问日志
     */
    @Async
    @Transactional
    public void recordAccessLog(String fileId, Long userId, Integer accessType,
                                String ipAddress, String userAgent) {
        try {
            FileAccessLog accessLog = FileAccessLog.builder()
                    .fileId(fileId)
                    .userId(userId)
                    .accessType(accessType)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            accessLogRepository.save(accessLog);
            log.debug("Access log recorded: fileId={}, userId={}, accessType={}",
                    fileId, userId, accessType);
        } catch (Exception e) {
            log.error("Failed to record access log", e);
        }
    }

    /**
     * 获取文件的访问日志
     */
    public List<FileAccessLog> getFileAccessLogs(String fileId) {
        return accessLogRepository.findByFileIdOrderByCreatedAtDesc(fileId);
    }

    /**
     * 获取用户的访问日志
     */
    public List<FileAccessLog> getUserAccessLogs(Long userId) {
        return accessLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 统计文件访问次数
     */
    public Long getFileAccessCount(String fileId) {
        return accessLogRepository.countByFileId(fileId);
    }

    /**
     * 统计文件在指定时间段的访问次数
     */
    public Long getFileAccessCountByTimeRange(String fileId, LocalDateTime startTime, LocalDateTime endTime) {
        return accessLogRepository.countByFileIdAndCreatedAtBetween(fileId, startTime, endTime);
    }

    /**
     * 统计文件的下载次数
     */
    public Long getFileDownloadCount(String fileId) {
        return accessLogRepository.countByFileIdAndAccessType(fileId, 2); // 2-下载
    }

    /**
     * 获取热门文件（最近N天）
     */
    public List<PopularFileStats> getPopularFiles(int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        List<Object[]> results = accessLogRepository.findPopularFiles(startTime);

        return results.stream()
                .map(row -> new PopularFileStats(
                        (String) row[0],  // fileId
                        ((Number) row[1]).longValue()  // accessCount
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取文件访问统计（按类型分组）
     */
    public Map<String, Long> getFileAccessStatsByType(String fileId) {
        List<Object[]> results = accessLogRepository.countAccessTypesByFileId(fileId);

        Map<String, Long> stats = new HashMap<>();
        for (Object[] row : results) {
            Integer accessType = (Integer) row[0];
            Long count = ((Number) row[1]).longValue();
            stats.put(getAccessTypeName(accessType), count);
        }

        return stats;
    }

    /**
     * 统计用户在指定时间段的访问次数
     */
    public Long getUserAccessCount(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return accessLogRepository.countByUserIdAndCreatedAtBetween(userId, startTime, endTime);
    }

    /**
     * 清理过期的访问日志
     */
    @Transactional
    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        accessLogRepository.deleteByCreatedAtBefore(cutoffTime);
        log.info("Cleaned up access logs older than {} days", daysToKeep);
    }

    /**
     * 根据IP地址获取访问日志
     */
    public List<FileAccessLog> getAccessLogsByIp(String ipAddress) {
        return accessLogRepository.findByIpAddressOrderByCreatedAtDesc(ipAddress);
    }

    /**
     * 获取指定时间段的访问日志
     */
    public List<FileAccessLog> getAccessLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return accessLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startTime, endTime);
    }

    /**
     * 获取访问类型名称
     */
    private String getAccessTypeName(Integer accessType) {
        switch (accessType) {
            case 1: return "查看";
            case 2: return "下载";
            case 3: return "分享";
            case 4: return "删除";
            default: return "未知";
        }
    }

    /**
     * 热门文件统计
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class PopularFileStats {
        private String fileId;
        private Long accessCount;
    }
}
