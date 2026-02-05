package com.project.repository;

import com.project.model.LoadTestFailLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface LoadTestFailLogRepository {

    void insertFailLog(LoadTestFailLog failLog);
    List<LoadTestFailLog> selectFailLogByResultId(Long resultId);
    void deleteFailLogByResultId(Long resultId);

}
