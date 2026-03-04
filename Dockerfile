FROM maven:3.8.5-eclipse-temurin-17-alpine as build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:17-alpine as production


WORKDIR /app

ARG JAR_FILE=target/*.jar

COPY --from=build /app/target/*.jar app.jar

EXPOSE 9999

ENTRYPOINT ["java", "-jar", "app.jar"]

# docker build --tag sv-center:v0.0.01 .
# docker build --tag apb.registry-img.com/api-uat/newcore/cbs-uploads-service:v1.0.01 .
# docker push apb.registry-img.com/api-uat/newcore/cbs-uploads-service:v1.0.01


# demo cbs registry
# docker build --tag cbs.registry-img.local/production/cbs-uploads-service:v1.0.01 .
# docker push cbs.registry-img.local/production/cbs-uploads-service:v1.0.01


# docker build --tag apb.registry-img.com/api/newcore/cbs-uploads-service-prd:v0.0.03 .
# docker push apb.registry-img.com/api/newcore/cbs-uploads-service-prd:v0.0.03