FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/*.jar
WORKDIR /app/deploy
RUN addgroup -S fn && adduser -S docmgmt -G fn
COPY --chown=fn:docmgmt ${JAR_FILE} FilenetDocumentManagement-lts.jar
USER docmgmt
ENTRYPOINT ["java","-Dspring.config.location=file:///app/shared_files/application_shared.properties,classpath:/application.properties","-jar","./FilenetDocumentManagement-lts.jar"]