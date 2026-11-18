# 多阶段构建：Maven 打包 → JRE 运行
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# 先单独拷贝 pom 并预拉依赖，利用 Docker 层缓存（src 不变时不重复下载）
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# 镜像内不放任何密钥，全部走环境变量（见 docker-compose.prod.yml）
COPY --from=build /app/target/fitness-ai-backend-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=default \
    APP_KNOWLEDGE_DIR=/app/knowledge-files
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
