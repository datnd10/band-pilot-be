package vn.com.datnd.bandpilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a saved IELTS Writing Task 2 essay submission with AI scores.
 */
@Entity
@Table(name = "essay_submission")
public class EssaySubmission {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "essay", nullable = false, columnDefinition = "TEXT")
    private String essay;

    @Column(name = "task_achievement", nullable = false)
    private double taskAchievement;

    @Column(name = "coherence_cohesion", nullable = false)
    private double coherenceCohesion;

    @Column(name = "lexical_resource", nullable = false)
    private double lexicalResource;

    @Column(name = "grammatical_range", nullable = false)
    private double grammaticalRange;

    @Column(name = "overall_band", nullable = false)
    private double overallBand;

    /** Strengths stored as a JSON array string e.g. ["item1","item2"] */
    @Column(name = "strengths_json", columnDefinition = "TEXT")
    private String strengthsJson;

    /** Improvements stored as a JSON array string e.g. ["item1","item2"] */
    @Column(name = "improvements_json", columnDefinition = "TEXT")
    private String improvementsJson;

    @Column(name = "improved_version", columnDefinition = "TEXT")
    private String improvedVersion;

    @Column(name = "encouragement", columnDefinition = "TEXT")
    private String encouragement;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected EssaySubmission() {
    }

    public EssaySubmission(UUID userId, String question, String essay,
                           double taskAchievement, double coherenceCohesion,
                           double lexicalResource, double grammaticalRange,
                           double overallBand, String strengthsJson,
                           String improvementsJson, String improvedVersion,
                           String encouragement, Instant submittedAt) {
        this.userId = userId;
        this.question = question;
        this.essay = essay;
        this.taskAchievement = taskAchievement;
        this.coherenceCohesion = coherenceCohesion;
        this.lexicalResource = lexicalResource;
        this.grammaticalRange = grammaticalRange;
        this.overallBand = overallBand;
        this.strengthsJson = strengthsJson;
        this.improvementsJson = improvementsJson;
        this.improvedVersion = improvedVersion;
        this.encouragement = encouragement;
        this.submittedAt = submittedAt;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getEssay() { return essay; }
    public void setEssay(String essay) { this.essay = essay; }

    public double getTaskAchievement() { return taskAchievement; }
    public void setTaskAchievement(double taskAchievement) { this.taskAchievement = taskAchievement; }

    public double getCoherenceCohesion() { return coherenceCohesion; }
    public void setCoherenceCohesion(double coherenceCohesion) { this.coherenceCohesion = coherenceCohesion; }

    public double getLexicalResource() { return lexicalResource; }
    public void setLexicalResource(double lexicalResource) { this.lexicalResource = lexicalResource; }

    public double getGrammaticalRange() { return grammaticalRange; }
    public void setGrammaticalRange(double grammaticalRange) { this.grammaticalRange = grammaticalRange; }

    public double getOverallBand() { return overallBand; }
    public void setOverallBand(double overallBand) { this.overallBand = overallBand; }

    public String getStrengthsJson() { return strengthsJson; }
    public void setStrengthsJson(String strengthsJson) { this.strengthsJson = strengthsJson; }

    public String getImprovementsJson() { return improvementsJson; }
    public void setImprovementsJson(String improvementsJson) { this.improvementsJson = improvementsJson; }

    public String getImprovedVersion() { return improvedVersion; }
    public void setImprovedVersion(String improvedVersion) { this.improvedVersion = improvedVersion; }

    public String getEncouragement() { return encouragement; }
    public void setEncouragement(String encouragement) { this.encouragement = encouragement; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
