# ==========================================
# STAGE 1: BUILD MÃ NGUỒN (Thay thế máy Host)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: CHẠY ỨNG DỤNG (Tối ưu dung lượng)
# ==========================================
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]