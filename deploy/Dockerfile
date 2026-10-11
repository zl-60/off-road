FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY ${MODULE}/target/${MODULE}-1.0.0.jar /app/application.jar
COPY frontend/ /app/frontend/
ENV SERVER_ADDRESS=0.0.0.0
ENTRYPOINT ["java", "-Xms48m", "-Xmx192m", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/application.jar", "--spring.config.name=outdoor", "--spring.profiles.active=middleware"]
