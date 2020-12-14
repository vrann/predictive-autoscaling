FROM openjdk:11.0.8-jdk

RUN apt-get update && apt-get install curl -y

COPY ./target/scala-2.12/LinearRegression-assembly-*.jar /application.jar

ENV PROMETHEUS_HOST localhost
ENV PROMETHEUS_PORT 9090
ENV WEB_PORT 8080

CMD java \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.port=1099 \
    -Dcom.sun.management.jmxremote.rmi.port=1099 \
    -Djava.rmi.server.hostname=127.0.0.1 \
 -jar application.jar