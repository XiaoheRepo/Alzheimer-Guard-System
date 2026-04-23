const fs = require('fs');
const path = require('path');

const baseDir = __dirname;

const dirs = [
  'app\\src\\main\\res\\values-en',
  'app\\src\\main\\res\\values-night',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\common',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\network',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\auth',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\storage',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\config',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\theme',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\i18n',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\logging',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\eventbus',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\navigation',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\core\\ui\\components',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\di',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\auth\\data',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\auth\\domain\\usecase',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\auth\\ui\\login',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\auth\\ui\\register',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\auth\\ui\\reset',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\me\\data',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\me\\domain',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\me\\ui',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\profile\\data',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\profile\\domain\\usecase',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\profile\\ui\\list',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\profile\\ui\\detail',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\profile\\ui\\edit',
  'app\\src\\main\\java\\com\\xiaohelab\\guard\\android\\feature\\profile\\ui\\guardian',
  'app\\src\\test\\java\\com\\xiaohelab\\guard\\android\\core\\network',
  'app\\src\\test\\java\\com\\xiaohelab\\guard\\android\\core\\config',
  'app\\src\\test\\java\\com\\xiaohelab\\guard\\android\\feature\\auth\\domain',
  'doc\\rfc'
];

// Create all directories
dirs.forEach(dir => {
  const fullPath = path.join(baseDir, dir);
  try {
    fs.mkdirSync(fullPath, { recursive: true });
  } catch (err) {
    console.error(`Failed to create ${fullPath}: ${err.message}`);
  }
});

// Clean up old setup scripts
const filesToRemove = ['create_dirs.bat', 'create_dirs.ps1'];
filesToRemove.forEach(file => {
  const filePath = path.join(baseDir, file);
  if (fs.existsSync(filePath)) {
    try {
      fs.unlinkSync(filePath);
      console.log(`Deleted: ${file}`);
    } catch (err) {
      console.error(`Failed to delete ${file}: ${err.message}`);
    }
  }
});

// Verify directories exist
let count = 0;
dirs.forEach(dir => {
  const fullPath = path.join(baseDir, dir);
  if (fs.existsSync(fullPath)) {
    count++;
  } else {
    console.error(`Missing: ${dir}`);
  }
});

console.log(`OK: ${count} directories confirmed`);
