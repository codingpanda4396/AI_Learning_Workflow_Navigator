@echo off
chcp 65001 >nul
setlocal
set "ROOT=%~dp0"

cd /d "%ROOT%backend" || (
  echo [错误] 找不到 backend 目录
  exit /b 1
)
start "AI Navigator - Backend" cmd /k "mvn spring-boot:run"

cd /d "%ROOT%frontend" || (
  echo [错误] 找不到 frontend 目录
  exit /b 1
)
start "AI Navigator - Frontend" cmd /k "npm run dev"

echo.
echo 已在新窗口启动：
echo   后端  http://localhost:8080
echo   前端  http://localhost:5173
echo 关闭对应标题的窗口即可停止服务。
echo.

endlocal
