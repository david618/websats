FROM tomcat:8.0.46-jre8-alpine

COPY ./target/websats-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/websats.war


