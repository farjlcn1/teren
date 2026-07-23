@echo off
setlocal

cd /d "%~dp0"

where git >nul 2>nul
if errorlevel 1 (
    echo Git ni najden na tem racunalniku. Namesti ga s strani https://git-scm.com/download/win
    pause
    exit /b 1
)

if not exist ".git" (
    echo Inicializiram git repozitorij ...
    git init
    git branch -M main
)

git remote get-url origin >nul 2>nul
if errorlevel 1 (
    echo Dodajam povezavo do GitHub repozitorija ...
    git remote add origin https://github.com/farjlcn1/teren.git
)

echo Dodajam vse datoteke ...
git add .

git diff --cached --quiet
if errorlevel 1 (
    echo Ustvarjam commit ...
    git commit -m "Posodobitev aplikacije Teren"
) else (
    echo Ni sprememb za commit.
)

echo Posiljam na GitHub (teren) ...
git push -u origin main

if errorlevel 1 (
    echo.
    echo Posiljanje ni uspelo. Mozni vzroki:
    echo  - repozitorij na GitHubu ni prazen ^(vsebuje README/licenco^) - najprej "git pull origin main --allow-unrelated-histories"
    echo  - se nisi prijavljen v Git/GitHub na tem racunalniku - ob pozivu se prijavi preko brskalnika
) else (
    echo.
    echo Uspesno poslano na GitHub.
)

echo.
pause
