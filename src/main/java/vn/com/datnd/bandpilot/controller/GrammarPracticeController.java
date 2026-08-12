package vn.com.datnd.bandpilot.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.com.datnd.bandpilot.dto.EssayHistoryDetailResponse;
import vn.com.datnd.bandpilot.dto.EssayHistoryItemResponse;
import vn.com.datnd.bandpilot.dto.EssayScoreRequest;
import vn.com.datnd.bandpilot.dto.EssayScoreResponse;
import vn.com.datnd.bandpilot.dto.EvaluateRequest;
import vn.com.datnd.bandpilot.dto.PromptRequest;
import vn.com.datnd.bandpilot.dto.PromptResponse;
import vn.com.datnd.bandpilot.dto.StructuredFeedbackResponse;
import vn.com.datnd.bandpilot.service.GrammarPracticeService;
import vn.com.datnd.bandpilot.service.ReviewSessionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/grammar")
public class GrammarPracticeController {

    private final GrammarPracticeService grammarPracticeService;

    public GrammarPracticeController(GrammarPracticeService grammarPracticeService) {
        this.grammarPracticeService = grammarPracticeService;
    }

    /**
     * Generate an IELTS writing prompt for the given grammar structure.
     *
     * @param request contains structureId and structureTitle
     * @return prompt text
     */
    @PostMapping("/prompt")
    public ResponseEntity<PromptResponse> generatePrompt(@Valid @RequestBody PromptRequest request) {
        String prompt = grammarPracticeService.generatePrompt(
                request.structureId(), request.structureTitle());
        return ResponseEntity.ok(new PromptResponse(prompt));
    }

    /**
     * Evaluate a student's response to a grammar practice prompt.
     *
     * @param request contains structureId, structureTitle, prompt, and userResponse
     * @return structured feedback
     */
    @PostMapping("/evaluate")
    public ResponseEntity<StructuredFeedbackResponse> evaluateResponse(
            @Valid @RequestBody EvaluateRequest request) {
        StructuredFeedbackResponse feedback = grammarPracticeService.evaluateResponse(
                request.structureId(),
                request.structureTitle(),
                request.prompt(),
                request.userResponse());
        return ResponseEntity.ok(feedback);
    }

    /**
     * Score a full IELTS Writing Task 2 essay and save it to history.
     *
     * @param request contains the question and the student's essay
     * @return band scores and feedback
     */
    @PostMapping("/essay/score")
    public ResponseEntity<EssayScoreResponse> scoreEssay(
            @Valid @RequestBody EssayScoreRequest request) {
        EssayScoreResponse response = grammarPracticeService.scoreEssay(
                request.question(), request.essay());
        return ResponseEntity.ok(response);
    }

    /**
     * Generate an IELTS Writing Task 2 question for a given topic.
     *
     * @param body JSON body with "topic" field
     * @return generated question
     */
    @PostMapping("/essay/generate-question")
    public ResponseEntity<PromptResponse> generateEssayQuestion(
            @RequestBody java.util.Map<String, String> body) {
        String topic = body.getOrDefault("topic", "").trim();
        if (topic.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String question = grammarPracticeService.generateEssayQuestion(topic);
        return ResponseEntity.ok(new PromptResponse(question));
    }

    /**
     * Returns the authenticated user's essay submission history, newest first.
     */
    @GetMapping("/essay/history")
    public ResponseEntity<List<EssayHistoryItemResponse>> getHistory() {
        UUID userId = ReviewSessionService.resolveUserId();
        List<EssayHistoryItemResponse> history = grammarPracticeService.getEssayHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Returns the full detail of a single essay submission.
     *
     * @param id the UUID of the essay submission
     * @return full detail including essay text and feedback
     */
    @GetMapping("/essay/history/{id}")
    public ResponseEntity<EssayHistoryDetailResponse> getDetail(@PathVariable UUID id) {
        UUID userId = ReviewSessionService.resolveUserId();
        EssayHistoryDetailResponse detail = grammarPracticeService.getEssayDetail(id, userId);
        return ResponseEntity.ok(detail);
    }
}
