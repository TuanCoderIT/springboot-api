-- Migration to fix ip_address column type
-- Run this SQL script in your PostgreSQL database

-- Change ip_address column from INET to VARCHAR(45)
ALTER TABLE exam_attempts ALTER COLUMN ip_address TYPE VARCHAR(45);

-- Verify the change
\d exam_attempts;