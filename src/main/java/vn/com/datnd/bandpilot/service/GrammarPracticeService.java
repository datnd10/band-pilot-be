package vn.com.datnd.bandpilot.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.com.datnd.bandpilot.dto.EssayHistoryDetailResponse;
import vn.com.datnd.bandpilot.dto.EssayHistoryItemResponse;
import vn.com.datnd.bandpilot.dto.EssayScoreResponse;
import vn.com.datnd.bandpilot.dto.StructuredFeedbackResponse;
import vn.com.datnd.bandpilot.entity.EssaySubmission;
import vn.com.datnd.bandpilot.exception.GeminiQuotaException;
import vn.com.datnd.bandpilot.exception.GeminiUnavailableException;
import vn.com.datnd.bandpilot.exception.ResourceNotFoundException;
import vn.com.datnd.bandpilot.repository.EssaySubmissionRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GrammarPracticeService {

    private static final Logger log = LoggerFactory.getLogger(GrammarPracticeService.class);

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=%s";

    @Value("${gemini.api.key}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final EssaySubmissionRepository essaySubmissionRepository;

    public GrammarPracticeService(ObjectMapper objectMapper,
                                  EssaySubmissionRepository essaySubmissionRepository) {
        this.objectMapper = objectMapper;
        this.essaySubmissionRepository = essaySubmissionRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    public String generatePrompt(String structureId, String structureTitle) {
        String body = buildPromptGenerationBody(structureTitle);
        return callGemini(body);
    }

    public StructuredFeedbackResponse evaluateResponse(
            String structureId,
            String structureTitle,
            String prompt,
            String userResponse) {
        String body = buildEvaluationBody(structureTitle, prompt, userResponse);
        String raw = callGemini(body);
        return parseStructuredFeedback(raw);
    }

    public EssayScoreResponse scoreEssay(String question, String essay) {
        String body = buildEssayScoringBody(question, essay);
        String raw = callGemini(body);
        EssayScoreResponse result = parseEssayScoreResponse(raw);

        // Persist the submission for history
        try {
            UUID userId = ReviewSessionService.resolveUserId();
            String strengthsJson = objectMapper.writeValueAsString(result.strengths());
            String improvementsJson = objectMapper.writeValueAsString(result.improvements());

            EssaySubmission submission = new EssaySubmission(
                    userId,
                    question,
                    essay,
                    result.taskAchievement(),
                    result.coherenceCohesion(),
                    result.lexicalResource(),
                    result.grammaticalRange(),
                    result.overallBand(),
                    strengthsJson,
                    improvementsJson,
                    result.improvedVersion(),
                    result.encouragement(),
                    Instant.now()
            );
            essaySubmissionRepository.save(submission);
        } catch (Exception e) {
            log.warn("Failed to save essay submission to history: {}", e.getMessage());
            // Non-critical — don't fail the scoring response
        }

        return result;
    }

    /**
     * Returns all essay submissions for the given user as summary items.
     */
    public List<EssayHistoryItemResponse> getEssayHistory(UUID userId) {
        return essaySubmissionRepository.findByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(this::toHistoryItem)
                .toList();
    }

    /**
     * Returns the full detail of a single essay submission, verifying user ownership.
     */
    public EssayHistoryDetailResponse getEssayDetail(UUID id, UUID userId) {
        EssaySubmission submission = essaySubmissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EssaySubmission", id));

        if (!submission.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("EssaySubmission", id);
        }

        List<String> strengths = parseJsonArray(submission.getStrengthsJson());
        List<String> improvements = parseJsonArray(submission.getImprovementsJson());

        return new EssayHistoryDetailResponse(
                submission.getId(),
                submission.getQuestion(),
                submission.getOverallBand(),
                submission.getTaskAchievement(),
                submission.getCoherenceCohesion(),
                submission.getLexicalResource(),
                submission.getGrammaticalRange(),
                submission.getSubmittedAt().toString(),
                submission.getEssay(),
                strengths,
                improvements,
                submission.getImprovedVersion(),
                submission.getEncouragement()
        );
    }

    private EssayHistoryItemResponse toHistoryItem(EssaySubmission s) {
        String question = s.getQuestion();
        if (question != null && question.length() > 120) {
            question = question.substring(0, 120) + "...";
        }
        return new EssayHistoryItemResponse(
                s.getId(),
                question,
                s.getOverallBand(),
                s.getTaskAchievement(),
                s.getCoherenceCohesion(),
                s.getLexicalResource(),
                s.getGrammaticalRange(),
                s.getSubmittedAt().toString()
        );
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> result = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    result.add(item.asText());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ─── Gemini HTTP call ──────────────────────────────────────────────────────

    String callGemini(String requestBody) {
        // Retry up to 3 attempts on 503 (high demand) with exponential backoff
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String result = callGeminiOnce(requestBody);
                return result;
            } catch (GeminiQuotaException e) {
                if (attempt < maxAttempts) {
                    log.warn("Gemini 503 high demand on attempt {}/{}, retrying in {}s...",
                            attempt, maxAttempts, attempt * 2);
                    try { Thread.sleep(attempt * 2000L); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new GeminiUnavailableException("AI service unavailable. Please try again.");
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new GeminiUnavailableException("AI service unavailable. Please try again.");
    }

    private String callGeminiOnce(String requestBody) {
        String url = String.format(GEMINI_URL_TEMPLATE, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("Gemini API call timed out", e);
            throw new GeminiUnavailableException("AI service unavailable. Please try again.");
        } catch (Exception e) {
            log.warn("Gemini API network error: {}", e.getMessage(), e);
            throw new GeminiUnavailableException("AI service unavailable. Please try again.");
        }

        int status = response.statusCode();

        if (status == 401 || status == 429 || status == 503) {
            log.warn("Gemini API returned {} (quota/auth/overload issue)", status);
            throw new GeminiQuotaException(
                    "AI service temporarily unavailable. Please try again.");
        }

        if (status < 200 || status >= 300) {
            log.warn("Gemini API returned unexpected status {}: {}", status, response.body());
            throw new GeminiUnavailableException("AI service unavailable. Please try again.");
        }

        String body = response.body();
        if (body == null || body.isBlank()) {
            log.warn("Gemini API returned empty response body");
            throw new GeminiUnavailableException("AI service unavailable. Please try again.");
        }

        return extractTextFromGeminiResponse(body);
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode text = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (text.isMissingNode() || text.isNull()) {
                log.warn("Gemini response missing candidates[0].content.parts[0].text: {}", responseBody);
                throw new GeminiUnavailableException("AI service unavailable. Please try again.");
            }

            String value = text.asText();
            if (value.isBlank()) {
                log.warn("Gemini returned blank text value");
                throw new GeminiUnavailableException("AI service unavailable. Please try again.");
            }

            return value;
        } catch (GeminiUnavailableException | GeminiQuotaException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response: {}", e.getMessage(), e);
            throw new GeminiUnavailableException("AI service unavailable. Please try again.");
        }
    }

    // ─── Prompt builders ───────────────────────────────────────────────────────

    String buildPromptGenerationBody(String structureTitle) {
        String systemInstruction =
                "You are an expert IELTS Writing Task 2 coach. Your role is to generate clear, " +
                "realistic IELTS-style writing prompts that encourage students to practise specific " +
                "grammar structures. Always produce academic English appropriate for IELTS Band 6\u20137.";

        String userPrompt =
                "Generate an IELTS Writing Task 2 practice prompt for a student to practise the following grammar structure:\n\n" +
                "Grammar structure: " + structureTitle + "\n\n" +
                "Your response must contain exactly two lines:\n" +
                "Line 1 \u2014 Topic sentence: An IELTS Writing Task 2\u2013style discussion topic (one sentence, 15\u201325 words).\n" +
                "Line 2 \u2014 Instruction: Tell the student to write 1\u20133 sentences on this topic using the \"" + structureTitle + "\" grammar structure.\n\n" +
                "Respond with only these two lines. No extra commentary, no numbering, no blank lines between them.";

        return buildGeminiRequestBody(systemInstruction, userPrompt);
    }

    String buildEvaluationBody(String structureTitle, String prompt, String userResponse) {
        String systemInstruction =
                "You are an expert IELTS Writing Task 2 examiner and grammar coach. Your role is to " +
                "evaluate a student's written response against a specific grammar structure and provide " +
                "structured, constructive feedback. Always respond with only valid JSON \u2014 no prose, " +
                "no markdown, no explanation outside the JSON object.";

        String userPrompt =
                "Evaluate the following student response for an IELTS Writing Task 2 practice exercise.\n\n" +
                "Target grammar structure: " + structureTitle + "\n\n" +
                "Writing prompt given to student:\n" + prompt + "\n\n" +
                "Student's response:\n" + userResponse + "\n\n" +
                "Return a JSON object with exactly these fields:\n" +
                "{\n" +
                "  \"structure_used\": <boolean \u2014 true if the student correctly used the target grammar structure>,\n" +
                "  \"errors\": <array of strings \u2014 list each grammar or usage error found; empty array if none>,\n" +
                "  \"suggestions\": <array of strings \u2014 list specific improvement suggestions; empty array if none>,\n" +
                "  \"model_sentence\": <string \u2014 one ideal example sentence using the target grammar structure in the context of the prompt>,\n" +
                "  \"score\": <integer 1\u20135 \u2014 overall quality score: 1=very poor, 3=acceptable, 5=excellent>,\n" +
                "  \"encouragement\": <string \u2014 one encouraging sentence personalised to the student's performance>\n" +
                "}\n\n" +
                "Respond with only the JSON object. Do not include any text before or after the JSON.";

        return buildGeminiRequestBody(systemInstruction, userPrompt);
    }

    private String buildGeminiRequestBody(String systemInstruction, String userPrompt) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            ObjectNode sysInstr = objectMapper.createObjectNode();
            ArrayNode sysParts = objectMapper.createArrayNode();
            ObjectNode sysPart = objectMapper.createObjectNode();
            sysPart.put("text", systemInstruction);
            sysParts.add(sysPart);
            sysInstr.set("parts", sysParts);
            root.set("system_instruction", sysInstr);

            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();
            content.put("role", "user");
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", userPrompt);
            parts.add(part);
            content.set("parts", parts);
            contents.add(content);
            root.set("contents", contents);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("Failed to build Gemini request body", e);
            throw new GeminiUnavailableException("AI service unavailable. Please try again.");
        }
    }

    String buildEssayScoringBody(String question, String essay) {
        String systemInstruction =
                "You are a professional IELTS Writing Task 2 examiner with 20 years of experience. " +
                "Provide detailed, sentence-level feedback on the student's essay. " +
                "Always respond with ONLY valid JSON \u2014 no prose, no markdown outside the JSON.";

        String userPrompt =
                "Evaluate the following IELTS Writing Task 2 essay in detail.\n\n" +
                "Question: " + question + "\n\n" +
                "Essay:\n" + essay + "\n\n" +
                "Return a JSON object with EXACTLY these fields:\n" +
                "{\n" +
                "  \"task_achievement\": <number 1-9, 0.5 increments>,\n" +
                "  \"coherence_cohesion\": <number 1-9, 0.5 increments>,\n" +
                "  \"lexical_resource\": <number 1-9, 0.5 increments>,\n" +
                "  \"grammatical_range\": <number 1-9, 0.5 increments>,\n" +
                "  \"overall_band\": <average of above 4, rounded to nearest 0.5>,\n" +
                "  \"strengths\": <array of 2-3 strings \u2014 cite SPECIFIC sentences or phrases from the essay that are well-written, explain WHY they are effective>,\n" +
                "  \"improvements\": <array of 3-5 objects, each with fields: 'original' (the problematic sentence or phrase quoted from the essay), 'issue' (what is wrong), 'correction' (the improved version), 'explanation' (why this is better)>,\n" +
                "  \"improved_version\": <string \u2014 rewrite the weakest body paragraph showing significant improvements in vocabulary, grammar, and argumentation>,\n" +
                "  \"encouragement\": <string \u2014 one personalised encouraging message referencing the student's current level>\n" +
                "}\n\n" +
                "IMPORTANT for 'improvements': Quote exact sentences/phrases from the essay, provide the corrected version, and explain each correction clearly.\n" +
                "Respond with only the JSON. No text before or after.";

        return buildGeminiRequestBody(systemInstruction, userPrompt);
    }

    public String generateEssayQuestion(String topic) {
        String systemInstruction =
                "You are an expert IELTS examiner. Generate authentic IELTS Writing Task 2 questions. " +
                "Questions must be clear, arguable, and appropriate for academic writing. " +
                "Respond with only the question text, no additional commentary.";

        String userPrompt =
                "Generate one IELTS Writing Task 2 question on the topic: " + topic + "\n\n" +
                "Requirements:\n" +
                "- 2-3 sentences total\n" +
                "- Must be a genuine IELTS-style question (Discuss both views / To what extent / Advantages & disadvantages / Causes & solutions)\n" +
                "- Relevant to the given topic\n" +
                "- Appropriate difficulty for IELTS Band 6-7 target\n\n" +
                "Respond with ONLY the question. No title, no explanation, no formatting.";

        String body = buildGeminiRequestBody(systemInstruction, userPrompt);
        return callGemini(body);
    }

    EssayScoreResponse parseEssayScoreResponse(String json) {
        try {
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                int lastFence = cleaned.lastIndexOf("```");
                if (firstNewline > 0 && lastFence > firstNewline) {
                    cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
                }
            }

            JsonNode node = objectMapper.readTree(cleaned);

            double taskAchievement = node.path("task_achievement").asDouble(5.0);
            double coherenceCohesion = node.path("coherence_cohesion").asDouble(5.0);
            double lexicalResource = node.path("lexical_resource").asDouble(5.0);
            double grammaticalRange = node.path("grammatical_range").asDouble(5.0);
            double overallBand = node.path("overall_band").asDouble(5.0);
            String improvedVersion = node.path("improved_version").asText("");
            String encouragement = node.path("encouragement").asText("");

            List<String> strengths = new ArrayList<>();
            JsonNode strengthsNode = node.path("strengths");
            if (strengthsNode.isArray()) {
                for (JsonNode s : strengthsNode) {
                    strengths.add(s.asText());
                }
            }

            List<String> improvements = new ArrayList<>();
            JsonNode improvementsNode = node.path("improvements");
            if (improvementsNode.isArray()) {
                for (JsonNode s : improvementsNode) {
                    improvements.add(s.asText());
                }
            }

            return new EssayScoreResponse(
                    taskAchievement,
                    coherenceCohesion,
                    lexicalResource,
                    grammaticalRange,
                    overallBand,
                    strengths,
                    improvements,
                    improvedVersion,
                    encouragement);
        } catch (Exception e) {
            log.warn("Failed to parse essay score JSON: {}", e.getMessage());
            throw new GeminiUnavailableException("AI service unavailable. Please try again.");
        }
    }

    // ─── JSON parsing ──────────────────────────────────────────────────────────

    public StructuredFeedbackResponse parseStructuredFeedback(String json) {
        try {
            // Strip markdown code fences if Gemini wraps JSON in ```json ... ```
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                int lastFence = cleaned.lastIndexOf("```");
                if (firstNewline > 0 && lastFence > firstNewline) {
                    cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
                }
            }

            JsonNode node = objectMapper.readTree(cleaned);

            boolean structureUsed = node.path("structure_used").asBoolean(false);
            int score = node.path("score").asInt(1);
            String modelSentence = node.path("model_sentence").asText("");
            String encouragement = node.path("encouragement").asText("");

            List<String> errors = new ArrayList<>();
            JsonNode errorsNode = node.path("errors");
            if (errorsNode.isArray()) {
                for (JsonNode e : errorsNode) {
                    errors.add(e.asText());
                }
            }

            List<String> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = node.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode s : suggestionsNode) {
                    suggestions.add(s.asText());
                }
            }

            return new StructuredFeedbackResponse(
                    structureUsed, errors, suggestions, modelSentence, score, encouragement);
        } catch (Exception e) {
            log.warn("Failed to parse structured feedback JSON: {}", e.getMessage());
            throw new GeminiUnavailableException("AI service unavailable. Please try again.");
        }
    }
}
