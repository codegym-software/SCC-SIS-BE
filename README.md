# SCC-SIS-BE

##Lưu ý
## Cấu hình Spring Boot

File cấu hình `application.properties` sẽ **không được đẩy lên Git** vì mỗi người cần cấu hình riêng.  
➝ Mỗi người cần tự tạo file tại `src/main/resources/application.properties` dựa trên mẫu dưới đây:
# Ví dụ cấu hình tham khảo
spring.application.name=sis
server.port=7000
spring.datasource.url=jdbc:mysql://localhost:3307/sis?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=123456
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

