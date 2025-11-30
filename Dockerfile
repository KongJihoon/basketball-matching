###############
# 1. Build Stage
###############
FROM gradle:8.7.0-jdk17 AS build

# 작업 디렉토리 설정
WORKDIR /app

# 소스 복사
COPY . /app

# gradlew 권한 설정 및 CRLF 문제 해결
RUN chmod +x ./gradlew
RUN sed -i 's/\r$//' ./gradlew

# Gradle 빌드 (테스트는 포함/제외 선택 가능)
# 테스트 포함:   RUN ./gradlew clean build --no-daemon
# 테스트 제외:   RUN ./gradlew clean build -x test --no-daemon
RUN ./gradlew clean build -x test --no-daemon

###############
# 2. Runtime Stage
###############
FROM FROM eclipse-temurin:17-jre-slim

# 보안 업데이트 (경량화 + 안전)
RUN apt-get update && \
    apt-get upgrade -y --only-upgrade --no-install-recommends && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 비루트 사용자 생성
RUN addgroup --system javauser && adduser --system --ingroup javauser javauser

# 작업 디렉토리
WORKDIR /app

# JAR 복사
COPY --from=build /app/build/libs/*.jar /app/basketball.jar

# 로그 디렉토리 준비
RUN mkdir -p /app/logs && chown -R javauser:javauser /app

# 비루트 계정으로 실행
USER javauser

# 앱 실행 포트
EXPOSE 8080

# EntryPoint
CMD ["java", "-jar", "/app/basketball.jar"]
