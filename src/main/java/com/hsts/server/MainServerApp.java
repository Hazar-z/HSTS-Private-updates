package com.hsts.server;

import com.hsts.server.network.HSTSServer;
import com.hsts.server.network.ServerRequestRouter;
import com.hsts.shared.net.Command;
import com.hsts.shared.net.Response;
import com.hsts.shared.net.dto.LoginData;
import com.hsts.shared.net.dto.SearchQuestionsData;
import server.controllers.LoginServerController;
import server.controllers.QuestionServerController;
import server.db.DatabaseManager;

/**
 * Production Central Entry Point for the HSTS Backend.
 * Dynamically queries MySQL for courses and maps client search operations over OCSF.
 */
public class MainServerApp {

    private static final int PORT = 3000; // Shared socket network port

    public static void main(String[] args) {
        System.out.println("=== INITIALIZING PRODUCTION HSTS CENTRAL SERVER ===");

        // 1. Initialize Partner 1's local MySQL connection pipeline
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.connect()) {
            System.err.println(">>> CRITICAL ERROR: Unable to connect to MySQL database. Aborting server startup.");
            return;
        }
        System.out.println("[DATABASE] Hard drive connection established successfully.");

        // 2. Instantiate Partner 1's backend logic controllers
        LoginServerController loginServerController = new LoginServerController();
        QuestionServerController questionServerController = new QuestionServerController();

        // 3. Setup Partner 2's functional Request Router framework
        ServerRequestRouter router = new ServerRequestRouter();

        // =========================================================================
        // ROUTE A: LOGIN (Fully Dynamic Database Query)
        // =========================================================================
        router.registerHandler(Command.LOGIN, request -> {
            LoginData data = (LoginData) request.getPayload();

            // Invoke YOUR real credentials verification query method!
            boolean isAuthenticated = loginServerController.login(data.getUsername(), data.getPassword());

            if (!isAuthenticated) {
                return Response.failure(Command.LOGIN, "Invalid username or password", request.getRequestId());
            }

            // Create a structured Teacher entity object for the GUI mapping
            com.hsts.shared.model.Teacher teacherProfile = new com.hsts.shared.model.Teacher();
            teacherProfile.setId(data.getUsername());
            teacherProfile.setFirstName("Authenticated");
            teacherProfile.setLastName("Teacher");
            teacherProfile.setRole("TEACHER");

            // DYNAMIC SQL STREAM: Pull the courses directly from your live MySQL tables!
            java.util.List<com.hsts.shared.model.Course> realDBCourses = new java.util.ArrayList<>();

            try {
                java.sql.Connection conn = DatabaseManager.getInstance().getConnection();
                String sql = "SELECT course_id, name FROM courses";

                try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
                     java.sql.ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        String id = rs.getString("course_id");
                        String name = rs.getString("name");
                        realDBCourses.add(new com.hsts.shared.model.Course(id, name));
                    }
                }
            } catch (java.sql.SQLException e) {
                System.err.println("[SERVER-ERROR] Failed to fetch real courses from SQL:");
                e.printStackTrace();
            }

            // Attach the real database row entries directly to the login payload
            teacherProfile.setCourses(realDBCourses);

            return Response.success(Command.LOGIN, teacherProfile, "Database authentication success.", request.getRequestId());
        });

        // =========================================================================
        // ROUTE B: SEARCH QUESTIONS (Dual-Filter Data-Type Standardized Mapping)
        // =========================================================================
        router.registerHandler(Command.SEARCH_QUESTIONS, request -> {
            SearchQuestionsData data = (SearchQuestionsData) request.getPayload();

            // 1. Determine the active course filter context (defaulting to "11" as a safe fallback)
            String courseFilter = data.getCourseId() != null ? data.getCourseId() : "11";

            // 2. Invoke YOUR updated dynamic database dual-filter query!
            java.util.List<shared.entities.Question> rawDbResults = questionServerController.searchQuestions(data.getTopic(), courseFilter);

            // 3. Convert and map your internal database items into the format the GUI expects
            java.util.List<com.hsts.shared.model.Question> formattedGuiResults = new java.util.ArrayList<>();

            if (rawDbResults != null) {
                for (shared.entities.Question dbQ : rawDbResults) {
                    com.hsts.shared.model.Question guiQ = new com.hsts.shared.model.Question();

                    guiQ.setQuestionId(dbQ.getQuestionId());
                    guiQ.setText(dbQ.getText());
                    guiQ.setTopic(dbQ.getTopic());
                    guiQ.setDifficulty(com.hsts.shared.model.Difficulty.MEDIUM);
                    guiQ.setCourseId(courseFilter);

                    // Map an empty answer layout array temporarily to keep UI binding functional
                    guiQ.setAnswers(new java.util.ArrayList<>());

                    formattedGuiResults.add(guiQ);
                }
            }

            return Response.success(Command.SEARCH_QUESTIONS, formattedGuiResults, "Database query loaded successfully.", request.getRequestId());
        });
        // =========================================================================
        // ROUTE C: CREATE QUESTION (Saves a brand new question row to MySQL)
        // =========================================================================
        router.registerHandler(Command.CREATE_QUESTION, request -> {
            // 1. Unpack the CreateQuestionData directly
            com.hsts.shared.net.dto.CreateQuestionData wrapperDto = (com.hsts.shared.net.dto.CreateQuestionData) request.getPayload();

            // 2. Extract values directly from the wrapperDto since it contains the getters!
            String text = wrapperDto.getText();
            String difficulty = wrapperDto.getDifficulty() != null ? wrapperDto.getDifficulty().toString() : "MEDIUM";
            String instructions = wrapperDto.getInstructions() != null ? wrapperDto.getInstructions() : "";
            String topic = wrapperDto.getTopic();
            String courseId = wrapperDto.getCourseId() != null ? wrapperDto.getCourseId() : "11";

            // 3. Invoke YOUR controller's exact parameter signature
            boolean isSaved = questionServerController.createQuestion(text, difficulty, instructions, topic, courseId);

            if (!isSaved) {
                return Response.failure(Command.CREATE_QUESTION, "Database insertion rejected the record.", request.getRequestId());
            }

            // Return a success message alongside the dto payload
            return Response.success(Command.CREATE_QUESTION, wrapperDto, "Question saved to MySQL database successfully!", request.getRequestId());
        });

        // =========================================================================
        // ROUTE D: EDIT QUESTION (Updates an existing matching question record in MySQL)
        // =========================================================================
        router.registerHandler(Command.EDIT_QUESTION, request -> {
            // 1. Unpack the EditQuestionData DTO wrapper packet sent by the network layer
            com.hsts.shared.net.dto.EditQuestionData wrapperDto = (com.hsts.shared.net.dto.EditQuestionData) request.getPayload();

            // 2. Extract the updated values directly using its internal getters
            // Note: If your team's DTO uses different method names, use IntelliJ's Ctrl+Space trick here!
            String questionId = wrapperDto.getQuestionId();
            String newText = wrapperDto.getText();
            String newInstruction = wrapperDto.getInstructions() != null ? wrapperDto.getInstructions() : "";

            // 3. Invoke YOUR controller's exact update parameters!
            boolean isUpdated = questionServerController.editQuestion(questionId, newText, newInstruction);

            if (!isUpdated) {
                return Response.failure(Command.EDIT_QUESTION, "Database update rejected the change.", request.getRequestId());
            }

            return Response.success(Command.EDIT_QUESTION, wrapperDto, "Question updated in MySQL successfully!", request.getRequestId());
        });

        // 4. Initialize the OCSF Server Socket Wrapper and launch the active listener thread
        HSTSServer server = new HSTSServer(PORT);
        server.setRouter(router); // Inject the complete router switchboard into OCSF

        try {
            server.startServer();
            System.out.println(">>> PRODUCTION SYSTEM ENGINE LISTENING LIVE ON SOCKET PORT " + PORT + " <<<");
        } catch (Exception e) {
            System.err.println(">>> FATAL: OCSF initialization frame collapsed. <<<");
            e.printStackTrace();
        }
    }
}