package server.controllers;

import server.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Controller handling creation, modification, deletion, and search filtering of exam questions.
 */
public class QuestionServerController {

    private final DatabaseManager dbManager;

    public QuestionServerController() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Inserts a new question into the database using a pre-compiled template script.
     */
    public boolean createQuestion(String text, String difficulty, String instructions, String topic, String courseId) {
        // FIXED: Passed courseId into the method argument slot
        String questionId = generateQuestionId(courseId);
        if (questionId == null) return false;

        String sql = "INSERT INTO questions (question_id, text, difficulty, instructions, topic, course_id) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = dbManager.getConnection();

        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, questionId);
            stmt.setString(2, text);
            stmt.setString(3, difficulty);
            stmt.setString(4, instructions);
            stmt.setString(5, topic);
            stmt.setString(6, courseId);

            int rowsAffected = stmt.executeUpdate(); //
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (e instanceof java.sql.SQLIntegrityConstraintViolationException) {
                System.out.println("[DB WARNING] Duplicate question text detected. Insertion blocked safely.");
            } else {
                System.err.println("[DB ERROR] Failed to create new question.");
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Modifies the text and instructions of an existing question row on the hard drive.
     */
    public boolean editQuestion(String questionId, String newText, String newInstruction) {
        // FIXED: Changed 'instruction' to 'instructions' to match your MySQL table schema rules
        String sql = "UPDATE questions SET text = ?, instructions = ? WHERE question_id = ?";
        Connection conn = dbManager.getConnection();

        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newText);
            stmt.setString(2, newInstruction);
            stmt.setString(3, questionId);

            int rowsAffected = stmt.executeUpdate(); //
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to update question: " + questionId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Permanently deletes a specific question row from the database using its ID.
     */
    public boolean deleteQuestion(String questionId) {
        String sql = "DELETE FROM questions WHERE question_id = ?";
        Connection conn = dbManager.getConnection();

        if (conn == null) return false;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, questionId);

            int rowsAffected = stmt.executeUpdate(); //
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to drop question: " + questionId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Searches for questions filtering by BOTH course ID and a specific topic keyword.
     * Converts raw database rows into Java Question objects dynamically.
     */
    public java.util.List<shared.entities.Question> searchQuestions(String topicKeyword, String courseId) {
        java.util.List<shared.entities.Question> resultsList = new java.util.ArrayList<>();

        // Robust dual-filter query: filters by course_id and matches the topic securely
        String sql = "SELECT question_id, text, instructions, difficulty, topic FROM questions WHERE course_id = ? AND topic LIKE ?";
        Connection conn = dbManager.getConnection();

        if (conn == null) return resultsList;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);
            // The % wildcards mean "find any topic that contains this word anywhere inside it"
            stmt.setString(2, "%" + (topicKeyword != null ? topicKeyword : "") + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("question_id");
                    String text = rs.getString("text");
                    String instructions = rs.getString("instructions");
                    String difficulty = rs.getString("difficulty");
                    String topic = rs.getString("topic");

                    shared.entities.Question questionObj = new shared.entities.Question(id, text, instructions, difficulty, topic);
                    resultsList.add(questionObj);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed fetching search query results for course: " + courseId + " and topic: " + topicKeyword);
            e.printStackTrace();
        }

        return resultsList;
    }

    /**
     * Automatically generates a structural 5-digit ID matching Section 3.1 specifications.
     */
    private String generateQuestionId(String courseId) {
        int parsedCourseNum = Integer.parseInt(courseId);
        String coursePrefix = String.format("%02d", parsedCourseNum);

        String sql = "SELECT COUNT(*) FROM questions WHERE course_id = ?";
        Connection conn = dbManager.getConnection();
        if (conn == null) return null;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseId);

            try (ResultSet rs = stmt.executeQuery()) { //
                if (rs.next()) {
                    int existingCount = rs.getInt(1);
                    int nextSerialNumber = existingCount + 1;

                    String counterSuffix = String.format("%03d", nextSerialNumber);
                    return coursePrefix + counterSuffix;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ID GEN ERROR] Failed calculating sequence metadata count.");
            e.printStackTrace();
        }
        return null;
    }
}