# 一键启动后端 (Spring Boot) 与前端 (Vite)
# 依赖：JDK 17+、Maven、Node.js；前端首次需先在 frontend 目录执行 npm install

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot

$backend = Join-Path $Root "backend"
$frontend = Join-Path $Root "frontend"

if (-not (Test-Path $backend)) { throw "找不到 backend 目录: $backend" }
if (-not (Test-Path $frontend)) { throw "找不到 frontend 目录: $frontend" }

Write-Host "正在打开两个窗口启动服务..." -ForegroundColor Cyan

Start-Process cmd -ArgumentList "/k", "title AI Navigator - Backend && mvn spring-boot:run" -WorkingDirectory $backend
Start-Sleep -Milliseconds 500
Start-Process cmd -ArgumentList "/k", "title AI Navigator - Frontend && npm run dev" -WorkingDirectory $frontend

Write-Host ""
Write-Host "后端  http://localhost:8080" -ForegroundColor Green
Write-Host "前端  http://localhost:5173" -ForegroundColor Green
Write-Host "关闭标题为「AI Navigator - Backend / Frontend」的窗口即可停止对应服务。" -ForegroundColor DarkGray
