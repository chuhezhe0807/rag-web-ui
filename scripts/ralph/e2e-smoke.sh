#!/usr/bin/env bash
# scripts/ralph/e2e-smoke.sh
# US-013: 从网关 http://127.0.0.1:8080 出发跑一次完整 RAG 流水线。
#
#   1) POST /api/ai/auth/register     注册新用户
#   2) POST /api/ai/auth/token        拿 access token
#   3) POST /knowledge-base           创建 KB，拿 kb_id
#   4) PUT  /knowledge-base/{kb}/documents/upload 上传 fixtures/sample.md，拿 upload_id
#   5) POST /api/ai/documents/process 触发处理，拿 task_id
#   6) GET  /api/ai/documents/process/{task_id} 轮询直到 completed (timeout 120s)
#   7) POST /api/ai/chat + /chat/{id}/messages 问一个只有 sample.md 里才有的
#       问题，断言回答里引用到 fixture 里的专有名词
#
# 任何一步失败脚本立即退出 1。成功要求 7 步全过。
#
# 前置：docker compose up -d → build-java.sh → start-all.sh → check-nacos.sh 全绿。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
FIXTURE="${REPO_ROOT}/fixtures/sample.md"

GATEWAY="${GATEWAY:-http://127.0.0.1:8080}"
AUTH_HEADER_NAME="${AUTH_HEADER_NAME:-Authorization}"

# 随机用户名 + 符合后端 UserRegisterDTO 正则 `(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{8,}` 的密码
SUFFIX="$(date +%s)$$"
USERNAME="smoke_${SUFFIX}"
PASSWORD="Ralph2026_${SUFFIX}"
EMAIL="smoke_${SUFFIX}@example.com"

# fixture 里独有的专有名词，只要回答里出现其一就认为 retrieval 打通了
EXPECTED_KEYWORDS_REGEX="Zephyr-7|Calipso-Mirror-Omega|Nadia Okonkwo|Tenerife Meson|muon-clustered"

step() {
    echo
    echo "=============================================================="
    echo "[smoke] $*"
    echo "=============================================================="
}

json_get() {
    # $1 = JSON body, $2 = dotted path like "data.accessToken" or "data.tasks.0.taskId"
    JSON_BODY="$1" JSON_PATH="$2" python3 -c '
import json, os, sys
body = os.environ.get("JSON_BODY", "")
path = os.environ.get("JSON_PATH", "")
try:
    node = json.loads(body)
except Exception as e:
    sys.stderr.write("json parse error: " + str(e) + "\n")
    sys.exit(2)
for key in path.split("."):
    if isinstance(node, list):
        node = node[int(key)]
    elif isinstance(node, dict):
        if key not in node:
            sys.exit(3)
        node = node[key]
    else:
        sys.exit(3)
print(node)
'
}

require_file() {
    if [[ ! -f "$1" ]]; then
        echo "[smoke] FATAL: fixture file not found: $1" >&2
        exit 1
    fi
}

require_file "${FIXTURE}"

# ---------------------------------------------------------------------------
# 1) register
# ---------------------------------------------------------------------------
step "1/7 register ${USERNAME}"
REG_BODY="$(curl -sS --max-time 15 -X POST "${GATEWAY}/api/ai/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\",\"email\":\"${EMAIL}\"}")"
echo "[smoke]   register response: ${REG_BODY}"
REG_CODE="$(json_get "${REG_BODY}" code || echo "")"
if [[ "${REG_CODE}" != "200" ]]; then
    echo "[smoke] FATAL: register did not return code=200" >&2
    exit 1
fi

# ---------------------------------------------------------------------------
# 2) login (form-urlencoded, /api/ai/auth/token)
# ---------------------------------------------------------------------------
step "2/7 login"
LOGIN_BODY="$(curl -sS --max-time 15 -X POST "${GATEWAY}/api/ai/auth/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "username=${USERNAME}" \
    --data-urlencode "password=${PASSWORD}")"
echo "[smoke]   login response: ${LOGIN_BODY}"
TOKEN="$(json_get "${LOGIN_BODY}" data.accessToken || true)"
if [[ -z "${TOKEN}" || "${TOKEN}" == "None" ]]; then
    echo "[smoke] FATAL: could not read data.accessToken from login response" >&2
    exit 1
fi
echo "[smoke]   token acquired (${#TOKEN} chars)"

AUTH_H=("-H" "${AUTH_HEADER_NAME}: ${TOKEN}")

# ---------------------------------------------------------------------------
# 3) create KB
# ---------------------------------------------------------------------------
step "3/7 create knowledge base"
KB_BODY="$(curl -sS --max-time 15 -X POST "${GATEWAY}/knowledge-base" \
    "${AUTH_H[@]}" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"smoke-${SUFFIX}\",\"description\":\"ralph e2e smoke knowledge base\"}")"
echo "[smoke]   KB create response: ${KB_BODY}"
KB_ID="$(json_get "${KB_BODY}" data.id || true)"
if [[ -z "${KB_ID}" || "${KB_ID}" == "None" ]]; then
    echo "[smoke] FATAL: could not read data.id from KB create response" >&2
    exit 1
fi
echo "[smoke]   kb_id=${KB_ID}"

# ---------------------------------------------------------------------------
# 4) upload fixture (PUT multipart; controller expects @RequestParam("file") List)
# ---------------------------------------------------------------------------
step "4/7 upload fixture (${FIXTURE})"
UP_BODY="$(curl -sS --max-time 30 -X PUT "${GATEWAY}/knowledge-base/${KB_ID}/documents/upload" \
    "${AUTH_H[@]}" \
    -F "file=@${FIXTURE};type=text/markdown")"
echo "[smoke]   upload response: ${UP_BODY}"
UPLOAD_ID="$(json_get "${UP_BODY}" data.0.uploadId || true)"
if [[ -z "${UPLOAD_ID}" || "${UPLOAD_ID}" == "None" ]]; then
    echo "[smoke] FATAL: could not read data[0].uploadId from upload response" >&2
    exit 1
fi
echo "[smoke]   upload_id=${UPLOAD_ID}"

# ---------------------------------------------------------------------------
# 5) kick document processing
#    ProcessDocDTO: { knowledgeId, processDocItemDTOList:[{documentUploadId,skipProcess}] }
# ---------------------------------------------------------------------------
step "5/7 process document"
PROC_BODY="$(curl -sS --max-time 30 -X POST "${GATEWAY}/api/ai/documents/process" \
    "${AUTH_H[@]}" \
    -H "Content-Type: application/json" \
    -d "{\"knowledgeId\":${KB_ID},\"processDocItemDTOList\":[{\"documentUploadId\":${UPLOAD_ID},\"skipProcess\":false}]}")"
echo "[smoke]   process response: ${PROC_BODY}"
TASK_ID="$(json_get "${PROC_BODY}" data.tasks.0.taskId || true)"
if [[ -z "${TASK_ID}" || "${TASK_ID}" == "None" ]]; then
    echo "[smoke] FATAL: could not read data.tasks[0].taskId from process response" >&2
    exit 1
fi
echo "[smoke]   task_id=${TASK_ID}"

# ---------------------------------------------------------------------------
# 6) poll status (timeout 120s)
# ---------------------------------------------------------------------------
step "6/7 poll processing status"
poll_timeout=120
poll_start="$(date +%s)"
while :; do
    STATUS_BODY="$(curl -sS --max-time 10 -X GET "${GATEWAY}/api/ai/documents/process/${TASK_ID}" \
        "${AUTH_H[@]}" || true)"
    STATUS="$(json_get "${STATUS_BODY}" data || echo '')"
    # 后端返回可能是 completed / processing / pending / failed（小写）
    status_lc="$(printf '%s' "${STATUS}" | tr '[:upper:]' '[:lower:]')"
    elapsed="$(( $(date +%s) - poll_start ))"
    echo "[smoke]   task ${TASK_ID} status=${STATUS} (${elapsed}s elapsed)"
    case "${status_lc}" in
        completed)
            echo "[smoke]   processing done"
            break
            ;;
        failed)
            echo "[smoke] FATAL: processing failed. full body: ${STATUS_BODY}" >&2
            exit 1
            ;;
    esac
    if (( $(date +%s) - poll_start > poll_timeout )); then
        echo "[smoke] FATAL: processing did not complete within ${poll_timeout}s (last status=${STATUS})" >&2
        exit 1
    fi
    sleep 3
done

# ---------------------------------------------------------------------------
# 7) chat with a question grounded in the fixture
# ---------------------------------------------------------------------------
step "7/7 chat with question grounded in fixture"
CHAT_BODY="$(curl -sS --max-time 15 -X POST "${GATEWAY}/api/ai/chat" \
    "${AUTH_H[@]}" \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"smoke-${SUFFIX}\",\"knowledge_base_ids\":[${KB_ID}]}")"
echo "[smoke]   chat create response: ${CHAT_BODY}"
CHAT_ID="$(json_get "${CHAT_BODY}" id || true)"
if [[ -z "${CHAT_ID}" || "${CHAT_ID}" == "None" ]]; then
    echo "[smoke] FATAL: could not read id from chat create response" >&2
    exit 1
fi

QUESTION='Who leads the Zephyr-7 Observatory science team, and what is the primary mirror called?'
ANSWER="$(curl -sS --max-time 90 -X POST "${GATEWAY}/api/ai/chat/${CHAT_ID}/messages" \
    "${AUTH_H[@]}" \
    -H "Content-Type: application/json" \
    -d "{\"messages\":[{\"role\":\"user\",\"content\":\"${QUESTION}\"}]}")"
echo "[smoke]   chat answer (raw):"
printf '%s\n' "${ANSWER}" | sed 's/^/[smoke]     /'

if grep -qE "${EXPECTED_KEYWORDS_REGEX}" <<<"${ANSWER}"; then
    echo "[smoke]   ✓ answer references fixture-only keyword (retrieval end-to-end working)"
else
    echo "[smoke] FATAL: chat answer did not reference any of: ${EXPECTED_KEYWORDS_REGEX}" >&2
    exit 1
fi

echo
echo "[smoke] ALL 7 STEPS PASSED"
exit 0
