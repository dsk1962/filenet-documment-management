FROM openjdk:17-jdk-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} FilenetDocumentManagement-lts.jar
EXPOSE  8072
ENTRYPOINT ["java","-Dspring.config.location=file:///app/shared_files/application_shared.properties,classpath:/application.properties'","-jar","/FilenetDocumentManagement-lts.jar"]