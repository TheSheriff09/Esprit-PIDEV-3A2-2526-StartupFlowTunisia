-- Database migration script for StartupFlow enhancements
-- Run this script to add new columns for AI evaluation features

USE startupflow; -- Replace with your actual database name

-- Add AI evaluation columns to fundingapplication table
ALTER TABLE fundingapplication
ADD COLUMN aiScore INT DEFAULT 0,
ADD COLUMN aiDecision VARCHAR(50) DEFAULT NULL,
ADD COLUMN aiComment TEXT DEFAULT NULL;

-- Create index for better performance on status queries
CREATE INDEX idx_fundingapplication_status ON fundingapplication(status);
CREATE INDEX idx_fundingapplication_submission_date ON fundingapplication(submissionDate);

-- Optional: Insert some sample data with AI evaluations for testing
-- Uncomment and modify as needed
/*
INSERT INTO fundingapplication (entrepreneurId, amount, status, submissionDate, applicationReason, projectId, paymentSchedule, attachment, aiScore, aiDecision, aiComment)
VALUES
(1, 50000.00, 'APPROVED', '2024-01-15', 'Tech startup for AI solutions', 1, 'Monthly', '/path/to/sample1.pdf', 85, 'APPROVE', 'Strong business model with clear market potential and solid financial projections.'),
(2, 75000.00, 'REVIEW', '2024-02-20', 'Sustainable energy project', 2, 'Quarterly', '/path/to/sample2.pdf', 65, 'REVIEW', 'Good environmental impact but needs more detailed financial analysis.'),
(3, 30000.00, 'REJECTED', '2024-03-10', 'Mobile app development', 3, 'One-time', '/path/to/sample3.pdf', 35, 'REJECT', 'Unclear business model and insufficient market research.');
*/