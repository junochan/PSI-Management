# 运行时镜像：不将 jar 打入镜像，由 compose 挂载宿主 target（Maven 包目录）与 data。
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh \
  && mkdir -p /app/data /app/packages

# 与 application.yml 中附件、向量缓存路径一致（绝对路径，避免工作目录歧义）
ENV JAR_FILE=psi-manage-system-backend-1.0.0.jar \
    JAVA_OPTS="-Xms256m -Xmx1024m" \
    SERVER_PORT=8080 \
    APP_ATTACHMENT_DIR=/app/data \
    APP_EMBEDDING_STORE_DIR=/app/data/image-embeddings \
    LOGGING_FILE_NAME=/app/data/logs/smart-ims.log

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
