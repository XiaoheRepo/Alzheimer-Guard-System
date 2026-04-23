# Create resource directories
New-Item -ItemType Directory -Path "app\src\main\res\values-en" -Force | Out-Null
New-Item -ItemType Directory -Path "app\src\main\res\values-night" -Force | Out-Null
New-Item -ItemType Directory -Path "app\src\main\res\values-night-en" -Force | Out-Null

# Create production source packages
@(
    "app\src\main\java\com\xiaohelab\guard\android\core\common",
    "app\src\main\java\com\xiaohelab\guard\android\core\network",
    "app\src\main\java\com\xiaohelab\guard\android\core\auth",
    "app\src\main\java\com\xiaohelab\guard\android\core\storage",
    "app\src\main\java\com\xiaohelab\guard\android\core\config",
    "app\src\main\java\com\xiaohelab\guard\android\core\theme",
    "app\src\main\java\com\xiaohelab\guard\android\core\i18n",
    "app\src\main\java\com\xiaohelab\guard\android\core\logging",
    "app\src\main\java\com\xiaohelab\guard\android\core\eventbus",
    "app\src\main\java\com\xiaohelab\guard\android\core\navigation",
    "app\src\main\java\com\xiaohelab\guard\android\core\ui",
    "app\src\main\java\com\xiaohelab\guard\android\core\ui\components",
    "app\src\main\java\com\xiaohelab\guard\android\di",
    "app\src\main\java\com\xiaohelab\guard\android\feature\auth\data",
    "app\src\main\java\com\xiaohelab\guard\android\feature\auth\domain",
    "app\src\main\java\com\xiaohelab\guard\android\feature\auth\domain\usecase",
    "app\src\main\java\com\xiaohelab\guard\android\feature\auth\ui",
    "app\src\main\java\com\xiaohelab\guard\android\feature\auth\ui\login",
    "app\src\main\java\com\xiaohelab\guard\android\feature\auth\ui\register",
    "app\src\main\java\com\xiaohelab\guard\android\feature\auth\ui\reset",
    "app\src\main\java\com\xiaohelab\guard\android\feature\me\data",
    "app\src\main\java\com\xiaohelab\guard\android\feature\me\domain",
    "app\src\main\java\com\xiaohelab\guard\android\feature\me\ui",
    "app\src\main\java\com\xiaohelab\guard\android\feature\profile\data",
    "app\src\main\java\com\xiaohelab\guard\android\feature\profile\domain",
    "app\src\main\java\com\xiaohelab\guard\android\feature\profile\domain\usecase",
    "app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui",
    "app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui\list",
    "app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui\detail",
    "app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui\edit",
    "app\src\main\java\com\xiaohelab\guard\android\feature\profile\ui\guardian",
    "app\src\test\java\com\xiaohelab\guard\android\core\network",
    "app\src\test\java\com\xiaohelab\guard\android\core\config",
    "app\src\test\java\com\xiaohelab\guard\android\feature\auth\domain",
    "doc\rfc",
    "app\src\main\assets"
) | ForEach-Object {
    New-Item -ItemType Directory -Path $_ -Force | Out-Null
}

# Delete stray file
$strayFile = "app\src\main\res\values\themes.xml.new"
if (Test-Path $strayFile) {
    Remove-Item $strayFile -Force
}

Write-Host "All directories created successfully!"
