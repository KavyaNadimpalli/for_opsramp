FROM adoptopenjdk/openjdk11:alpine-jre
ARG JAR_FILE=target/syslog-jar-with-dependencies.jar

RUN mkdir -p /opt/app /opt/app/config /opt/app/lib /opt/opsramp/bin

WORKDIR /opt/app
COPY ./info.json .
COPY ./config.json ./config/config.json
COPY ./dependencies.json ./config/dependencies.json
COPY ${JAR_FILE} ./lib/syslog.jar

RUN cp /bin/* /opt/opsramp/bin
RUN rm -rf /bin/

ENTRYPOINT ["java","-XX:VMOptionsFile=/opt/app/config/jvm.properties","-jar","./lib/syslog.jar"]
