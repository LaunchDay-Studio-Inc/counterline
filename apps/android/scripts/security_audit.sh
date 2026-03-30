#!/usr/bin/env bash
# security_audit.sh — Local security audit for CounterLine Android
#
# Run from the apps/android directory:
#   bash scripts/security_audit.sh
#
# Exit code 0 = all checks pass
# Exit code 1 = one or more checks failed

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PASS=0
FAIL=0

green() { printf '\033[0;32m%s\033[0m\n' "$1"; }
red()   { printf '\033[0;31m%s\033[0m\n' "$1"; }
check() {
    if eval "$2"; then
        green "  ✓ $1"
        PASS=$((PASS + 1))
    else
        red "  ✗ $1"
        FAIL=$((FAIL + 1))
    fi
}

echo "╔══════════════════════════════════════════╗"
echo "║  CounterLine Android Security Audit      ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── 1. Exported component audit ──────────────────────────────────────────────
echo "1. Exported Component Audit"

check "Only launcher activity is exported" \
    '
    cd "$ANDROID_ROOT"
    EXPORTED=$(grep -r "android:exported=\"true\"" --include="AndroidManifest.xml" app/src/ core/ feature/ 2>/dev/null | wc -l)
    [ "$EXPORTED" -le 1 ]
    '

check "No exported services, receivers, or providers" \
    '
    cd "$ANDROID_ROOT"
    # Check that no <service>, <receiver>, or <provider> tags have exported=true
    ! grep -rzlP "<(service|receiver|provider)[^>]*exported=\"true\"" --include="AndroidManifest.xml" app/src/ core/*/src/ feature/*/src/ 2>/dev/null
    '

echo ""

# ── 2. Release build flags ───────────────────────────────────────────────────
echo "2. Release Build Flags"

check "Minification enabled for release" \
    'grep -q "isMinifyEnabled = true" "$ANDROID_ROOT/app/build.gradle.kts"'

check "Resource shrinking enabled for release" \
    'grep -q "isShrinkResources = true" "$ANDROID_ROOT/app/build.gradle.kts"'

check "No hardcoded signing passwords" \
    '! grep -En "(storePassword|keyPassword)\s*=\s*\"" "$ANDROID_ROOT/app/build.gradle.kts"'

echo ""

# ── 3. Network security ─────────────────────────────────────────────────────
echo "3. Network Security"

check "Cleartext traffic disabled in manifest" \
    'grep -q "usesCleartextTraffic=\"false\"" "$ANDROID_ROOT/app/src/main/AndroidManifest.xml"'

check "Network security config present" \
    'test -f "$ANDROID_ROOT/app/src/main/res/xml/network_security_config.xml"'

check "No INTERNET permission declared" \
    '! grep -rn "android.permission.INTERNET" --include="AndroidManifest.xml" "$ANDROID_ROOT/app/" "$ANDROID_ROOT/core/" "$ANDROID_ROOT/feature/" 2>/dev/null'

echo ""

# ── 4. Secret scanning ──────────────────────────────────────────────────────
echo "4. Secret Scanning"

check "No keystore files in repo" \
    '! find "$ANDROID_ROOT" -name "*.jks" -o -name "*.keystore" -o -name "*.p12" 2>/dev/null | grep -q .'

check "No private keys in source" \
    '! grep -rn "BEGIN.*PRIVATE KEY" --include="*.kt" --include="*.java" --include="*.xml" --include="*.json" "$ANDROID_ROOT/app/" "$ANDROID_ROOT/core/" "$ANDROID_ROOT/feature/" 2>/dev/null'

check "No hardcoded API keys" \
    '! grep -rnE "AKIA[0-9A-Z]{16}" --include="*.kt" --include="*.java" --include="*.xml" --include="*.json" --include="*.properties" "$ANDROID_ROOT/" 2>/dev/null'

echo ""

# ── 5. Data extraction rules ────────────────────────────────────────────────
echo "5. Backup/Data Safety"

check "Data extraction rules for Android 12+" \
    'test -f "$ANDROID_ROOT/app/src/main/res/xml/data_extraction_rules.xml"'

check "Backup rules present" \
    'test -f "$ANDROID_ROOT/app/src/main/res/xml/backup_rules.xml"'

echo ""

# ── 6. Security documentation ───────────────────────────────────────────────
echo "6. Security Documentation"

check "Threat model exists" \
    'test -f "$ANDROID_ROOT/docs/threat-model.md"'

check "Privacy documentation exists" \
    'test -f "$ANDROID_ROOT/docs/privacy-and-observability.md"'

check "Signing documentation exists" \
    'test -f "$ANDROID_ROOT/docs/signing.md"'

echo ""

# ── Summary ──────────────────────────────────────────────────────────────────
echo "═══════════════════════════════════════════"
echo "Results: $PASS passed, $FAIL failed"
echo "═══════════════════════════════════════════"

if [ "$FAIL" -gt 0 ]; then
    red "Security audit FAILED"
    exit 1
else
    green "Security audit PASSED"
    exit 0
fi
