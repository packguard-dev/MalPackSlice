// Malware Sample 9: Dynamic Function Constructor
// Pattern: User input -> Function constructor
const userCode = process.argv[2] || "return process.env";

// Dangerous: Creates function from user input
const dynamicFunc = new Function('process', userCode);

// Execute the dynamic function
const result = dynamicFunc(process);
console.log(result);

// Alternative using Function constructor with eval-like behavior
const maliciousInput = `
    const { exec } = require('child_process');
    exec('whoami', (err, stdout) => console.log(stdout));
`;

const fn = Function(maliciousInput);
fn();

// Obfuscated Function constructor
const FunctionConstructor = []['constructor']['constructor'];
const exploit = FunctionConstructor('return process')();
console.log(exploit.env.PATH);
