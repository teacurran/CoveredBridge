####
# debian needed for protoc to work. it isn't compiled for alpine.
FROM maven:3.9-amazoncorretto-21-debian

RUN mkdir -p /app

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'

RUN echo \
    "<settings xmlns='http://maven.apache.org/SETTINGS/1.0.0\' \
    xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' \
    xsi:schemaLocation='http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd'> \
        <localRepository>/app/.m2/repository</localRepository> \
        <interactiveMode>true</interactiveMode> \
        <usePluginRegistry>false</usePluginRegistry> \
        <offline>false</offline> \
    </settings>" \
    > /usr/share/maven/conf/settings.xml

WORKDIR /app

EXPOSE 8080

ENV AB_JOLOKIA_OFF=""
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

CMD ["mvn", "-Dquarkus.http.host=0.0.0.0", "quarkus:dev"]

