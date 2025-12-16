const https = require("https");

https.get("https://api.example.com", (res) => {
  res.on("data", (d) => process.stdout.write(d));
});