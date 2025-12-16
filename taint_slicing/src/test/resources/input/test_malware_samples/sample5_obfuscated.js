// Malware Sample 5: Obfuscated Payload
// Pattern: String.fromCharCode -> eval
const obfuscated = [101, 118, 97, 108, 40, 34, 99, 111, 110, 115, 111, 108, 101, 46, 108, 111, 103, 40, 39, 104, 97, 99, 107, 101, 100, 39, 41, 34, 41];

// Deobfuscate
const code = String.fromCharCode(...obfuscated);

// Dangerous: Executes obfuscated code
// Decoded: eval("console.log('hacked')")
eval(code);

// Additional obfuscation layer
const payload = atob('Y29uc29sZS5sb2coJ21hbGljaW91cycpOw=='); // console.log('malicious');
eval(payload);
