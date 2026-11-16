#!/bin/bash
# Usage Monitor 简体中文版 —— 一键汉化并构建
#
# 用法：双击本文件。
#   - 源码已经汉化过：重新生成中文词典并编译，用新应用替换旧应用。
#   - 源码是更新后的原版：先套用汉化文件，再生成词典、编译、替换应用。
#
# 汉化文件与中文词典都在「汉化资源」目录里；版本号写在「版本.txt」。

set -uo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$DIR/源码"
RES="$DIR/汉化资源"
OVERLAY="$RES/覆盖文件"
TOOLS="$RES/翻译工具"
APP_OUT="$DIR/Usage Monitor.app"
BACKUP_DIR="$DIR/备份"
LOG="$DIR/汉化构建日志.txt"
STAMP="$(date +%Y%m%d-%H%M%S)"
VERSION="$(cat "$DIR/版本.txt" 2>/dev/null || cat "$RES/版本.txt" 2>/dev/null || echo '42.3.0-beta.9')"
VERSION="${VERSION#v}"
VERSION="$(echo "$VERSION" | tr -d '[:space:]')"

mkdir -p "$DIR"
exec > >(tee "$LOG") 2>&1

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
warn() { printf '\033[33m⚠ %s\033[0m\n' "$*"; }
ok()   { printf '\033[32m✓ %s\033[0m\n' "$*"; }
fail() {
  printf '\n\033[31m✗ %s\033[0m\n' "$*"
  printf '日志：%s\n' "$LOG"
  printf '\n按回车关闭此窗口…'
  read -r _ || true
  exit 1
}

say "Usage Monitor 一键汉化并构建（版本 ${VERSION}）"

[ -d "$SRC" ]      || fail "找不到源码目录：$SRC"
[ -d "$OVERLAY" ]  || fail "找不到汉化资源：$OVERLAY"
[ -d "$TOOLS" ]    || fail "找不到翻译工具：$TOOLS"

# ── Java ────────────────────────────────────────────────────────────────────
JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null)" || true
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  fail "需要 JDK 17。请先安装 JDK 17（当前 /usr/libexec/java_home -v 17 找不到）。"
fi
export JAVA_HOME

# ── Gradle ──────────────────────────────────────────────────────────────────
GRADLE_CMD=()
if [ -x "/tmp/gradle-9.9.1/gradle-9.9.1/bin/gradle" ]; then
  GRADLE_CMD=("/tmp/gradle-9.9.1/gradle-9.9.1/bin/gradle")
elif [ -f "$SRC/gradlew" ]; then
  GRADLE_CMD=(sh "$SRC/gradlew")
elif command -v gradle >/dev/null 2>&1; then
  GRADLE_CMD=("$(command -v gradle)")
fi
[ "${#GRADLE_CMD[@]}" -gt 0 ] || fail "找不到 Gradle。请确认源码里有 gradlew，或把 Gradle 9.9.1 放到 /tmp/gradle-9.9.1。"

# ── 是否已经汉化 ────────────────────────────────────────────────────────────
LOCALIZED=0
if [ -f "$SRC/src/commonMain/kotlin/com/usagemonitor/presentation/ui/ZhTranslations.kt" ] \
   && grep -q 'AppLanguage.ZH' "$SRC/src/commonMain/kotlin/com/usagemonitor/presentation/ui/CommonLabels.kt" 2>/dev/null; then
  LOCALIZED=1
fi

APPLIED_OVERLAY=0
OVERLAY_BACKUP="$RES/覆盖前备份-$STAMP"

if [ "$LOCALIZED" -eq 0 ]; then
  say "检测到未汉化的源码，开始套用汉化文件…"
  warn "如果这是更新后的新版本源码，汉化文件基于 ${VERSION}；编译失败时旧应用不会被替换，可把日志发给我。"
  mkdir -p "$OVERLAY_BACKUP"
  : > "$OVERLAY_BACKUP/新增文件列表.txt"
  APPLIED_OVERLAY=1

  cd "$OVERLAY" || fail "无法进入 $OVERLAY"
  while IFS= read -r rel; do
    rel="${rel#./}"
    if [ -f "$SRC/$rel" ]; then
      mkdir -p "$OVERLAY_BACKUP/$(dirname "$rel")"
      cp -p "$SRC/$rel" "$OVERLAY_BACKUP/$rel"
    else
      printf '%s\n' "$rel" >> "$OVERLAY_BACKUP/新增文件列表.txt"
    fi
    mkdir -p "$SRC/$(dirname "$rel")"
    cp -p "$rel" "$SRC/$rel"
  done < <(find . -type f -name '*.kt')

  mkdir -p "$SRC/tools"
  for f in chinese_ui_translation.py zh-translations.json; do
    if [ -f "$SRC/tools/$f" ]; then
      cp -p "$SRC/tools/$f" "$OVERLAY_BACKUP/$f"
    fi
    cp -p "$TOOLS/$f" "$SRC/tools/$f"
  done
  ok "汉化文件已套用（原文件备份在「汉化资源/覆盖前备份-${STAMP}」）"
else
  ok "源码已经是汉化版，跳过文件覆盖"
fi

# ── 重新生成中文词典 ────────────────────────────────────────────────────────
if [ -f "$SRC/tools/chinese_ui_translation.py" ] && command -v python3 >/dev/null 2>&1; then
  say "重新生成中文词典（新出现的文案会联网翻译，已有的走缓存）…"
  if ! ( cd "$SRC" && python3 tools/chinese_ui_translation.py ); then
    warn "词典生成失败或超时，继续使用源码里已有的 ZhTranslations.kt"
  fi
else
  warn "没有 python3 或词典生成脚本，跳过词典生成"
fi

# ── 编译 ────────────────────────────────────────────────────────────────────
say "开始编译（首次可能需要几分钟，请保持网络畅通）…"
cd "$SRC" || fail "无法进入源码目录"
if ! "${GRADLE_CMD[@]}" createDistributable -PappVersion="$VERSION" --console=plain; then
  if [ "$APPLIED_OVERLAY" -eq 1 ]; then
    say "编译失败，恢复覆盖前的源码…"
    cd "$OVERLAY_BACKUP" || true
    if [ -f "$OVERLAY_BACKUP/新增文件列表.txt" ]; then
      while IFS= read -r rel; do
        [ -n "$rel" ] && rm -f "$SRC/$rel"
      done < "$OVERLAY_BACKUP/新增文件列表.txt"
    fi
    find . -type f -name '*.kt' | while IFS= read -r rel; do
      cp -p "$OVERLAY_BACKUP/$rel" "$SRC/$rel"
    done
    for f in chinese_ui_translation.py zh-translations.json; do
      [ -f "$OVERLAY_BACKUP/$f" ] && cp -p "$OVERLAY_BACKUP/$f" "$SRC/tools/$f"
    done
    ok "源码已恢复"
  fi
  fail "编译失败，旧版应用未被替换。"
fi

NEW_APP="$SRC/build/compose/binaries/main/app/Usage Monitor.app"
[ -d "$NEW_APP" ] || fail "编译结束但找不到产物：$NEW_APP"

# ── 关闭正在运行的应用 ──────────────────────────────────────────────────────
if pgrep -x "Usage Monitor" >/dev/null 2>&1; then
  say "关闭正在运行的 Usage Monitor…"
  osascript -e 'tell application id "com.usagemonitor.app" to quit' >/dev/null 2>&1 || true
  for _ in $(seq 1 20); do
    pgrep -x "Usage Monitor" >/dev/null 2>&1 || break
    sleep 0.25
  done
  if pgrep -x "Usage Monitor" >/dev/null 2>&1; then
    pkill -x "Usage Monitor" >/dev/null 2>&1 || true
    sleep 1
  fi
fi

# ── 替换应用（旧版备份，不删除） ────────────────────────────────────────────
mkdir -p "$BACKUP_DIR"
if [ -d "$APP_OUT" ]; then
  OLD_BACKUP="$BACKUP_DIR/Usage Monitor-$VERSION-$STAMP.app"
  mv "$APP_OUT" "$OLD_BACKUP" || fail "无法备份旧应用：$APP_OUT"
  ok "旧应用已备份到：$OLD_BACKUP"
fi
ditto "$NEW_APP" "$APP_OUT" || fail "复制新应用到 $APP_OUT 失败"
xattr -dr com.apple.quarantine "$APP_OUT" >/dev/null 2>&1 || true

say "完成"
echo "应用：$APP_OUT"
echo "日志：$LOG"
open "$APP_OUT" >/dev/null 2>&1 || true
printf '\n按回车关闭此窗口…'
read -r _ || true
