FROM openjdk:11-slim
EXPOSE 8208
COPY build/libs/*.jar app.jar
CMD ["java", "-Xmx768m", "-Xms768m", "-jar", "app.jar"]
