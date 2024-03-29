# TAG=v2.0
# docker buildx build --platform=linux/amd64 -t david62243/websats:${TAG} .   # Use this line to complie on ARM based Mac
# docker build -t david62243/websats:${TAG} .
# docker push david62243/websats:${TAG}
# docker run -it --rm -p 8080:8080 david62243/websats:${TAG}
#
# Build stage
#

FROM maven:3.8.5-openjdk-17-slim AS build

COPY sat/src /home/app/sat/src
COPY sat/pom.xml /home/app/sat/pom.xml
RUN mvn -f /home/app/sat/pom.xml install

COPY geotools/src /home/app/geotools/src
COPY geotools/pom.xml /home/app/geotools/pom.xml
RUN mvn -f /home/app/geotools/pom.xml install

COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -DskipTests=true -f /home/app/pom.xml install

#
# Package stage
#
FROM tomcat:9.0.81-jre17-temurin-jammy

COPY --from=build /home/app/target/websats-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/websats.war
COPY index.jsp /usr/local/tomcat/webapps/ROOT/index.jsp

RUN useradd -u 1000 -ms /bin/bash ubuntu
RUN chown -R ubuntu.ubuntu /usr/local/tomcat

USER ubuntu
WORKDIR /home/ubuntu

EXPOSE 8080