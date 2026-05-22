FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
	&& addgroup -S app \
	&& adduser -S app -G app

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

ENV TZ=Asia/Ho_Chi_Minh

USER app
EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_TOOL_OPTIONS -jar app.jar"]
