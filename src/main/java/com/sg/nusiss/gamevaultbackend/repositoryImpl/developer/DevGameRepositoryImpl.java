package com.sg.nusiss.gamevaultbackend.repositoryImpl.developer;

import com.sg.nusiss.gamevaultbackend.entity.developer.DevGame;
import com.sg.nusiss.gamevaultbackend.mapper.developer.DevGameMapper;
import com.sg.nusiss.gamevaultbackend.repository.developer.DevGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DevGameRepositoryImpl implements DevGameRepository {
    private final DevGameMapper devGameMapper;

    @Override
    public Optional<DevGame> findById(String id) {
        return Optional.ofNullable(devGameMapper.selectById(id));
    }

    @Override
    public List<DevGame> findByDeveloperProfileId(String developerProfileId) {
        return devGameMapper.selectByDeveloperProfileId(developerProfileId);
    }

    @Override
    public boolean deleteById(String id) {
        int affectedRows = devGameMapper.deleteById(id);
        return affectedRows > 0;
    }

    @Override
    public void insert(DevGame game) {
        devGameMapper.insert(game);
    }

    @Override
    public void update(DevGame game) {
        devGameMapper.update(game);
    }

    @Override
    public boolean existsById(String gameId) {
        Integer count = devGameMapper.countById(gameId);
        return count != null && count > 0;
    }

    @Override
    public List<DevGame> findAllPaged(int offset, int pageSize) {
        return devGameMapper.findAllPaged(offset, pageSize);
    }

    @Override
    public long countAll() {
        return devGameMapper.countAll();
    }


}
