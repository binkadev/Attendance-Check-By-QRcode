# ==========================================
# STAGE 1: BUILD MÃ NGUỒN (Thay thế máy Host)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy file cấu hình maven và toàn bộ source code vào container
COPY pom.xml .
COPY src ./src

# Tự động tải thư viện và build ra file .jar (Bỏ qua khâu test)
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: CHẠY ỨNG DỤNG (Tối ưu dung lượng)
# ==========================================
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Chỉ copy đúng file .jar đã được build thành công từ STAGE 1 sang
COPY --from=builder /app/target/*.jar app.jar

# Lệnh khởi chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]