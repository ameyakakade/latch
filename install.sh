#!/usr/bin/env sh
set -e

# ==============================================================================
# Latch Universal Linux Installer Script for latch.vinnovateit.com
# Usage: curl -fsSL https://latch.vinnovateit.com/install.sh | sudo sh
# ==============================================================================

BASE_URL="https://latch.vinnovateit.com"
APP_NAME="latch"

echo "==== Installing Latch Desktop by VinnovateIT ===="

if command -v dnf >/dev/null 2>&1; then
    echo "--> Detected Fedora / RHEL system (dnf)"
    sudo dnf install -y "${BASE_URL}/fedora/latch-latest.rpm"
elif command -v yum >/dev/null 2>&1; then
    echo "--> Detected RHEL / CentOS system (yum)"
    sudo yum install -y "${BASE_URL}/fedora/latch-latest.rpm"
elif command -v apt-get >/dev/null 2>&1; then
    echo "--> Detected Debian / Ubuntu system (apt)"
    TMP_DEB=$(mktemp /tmp/latch-XXXXXX.deb)
    curl -fsSL "${BASE_URL}/debian/latch-latest.deb" -o "$TMP_DEB"
    sudo apt-get update -qq || true
    sudo apt-get install -y "$TMP_DEB"
    rm -f "$TMP_DEB"
elif command -v pacman >/dev/null 2>&1; then
    echo "--> Detected Arch Linux system (pacman/tar)"
    TMP_TAR=$(mktemp /tmp/latch-XXXXXX.tar.gz)
    curl -fsSL "${BASE_URL}/tar/latch-latest.tar.gz" -o "$TMP_TAR"
    sudo mkdir -p /opt/Latch
    sudo tar -xzf "$TMP_TAR" -C /opt/
    sudo ln -sf /opt/Latch/bin/Latch /usr/local/bin/latch
    rm -f "$TMP_TAR"
else
    echo "--> Generic Linux distribution: installing tarball"
    TMP_TAR=$(mktemp /tmp/latch-XXXXXX.tar.gz)
    curl -fsSL "${BASE_URL}/tar/latch-latest.tar.gz" -o "$TMP_TAR"
    sudo mkdir -p /opt/Latch
    sudo tar -xzf "$TMP_TAR" -C /opt/
    sudo ln -sf /opt/Latch/bin/Latch /usr/local/bin/latch
    rm -f "$TMP_TAR"
fi

echo "==== Latch Desktop successfully installed! ===="
echo "Launch from your application menu or run 'latch' in terminal."
