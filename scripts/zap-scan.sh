#!/bin/bash
# ==========================================
# OWASP ZAP DAST Scan Script
# Based on Workshop 8 - DAST Scan
# ==========================================

set -e

echo "=========================================="
echo "Running OWASP ZAP DAST Scan..."
echo "=========================================="

# Default target URL
TARGET_URL="${ZAP_TARGET_URL:-http://47.130.173.114:8080}"

echo "Target URL: $TARGET_URL"
echo "Waiting for application to be ready..."

# Wait for application to be ready
max_attempts=30
attempt=0
until curl -sf "$TARGET_URL/actuator/health" > /dev/null 2>&1 || [ $attempt -eq $max_attempts ]; do
    attempt=$((attempt + 1))
    echo "Attempt $attempt/$max_attempts: Waiting for application..."
    sleep 10
done

if [ $attempt -eq $max_attempts ]; then
    echo "⚠ Warning: Application not responding, proceeding with scan anyway..."
fi

# Pull ZAP Docker image
echo "Pulling OWASP ZAP Docker image..."
docker pull ghcr.io/zaproxy/zaproxy:stable

# Run ZAP baseline scan
echo "Running ZAP baseline scan..."
docker run --rm \
    --network host \
    -v $(pwd):/zap/wrk:rw \
    ghcr.io/zaproxy/zaproxy:stable \
    zap-baseline.py \
    -t "$TARGET_URL" \
    -r zap_baseline_report.html \
    -l PASS || true

# Check if report was generated
if [ -f "zap_baseline_report.html" ]; then
    echo "✓ ZAP scan report generated: zap_baseline_report.html"
else
    echo "ZAP scan report not generated"
fi

echo "=========================================="
echo "OWASP ZAP scan completed"
echo "=========================================="

exit 0
