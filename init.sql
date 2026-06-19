-- =========================================================================
-- HSTS SYSTEM DATA ARCHITECTURE BLUEPRINT (init.sql)
-- =========================================================================
CREATE DATABASE IF NOT EXISTS hsts_database;
USE hsts_database;

-- Drop existing tables sequentially to respect Foreign Key integrity constraints
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS question_answers;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS courses;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Create Courses Schema Table
CREATE TABLE courses (
                         course_id VARCHAR(10) PRIMARY KEY,
                         name VARCHAR(100) NOT NULL
);

-- 2. Create Questions Schema Table
CREATE TABLE questions (
                           question_id VARCHAR(10) PRIMARY KEY,
                           text TEXT NOT NULL,
                           difficulty VARCHAR(20) NOT NULL,
                           instructions TEXT,
                           topic VARCHAR(100) NOT NULL,
                           course_id VARCHAR(10),
                           FOREIGN KEY (course_id) REFERENCES courses(course_id)
);

-- 3. Create Question Answers Schema Table (Cascades on Deletion)
CREATE TABLE question_answers (
                                  id INT AUTO_INCREMENT PRIMARY KEY,
                                  question_id VARCHAR(10),
                                  answer_text TEXT NOT NULL,
                                  is_correct TINYINT(1) DEFAULT 0,
                                  FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
);

-- 4. Seed baseline infrastructure rows
INSERT INTO courses (course_id, name) VALUES
                                          ('11', 'Introduction to Computer Science'),
                                          ('22', 'Mathematical Logic'),
                                          ('33', 'Database Systems');

INSERT INTO questions (question_id, text, difficulty, instructions, topic, course_id) VALUES
                                                                                          ('11001', 'What is the time complexity of searching in a perfectly balanced Binary Search Tree (BST)?', 'MEDIUM', 'Choose the single most accurate asymptotic upper bound.', 'Data Structures', '11'),
                                                                                          ('11002', 'Which of the following data structures operates strictly on a Last-In, First-Out (LIFO) basis?', 'EASY', 'Select the correct foundational abstract data type.', 'Data Structures', '11');

INSERT INTO question_answers (question_id, answer_text, is_correct) VALUES
                                                                        ('11001', 'O(1)', 0), ('11001', 'O(log n)', 1), ('11001', 'O(n)', 0), ('11001', 'O(n log n)', 0),
                                                                        ('11002', 'Queue', 0), ('11002', 'Stack', 1), ('11002', 'Singly Linked List', 0), ('11002', 'Binary Tree', 0);