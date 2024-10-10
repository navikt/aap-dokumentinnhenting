FROM eclipse-temurin:21-jre-alpine
ENV LANG="nb_NO.UTF-8"
ENV LC_ALL="nb_NO.UTF-8"
ENV TZ="Europe/Oslo"
RUN apk --update --no-cache add libstdc++
COPY /app/build/libs/app-all.jar app.jar
CMD ["java", "-XX:ActiveProcessorCount=2", "-jar", "app.jar"]