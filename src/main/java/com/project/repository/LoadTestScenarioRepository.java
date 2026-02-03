package com.project.repository;

import com.project.model.LoadTestScenario;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface LoadTestScenarioRepository {


    void insertScenario(LoadTestScenario scenario);
    List<LoadTestScenario> selectScenarios(String name);
    LoadTestScenario selectScenarioById(Long id);
    void deleteScenarioById(Long id);

}
