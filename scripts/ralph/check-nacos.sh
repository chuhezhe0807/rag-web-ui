#!/usr/bin/env bash
# scripts/ralph/check-nacos.sh
# US-012: 从 Nacos open API 拉 5 个业务服务的 instance list，打印
#   ✓ <service>  (host:port, healthy=<true|false>)
# 每个服务至少一个 healthy=true 实例才算通过；任何一个服务 0 实例 或全部
# healthy=false → 脚本最后退出 1。
#
# Nacos API（兼容 v2.x 的 v1 路径）：
#   GET /nacos/v1/ns/instance/list?serviceName=<name>&namespaceId=&groupName=DEFAULT_GROUP
#
# 环境变量（可选覆盖）：
#   NACOS_ADDR   默认 127.0.0.1:8848
#   NACOS_NS     默认空串（public）
#   NACOS_GROUP  默认 DEFAULT_GROUP

set -uo pipefail

NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
NACOS_NS="${NACOS_NS:-}"
NACOS_GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"

# Nacos 2.x 开启了鉴权后 naming open API 需要 accessToken；先 POST /auth/login
# 拿 token，拿不到就继续（旧版本未开鉴权时 naming API 也能直连）。
ACCESS_TOKEN="$(
    curl -sS --max-time 5 \
        -X POST "http://${NACOS_ADDR}/nacos/v1/auth/login" \
        --data-urlencode "username=${NACOS_USERNAME}" \
        --data-urlencode "password=${NACOS_PASSWORD}" \
        2>/dev/null \
    | python3 -c 'import json,sys
try:
    print(json.load(sys.stdin).get("accessToken",""))
except Exception:
    print("")
' 2>/dev/null || true
)"

SERVICES=(
    rag-ai-service
    rag-user-service
    rag-knowledge-service
    rag-document-service
    rag-gateway
)

# 终端支持颜色时用绿色 ✓ / 红色 ✗，否则纯文本
if [[ -t 1 ]]; then
    OK="$(printf '\033[32m✓\033[0m')"
    FAIL="$(printf '\033[31m✗\033[0m')"
else
    OK="[OK]"
    FAIL="[FAIL]"
fi

echo "[check-nacos] target: http://${NACOS_ADDR}   group=${NACOS_GROUP}   namespace='${NACOS_NS}'"
echo

overall=0
for svc in "${SERVICES[@]}"; do
    url="http://${NACOS_ADDR}/nacos/v1/ns/instance/list?serviceName=${svc}&groupName=${NACOS_GROUP}&namespaceId=${NACOS_NS}"
    if [[ -n "${ACCESS_TOKEN}" ]]; then
        url="${url}&accessToken=${ACCESS_TOKEN}"
    fi
    body="$(curl -sS --max-time 5 "${url}" || true)"

    if [[ -z "${body}" ]]; then
        echo "  ${FAIL} ${svc}  (nacos unreachable or empty response)"
        overall=1
        continue
    fi

    # 解析 instance count 与 healthy 状态：优先用 python3，避免强依赖 jq。
    # body 通过 env 传进 python；python 代码整段用 shell 单引号包，内部只用
    # 双引号，不使用 f-string（f-string 里不能出现 \" 转义，与 shell quoting 冲突）
    summary="$(
        NACOS_BODY="${body}" python3 -c '
import json, os, sys
body = os.environ.get("NACOS_BODY", "")
try:
    data = json.loads(body)
except Exception as e:
    print("__PARSE_ERR__:" + str(e))
    sys.exit(0)
hosts = data.get("hosts") or []
if not hosts:
    print("NO_INSTANCE")
    sys.exit(0)
for h in hosts:
    print("{}:{} healthy={} enabled={} weight={}".format(
        h.get("ip"), h.get("port"), h.get("healthy"),
        h.get("enabled"), h.get("weight")
    ))
'
    )"
    if [[ -z "${summary}" ]]; then
        summary="__PARSE_ERR__"
    fi

    if [[ "${summary}" == __PARSE_ERR__* ]]; then
        echo "  ${FAIL} ${svc}  (failed to parse nacos response: ${summary#__PARSE_ERR__})"
        overall=1
        continue
    fi
    if [[ "${summary}" == "NO_INSTANCE" ]]; then
        echo "  ${FAIL} ${svc}  (0 instances registered)"
        overall=1
        continue
    fi

    # 只要有一行 healthy=True 就算整体 healthy
    if grep -q 'healthy=True' <<<"${summary}"; then
        echo "  ${OK} ${svc}"
        while IFS= read -r line; do
            echo "        ${line}"
        done <<<"${summary}"
    else
        echo "  ${FAIL} ${svc}  (all instances unhealthy)"
        while IFS= read -r line; do
            echo "        ${line}"
        done <<<"${summary}"
        overall=1
    fi
done

echo
if (( overall == 0 )); then
    echo "[check-nacos] all 5 services healthy in Nacos"
else
    echo "[check-nacos] some services NOT healthy; see above"
fi
exit "${overall}"
