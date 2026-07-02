package com.aislam.rag.service;

import com.aislam.rag.repository.QuizQuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quizseed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class QuizSeedServiceTest {

    @Autowired
    QuizQuestionRepository quizQuestionRepository;

    @Test
    void seedsAbdestQuestionWithFourOptions() {
        assertThat(quizQuestionRepository.count()).isEqualTo(1);

        var question = quizQuestionRepository.findAll().getFirst();
        assertThat(question.getQuestionText()).contains("temizlenenleri sever");
        assertThat(question.getCategory()).isEqualTo("ibadet");
        assertThat(question.getOptions()).hasSize(4);

        long correctCount = question.getOptions().stream().filter(option -> option.isCorrect()).count();
        assertThat(correctCount).isEqualTo(1);

        var correct = question.getOptions().stream().filter(option -> option.isCorrect()).findFirst().orElseThrow();
        assertThat(correct.getOptionText()).isEqualTo("Abdest");
    }
}
