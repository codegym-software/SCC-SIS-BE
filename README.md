# SCC-SIS-BE

## Lưu ý

- Repo **không** chứa file `application.properties` vì mỗi người cần cấu hình riêng.
- Mỗi người **tự tạo** file tại: `src/main/resources/application.properties`.
- (Khuyến nghị) Tham khảo mẫu trong `src/main/resources/application.example.properties` dưới đây.

## Cấu hình Spring Boot (mẫu)

```properties
spring.application.name=sis
server.port=7000

spring.datasource.url=jdbc:mysql://localhost:3307/sis?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=123456

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
