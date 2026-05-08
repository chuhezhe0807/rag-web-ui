#!/usr/bin/env bash
# scripts/ralph/build-java.sh
# US-011: 按依赖顺序构建 6 个 Java 模块，任何一步失败即退出非 0。
#
# 顺序：
#   rag-common            (pom → 被 rag-common-service 依赖)
#   rag-common-service    (pom → 被所有业务模块依赖)
#   rag-user-service      (runnable Spring Boot jar)
#   rag-document-service  (runnable Spring Boot jar)
#   rag-knowledge-service (runnable Spring Boot jar)
#   rag-gateway           (runnable Spring Boot jar)
#
# common / common-service 用 `clean install`（要装进本地 Maven 仓库供后续模块
# 解析）；四个业务模块用 `clean package`（只产 fat jar）。PRD US-011 acceptance
# criteria 1 里列的就是这个命令序列，脚本等价实现。

set -euo pipefail

# 脚本所在目录的上两级就是项目根（scripts/ralph/ -> <repo>/）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "[build-java] repo root: ${REPO_ROOT}"

run() {
    local module="$1"
    local goal="$2"   # install | package
    local module_dir="${REPO_ROOT}/${module}"

    if [[ ! -d "${module_dir}" ]]; then
        echo "[build-java] FATAL: module directory not found: ${module_dir}" >&2
        exit 1
    fi

    echo
    echo "======================================================================"
    echo "[build-java] ${module}: ./mvnw clean ${goal} -DskipTests"
    echo "======================================================================"
    (cd "${module_dir}" && ./mvnw clean "${goal}" -DskipTests)
}

run rag-common            install
run rag-common-service    install
run rag-user-service      package
run rag-document-service  package
run rag-knowledge-service package
run rag-gateway           package

echo
echo "[build-java] 所有模块构建成功。产物位置："
for m in rag-user-service rag-document-service rag-knowledge-service rag-gateway; do
    jar_path="$(ls "${REPO_ROOT}/${m}/target/"*.jar 2>/dev/null | grep -vE '(sources|javadoc|\.original)$' | head -n1 || true)"
    if [[ -n "${jar_path}" ]]; then
        echo "  - ${jar_path}"
    else
        echo "  - (missing) ${m}/target/*.jar"
        exit 1
    fi
done
