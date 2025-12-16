// Thực thi code động (rất nhạy cảm)
eval("console.log('Executed!')");

const { exec } = require("child_process");
exec("ls -la", (err, stdout) => {
  console.log(stdout);
});