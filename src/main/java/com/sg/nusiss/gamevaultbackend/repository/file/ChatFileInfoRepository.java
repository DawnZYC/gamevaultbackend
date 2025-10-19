package com.sg.nusiss.gamevaultbackend.repository.file;

import com.sg.nusiss.gamevaultbackend.entity.file.ChatFileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName ChatFileInfoRepository
 * @Author HUANG ZHENJIA
 * @Date 2025/10/19
 * @Description
 */
@Repository
public interface ChatFileInfoRepository extends JpaRepository<ChatFileInfo, Long> {

    /**
     * 根据文件ID查询
     */
    Optional<ChatFileInfo> findByFileId(String fileId);

    /**
     * 根据文件MD5查询（用于秒传）
     */
    // Optional<ChatFileInfo> findByFileMd5AndStatus(String fileMd5, Integer status);

    Optional<ChatFileInfo> findFirstByFileMd5AndStatusOrderByCreatedAtDesc(String fileMd5, Integer status);
    /**
     * 根据用户ID查询文件列表
     */
    List<ChatFileInfo> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status);

    /**
     * 根据业务类型和业务ID查询文件列表
     */
    List<ChatFileInfo> findByBizTypeAndBizIdAndStatusOrderByCreatedAtDesc(
            String bizType, String bizId, Integer status);

    /**
     * 根据文件类型查询
     */
    List<ChatFileInfo> findByFileTypeAndStatusOrderByCreatedAtDesc(
            String fileType, Integer status);

    /**
     * 根据用户ID和文件类型查询
     */
    List<ChatFileInfo> findByUserIdAndFileTypeAndStatusOrderByCreatedAtDesc(
            Long userId, String fileType, Integer status);

    /**
     * 查询过期的预签名URL（需要刷新）
     */
    @Query("SELECT f FROM ChatFileInfo f WHERE f.urlExpiresAt < :now AND f.status = 1")
    List<ChatFileInfo> findExpiredPresignedUrls(@Param("now") LocalDateTime now);

    /**
     * 统计用户上传的文件总大小
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM ChatFileInfo f WHERE f.userId = :userId AND f.status = 1")
    Long sumFileSizeByUserId(@Param("userId") Long userId);

    /**
     * 统计用户上传的文件数量
     */
    Long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 根据文件ID列表批量查询
     */
    List<ChatFileInfo> findByFileIdInAndStatus(List<String> fileIds, Integer status);

    /**
     * 删除指定时间之前的已删除文件记录（物理删除）
     */
    void deleteByStatusAndUpdatedAtBefore(Integer status, LocalDateTime before);
}
