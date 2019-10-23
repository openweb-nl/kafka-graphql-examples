FROM azul/zulu-openjdk-alpine:11-jre
MAINTAINER Gerard Klijs <gerard@openweb.nl>

ADD target/ge-docker.jar /app.jar

CMD ["java", "-Xmx512m", "-jar", "/app.jar"]