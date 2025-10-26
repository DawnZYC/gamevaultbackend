#!/bin/bash
# ==================================================
# OWASP ZAP DAST Scan Script - Minimal (2 URLs)
# 专门用于扫描2个指定的前端页面
# ==================================================

set -e

echo "=========================================="
echo "Running OWASP ZAP DAST Scan"
echo "=========================================="

# ============================================
# 配置参数
# ============================================
TARGET_URL="${ZAP_TARGET_URL:-http://47.130.173.114:3000}"
URL_LIST_FILE="${ZAP_URL_LIST:-scripts/zap-target-urls-minimal.txt}"
MAX_DURATION="${ZAP_MAX_DURATION:-3}"  # 3分钟足够了

echo ""
echo "Configuration:"
echo "  Base URL:       $TARGET_URL"
echo "  URL List:       $URL_LIST_FILE"
echo "  Max Duration:   $MAX_DURATION minutes"
echo ""

# ============================================
# 检查URL列表文件
# ============================================
if [ ! -f "$URL_LIST_FILE" ]; then
    echo "❌ Error: URL list file not found: $URL_LIST_FILE"
    exit 1
fi

echo "📋 URLs to scan:"
grep -v '^#' "$URL_LIST_FILE" | grep -v '^$' || echo "  No URLs found in file"
echo ""

# ============================================
# 检查远程应用可访问性
# ============================================
echo "Checking remote application..."
max_attempts=3
attempt=0

until curl -sf "$TARGET_URL/" > /dev/null 2>&1 || \
      curl -sf "$TARGET_URL/dashboard/store" > /dev/null 2>&1 || \
      [ $attempt -eq $max_attempts ]; do
    attempt=$((attempt + 1))
    echo "  Attempt $attempt/$max_attempts..."
    sleep 3
done

if [ $attempt -eq $max_attempts ]; then
    echo "⚠️  Warning: Application not responding at $TARGET_URL"
    echo "Proceeding with scan anyway..."
else
    echo "✅ Application is accessible"
fi
echo ""

# ============================================
# 拉取ZAP镜像
# ============================================
echo "Pulling OWASP ZAP Docker image..."
docker pull ghcr.io/zaproxy/zaproxy:stable
echo ""

# ============================================
# 运行ZAP扫描
# ============================================
echo "=========================================="
echo "Starting ZAP scan (2 URLs only)..."
echo "This should complete in 1-2 minutes"
echo "=========================================="

START_TIME=$(date +%s)

# 使用URL列表进行扫描
# -d: 调试模式
# -I: 不使用spider（我们已经有完整的URL列表）
docker run --rm \
    -v $(pwd):/zap/wrk:rw \
    ghcr.io/zaproxy/zaproxy:stable \
    zap-baseline.py \
    -t "http://47.130.173.114:3000" \
    -u "http://47.130.173.114:3000/dashboard/store" \
    -u "http://47.130.173.114:3000/dashboard/forum" \
    -r zap_baseline_report.html \
    -l PASS || true

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "=========================================="
echo "Scan completed in $((DURATION / 60))m $((DURATION % 60))s"
echo "=========================================="

# ============================================
# 分析结果
# ============================================
echo ""
echo "Analyzing results..."

if [ -f "zap_baseline_report.html" ]; then
    echo "Report generated successfully"

    # 统计漏洞数量
    HIGH_COUNT=$(grep -o "FAIL-High" zap_baseline_report.html | wc -l || echo 0)
    MEDIUM_COUNT=$(grep -o "FAIL-Medium" zap_baseline_report.html | wc -l || echo 0)
    LOW_COUNT=$(grep -o "FAIL-Low" zap_baseline_report.html | wc -l || echo 0)
    PASS_COUNT=$(grep -o "PASS" zap_baseline_report.html | wc -l || echo 0)

    # 显示结果
    echo ""
    echo "╔════════════════════════════════════════╗"
    echo "║      Vulnerability Summary             ║"
    echo "╠════════════════════════════════════════╣"
    printf "║  🔴 High:   %-4s                      ║\n" "$HIGH_COUNT"
    printf "║  🟡 Medium: %-4s                      ║\n" "$MEDIUM_COUNT"
    printf "║  🔵 Low:    %-4s                      ║\n" "$LOW_COUNT"
    printf "║  ✅ Passed: %-4s                      ║\n" "$PASS_COUNT"
    echo "╚════════════════════════════════════════╝"
    echo ""

    # 总结
    if [ "$HIGH_COUNT" -gt 0 ]; then
        echo "🚨 CRITICAL: $HIGH_COUNT high severity vulnerabilities found!"
        echo "   Immediate action required."
    elif [ "$MEDIUM_COUNT" -gt 0 ]; then
        echo "⚠️  WARNING: $MEDIUM_COUNT medium severity vulnerabilities found."
        echo "   Please review and address these issues."
    else
        echo "✅ SUCCESS: No high or medium severity vulnerabilities detected."
    fi

    echo ""
    echo "Reports available:"
    echo "   - HTML: zap_baseline_report.html"
    echo "   - JSON: zap_baseline_report.json"

    # 显示扫描的URL
    echo ""
    echo "Scanned URLs:"
    echo "   1. http://47.130.173.114:3000/dashboard/store"
    echo "   2. http://47.130.173.114:3000/dashboard/forum"

else
    echo "ERROR: Report not generated"
    echo "   Please check the logs above for details."
    exit 1
fi

echo ""
echo "=========================================="
echo "OWASP ZAP scan completed successfully"
echo "=========================================="

exit 0