# 🚀 CI/CD Pipeline - Docker Hub

## 📋 Tổng Quan

**CI/CD tự động:** Push code → Run tests → Build Docker image → Push to Docker Hub

**Docker Hub Repository:** `huy210205/scc-sis-be`

**Test Coverage:** 92 tests (67 service + 25 controller) ✅

---

## 🔄 Quy Trình CI/CD

### **Flow Tự Động:**

```
Developer Push Code (main/dev)
    ↓
GitHub Actions Triggered
    ↓
Job 1: Run Tests
  ✅ Checkout code
  ✅ Setup JDK 17
  ✅ Cache Maven dependencies
  ✅ Run 92 tests (~10s)
  ✅ Generate JaCoCo coverage report
  ✅ Upload test results
    ↓
Tests Pass? ────→ No ──→ ❌ Pipeline Failed (Fix code)
    ↓ Yes
Job 2: Build & Push Docker
  ✅ Setup Docker Buildx
  ✅ Login Docker Hub (huy210205)
  ✅ Build Docker image
  ✅ Push to Docker Hub
    ↓
✅ Image Available on Docker Hub!
```

---

## 🎯 Setup GitHub Secrets (QUAN TRỌNG)

### **Bước 1: Tạo Access Token trên Docker Hub**

1. Đăng nhập Docker Hub: https://hub.docker.com/
2. Click avatar → **Account Settings**
3. Sidebar → **Security**
4. Click **New Access Token**
5. Description: `GitHub Actions CI/CD`
6. Permissions: ✅ Read, Write, Delete
7. Click **Generate**
8. **COPY TOKEN** (chỉ hiện 1 lần!)

``` hi
Token format: dckr_pat_xxxxxxxxxxxxxxxxxxxxx
```

### **Bước 2: Thêm Secrets vào GitHub**

1. Vào repository: https://github.com/codegym-software/SCC-SIS-BE
2. **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

**Secret 1:**
```
Name: DOCKER_USERNAME
Value: huy210205
```
Click **Add secret**

**Secret 2:**
```
Name: DOCKER_PASSWORD
Value: dckr_pat_xxxxxxxxxxxxxxxxxxxxx (paste token vừa copy)
```
Click **Add secret**

### **Verify Secrets:**
```
Repository secrets:
✅ DOCKER_USERNAME
✅ DOCKER_PASSWORD
```

---

## 📝 Quy Trình Làm Việc

### **Scenario 1: Phát triển feature mới**

```bash
# 1. Tạo branch mới
git checkout -b feature/new-feature

# 2. Code và viết tests
# ... coding ...

# 3. Chạy tests local trước khi push
mvn clean test

# 4. Nếu tests pass → Commit
git add .
git commit -m "feat: add new feature"
git push origin feature/new-feature

# 5. Tạo Pull Request trên GitHub
# GitHub → Pull requests → New pull request
# Base: dev ← Compare: feature/new-feature

# 6. GitHub Actions sẽ tự động run tests
# Nếu tests pass → Merge được enable
# Nếu tests fail → Fix code và push lại
```

### **Scenario 2: Merge và Deploy**

```bash
# 1. Sau khi PR được approve → Merge vào dev
# Click "Merge pull request" trên GitHub

# 2. GitHub Actions tự động triggered trên branch dev:
#    - Run tests
#    - Build Docker image
#    - Push to huy210205/scc-sis-be:dev

# 3. Khi ready production → Merge dev vào main
git checkout main
git pull
git merge dev
git push origin main

# 4. GitHub Actions tự động triggered trên branch main:
#    - Run tests
#    - Build Docker image
#    - Push to huy210205/scc-sis-be:latest
```

---

## 🐳 Docker Images

### **Tags tự động:**

| Branch | Docker Tag | Khi nào tạo |
|--------|-----------|-------------|
| `main` | `huy210205/scc-sis-be:latest` | Push vào main |
| `dev` | `huy210205/scc-sis-be:dev` | Push vào dev |
| `huy2.3` | `huy210205/scc-sis-be:huy2.3` | Push vào huy2.3 |
| Any | `huy210205/scc-sis-be:main-abc1234` | Mỗi commit (SHA) |

### **Xem images trên Docker Hub:**
```
https://hub.docker.com/r/huy210205/scc-sis-be/tags
```

---

## 🔍 Monitoring CI/CD

### **Xem Workflow trên GitHub Actions:**

1. Vào repository: https://github.com/codegym-software/SCC-SIS-BE
2. Click tab **Actions**
3. Xem danh sách workflow runs

**Status icons:**
- ✅ Green checkmark = Success
- ❌ Red X = Failed
- 🟡 Yellow dot = Running
- ⏸️ Gray = Cancelled

### **Xem chi tiết logs:**

1. Click vào workflow run muốn xem
2. Click vào job: `test` hoặc `build-and-push-docker`
3. Expand các step để xem logs

**Example logs khi success:**
```
Run Tests
  Tests run: 92, Failures: 0, Errors: 0, Skipped: 0
  BUILD SUCCESS

Build & Push Docker Image
  ✅ Docker image pushed successfully!
  📦 Image: huy210205/scc-sis-be
  🏷️ Tags: huy210205/scc-sis-be:dev
```

---

## 🧪 Chạy Tests Local

### **Chạy tất cả tests:**
```bash
mvn clean test
```

### **Chạy 1 test class cụ thể:**
```bash
mvn test -Dtest=RoleServiceImplTest
```

### **Chạy tests với coverage:**
```bash
mvn clean test jacoco:report
start target/site/jacoco/index.html
```

### **Build không chạy tests:**
```bash
mvn clean package -DskipTests
```

---

## 🐳 Sử Dụng Docker Image

### **Pull image từ Docker Hub:**

```bash
# Pull latest (từ main branch)
docker pull huy210205/scc-sis-be:latest

# Pull dev version
docker pull huy210205/scc-sis-be:dev

# Pull specific commit
docker pull huy210205/scc-sis-be:main-abc1234
```

### **Chạy container:**

```bash
# Chạy đơn giản
docker run -p 8080:8080 huy210205/scc-sis-be:latest

# Chạy với environment variables
docker run -d \
  --name scc-sis-be \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  huy210205/scc-sis-be:latest

# Xem logs
docker logs -f scc-sis-be
```

### **Sử dụng docker-compose:**

```bash
# Start tất cả services (MySQL + Backend)
docker-compose up -d

# Chỉ start backend
docker-compose up -d backend

# Xem logs
docker-compose logs -f backend

# Stop services
docker-compose down
```

---

## 🐛 Troubleshooting

### **Problem 1: Tests fail trên GitHub Actions**

**Hiện tượng:**
```
Tests run: 92, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

**Solution:**
```bash
# 1. Xem logs chi tiết trên GitHub Actions
# 2. Reproduce lỗi trên local
mvn clean test

# 3. Fix lỗi và push lại
git add .
git commit -m "fix: resolve test failure"
git push
```

### **Problem 2: Docker login failed**

**Hiện tượng:**
```
Error: Error response from daemon: unauthorized: 
incorrect username or password
```

**Solution:**
```
1. Verify GitHub Secrets:
   Settings → Secrets → Actions
   ✅ DOCKER_USERNAME = huy210205
   ✅ DOCKER_PASSWORD = dckr_pat_xxx...

2. Nếu token expired → Tạo token mới:
   Docker Hub → Account Settings → Security
   → New Access Token → Copy → Update DOCKER_PASSWORD
```

### **Problem 3: Docker build failed**

**Hiện tượng:**
```
Error: failed to solve: failed to fetch oauth token
```

**Solution:**
```
1. Check Dockerfile syntax
2. Check Maven dependencies trong pom.xml
3. Re-run workflow:
   Actions → Click workflow → Re-run failed jobs
```

### **Problem 4: Cannot pull image**

**Hiện tượng:**
```
Error: pull access denied for huy210205/scc-sis-be
```

**Solution:**
```bash
# 1. Login to Docker Hub
docker login -u huy210205

# 2. Enter access token when prompted

# 3. Try pull again
docker pull huy210205/scc-sis-be:latest
```

---

## ✅ Checklist

### **Setup lần đầu:**

- [ ] Tạo Docker Hub account
- [ ] Tạo Access Token
- [ ] Thêm DOCKER_USERNAME secret vào GitHub
- [ ] Thêm DOCKER_PASSWORD secret vào GitHub
- [ ] Push code để trigger workflow
- [ ] Verify workflow success trên GitHub Actions
- [ ] Verify image xuất hiện trên Docker Hub

### **Mỗi lần phát triển:**

- [ ] Tạo branch mới cho feature
- [ ] Code và viết tests
- [ ] Chạy tests local: `mvn clean test`
- [ ] Commit và push code
- [ ] Tạo Pull Request
- [ ] Đợi GitHub Actions run tests
- [ ] Nếu tests pass → Merge PR
- [ ] GitHub Actions tự động build và push Docker image

---

## 📊 Thống Kê

**Current Status:**
- ✅ 92 tests passing
- ✅ CI/CD automated
- ✅ Docker Hub: huy210205/scc-sis-be
- ⏱️ Test execution: ~10 seconds
- 🐳 Image size: ~200MB (optimized)

**Workflow file:** `.github/workflows/ci.yml`

**Docker file:** `Dockerfile`

---

## 🔗 Links Quan Trọng

- **Repository:** https://github.com/codegym-software/SCC-SIS-BE
- **GitHub Actions:** https://github.com/codegym-software/SCC-SIS-BE/actions
- **Docker Hub:** https://hub.docker.com/r/huy210205/scc-sis-be
- **Tags:** https://hub.docker.com/r/huy210205/scc-sis-be/tags

---

## 📞 Support

Nếu gặp vấn đề:
1. Check logs trên GitHub Actions
2. Xem issues trên GitHub repository
3. Liên hệ team lead hoặc DevOps

---

**Happy Coding! 🚀**
