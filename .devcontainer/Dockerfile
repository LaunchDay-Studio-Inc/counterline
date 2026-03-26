FROM mcr.microsoft.com/devcontainers/base:ubuntu-24.04

# ── system packages ──────────────────────────────────────────────────────────
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
    pipx \
    sqlite3 \
    shellcheck \
    graphviz \
    librsvg2-bin \
    pandoc \
  && rm -rf /var/lib/apt/lists/*

# ── tectonic PDF engine ──────────────────────────────────────────────────────
# Tectonic is a self-contained TeX engine that downloads font/format bundles on
# first use.  We install via pre-built binary so the Dockerfile stays fast.
RUN curl -fsSL https://drop-sh.fullyjustified.net | sh \
  && mv tectonic /usr/local/bin/tectonic \
  && tectonic --version

# ── Quarto CLI ───────────────────────────────────────────────────────────────
ARG QUARTO_VERSION=1.6.43
RUN curl -fsSL -o /tmp/quarto.deb \
      "https://github.com/quarto-dev/quarto-cli/releases/download/v${QUARTO_VERSION}/quarto-${QUARTO_VERSION}-linux-amd64.deb" \
  && dpkg -i /tmp/quarto.deb \
  && rm /tmp/quarto.deb \
  && quarto --version

