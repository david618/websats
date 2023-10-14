#docker build -t david62243/websats:v1.0 .
#docker push david62243/websats:v1.0 
FROM tomcat:8.0.46-jre8-alpine

COPY ./target/websats-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/websats.war
COPY index.jsp /usr/local/tomcat/webapps/ROOT/index.jsp



