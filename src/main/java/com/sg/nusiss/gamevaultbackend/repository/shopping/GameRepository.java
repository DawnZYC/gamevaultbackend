package com.sg.nusiss.gamevaultbackend.repository.shopping;

import com.sg.nusiss.gamevaultbackend.entity.shopping.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long>, GameRepositoryCustom {

    // 按类型查找
    List<Game> findByGenre(String genre);

    // 按平台查找
    List<Game> findByPlatform(String platform);

    // 模糊搜索（忽略大小写）
    List<Game> findByTitleContainingIgnoreCase(String q);
}

/* 放在同一文件，包可见即可 */
interface GameRepositoryCustom {
    List<Game> findTopDiscountedGames(int limit);
}
