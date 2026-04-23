@echo off
setlocal enabledelayedexpansion

cd /d c:\Users\Xiaohe\Desktop\Alzheimer-Guard-System\guard-android

REM Create resource directories
mkdir app\src\main\res\values-en 2>nul
mkdir app\src\main\res\values-night 2>nul
mkdir app\src\main\res\values-night-en 2>nul

REM Create production source packages
mkdir app\src\main\java\com\xiaohelab\guard\android\core\common 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\network 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\auth 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\storage 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\config 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\theme 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\i18n 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\logging 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\eventbus 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\navigation 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\ui 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\core\ui\components 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\di 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\auth\data 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\auth\domain 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\auth\domain\usecase 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\auth\ui 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\auth\ui\login 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\auth\ui\register 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\auth\ui\reset 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\me\data 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\me\domain 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\me\ui 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\profile\data 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\profile\domain 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\profile\domain\usecase 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui\list 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui\detail 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui\edit 2>nul
mkdir app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui\guardian 2>nul

REM Create test source packages
mkdir app\src\test\java\com\xiaohelab\guard\android\core\network 2>nul
mkdir app\src\test\java\com\xiaohelab\guard\android\core\config 2>nul
mkdir app\src\test\java\com\xiaohelab\guard\android\feature\auth\domain 2>nul

REM Create doc directories
mkdir doc\rfc 2>nul

REM Create assets directory
mkdir app\src\main\assets 2>nul

REM Delete stray file
del /q app\src\main\res\values\themes.xml.new 2>nul

echo Directory structure created successfully.
