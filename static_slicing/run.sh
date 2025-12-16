#!/bin/bash

# Export ASTGEN_BIN environment variable
export ASTGEN_BIN="$(pwd)/astgen/astgen-linux"

# Check if ASTGEN_BIN exists
if [ ! -f "$ASTGEN_BIN" ]; then
    echo "Error: astgen-linux not found at $ASTGEN_BIN"
    exit 1
fi

echo "ASTGEN_BIN: $ASTGEN_BIN"
echo ""

# Process benign packages
echo "========================================"
echo "Processing BENIGN packages"
echo "========================================"
INPUT_FOLDER="../unpacked_data/ben"
OUTPUT_FOLDER="../../../workspace/MalLLM/data/benign"

if [ ! -d "$INPUT_FOLDER" ]; then
    echo "Warning: Benign input folder does not exist: $INPUT_FOLDER"
else
    mkdir -p "$OUTPUT_FOLDER"
    echo "Input folder: $INPUT_FOLDER"
    echo "Output folder: $OUTPUT_FOLDER"
    echo ""
    sbt "run $INPUT_FOLDER $OUTPUT_FOLDER"
    echo ""
    echo "Benign packages processing completed!"
    echo ""
fi

# Process malicious packages
echo "========================================"
echo "Processing MALICIOUS packages"
echo "========================================"
INPUT_FOLDER="../unpacked_data/mal"
OUTPUT_FOLDER="../../../workspace/MalLLM/data/malicious"

if [ ! -d "$INPUT_FOLDER" ]; then
    echo "Warning: Malicious input folder does not exist: $INPUT_FOLDER"
else
    mkdir -p "$OUTPUT_FOLDER"
    echo "Input folder: $INPUT_FOLDER"
    echo "Output folder: $OUTPUT_FOLDER"
    echo ""
    sbt "run $INPUT_FOLDER $OUTPUT_FOLDER"
    echo ""
    echo "Malicious packages processing completed!"
    echo ""
fi

echo "========================================"
echo "ALL PROCESSING COMPLETED!"
echo "========================================"