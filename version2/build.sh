#!/usr/bin/env bash
# BMI 体质评估与预测系统 v2.0 —— 模块化编译并运行 (Linux / macOS)
# 说明：JDK 17 + JavaFX 17。项目为模块化工程（src/module-info.java），
#       所有 lib 下的 jar（JavaFX / PostgreSQL / webcam 等）均置于模块路径（module path）。
set -e
cd "$(dirname "$0")"

OUT=out
rm -rf "$OUT"
mkdir -p "$OUT"

# 构造模块路径：lib 下全部 jar（自动跳过与当前平台不匹配的 JavaFX 包，避免重复模块）
OS=$(uname -s)
MP=""
for j in lib/*.jar; do
  case "$j" in
    *-linux.jar) [[ "$OS" == *Linux* ]] && MP="$MP$j:" ;;
    *-win.jar)   [[ "$OS" != *Linux* ]] && MP="$MP$j:" ;;
    *) MP="$MP$j:" ;;
  esac
done
MP=${MP%:}

echo "[1/3] 编译（模块化）..."
javac --module-path "$MP" -d "$OUT" $(find src -name '*.java')

echo "[2/3] 复制样式表..."
cp -f src/style.css "$OUT"/style.css

echo "[3/3] 启动系统..."
java --module-path "$MP:$OUT" -m com.bmi.app/com.bmi.App
