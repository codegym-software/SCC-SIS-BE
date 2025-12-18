
# 🚀 Start SIS Services Script
# PowerShell script to start services in order

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Starting SIS Services with Qdrant" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if .env exists
if (-Not (Test-Path ".env")) {
    Write-Host "❌ Error: .env file not found!" -ForegroundColor Red
    Write-Host "Please create .env file with OPENAI_API_KEY" -ForegroundColor Yellow
    exit 1
}

# Step 1: Start Qdrant first
Write-Host "1️⃣ Starting Qdrant Vector Database..." -ForegroundColor Cyan
docker-compose up -d qdrant

Write-Host "   Waiting for Qdrant to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# Check Qdrant health
$maxRetries = 15
$retries = 0
$qdrantReady = $false

while ($retries -lt $maxRetries) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:6333/collections" -Method Get -TimeoutSec 2
        Write-Host "   ✅ Qdrant is ready!" -ForegroundColor Green
        $qdrantReady = $true
        break
    } catch {
        $retries++
        Write-Host "   ⏳ Waiting... ($retries/$maxRetries)" -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
}

if (-Not $qdrantReady) {
    Write-Host "   ❌ Qdrant failed to start!" -ForegroundColor Red
    Write-Host "   Check logs: docker-compose logs qdrant" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Step 2: Start MySQL and phpMyAdmin
Write-Host "2️⃣ Starting MySQL and phpMyAdmin..." -ForegroundColor Cyan
docker-compose up -d mysql phpmyadmin
Start-Sleep -Seconds 5
Write-Host "   ✅ MySQL and phpMyAdmin started!" -ForegroundColor Green
Write-Host ""

# Step 3: Ask if user wants to start backend
Write-Host "3️⃣ Backend Service" -ForegroundColor Cyan
$startBackend = Read-Host "   Do you want to start the backend now? (y/n)"

if ($startBackend -eq "y" -or $startBackend -eq "Y") {
    Write-Host "   Starting Spring Boot Backend..." -ForegroundColor Cyan
    docker-compose up -d backend
    
    Write-Host "   ⏳ Waiting for Backend to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    
    # Check backend health
    $backendRetries = 0
    $backendMaxRetries = 20
    $backendReady = $false
    
    while ($backendRetries -lt $backendMaxRetries) {
        try {
            $healthCheck = Invoke-RestMethod -Uri "http://localhost:7000/actuator/health" -Method Get -TimeoutSec 2
            if ($healthCheck.status -eq "UP") {
                Write-Host "   ✅ Backend is UP and running!" -ForegroundColor Green
                $backendReady = $true
                break
            }
        } catch {
            $backendRetries++
            Write-Host "   ⏳ Backend starting... ($backendRetries/$backendMaxRetries)" -ForegroundColor Yellow
            Start-Sleep -Seconds 3
        }
    }
    
    if (-Not $backendReady) {
        Write-Host "   ⚠️  Backend is still starting up" -ForegroundColor Yellow
        Write-Host "   Check logs: docker-compose logs -f backend" -ForegroundColor Cyan
    }
} else {
    Write-Host "   ⏭️  Backend not started" -ForegroundColor Yellow
    Write-Host "   Start manually: docker-compose up -d backend" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  📊 Services Status" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
docker-compose ps

Write-Host ""
Write-Host "🌐 Service URLs:" -ForegroundColor Green
Write-Host "   Qdrant Dashboard:  http://localhost:6333/dashboard" -ForegroundColor Cyan
Write-Host "   phpMyAdmin:        http://localhost:8090" -ForegroundColor Cyan
Write-Host "   Backend API:       http://localhost:7000" -ForegroundColor Cyan
Write-Host "   Backend Health:    http://localhost:7000/actuator/health" -ForegroundColor Cyan
Write-Host "   Swagger UI:        http://localhost:7000/swagger-ui.html" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 Useful Commands:" -ForegroundColor Green
Write-Host "   View backend logs:  docker-compose logs -f backend" -ForegroundColor Yellow
Write-Host "   View qdrant logs:   docker-compose logs -f qdrant" -ForegroundColor Yellow
Write-Host "   Stop all services:  docker-compose down" -ForegroundColor Yellow
Write-Host "   Restart backend:    docker-compose restart backend" -ForegroundColor Yellow
Write-Host ""
