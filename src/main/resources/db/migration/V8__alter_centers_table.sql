ALTER TABLE centers 
ADD COLUMN email VARCHAR(255) NOT NULL,
ADD COLUMN phone VARCHAR(20) NOT NULL,
ADD COLUMN established_date DATE,
ADD COLUMN description TEXT,
ADD COLUMN deleted_at DATETIME,
ADD COLUMN created_by INT,
ADD COLUMN updated_by INT,
ADD COLUMN address_line VARCHAR(500) NOT NULL,
ADD COLUMN province VARCHAR(100) NOT NULL,
ADD COLUMN district VARCHAR(100) NOT NULL,
ADD COLUMN ward VARCHAR(100) NOT NULL;

ALTER TABLE centers 
ADD INDEX idx_centers_deleted_at (deleted_at),
ADD INDEX idx_centers_province (province),
ADD INDEX idx_centers_district (district);