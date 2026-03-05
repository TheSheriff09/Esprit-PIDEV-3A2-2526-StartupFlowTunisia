CREATE TABLE IF NOT EXISTS mentor_favorites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    entrepreneurID INT NOT NULL,
    mentorID INT NOT NULL,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_fav (entrepreneurID, mentorID)
);

CREATE TABLE IF NOT EXISTS mentor_evaluations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    entrepreneurID INT NOT NULL,
    mentorID INT NOT NULL,
    sessionID INT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS session_todos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sessionID INT NOT NULL,
    taskDescription VARCHAR(255) NOT NULL,
    isDone BOOLEAN DEFAULT FALSE,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
