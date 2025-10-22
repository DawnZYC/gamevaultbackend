package com.sg.nusiss.gamevaultbackend.repositoryImpl.developer;

import com.sg.nusiss.gamevaultbackend.entity.developer.DevGameStatistics;
import com.sg.nusiss.gamevaultbackend.mapper.developer.DevGameStatisticsMapper;
import com.sg.nusiss.gamevaultbackend.repository.developer.DevGameStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DevGameStatisticsRepositoryImpl implements DevGameStatisticsRepository {

    private final DevGameStatisticsMapper devGameStatisticsMapper;

    @Override
    public Optional<DevGameStatistics> findByGameId(String gameId) {
        return devGameStatisticsMapper.findByGameId(gameId);
    }

    @Override
    public void insert(DevGameStatistics stats) {
        devGameStatisticsMapper.insert(stats);
    }

    @Override
    public void updateCounts(String gameId, int viewIncrement, int downloadIncrement) {
        devGameStatisticsMapper.updateCounts(gameId, viewIncrement, downloadIncrement);
    }

    @Override
    public void deleteByGameId(String gameId) {
        devGameStatisticsMapper.deleteByGameId(gameId);
    }


}