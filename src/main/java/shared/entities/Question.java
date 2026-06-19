package shared.entities;

/**
 * Shared Entity representing an exam question (Shared Agreement).
 */
public class Question {
    // 1. Private variables to lock down the exact fields from your shared agreement
    private String questionId;
    private String text;
    private String instructions;
    private String difficulty;
    private String topic;

    // 2. The Constructor: Used to easily pack raw database cells into this Java object box
    public Question(String questionId, String text, String instructions, String difficulty, String topic) {
        this.questionId = questionId;
        this.text = text;
        this.instructions = instructions;
        this.difficulty = difficulty;
        this.topic = topic;
    }

    // 3. Getters and Setters: Required so Partner 2 can read the data to send it over the network,
    // and Partner 3 can display it on the JavaFX UI screens
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
}