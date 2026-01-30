package com.project.repository;

import com.project.model.LoadTestFailLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LoadTestFailLogRepository {

    void insertFailLog(LoadTestFailLog failLog);
    List<LoadTestFailLog> selectFailLogByResultId(Long resultId);
    void deleteFailLogByResultId(Long resultId);

}
