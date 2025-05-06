FROM eclipse-temurin:21
LABEL maintainer="rayeaster@hotmail.com"
WORKDIR app
COPY target/trxmgr-202505.jar trxmgr-202505.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "trxmgr-202505.jar"]