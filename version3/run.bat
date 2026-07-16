@echo off
chcp 65001 >nul
echo ========================================
echo   BMI 体质评估与预测系统 v3.0 (Aurora Design System)
echo ========================================
echo.

set JDK_PATH=C:\Users\huang\Desktop\jdk\bin
set LIB_PATH=%~dp0lib\postgresql-42.7.3.jar
set SRC_PATH=%~dp0src

echo [1/3] 编译 Java 源文件...
"%JDK_PATH%\javac" -encoding UTF-8 -cp "%LIB_PATH%" -d "%~dp0out" "%SRC_PATH%\HealthSystem.java" "%SRC_PATH%\InitDB.java" "%SRC_PATH%\CheckDB.java"

if %errorlevel% neq 0 (
    echo [错误] 编译失败！请检查 JDK 路径是否正确。
    pause
    exit /b 1
)

echo [2/3] 编译成功！
echo.

echo [3/3] 启动系统...
"%JDK_PATH%\java" -cp "%~dp0out;%LIB_PATH%" HealthSystem

pause
