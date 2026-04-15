#!/usr/bin/env bash
# Docker Compose 启动 PSI-Management 后端（及 compose 中的 MySQL）
# 依赖：Docker、Docker Compose v2；后端需已打包 jar 至 psi-manage-system-backend/target/
#
# 用法：
#   ./scripts/docker-compose-deploy.sh           # 使用默认环境变量
#   ./scripts/docker-compose-deploy.sh --build # 构建镜像后启动
# 配置：复制 scripts/docker-compose.env.example 为 scripts/docker-compose.env
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT}"

ENV_FILE="${SCRIPT_DIR}/docker-compose.env"
COMPOSE_ARGS=(-f "${ROOT}/docker-compose.yml")
if [[ -f "${ENV_FILE}" ]]; then
  COMPOSE_ARGS+=(--env-file "${ENV_FILE}")
fi

DEFAULT_JAR="${ROOT}/psi-manage-system-backend/target/psi-manage-system-backend-1.0.0.jar"
if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck source=/dev/null
  set -a
  source "${ENV_FILE}"
  set +a
fi

JAR_FILE_NAME="${JAR_FILE:-psi-manage-system-backend-1.0.0.jar}"
JAR_DIR_RESOLVED="${JAR_DIR:-./psi-manage-system-backend/target}"
# 将相对路径解析为绝对路径，便于检查
if [[ "${JAR_DIR_RESOLVED}" != /* ]]; then
  JAR_DIR_RESOLVED="${ROOT}/${JAR_DIR_RESOLVED#./}"
fi
JAR_PATH="${JAR_DIR_RESOLVED}/${JAR_FILE_NAME}"

if [[ ! -f "${JAR_PATH}" ]]; then
  echo "错误: 未找到后端 JAR: ${JAR_PATH}" >&2
  echo "请先执行: (cd \"${ROOT}/psi-manage-system-backend\" && mvn -DskipTests package)" >&2
  exit 1
fi

echo ">>> docker compose 启动（仓库根: ${ROOT}）"
exec docker compose "${COMPOSE_ARGS[@]}" up -d "$@"
