FROM eclipse-temurin:21.0.12_8-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21.0.12_8-jre
RUN addgroup --system banking && adduser --system --ingroup banking banking
WORKDIR /app
COPY --from=build /workspace/target/online-banking-0.0.1-SNAPSHOT.jar app.jar
USER banking
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
