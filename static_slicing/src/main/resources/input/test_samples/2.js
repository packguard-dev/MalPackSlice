const fs = require("fs");

// Đọc file (sensitive)
const data = fs.readFileSync("/etc/passwd", "utf8");

// Ghi file (sensitive)
fs.writeFileSync("log.txt", "Hello");

// Xóa file
fs.unlinkSync("temp.txt");  