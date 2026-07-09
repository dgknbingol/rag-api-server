package com.aislam.rag.service;

import com.aislam.rag.dto.EducationQuizDetailResponse;
import com.aislam.rag.dto.EducationQuizOptionDto;
import com.aislam.rag.dto.EducationQuizQuestionDto;
import com.aislam.rag.exception.RagException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class EducationQuizLoader {

  private static final String QUIZ_PATH = "education/quizzes/%s.json";

  private final ObjectMapper objectMapper;

  public EducationQuizLoader(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public EducationQuizDetailResponse loadQuiz(String quizId) {
    String path = QUIZ_PATH.formatted(quizId);
    ClassPathResource resource = new ClassPathResource(path);
    if (!resource.exists()) {
      throw new RagException("Education quiz not found: " + quizId, "EDUCATION_QUIZ_NOT_FOUND");
    }

    try (InputStream input = resource.getInputStream()) {
      JsonNode root = objectMapper.readTree(input);
      if (root == null || !root.has("questions")) {
        throw new RagException("Education quiz is empty: " + quizId, "EDUCATION_QUIZ_ERROR");
      }

      List<EducationQuizQuestionDto> questions = new ArrayList<>();
      for (JsonNode questionNode : root.get("questions")) {
        List<EducationQuizOptionDto> options = new ArrayList<>();
        for (JsonNode optionNode : questionNode.get("options")) {
          options.add(
              new EducationQuizOptionDto(
                  optionNode.path("label").asText(),
                  optionNode.path("text").asText(),
                  optionNode.path("correct").asBoolean(false)));
        }
        questions.add(new EducationQuizQuestionDto(questionNode.path("question").asText(), options));
      }

      return new EducationQuizDetailResponse(
          root.path("id").asText(quizId),
          root.path("title").asText(),
          root.path("moduleId").asText(""),
          questions);
    } catch (IOException ex) {
      throw new RagException("Failed to load education quiz: " + quizId, "EDUCATION_QUIZ_ERROR", ex);
    }
  }
}
