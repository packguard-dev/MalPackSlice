# JSCodeSlicing

[![Scala](https://img.shields.io/badge/Scala-3.6.4-red.svg)](https://www.scala-lang.org/)
[![Joern](https://img.shields.io/badge/Joern-4.0.436-blue.svg)](https://joern.io/)
[![Docker](https://img.shields.io/badge/Docker-Supported-blue.svg)](https://www.docker.com/)

---

## Modules

- **taint_slicing**: Core implementation of our taint-based slicing approach. It integrates both Control Flow Graph (CFG) and Data Flow Graph (DFG) to track explicit information flows from sources to sinks.
- **static_slicing**: Baseline implementation using static slicing based solely on Control Flow Graph (CFG). This module serves as a benchmark for evaluating the effectiveness of the taint-based approach.
- **prompting**: Contains the prompting schemes for automated malicious code assessment using Large Language Models (see installation and procedure at [Run Prompting Scheme](#run-prompting-scheme).


## Dataset

This research uses the malicious package dataset provided by Wang et al. in their repository [MalPacDetector](https://github.com/CGCL-codes/MalPacDetector-core)

---

## 🚀 Quick Start

Choose the method that works best for you:

### Option 1: Docker (Recommended)

**Best for:** Testing with real malware samples in a safe, isolated environment.

#### Prerequisites
- Docker Desktop installed and running
- PowerShell (Windows) or Bash (Linux/Mac)

#### Steps

```powershell
# Windows
.\docker-test.ps1

# Linux/Mac
./docker-test.sh
```

**What happens:**
1. ✅ Builds Docker image with Java 21, SBT, and astgen
2. ✅ Downloads 10 real malware samples inside container
3. ✅ Runs analysis (`sbt run`)
4. ✅ Exports results to `./output/` directory
5. ✅ Cleans up (malware never touches your host machine)

**Output:** `./output/malware_samples/code_slice.txt` (6KB+)

---

### Option 2: Local Installation

**Best for:** Development, debugging, or analyzing your own JavaScript files.

#### Prerequisites

- **Java:** JDK 17 or later
- **SBT:** Scala Build Tool ([install guide](https://www.scala-sbt.org/download.html))
- **astgen:** Binary for your OS ([download here](https://github.com/joernio/astgen/releases))

#### Automated Setup (Windows)

```powershell
.\setup.ps1
```

This script will:
- Check for Java and SBT
- Download the correct `astgen` binary for Windows
- Set environment variables

#### Manual Setup

**1. Download astgen**

Visit [astgen releases](https://github.com/joernio/astgen/releases/tag/v3.35.0) and download:
- Windows: `astgen-win.exe`
- Linux: `astgen-linux`
- macOS: `astgen-macos`

**2. Install astgen**

```bash
# Create directory
mkdir -p ./astgen

# Move downloaded binary
mv ~/Downloads/astgen-* ./astgen/

# Linux/Mac: Make executable
chmod +x ./astgen/astgen-*

# Set environment variable
# Windows (PowerShell)
$env:ASTGEN_BIN = "$PWD\astgen\astgen-win.exe"

# Linux/Mac (Bash)
export ASTGEN_BIN="$(pwd)/astgen/astgen-linux"
```

**3. Verify Installation**

```bash
sbt compile
```

If successful, you're ready to run!

---

## 📖 Usage

### Basic Usage

**1. Prepare Input Files**

Place your JavaScript files in a package directory:

```
src/main/resources/input/
└── my_package/
    ├── app.js
    ├── malicious.js
    └── utils.js
```

**2. Run Analysis**

```bash
sbt run
```

**3. View Results**

```
src/main/resources/output/
└── my_package/
    ├── code_slice.txt    # Extracted malicious code flows
    └── cpg.bin           # Code Property Graph (binary)
```

---

### Docker Usage (Real Malware)

**Run with Real Malware Samples:**

```powershell
# Windows
.\docker-test.ps1

# Linux/Mac
docker build -t jscodeslicing-test .
docker run --rm jscodeslicing-test
```

**Add Custom Malware URLs:**

Edit `download-malware.sh`:
```bash
wget -q https://your-malware-source.com/sample.js -O custom_malware.js
```

**Export Results:**

```bash
# Create container
docker create --name malware-test jscodeslicing-test

# Start analysis
docker start -a malware-test

# Copy results to host
docker cp malware-test:/app/src/main/resources/output ./output

# Cleanup
docker rm malware-test
```

---

## 🐛 Troubleshooting

### Common Issues

| Problem | Solution |
|---------|----------|
| **"ASTGEN_BIN not set"** | Set environment variable: `$env:ASTGEN_BIN = "$PWD\astgen\astgen-win.exe"` |
| **"Cannot find astgen binary"** | Download from [releases](https://github.com/joernio/astgen/releases) |
| **"Timeout processing package"** | Increase `TIMEOUT_SECONDS` in `Main.scala` |
| **Empty code_slice.txt** | No flows detected - check source/sink definitions |
| **Docker build fails** | Ensure Docker is running: `docker ps` |

### Debug Mode

Enable verbose logging:

```bash
# Edit build.sbt and add:
logLevel := Level.Debug
```

### Clean Build

```bash
sbt clean compile
```

---

## Run Prompting Scheme

### Installation
1. Create a vitural environment and activate it
```bash
python3 -m venv venv
# On Window 
.\venv\Scripts\Activate.ps1
# On Macos/linux
source venv/bin/active
```
2. Install dependencies (in venv)
```bash
pip install -r requirements.txt
```
### Procedure
1. Create your own config json file to set up your favorite config and model

 For example:
```json
{
  "model_name": "deepseek-ai/deepseek-coder-6.7b-instruct",
  "max_new_tokens": 512,
  "do_sample": true,
  "top_k": 50,
  "top_p": 0.95,
  "temperature": 0.7,
  "num_return_sequences": 1
}
```
2. In file main.py config the path to your json config file
```python
config = ModelConfig.from_json_file("<PATH TO JSON CONFIG FILE>")
```
3. Config slices to detect

4. Run the model 
```bash
python3 main.py
```
5. Output JSON example:
```
{
  "purpose": string,                 // short single-line description of what this code appears to do
  "sources": [string],               // array of places where input/data is read (e.g., "req.body · line 4", "process.env · line ~1")
  "sinks": [string],                 // array of sensitive sinks or effects (e.g., "eval(...) · line 10", "fs.writeFile · line 12", "exec(...) · line 17")
  "flows": [string],                 // array of source→sink paths with evidence (e.g., "req.body -> eval (line 4 -> line 10)")
  "anomalies": [string],             // unusual patterns, syntax oddities, obfuscation indicators, commented-out dangerous code etc.
  "analysis": string,                // step-by-step numbered analysis of the entire fragment (concise paragraphs)
  "conclusion": string,              // short summary conclusion (one or two sentences)
  "confidence": float,               // overall confidence (0.00-1.00)
  "obfuscated": float,               // estimated obfuscation likelihood (0.00-1.00)
  "malware": float,                  // estimated malware likelihood (0.00-1.00)
  "securityRisk": float              // estimated security risk severity (0.00-1.00)
}
```