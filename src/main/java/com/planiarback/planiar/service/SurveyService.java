package com.planiarback.planiar.service;

import com.planiarback.planiar.model.Survey;
import com.planiarback.planiar.model.User;
import com.planiarback.planiar.repository.SurveyRepository;
import com.planiarback.planiar.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    public SurveyService(SurveyRepository surveyRepository, UserRepository userRepository) {
        this.surveyRepository = surveyRepository;
        this.userRepository = userRepository;
    }

    public Survey createSurvey(Survey survey, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con id: " + userId));
        survey.setUser(user);
        // ensure date present
        if (survey.getDate() == null) survey.setDate(LocalDate.now());
        return surveyRepository.save(survey);
    }

    @Transactional(readOnly = true)
    public List<Survey> getAllSurveys() {
        return surveyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Survey> getAllSurveysByUser(Long userId) {
        return surveyRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Survey> getSurveyById(Long id) {
        return surveyRepository.findById(id);
    }

    public void deleteSurvey(Long id) {
        if (!surveyRepository.existsById(id)) {
            throw new IllegalArgumentException("Survey not found with id: " + id);
        }
        surveyRepository.deleteById(id);
    }
}
