FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY . .

RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

CMD ["java","-jar","target/studentDetails-0.0.1-SNAPSHOT.jar"]
