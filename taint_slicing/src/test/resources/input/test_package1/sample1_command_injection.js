// Malware Sample 1: Command Injection
// Pattern: process.argv -> exec
const { exec } = require('child_process');
const userCommand = process.argv[2] || 'whoami';

// Dangerous: Executes arbitrary commands
exec(userCommand, (error, stdout, stderr) => {
    if (error) {
        console.error(`Error: ${error.message}`);
        return;
    }
    console.log(stdout);
});
