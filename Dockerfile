FROM eclipse-temurin:17-jdk
ARG JAR_FILE=target/*.jar
RUN groupadd -g 1234 fn && useradd -m -u 1234 -g fn docmgmt
USER docmgmt
WORKDIR /deploy/bin
COPY --chown=fn:docmgmt ${JAR_FILE} ./FilenetDocumentManagement-lts.jar
EXPOSE 8072
ENTRYPOINT ["java","-Dspring.config.location=file:///springbootservices/shared_files/application_shared.properties,classpath:/application.properties","-jar","./FilenetDocumentManagement-lts.jar"]