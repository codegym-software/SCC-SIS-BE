# SCC-SIS-BE

## 📌 Lưu ý

- Repo **không** chứa file `application.properties`, mỗi người cần tự tạo riêng.  
- Vị trí: `src/main/resources/application.properties`.  
- Có thể tham khảo mẫu trong `src/main/resources/application.example.properties`.  

---

## Cấu hình Spring Boot (mẫu)

```properties
spring.application.name=sis
server.port=7000

spring.datasource.url=jdbc:mysql://localhost:3307/sis?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=123456

# Hibernate chỉ kiểm tra schema, không tự động sửa
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Bật Flyway để quản lý schema DB
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

## 🐳 Hướng dẫn chạy với Docker

Repo đã có sẵn `docker-compose.yml` để chạy **MySQL** và **phpMyAdmin**.

### 1. Tạo Docker volume (chỉ cần 1 lần trên máy)

```bash
docker volume create --name=scc_sis_be_db_data

### 2. Chạy containers
```bash
docker compose up -d

- MySQL: chạy ở port 3307 (hoặc theo giá trị DB_PORT trong file .env).

- phpMyAdmin: truy cập tại http://localhost:8090


## Thêm mới / thay đổi DB

- Không dùng `ddl-auto=update` nữa.  
- Mọi thay đổi DB (tạo bảng mới, thêm cột, seed dữ liệu, …) phải viết thành **file migration SQL** trong thư mục:

## 📖 Quy tắc đặt tên file migration (chuẩn Flyway)

Tên file theo dạng:
V{version}__{description}.sql

Ví dụ:

- `V2__seed_roles.sql`  
- `V3__create_courses.sql`  

➡️ Sau khi thêm file migration, **restart app** → Flyway sẽ tự động apply migration mới.

---

## 🛠️ Quy trình làm việc khi thay đổi DB

1. Xác định thay đổi cần thực hiện (VD: thêm bảng, thêm cột, seed dữ liệu...).  
2. Tạo file SQL migration trong thư mục `src/main/resources/db/migration/`.  
3. Đặt tên file theo chuẩn Flyway (V{version}__{description}.sql).  
4. Commit file migration vào repo.  
5. Thành viên khác chỉ cần pull code + restart app, Flyway sẽ tự apply.  

---

## 🔑 Lợi ích của Flyway

- Quản lý **version DB** tập trung, đồng bộ giữa tất cả thành viên.  
- Dễ rollback, dễ kiểm soát thay đổi.  
- Tránh lỗi do mỗi người tự chỉnh DB thủ công.  

