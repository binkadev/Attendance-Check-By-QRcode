ALTER TABLE class_groups
    ADD COLUMN course_code VARCHAR(50) NULL AFTER code,
    ADD COLUMN class_code VARCHAR(50) NULL AFTER course_code,
    ADD COLUMN academic_year VARCHAR(30) NULL AFTER semester,
    ADD COLUMN campus VARCHAR(120) NULL AFTER room,
    ADD COLUMN thumbnail_url VARCHAR(500) NULL AFTER description;

CREATE INDEX idx_class_groups_academic_year_semester
    ON class_groups (academic_year, semester);

CREATE INDEX idx_class_groups_course_code
    ON class_groups (course_code);

CREATE INDEX idx_class_groups_class_code
    ON class_groups (class_code);