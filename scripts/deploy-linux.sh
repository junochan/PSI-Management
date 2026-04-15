#!/usr/bin/env bash
# PSI-Management 前后端 Linux 部署脚本
# 用法: ./deploy-linux.sh {build|start|stop|restart|status}
# 配置: 复制 deploy.env.example 为 deploy.env，可单独设置 BACKEND_PORT、FRONTEND_PORT
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEPLOY_STATE="${SCRIPT_DIR}/.deploy"
PID_DIR="${DEPLOY_STATE}/pids"
LOG_DIR="${DEPLOY_STATE}/logs"

BACKEND_PID_FILE="${PID_DIR}/backend.pid"
FRONTEND_PID_FILE="${PID_DIR}/frontend.pid"
BACKEND_LOG="${LOG_DIR}/backend.log"
FRONTEND_LOG="${LOG_DIR}/frontend.log"

# 默认 jar 版本与 pom.xml 中 version 一致
DEFAULT_JAR_NAME="psi-manage-system-backend-1.0.0.jar"

if [[ -f "${SCRIPT_DIR}/deploy.env" ]]; then
  # shellcheck source=/dev/null
  source "${SCRIPT_DIR}/deploy.env"
fi

PROJECT_ROOT="${PROJECT_ROOT:-$(cd "${SCRIPT_DIR}/.." && pwd)}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-4173}"
BACKEND_JAR="${BACKEND_JAR:-${PROJECT_ROOT}/psi-manage-system-backend/target/${DEFAULT_JAR_NAME}}"
FRONTEND_DIR="${FRONTEND_DIR:-${PROJECT_ROOT}/psi-manage-system-front}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx1024m}"

ensure_dirs() {
  mkdir -p "${PID_DIR}" "${LOG_DIR}"
}

die() {
  echo "错误: $*" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "未找到命令: $1"
}

is_running_pid() {
  local pid="$1"
  [[ -n "$pid" ]] && [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null
}

cmd_build() {
  require_cmd mvn
  require_cmd npm

  echo ">>> 构建后端 (Maven)..."
  (cd "${PROJECT_ROOT}/psi-manage-system-backend" && mvn -DskipTests package)

  echo ">>> 构建前端 (npm)..."
  (cd "${FRONTEND_DIR}" && npm ci && npm run build)

  echo ">>> 构建完成。"
  echo "    后端: ${BACKEND_JAR}"
  echo "    前端: ${FRONTEND_DIR}/dist"
}

start_backend() {
  [[ -f "${BACKEND_JAR}" ]] || die "找不到后端 jar: ${BACKEND_JAR}（先执行 build 或设置 BACKEND_JAR）"
  command -v java >/dev/null 2>&1 || die "未安装 Java，请设置 JAVA_HOME 并将 java 加入 PATH"

  if [[ -f "${BACKEND_PID_FILE}" ]] && is_running_pid "$(cat "${BACKEND_PID_FILE}")"; then
    echo "后端已在运行 (PID $(cat "${BACKEND_PID_FILE}"))，端口 ${BACKEND_PORT}"
    return 0
  fi

  echo ">>> 启动后端: 端口 ${BACKEND_PORT}，日志 ${BACKEND_LOG}"
  nohup java ${JAVA_OPTS} -jar "${BACKEND_JAR}" --server.port="${BACKEND_PORT}" >>"${BACKEND_LOG}" 2>&1 &
  echo $! >"${BACKEND_PID_FILE}"
  sleep 2
  if ! is_running_pid "$(cat "${BACKEND_PID_FILE}")"; then
    die "后端启动失败，请查看 ${BACKEND_LOG}"
  fi
  echo "    后端 PID $(cat "${BACKEND_PID_FILE}")"
}

start_frontend() {
  [[ -d "${FRONTEND_DIR}/dist" ]] || die "找不到 ${FRONTEND_DIR}/dist（先执行 build）"
  require_cmd npm

  if [[ -f "${FRONTEND_PID_FILE}" ]] && is_running_pid "$(cat "${FRONTEND_PID_FILE}")"; then
    echo "前端已在运行 (PID $(cat "${FRONTEND_PID_FILE}"))，端口 ${FRONTEND_PORT}"
    return 0
  fi

  echo ">>> 启动前端 (vite preview): 端口 ${FRONTEND_PORT}，代理 /api -> 127.0.0.1:${BACKEND_PORT}"
  (
    cd "${FRONTEND_DIR}"
    export BACKEND_PORT FRONTEND_PORT
    nohup npm run preview >>"${FRONTEND_LOG}" 2>&1 &
    echo $! >"${FRONTEND_PID_FILE}"
  )
  sleep 2
  if ! is_running_pid "$(cat "${FRONTEND_PID_FILE}")"; then
    die "前端启动失败，请查看 ${FRONTEND_LOG}"
  fi
  echo "    前端 PID $(cat "${FRONTEND_PID_FILE}")"
}

cmd_start() {
  ensure_dirs
  start_backend
  start_frontend
  echo ""
  echo "访问地址: http://<本机IP>:${FRONTEND_PORT}"
  echo "API 经前端同域 /api 转发至后端 ${BACKEND_PORT}（context-path: /api）"
}

stop_one() {
  local name="$1"
  local pidfile="$2"
  if [[ ! -f "${pidfile}" ]]; then
    echo "${name}: 无 PID 文件，跳过"
    return 0
  fi
  local pid
  pid="$(cat "${pidfile}")"
  if is_running_pid "${pid}"; then
    echo ">>> 停止 ${name} (PID ${pid})"
    kill "${pid}" 2>/dev/null || true
    local n=0
    while is_running_pid "${pid}" && [[ $n -lt 30 ]]; do
      sleep 1
      n=$((n + 1))
    done
    if is_running_pid "${pid}"; then
      echo "    强制结束 ${pid}"
      kill -9 "${pid}" 2>/dev/null || true
    fi
  else
    echo "${name}: PID ${pid} 未运行，清理 PID 文件"
  fi
  rm -f "${pidfile}"
}

cmd_stop() {
  stop_one "前端" "${FRONTEND_PID_FILE}"
  stop_one "后端" "${BACKEND_PID_FILE}"
  echo "已停止。"
}

cmd_status() {
  echo "配置: BACKEND_PORT=${BACKEND_PORT} FRONTEND_PORT=${FRONTEND_PORT}"
  echo "后端 jar: ${BACKEND_JAR}"
  echo "前端目录: ${FRONTEND_DIR}"
  echo ""
  if [[ -f "${BACKEND_PID_FILE}" ]] && is_running_pid "$(cat "${BACKEND_PID_FILE}")"; then
    echo "后端: 运行中 PID $(cat "${BACKEND_PID_FILE}")"
  else
    echo "后端: 未运行"
  fi
  if [[ -f "${FRONTEND_PID_FILE}" ]] && is_running_pid "$(cat "${FRONTEND_PID_FILE}")"; then
    echo "前端: 运行中 PID $(cat "${FRONTEND_PID_FILE}")"
  else
    echo "前端: 未运行"
  fi
}

usage() {
  echo "PSI-Management Linux 部署脚本"
  echo "配置: scripts/deploy.env（可复制 deploy.env.example）"
  echo ""
  echo "命令:"
  echo "  build    编译后端 jar 与前端 dist"
  echo "  start    以后台方式启动后端 + 前端（需先 build）"
  echo "  stop     停止前后端"
  echo "  restart  stop 后 start"
  echo "  status   查看进程与端口配置"
}

main() {
  local sub="${1:-}"
  case "${sub}" in
    build)   cmd_build ;;
    start)   cmd_start ;;
    stop)    cmd_stop ;;
    restart) cmd_stop; cmd_start ;;
    status)  cmd_status ;;
    ""|-h|--help) usage; exit 0 ;;
    *) usage; die "未知命令: ${sub}" ;;
  esac
}

main "$@"
