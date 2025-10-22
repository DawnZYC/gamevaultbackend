package com.sg.nusiss.gamevaultbackend.repository.developer;

import com.sg.nusiss.gamevaultbackend.entity.developer.DevGameStatistics;

import java.util.Optional;

public interface DevGameStatisticsRepository {
    Optional<DevGameStatistics> findByGameId(String gameId);
    void insert(DevGameStatistics stats);
    void updateCounts(String gameId, int viewIncrement, int downloadIncrement);

    void deleteByGameId(String gameId);
}
