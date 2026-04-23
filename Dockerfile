FROM eclipse-temurin:17-jdk-alpine AS common-builder
WORKDIR /workspace

COPY common/gradle gradle
COPY common/gradlew gradlew
COPY common/build.gradle build.gradle
COPY common/settings.gradle settings.gradle
COPY common/src src

RUN chmod +x gradlew && ./gradlew publishToMavenLocal -x test --no-daemon

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

COPY --from=common-builder /root/.m2 /root/.m2

COPY ranking-service/gradle gradle
COPY ranking-service/gradlew gradlew
COPY ranking-service/build.gradle build.gradle
COPY ranking-service/settings.gradle settings.gradle
COPY ranking-service/src src

RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]