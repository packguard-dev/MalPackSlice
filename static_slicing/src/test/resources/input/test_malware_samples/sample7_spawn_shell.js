// Malware Sample 7: Process Spawning with Shell Commands
// Pattern: process.argv -> child_process.spawn
const { spawn } = require('child_process');

const maliciousCommand = process.argv[2] || 'cat /etc/passwd';
const args = maliciousCommand.split(' ');
const cmd = args[0];
const cmdArgs = args.slice(1);

// Dangerous: Spawns arbitrary processes
const child = spawn(cmd, cmdArgs, {
    shell: true
});

child.stdout.on('data', (data) => {
    console.log(data.toString());
});

child.stderr.on('data', (data) => {
    console.error(data.toString());
});

// Also try execSync for synchronous execution
const { execSync } = require('child_process');
const userInput = process.env.MALICIOUS_CMD || 'id';
const output = execSync(userInput);
console.log(output.toString());
