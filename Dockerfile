FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/seminar.jar /seminar/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/seminar/app.jar"]
