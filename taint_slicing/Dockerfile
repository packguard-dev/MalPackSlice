# Dockerfile for JSCodeSlicing malware testing
FROM eclipse-temurin:21-jdk

# Install SBT
RUN apt-get update && \
    apt-get install -y curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add && \
    apt-get update && \
    apt-get install -y sbt wget unzip && \
    rm -rf /var/lib/apt/lists/*

# Download and install astgen
RUN mkdir -p /app/astgen && \
    cd /app/astgen && \
    wget -q https://github.com/joernio/astgen/releases/download/v3.35.0/astgen-linux && \
    chmod +x astgen-linux && \
    ln -s /app/astgen/astgen-linux /usr/local/bin/astgen

# Set environment variable
ENV ASTGEN_BIN=/app/astgen/astgen-linux

# Set working directory
WORKDIR /app

# Copy project files (excluding malware samples from host)
COPY . .

# Copy malware download script
COPY download-malware.sh /app/download-malware.sh
RUN sed -i 's/\r$//' /app/download-malware.sh
RUN chmod +x /app/download-malware.sh

# Pre-download SBT dependencies
RUN sbt update compile

# Create input/output directories
RUN mkdir -p /app/src/main/resources/input/malware_samples && \
    mkdir -p /app/src/main/resources/output

# Default command: download malware then run the application
CMD ["/bin/bash", "-c", "\
    echo 'Downloading malware samples...' && \
    /app/download-malware.sh && \
    echo 'Running JSCodeSlicing...' && \
    sbt run\
    "]
