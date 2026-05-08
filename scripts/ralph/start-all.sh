#!/usr/bin/env bash
# scripts/ralph/start-all.sh
# US-012: 本地把整套 RAG 栈拉起来——先校验 docker compose 里的基础设施
# (nacos/db/chromadb/minio) 是 healthy，然后后台启动 rag-ai-service (uvicorn)
# + 4 个 Java 业务模块 fat jar，最后等每个服务端口可连。
#
# 前置：
#   - docker compose -f docker-compose.dev.yml up -d 已经把基础设施拉起来
#   - 先跑 scripts/ralph/build-java.sh 把 4 个 jar 打出来
#   - rag-ai-service/.env 已经根据 .env.example 配好（至少 OPENAI_API_KEY）
#
# 产物：
#   .run/ 目录：每个服务一个 .pid + .log，方便 tail -f 调试 / kill $(cat *.pid) 收尾
#
# 停服：
#   for p in .run/*.pid; do kill "$(cat "$p")" 2>/dev/null; done

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
RUN_DIR="${REPO_ROOT}/.run"
mkdir -p "${RUN_DIR}"

echo "[start-all] repo root: ${REPO_ROOT}"
echo "[start-all] pid/log dir: ${RUN_DIR}"

# ---------------------------------------------------------------------------
# 1. 基础设施 health 检查
# ---------------------------------------------------------------------------
COMPOSE_FILE="${REPO_ROOT}/docker-compose.dev.yml"
if [[ ! -f "${COMPOSE_FILE}" ]]; then
    echo "[start-all] FATAL: ${COMPOSE_FILE} not found" >&2
    exit 1
fi

echo
echo "[start-all] 1/5 verifying docker compose infra (nacos/db/chromadb/minio) ..."
docker compose -f "${COMPOSE_FILE}" ps
# 简单 grep：这些容器必须出现且状态包含 Up
for svc in nacos db chromadb minio; do
    if ! docker compose -f "${COMPOSE_FILE}" ps "${svc}" 2>/dev/null | tail -n +2 | grep -qE 'Up'; then
        echo "[start-all] FATAL: docker compose service '${svc}' not Up. Please run: docker compose -f docker-compose.dev.yml up -d" >&2
        exit 1
    fi
done
echo "[start-all]   infra OK: nacos / db / chromadb / minio all Up"

# ---------------------------------------------------------------------------
# 2. 启动 rag-ai-service (uvicorn, port 8000)
# ---------------------------------------------------------------------------
echo
echo "[start-all] 2/5 starting rag-ai-service (uvicorn :8000)"
AI_PID_FILE="${RUN_DIR}/rag-ai-service.pid"
AI_LOG_FILE="${RUN_DIR}/rag-ai-service.log"
(
    cd "${REPO_ROOT}/rag-ai-service"
    # 用 uv run 起 uvicorn；--app-dir 直接指向当前目录，靠 PYTHONPATH 找 app.main
    nohup uv run python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 \
        > "${AI_LOG_FILE}" 2>&1 &
    echo $! > "${AI_PID_FILE}"
)
echo "[start-all]   pid=$(cat "${AI_PID_FILE}") log=${AI_LOG_FILE}"

# ---------------------------------------------------------------------------
# 3. 启动 4 个 Java 服务
# ---------------------------------------------------------------------------
start_java() {
    local module="$1"
    local port="$2"
    local jar
    jar="$(ls "${REPO_ROOT}/${module}/target/${module}-"*.jar 2>/dev/null \
        | grep -vE '(sources|javadoc|\.original)$' | head -n1 || true)"

    if [[ -z "${jar}" ]]; then
        echo "[start-all] FATAL: ${module} fat jar not found under target/. Run scripts/ralph/build-java.sh first." >&2
        exit 1
    fi

    local pid_file="${RUN_DIR}/${module}.pid"
    local log_file="${RUN_DIR}/${module}.log"
    nohup java -jar "${jar}" > "${log_file}" 2>&1 &
    echo $! > "${pid_file}"
    echo "[start-all]   ${module} :${port}  pid=$(cat "${pid_file}")  log=${log_file}"
}

echo
echo "[start-all] 3/5 starting 4 Java services"
start_java rag-user-service      8081
start_java rag-knowledge-service 8082
start_java rag-document-service  8083
start_java rag-gateway           8080

# ---------------------------------------------------------------------------
# 4. 等所有端口可连
# ---------------------------------------------------------------------------
wait_port() {
    local name="$1"
    local port="$2"
    local timeout_s="${3:-90}"
    local start
    start="$(date +%s)"

    while :; do
        if command -v nc >/dev/null 2>&1 && nc -z 127.0.0.1 "${port}" 2>/dev/null; then
            echo "[start-all]   ${name} :${port}  READY ($(($(date +%s) - start))s)"
            return 0
        fi
        # 退化方案：用 bash 的 /dev/tcp 探活
        if bash -c "echo > /dev/tcp/127.0.0.1/${port}" 2>/dev/null; then
            echo "[start-all]   ${name} :${port}  READY ($(($(date +%s) - start))s)"
            return 0
        fi
        if (( $(date +%s) - start > timeout_s )); then
            echo "[start-all] FATAL: ${name} :${port} did not come up in ${timeout_s}s. See ${RUN_DIR}/${name}.log" >&2
            return 1
        fi
        sleep 2
    done
}

echo
echo "[start-all] 4/5 waiting for ports to accept connections (timeout 90s each)"
wait_port rag-ai-service      8000
wait_port rag-user-service      8081
wait_port rag-knowledge-service 8082
wait_port rag-document-service  8083
wait_port rag-gateway           8080

# ---------------------------------------------------------------------------
# 5. 打印服务发现提示
# ---------------------------------------------------------------------------
echo
echo "[start-all] 5/5 all services listening. 下一步建议："
echo "  - bash ${SCRIPT_DIR}/check-nacos.sh     # 校验 5 个服务都注册到 Nacos"
echo "  - bash ${SCRIPT_DIR}/e2e-smoke.sh       # 端到端走一遍 (US-013 产物)"
echo "  - tail -f ${RUN_DIR}/*.log"
echo "  - kill \$(cat ${RUN_DIR}/*.pid)          # 收尾"
