FROM maven:3.8.5-eclipse-temurin-17-alpine as build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

COPY src ./src

RUN mvn clean package -DskipTests

ENV PROFILE_ACTIVES=prd

FROM eclipse-temurin:17-alpine as production


WORKDIR /app

ARG JAR_FILE=target/*.war

COPY --from=build /app/target/*.war app.war

EXPOSE 9999

ENTRYPOINT ["java", "-jar", "app.war"]

# docker build --tag sv-center:v0.0.01 .
# docker build --tag apb.registry-img.com/api-uat/newcore/cbs-uploads-service:v1.0.01 .
# docker push apb.registry-img.com/api-uat/newcore/cbs-uploads-service:v1.0.01


# demo cbs registry
# docker build --tag cbs.registry-img.local/production/cbs-uploads-service:v1.0.01 .
# docker push cbs.registry-img.local/production/cbs-uploads-service:v1.0.01


# docker build --tag apb.registry-img.com/api/newcore/cbs-uploads-service-prd:v0.0.03 .
# docker push apb.registry-img.com/api/newcore/cbs-uploads-service-prd:v0.0.03