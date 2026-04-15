#!/bin/sh
set -e
mkdir -p /app/data/image-embeddings /app/data/logs

JAR_PATH="/app/packages/${JAR_FILE}"
if [ ! -f "$JAR_PATH" ]; then
  echo "错误: 未找到 JAR: $JAR_PATH"
  echo "请在宿主机先打包: (cd psi-manage-system-backend && mvn -DskipTests package)"
  echo "并确认已将 target 目录挂载到容器的 /app/packages（见 docker-compose.yml）。"
  exit 1
fi

exec java ${JAVA_OPTS} -jar "$JAR_PATH" --server.port="${SERVER_PORT}" "$@"
