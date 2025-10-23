# 📝 Hướng dẫn Test API POST Module (Tạo Module Mới)

## 📋 Mục lục

1. [Tổng quan](#tổng-quan)
2. [Chuẩn bị](#chuẩn-bị)
3. [Request Format](#request-format)
4. [Test Cases](#test-cases)
5. [Validation Rules](#validation-rules)
6. [Troubleshooting](#troubleshooting)

---

## 🎯 Tổng quan

### Endpoint
```
POST /api/modules
```

### Chức năng
Tạo module mới trong một program (chương trình đào tạo)

### Phân quyền
- ✅ **SUPER_ADMIN**: Tạo module cho bất kỳ program nào
- ✅ **ACADEMIC_STAFF**: Tạo module cho programs trong center của mình
- ❌ **Roles khác**: Không có quyền

### Response
- **201 Created**: Tạo thành công
- **400 Bad Request**: Dữ liệu không hợp lệ
- **401 Unauthorized**: Chưa đăng nhập
- **403 Forbidden**: Không có quyền
- **404 Not Found**: Program không tồn tại
- **409 Conflict**: Mã module hoặc sequenceOrder bị trùng

---

## 🛠️ Chuẩn bị

### 1. Khởi động server
```bash
cd SCC-SIS-BE
mvn spring-boot:run
```

Server sẽ chạy tại: `http://localhost:8080`

---

### 2. Đăng nhập để lấy token

#### Option 1: Super Admin
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "admin123"
}
```

#### Option 2: Academic Staff
```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "academic.staff@example.com",
  "password": "password123"
}
```

#### Response
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "admin@example.com",
    "fullName": "Super Admin"
  }
}
```

**Lưu lại `accessToken` để dùng cho các request tiếp theo!**

---

### 3. Kiểm tra Program ID

Trước khi tạo module, cần biết `programId` hợp lệ:

```bash
GET http://localhost:8080/api/programs
Authorization: Bearer {your_token}
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Lập trình Java Backend",
    "code": "JAVA_BACKEND_2024",
    "centerId": 1,
    "centerName": "Trung tâm Hà Nội"
  }
]
```

Ghi nhớ `programId` (ví dụ: `1`)

---

## 📤 Request Format

### Headers
```
Authorization: Bearer {your_access_token}
Content-Type: application/json
```

### Request Body

#### ⚠️ Các trường BẮT BUỘC

```json
{
  "programId": 1,
  "code": "MODULE_CODE",
  "name": "Tên module",
  "sequenceOrder": 1,
  "credits": 3
}
```

#### ✅ Request Body đầy đủ (có optional fields)

```json
{
  "programId": 1,
  "code": "JAVA_OOP",
  "name": "Lập trình hướng đối tượng với Java",
  "description": "Học các khái niệm cơ bản về OOP: Class, Object, Inheritance, Polymorphism, Encapsulation, Abstraction",
  "sequenceOrder": 8,
  "credits": 4,
  "durationHours": 60,
  "level": "BEGINNER",
  "isMandatory": true,
  "hasSyllabus": true,
  "syllabusUrl": "https://example.com/syllabus/java-oop.pdf",
  "notes": "Môn học bắt buộc cho sinh viên năm nhất"
}
```

**🎯 Lưu ý về Semester:**
- ❌ **KHÔNG cần** gửi field `semester` trong request
- ✅ Hệ thống **tự động tính** semester dựa trên `sequenceOrder`:
  - sequenceOrder 1-6 → semester 1
  - sequenceOrder 7-13 → semester 2  ← Ví dụ trên: `sequenceOrder: 8` → `semester: 2`
  - sequenceOrder 14-20 → semester 3
  - sequenceOrder 21+ → semester 4

---

### Các trường trong Request Body

| Field | Type | Required | Mô tả | Ví dụ |
|-------|------|----------|-------|-------|
| `programId` | Integer | ✅ Bắt buộc | ID của program | `1` |
| `code` | String | ✅ Bắt buộc | Mã module (unique trong program) | `"JAVA_OOP"` |
| `name` | String | ✅ Bắt buộc | Tên module | `"Lập trình OOP"` |
| `sequenceOrder` | Integer | ✅ Bắt buộc | Thứ tự môn học (unique, tự động tính semester) | `8` |
| `credits` | Integer | ✅ Bắt buộc | Số tín chỉ (1-10) | `4` |
| `description` | String | ❌ Tùy chọn | Mô tả chi tiết | `"Học về OOP..."` |
| ~~`semester`~~ | ~~Integer~~ | ⛔ **KHÔNG dùng** | **TỰ ĐỘNG tính từ sequenceOrder** | ~~`1`~~ |
| `durationHours` | Integer | ❌ Tùy chọn | Số giờ học | `60` |
| `level` | String | ❌ Tùy chọn | Độ khó: `BEGINNER`, `INTERMEDIATE`, `ADVANCED` | `"BEGINNER"` |
| `isMandatory` | Boolean | ❌ Tùy chọn | Môn bắt buộc hay không (mặc định: `true`) | `true` |
| `hasSyllabus` | Boolean | ❌ Tùy chọn | Có syllabus không (mặc định: `false`) | `true` |
| `syllabusUrl` | String | ❌ Tùy chọn | Link syllabus | `"https://..."` |
| `notes` | String | ❌ Tùy chọn | Ghi chú | `"Môn bắt buộc"` |

---

## 🧪 Test Cases

### Test Case 1: ✅ Tạo module thành công (minimal fields)

**Request:**
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "JAVA_BASIC",
  "name": "Lập trình Java cơ bản",
  "sequenceOrder": 1,
  "credits": 3
}
```

**Response: 201 Created**
```json
{
  "id": 21,
  "programId": 1,
  "code": "JAVA_BASIC",
  "name": "Lập trình Java cơ bản",
  "description": null,
  "sequenceOrder": 1,
  "semester": 1,
  "credits": 3,
  "durationHours": null,
  "level": null,
  "isMandatory": true,
  "hasSyllabus": false,
  "syllabusUrl": null,
  "notes": null,
  "isActive": true,
  "createdAt": "2025-10-23T10:30:00",
  "updatedAt": "2025-10-23T10:30:00",
  "createdBy": {
    "id": 1,
    "fullName": "Super Admin",
    "email": "admin@example.com"
  }
}
```

**Kiểm tra:**
- [x] HTTP Status: 201 Created
- [x] Response có `id` mới
- [x] `semester` = `1` (tự động tính từ sequenceOrder 1-6)
- [x] `isMandatory` mặc định = `true`
- [x] `hasSyllabus` mặc định = `false`
- [x] `isActive` = `true`

---

### Test Case 2: ✅ Tạo module với đầy đủ thông tin

**Request:**
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "JAVA_OOP",
  "name": "Lập trình hướng đối tượng với Java",
  "description": "Học các khái niệm OOP: Class, Object, Inheritance, Polymorphism",
  "sequenceOrder": 8,
  "credits": 4,
  "durationHours": 60,
  "level": "BEGINNER",
  "isMandatory": true,
  "hasSyllabus": true,
  "syllabusUrl": "https://example.com/syllabus/java-oop.pdf",
  "notes": "Môn học bắt buộc"
}
```

**Response: 201 Created**
```json
{
  "id": 22,
  "programId": 1,
  "code": "JAVA_OOP",
  "name": "Lập trình hướng đối tượng với Java",
  "description": "Học các khái niệm OOP: Class, Object, Inheritance, Polymorphism",
  "sequenceOrder": 8,
  "semester": 2,
  "credits": 4,
  "durationHours": 60,
  "level": "BEGINNER",
  "isMandatory": true,
  "hasSyllabus": true,
  "syllabusUrl": "https://example.com/syllabus/java-oop.pdf",
  "notes": "Môn học bắt buộc",
  "isActive": true,
  "createdAt": "2025-10-23T10:35:00",
  "updatedAt": "2025-10-23T10:35:00"
}
```

**📍 Lưu ý:**
- `semester: 2` được **tự động tính** từ `sequenceOrder: 8` (vì 7-13 → semester 2)
- Không cần gửi `semester` trong request

---

### Test Case 3: ❌ Thiếu trường bắt buộc

**Request:**
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "name": "Module thiếu code"
}
```

**Response: 400 Bad Request**
```json
{
  "error": "BAD_REQUEST",
  "message": "Dữ liệu không hợp lệ",
  "details": {
    "code": "Mã module không được để trống",
    "sequenceOrder": "Thứ tự môn học không được null",
    "credits": "Số tín chỉ không được null"
  }
}
```

---

### Test Case 4: ❌ Mã module trùng lặp

**Request:**
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "JAVA_BASIC",  // Đã tồn tại trong program 1
  "name": "Module trùng code",
  "sequenceOrder": 10,
  "credits": 3
}
```

**Response: 409 Conflict**
```json
{
  "error": "CONFLICT",
  "message": "Mã module 'JAVA_BASIC' đã tồn tại trong program này"
}
```

---

### Test Case 5: ❌ Sequence Order trùng lặp

**Request:**
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "NEW_MODULE",
  "name": "Module mới",
  "sequenceOrder": 1,  // Vị trí 1 đã có module khác
  "credits": 3
}
```

**Response: 409 Conflict**
```json
{
  "error": "CONFLICT",
  "message": "Thứ tự 1 đã được sử dụng trong program này"
}
```

---

### Test Case 6: ❌ Code không đúng format

**Request:**
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "java basic",  // ❌ Có khoảng trắng, chữ thường
  "name": "Module sai format",
  "sequenceOrder": 20,
  "credits": 3
}
```

**Response: 400 Bad Request**
```json
{
  "error": "BAD_REQUEST",
  "message": "Dữ liệu không hợp lệ",
  "details": {
    "code": "Mã module chỉ chứa chữ IN HOA, số, dấu gạch ngang và gạch dưới"
  }
}
```

**✅ Format đúng:**
- `JAVA_OOP`
- `DATABASE-101`
- `WEB_DEV_2024`
- `ALGO_DS`

---

### Test Case 7: ❌ Credits ngoài phạm vi

**Request:**
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "INVALID_CREDITS",
  "name": "Module credits sai",
  "sequenceOrder": 25,
  "credits": 15  // ❌ Vượt quá 10
}
```

**Response: 400 Bad Request**
```json
{
  "error": "BAD_REQUEST",
  "message": "Dữ liệu không hợp lệ",
  "details": {
    "credits": "Số tín chỉ phải từ 1-10"
  }
}
```

---

### Test Case 8: ❌ Program không tồn tại

**Request:**
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 999,  // Program không tồn tại
  "code": "TEST_MODULE",
  "name": "Test Module",
  "sequenceOrder": 1,
  "credits": 3
}
```

**Response: 404 Not Found**
```json
{
  "error": "NOT_FOUND",
  "message": "Program không tồn tại với ID: 999"
}
```

---

### Test Case 9: ❌ Không có token (Unauthorized)

**Request:**
```bash
POST http://localhost:8080/api/modules
Content-Type: application/json

{
  "programId": 1,
  "code": "NO_AUTH",
  "name": "Module không có token",
  "sequenceOrder": 30,
  "credits": 3
}
```

**Response: 401 Unauthorized**
```json
{
  "error": "UNAUTHORIZED",
  "message": "Vui lòng đăng nhập"
}
```

---

### Test Case 10: ✅ Tạo nhiều modules cho cùng program

**Mục đích:** Tạo modules cho các semester khác nhau (semester tự động tính)

#### Module 1 (sequenceOrder 1 → Semester 1)
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "JAVA_BASIC",
  "name": "Java cơ bản",
  "sequenceOrder": 1,
  "credits": 3
}
```
**Response:** `semester: 1` (tự động từ sequenceOrder 1-6)

---

#### Module 2 (sequenceOrder 2 → Semester 1)
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "JAVA_OOP",
  "name": "Java OOP",
  "sequenceOrder": 2,
  "credits": 4
}
```
**Response:** `semester: 1` (tự động từ sequenceOrder 1-6)

---

#### Module 7 (sequenceOrder 7 → Semester 2)
```bash
POST http://localhost:8080/api/modules
Authorization: Bearer {token}
Content-Type: application/json

{
  "programId": 1,
  "code": "SPRING_BOOT",
  "name": "Spring Boot Framework",
  "sequenceOrder": 7,
  "credits": 5
}
```
**Response:** `semester: 2` (tự động từ sequenceOrder 7-13)

---

**Kiểm tra danh sách modules:**
```bash
GET http://localhost:8080/api/modules?programId=1
Authorization: Bearer {token}
```

---

## ⚠️ Validation Rules

### 1. `programId`
- ✅ Bắt buộc
- ✅ Phải là số dương
- ✅ Program phải tồn tại trong database

### 2. `code`
- ✅ Bắt buộc
- ✅ Không được để trống
- ✅ Tối đa 50 ký tự
- ✅ CHỈ chứa: Chữ IN HOA, số, dấu gạch ngang (`-`), dấu gạch dưới (`_`)
- ✅ **Unique trong program** (không trùng với module khác)

**Ví dụ hợp lệ:**
```
JAVA_OOP
DATABASE-101
WEB_DEV_2024
ALGO_DS
SPRING-BOOT
```

**Ví dụ KHÔNG hợp lệ:**
```
java oop         ❌ Có khoảng trắng
Java-OOP         ❌ Có chữ thường
java@oop         ❌ Có ký tự đặc biệt
Lập trình Java   ❌ Có khoảng trắng, chữ thường, tiếng Việt
```

### 3. `name`
- ✅ Bắt buộc
- ✅ Không được để trống
- ✅ Tối đa 255 ký tự

### 4. `sequenceOrder`
- ✅ Bắt buộc
- ✅ Phải là số dương (> 0)
- ✅ **Unique trong program** (không trùng với module khác)

### 5. `credits`
- ✅ Bắt buộc
- ✅ Từ 1 đến 10

### 6. `description`
- ❌ Tùy chọn
- ✅ Tối đa 5000 ký tự

### 7. `semester`
- ⛔ **KHÔNG cần gửi** (TỰ ĐỘNG tính từ `sequenceOrder`)
- ✅ Hệ thống tự động gán:
  - sequenceOrder 1-6 → semester 1
  - sequenceOrder 7-13 → semester 2
  - sequenceOrder 14-20 → semester 3
  - sequenceOrder 21+ → semester 4

### 8. `durationHours`
- ❌ Tùy chọn
- ✅ Phải là số dương nếu có

### 9. `level`
- ❌ Tùy chọn
- ✅ Giá trị hợp lệ: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`

### 10. `isMandatory`
- ❌ Tùy chọn
- ✅ Mặc định: `true`

### 11. `hasSyllabus`
- ❌ Tùy chọn
- ✅ Mặc định: `false`

---

## 💡 Tips & Best Practices

### 1. Quy tắc đặt tên `code`
```
✅ Tốt:
- JAVA_BASIC
- DATABASE_101
- WEB-DEVELOPMENT
- SPRING_BOOT_FUNDAMENTALS

❌ Tránh:
- java_basic (chữ thường)
- Java Basic (có khoảng trắng)
- java@basic (ký tự đặc biệt)
```

### 2. Semester tự động tính

**Quy tắc mới (Auto-calculate):**
- ❌ **KHÔNG cần** gửi field `semester` trong request
- ✅ Hệ thống **TỰ ĐỘNG TÍNH** dựa trên `sequenceOrder`:

| sequenceOrder | Semester (tự động) |
|---------------|-------------------|
| 1-6 | 1 |
| 7-13 | 2 |
| 14-20 | 3 |
| 21+ | 4 |

**Ví dụ:**
```json
// Request: CHỈ gửi sequenceOrder
{
  "code": "JAVA_BASIC",
  "sequenceOrder": 2
}
// Response: Hệ thống tự gán semester: 1

// Request
{
  "code": "SPRING_BOOT",
  "sequenceOrder": 8
}
// Response: Hệ thống tự gán semester: 2
```

**⚠️ Quan trọng:**
- Semester CỐ ĐỊNH sau khi tạo
- Semester KHÔNG thay đổi khi reorder
- Chỉ cần gửi `sequenceOrder`, hệ thống lo phần còn lại!

### 3. Chọn `sequenceOrder`

**Quy tắc:**
- Bắt đầu từ 1
- Không trùng với module khác trong program
- Tăng dần theo thứ tự học

**Ví dụ:**
```
Module 1: sequenceOrder = 1
Module 2: sequenceOrder = 2
Module 3: sequenceOrder = 3
...
```

### 4. Kiểm tra program trước khi tạo module

```bash
# Xem danh sách programs
GET /api/programs

# Xem modules hiện có trong program
GET /api/modules?programId=1
```

### 5. Test với cURL

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "admin123"
  }'

# 2. Lưu token vào biến
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# 3. Tạo module
curl -X POST http://localhost:8080/api/modules \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "programId": 1,
    "code": "JAVA_BASIC",
    "name": "Lập trình Java cơ bản",
    "sequenceOrder": 1,
    "semester": 1,
    "credits": 3
  }'
```

---

## 🐛 Troubleshooting

### Lỗi 1: `ECONNREFUSED`
```
Error: connect ECONNREFUSED 127.0.0.1:8080
```

**Nguyên nhân:** Server chưa chạy

**Giải pháp:**
```bash
cd SCC-SIS-BE
mvn spring-boot:run
```

---

### Lỗi 2: `401 Unauthorized`
```json
{
  "error": "UNAUTHORIZED",
  "message": "Vui lòng đăng nhập"
}
```

**Nguyên nhân:** 
- Thiếu token
- Token hết hạn
- Token không hợp lệ

**Giải pháp:**
1. Đăng nhập lại để lấy token mới
2. Thêm header: `Authorization: Bearer {token}`
3. Kiểm tra token còn hạn (mặc định 1 giờ)

---

### Lỗi 3: `403 Forbidden`
```json
{
  "error": "FORBIDDEN",
  "message": "Bạn không có quyền thực hiện thao tác này"
}
```

**Nguyên nhân:** User không có quyền tạo module

**Giải pháp:**
- Đăng nhập bằng tài khoản **SUPER_ADMIN** hoặc **ACADEMIC_STAFF**

---

### Lỗi 4: `409 Conflict - Code đã tồn tại`
```json
{
  "error": "CONFLICT",
  "message": "Mã module 'JAVA_BASIC' đã tồn tại trong program này"
}
```

**Giải pháp:**
- Đổi `code` thành giá trị khác
- Hoặc kiểm tra xem module đã tồn tại chưa:
  ```bash
  GET /api/modules?programId=1
  ```

---

### Lỗi 5: `409 Conflict - Sequence Order trùng`
```json
{
  "error": "CONFLICT",
  "message": "Thứ tự 1 đã được sử dụng trong program này"
}
```

**Giải pháp:**
- Đổi `sequenceOrder` thành vị trí chưa có
- Xem danh sách modules hiện có:
  ```bash
  GET /api/modules?programId=1
  ```

---

### Lỗi 6: `400 Bad Request - Format code sai`
```json
{
  "details": {
    "code": "Mã module chỉ chứa chữ IN HOA, số, dấu gạch ngang và gạch dưới"
  }
}
```

**Giải pháp:**
- Chuyển code sang chữ IN HOA
- Loại bỏ khoảng trắng
- Thay khoảng trắng bằng `_` hoặc `-`

**Ví dụ:**
```
"java basic"  →  "JAVA_BASIC"
"Java OOP"    →  "JAVA_OOP"
"web dev"     →  "WEB_DEV"
```

---

## 📊 Checklist Test

Sau khi tạo module thành công, kiểm tra:

- [ ] HTTP Status = **201 Created**
- [ ] Response có `id` mới
- [ ] `code` giống với request
- [ ] `name` giống với request
- [ ] `sequenceOrder` đúng
- [ ] `semester` đúng (nếu có gửi)
- [ ] `credits` đúng
- [ ] `isMandatory` = `true` (nếu không gửi)
- [ ] `hasSyllabus` = `false` (nếu không gửi)
- [ ] `isActive` = `true`
- [ ] `createdAt` và `updatedAt` được set
- [ ] `createdBy` chứa thông tin user hiện tại

**Kiểm tra trong database:**
```bash
GET /api/modules?programId=1
```

Module mới phải xuất hiện trong danh sách!

---

## 🔗 Tài liệu liên quan

- [API Test Reorder Module](./API_TEST_REORDER_MODULE.md) - Test API sắp xếp lại module
- [Module Semester Logic](./MODULE_SEMESTER_LOGIC.md) - Logic semester cố định
- [Reorder Quick Guide](./REORDER_QUICK_GUIDE.md) - Hướng dẫn nhanh reorder

---

## 📞 Hỗ trợ

Nếu gặp vấn đề, vui lòng:
1. Kiểm tra lại [Troubleshooting](#troubleshooting)
2. Xem logs của server
3. Liên hệ Backend Team

---

**Cập nhật:** 2025-10-23

