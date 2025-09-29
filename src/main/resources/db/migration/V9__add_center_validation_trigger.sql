-- Thêm trigger để chặn insert user_roles nếu center bị vô hiệu hóa
DELIMITER $$

CREATE TRIGGER check_center_active_before_insert_user_roles
BEFORE INSERT ON user_roles
FOR EACH ROW
BEGIN
    DECLARE center_deleted_at DATETIME;
    
    -- Chỉ kiểm tra nếu có center_id
    IF NEW.center_id IS NOT NULL THEN
        -- Lấy deleted_at của center
        SELECT deleted_at INTO center_deleted_at 
        FROM centers 
        WHERE center_id = NEW.center_id;
        
        -- Nếu center đã bị vô hiệu hóa, từ chối insert
        IF center_deleted_at IS NOT NULL THEN
            SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = 'Không thể gán role cho trung tâm đã bị vô hiệu hóa';
        END IF;
    END IF;
END$$

DELIMITER ;