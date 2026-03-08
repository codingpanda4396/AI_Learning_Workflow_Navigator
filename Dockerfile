# syntax=docker/dockerfile:1
# ============================================================
# 多阶段构建：backend 在 backend/ 目录下
# 构建上下文：仓库根目录（docker build 在根目录执行）
# ============================================================

# 阶段1：Maven 构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 复制 pom 和源码（来自 backend/）
COPY backend/pom.xml .
COPY backend/src ./src

RUN mvn -q -DskipTests package

# 阶段2：运行时
FROM eclipse-temurin:17-jre
WORKDIR /app

# jar 名称与 pom.xml 中 artifactId + version 一致
COPY --from=builder /build/target/learning-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

