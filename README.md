# SCC-SIS-BE ok15

## 📌 Lưu ý

* Repo **không** chứa file `application.properties` và `.env`, mỗi người cần tự tạo riêng.
* Vị trí file config BE: `src/main/resources/application.properties`.
* Vị trí file config Docker: `.env`.
* Có thể tham khảo mẫu trong:

  * `src/main/resources/application.properties.example`
  * `.env.example`

---

## ⚙️ Cấu hình Spring Boot (mẫu)

```properties
spring.application.name=sis
server.port=7000

spring.datasource.url=jdbc:mysql://localhost:${DB_PORT}/sis?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=${MYSQL_ROOT_PASSWORD}

# Hibernate chỉ kiểm tra schema, không tự động sửa
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Bật Flyway để quản lý schema DB
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Cho phép kết nối từ FE (Vite chạy ở port 5173)
app.cors.allowed-origins=http://localhost:5173
```

---

## 🐳 Hướng dẫn chạy với Docker

Repo đã có sẵn `docker-compose.yml` để chạy **MySQL** và **phpMyAdmin**.

### 1. Chuẩn bị file `.env`

Tạo file `.env` từ mẫu:

```bash
cp .env.example .env
```

Điền giá trị thật (port, password, volume name…).

### 2. Chuẩn bị file `application.properties`

Tạo file `application.properties` từ mẫu:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Điền giá trị thật nếu cần (DB\_PORT, MYSQL\_ROOT\_PASSWORD…), Spring Boot sẽ dùng file này khi chạy app.

### 3. Tạo Docker volume (chỉ cần 1 lần trên máy)

```bash
docker volume create --name=scc_sis_be_db_data
```

### 4. Chạy containers

```bash
docker compose up -d
```

* **MySQL**: chạy ở port `3307` (hoặc theo giá trị `DB_PORT` trong `.env`).
* **phpMyAdmin**: truy cập tại [http://localhost:8090](hoặc theo giá trị `PHPMYADMIN_PORT` trong `.env`)

---

## 🗄️ Thêm mới / thay đổi DB

* Không dùng `ddl-auto=update` nữa.
* Mọi thay đổi DB (tạo bảng, thêm cột, seed dữ liệu, …) phải viết thành **file migration SQL** trong thư mục:

  ```
  src/main/resources/db/migration/
  ```

### 📖 Quy tắc đặt tên file migration (chuẩn Flyway)

Tên file theo dạng:

```
V{version}__{description}.sql
```

Ví dụ:

* `V2__seed_roles.sql`
* `V3__create_courses.sql`

➡️ Sau khi thêm file migration, **restart app** → Flyway sẽ tự động apply migration mới.

---

## 🛠️ Quy trình làm việc khi thay đổi DB

1. Xác định thay đổi cần thực hiện (VD: thêm bảng, thêm cột, seed dữ liệu...).
2. Tạo file SQL migration trong thư mục `src/main/resources/db/migration/`.
3. Đặt tên file theo chuẩn Flyway (`V{version}__{description}.sql`).
4. Commit file migration vào repo.
5. Thành viên khác chỉ cần pull code + restart app, Flyway sẽ tự apply.

---

## 🔑 Lợi ích của Flyway

* Quản lý **version DB** tập trung, đồng bộ giữa tất cả thành viên.
* Dễ rollback, dễ kiểm soát thay đổi.
* Tránh lỗi do mỗi người tự chỉnh DB thủ công.

---

