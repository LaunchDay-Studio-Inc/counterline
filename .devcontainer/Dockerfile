FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

RUN apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    build-essential \
    clang \
    g++ \
    make \
    cmake \
    ninja-build \
    git \
    curl \
    wget \
    unzip \
    jq \
    rsync \
    python3 \
    python3-pip \
    python3-venv \
    sqlite3 \
    shellcheck \
  && rm -rf /var/lib/apt/lists/*

