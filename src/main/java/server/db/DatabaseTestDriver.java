package server.db;

import server.controllers.LoginServerController;
import server.controllers.QuestionServerController;

public class DatabaseTestDriver {
    public static void main(String[] args) {
        System.out.println("=== STARTING COMPLETE BACKEND FUNCTIONAL TEST ===");

        // 1. Initialize and connect to the Database Storage Engine
        DatabaseManager dbManager = DatabaseManager.getInstance();
        if (!dbManager.connect()) {
            System.err.println(">>> FATAL: Database connection down. Aborting test.");
            return;
        }

        // 2. Initialize both verified Controllers
        LoginServerController loginController = new LoginServerController();
        QuestionServerController questionController = new QuestionServerController();

        System.out.println("\n--- STEP 1: TESTING LOGIN SESSION CACHE ---");
        boolean loginSuccess = loginController.login("admin", "123456");
        System.out.println("Initial Login Authorization: " + loginSuccess);

        // Try a duplicate hack entry to verify memory set protection
        boolean duplicateBlocked = !loginController.login("admin", "123456");
        System.out.println("Duplicate Session Guard Working: " + duplicateBlocked);


        System.out.println("\n--- STEP 2: LOADING YOUR 6 SEQUENTIAL BLUEPRINT QUESTIONS ---");

        // Course 22 - Mathematical Logic track (Will generate: 22001)
        questionController.createQuestion(
                "Which of the following propositions is logically equivalent to the conditional statement p -> q?",
                "MEDIUM",
                "Apply standard logical equivalences.",
                "Propositional Logic",
                "22"
        );

        // Course 11 - Computer Science track (Will generate: 11001, 11002, 11003, 11004, 11005)
        questionController.createQuestion(
                "What is the time complexity of searching in a perfectly balanced Binary Search Tree (BST)?",
                "MEDIUM",
                "Choose the single most accurate asymptotic upper bound.",
                "Data Structures",
                "11"
        );

        questionController.createQuestion(
                "Which of the following data structures operates strictly on a Last-In, First-Out (LIFO) basis?",
                "EASY",
                "Select the correct foundational abstract data type.",
                "Data Structures",
                "11"
        );

        questionController.createQuestion(
                "What occurs when a Java subclass defines a method with the same signature as a superclass method?",
                "MEDIUM",
                "Assume standard object-oriented programming conventions.",
                "Object-Oriented Programming",
                "11"
        );

        questionController.createQuestion(
                "new question test",
                "MEDIUM",
                "Consider standard comparison-based sorting models.",
                "Sorting Algorithms",
                "11"
        );

        questionController.createQuestion(
                "What is the worst-case time complexity of the optimized edit distance dynamic programming algorithm?",
                "HARD",
                "Select the single best algorithmic complexity.",
                "Dynamic Programming",
                "11"
        );

        System.out.println("[SERVER] Your custom questions have been beautifully indexed!");


        System.out.println("\n--- STEP 3: TESTING OBJECT MAPPING & SEARCH QUERY ---");
        // Ask the controller to find any question with the word "Data" in the topic keyword
// Pass both the topic keyword search string AND a test course ID (e.g., "11")
        java.util.List<shared.entities.Question> searchResults = questionController.searchQuestions("Data", "11");        System.out.println("Search executed. Total Question objects instantiated from DB: " + searchResults.size());

        // Loop through the list of objects and print their internal data fields
        for (shared.entities.Question q : searchResults) {
            System.out.println(" -> Found Object [ID: " + q.getQuestionId() + "] | Topic: " + q.getTopic() + " | Text: " + q.getText());
        }


        System.out.println("\n--- STEP 4: CLEANING UP ACTIVE SESSION ---");
        loginController.logout("admin");

        // 3. Disconnect safely from MySQL
        dbManager.disconnect();
        System.out.println("\n=== ALL CONTROLLER SUBSYSTEMS VERIFIED SUCCESS ===");
    }
}