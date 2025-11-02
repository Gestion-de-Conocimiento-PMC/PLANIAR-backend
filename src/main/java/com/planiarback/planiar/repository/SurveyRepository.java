package com.planiarback.planiar.repository;

import com.planiarback.planiar.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {
    List<Survey> findByUserId(Long userId);
    long countByUserId(Long userId);
}
