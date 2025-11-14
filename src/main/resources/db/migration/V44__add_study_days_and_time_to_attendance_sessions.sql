-- Add study_days and study_time columns to attendance_sessions table
-- to preserve original schedule information at the time of attendance

ALTER TABLE attendance_sessions
ADD COLUMN study_days VARCHAR(50) NULL COMMENT 'Study days at time of attendance (e.g., MONDAY,WEDNESDAY,FRIDAY)',
ADD COLUMN study_time VARCHAR(20) NULL COMMENT 'Study time at time of attendance (e.g., MORNING, AFTERNOON, EVENING)';

-- Migrate existing data: copy study_days and study_time from classes table
UPDATE attendance_sessions ats
INNER JOIN classes c ON ats.class_id = c.class_id
SET ats.study_days = c.study_days,
    ats.study_time = c.study_time
WHERE ats.study_days IS NULL OR ats.study_time IS NULL;

-- Add comment to explain the purpose
ALTER TABLE attendance_sessions 
MODIFY COLUMN study_days VARCHAR(50) NULL 
COMMENT 'Original study days when attendance was taken - preserved even if class schedule changes';

ALTER TABLE attendance_sessions 
MODIFY COLUMN study_time VARCHAR(20) NULL 
COMMENT 'Original study time when attendance was taken - preserved even if class schedule changes';
