# websats
Web Application that uses sat Application to create ephemeris for Satellites.

## Building

### Install some development software

*** Linux ***
<pre>
# yum -y install epel-release
# yum -y install git
# yum -y install java-1.8.0-openjdk
# yum -y install maven
</pre>

*** Windows ***
- Install [Java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 
  -- Java SE Development Kit Windows x64.  
  -- Accept Defaults for installs.
  -- Open Windows Advanced System Settings; Environemnt Variables
  -- Create a new System Variable; Variable name: JAVA_HOME, Variable value: C:\Program Files\Java\jdk1.8.0_121.  
  -- NOTE: The last number should match the version of Java you installed. You can use Explorer and navigate to the folder. then cut and paste the value.
- Install [Git](https://git-scm.com/download/win) Download 64-bit Git for Windows Setup. Defaults for all installation dialogs. 
  -- You can use defaults during install
- Install [Maven](https://maven.apache.org/download.cgi)
  -- Download the Binary zip archive and unzip (I usually put it in C:\) 
  -- Open Windows Advanced System Settings; Environemnt Variables
  -- Edit the Path variable and append Maven to the path. Append a semicolon followed by path to Maven bin folder (e.g. ;C:\apache-maven-3.5.0\bin).  Cut and paste is helpful.

### Clone and Install Github Projects

First the [sat](https://github.com/david618/sat) project
<pre>
$ git clone https://github.com/david618/sat
$ cd sat
$ mvn install
</pre>

Now the [websats](https://github.com/david618/websats)
<pre>
$ cd 
$ git clone https://github.com/david618/websats
$ cd websats
$ mvn install 
</pre>

Both should end with "BUILD SUCCESS"

To build the docker image.  

**NOTE** User must have permissions to create docker images.

<pre>
mvn clean install -Ddocker.skip=false
</pre>


## Deploy

### Deploy on Tomcat

Download and install [Tomcat](http://apache.org/dist//tomcat/tomcat-8/)

*** Windows ***
Download the zip file. Unzip.
- Downloaded zip (e.g. apache-tomcat-8.0.45.zip)
- Unzip to folder (e.g. C:\ Tomcat will be in  C:\apache-tomcat-8.0.45)


*** Linux ***
Download the tar.gz. 

<pre>
$ curl -O http://apache.org/dist//tomcat/tomcat-8/v8.0.46/bin/apache-tomcat-8.0.46.tar.gz 
$ md5sum apache-tomcat-8.0.46.tar.gz
47b60f7bf757cedcb59c519ea3dcede2  apache-tomcat-8.0.46.tar.gz
$ tar xvzf apache-tomcat-8.0.46.tar.gz 
</pre>

From the target folder of websats project you cloned earlier; copy the war file (e.g. websats-1.0-SNAPSHOT.war) to the "webapps" folder of tomcat. I usually rename the war file to "websats.war"

Start Tomcat (Scripts are in tomcat bin folder).

The websats will autodeploy.

You should now be able to access websats.  (e.g.  http://localhost:8080/websats/) 

### Deploy Docker

Run Docker 

<pre>
docker run --rm -p 8080:8080 david62243/websats
</pre>

You can then access via http://localhost:8080/websats.

### Deploy on DC/OS

Deloy "Single Container"

Service
- SERVICE ID: /websats
- CONTAINER IMAGE: david62243/websats
- CPUs: 1
- Memory (MiB): 1000
- COMMAND: `/usr/local/tomcat/bin/startup.sh; tail -f /etc/motd`

Networking
- NETWORK TYPE: Virtual Network: dcos
- CONTAINER PORT: 8080
- SERVICE ENDPOINT NAME: tomcat
- PORT MAPPING: Enabled Checked
- ASSIGN AUTOMATICALLY: Checked
- PROTOCOL: TCP
- ENABLE LOAD BLANACED SERVICE ADDRESS: Checked

Health Checks
- PROTOCOL: HTTP
- SERVICE ENDPOINT: tomcat
- With this configuration if Tomcat stops; Marathon will restart the webapps Mesos Application

Run the service.

From within the cluster you can access via websats.marathon.l4lb.thisdcos.directory:8080/websats.


### Update Two Line Elements (TLE)

The accuracy of the satellite positions depends on the currency of the TLE data in &lt;webapps&gt;/websats/WEB-INF/classes/sats.tle file. There is a bash shell script in websats/scripts folder (update_tle.sh) that can be configured as a crontab task to update the sats.tle.

You can do this manually by downloading the TLE's from [Celestrack](https://www.celestrak.com/NORAD/elements/).  The script downloads several different sets and concatenates them into the one file (sats.tle).

## Setting up Proxy

### Apache

Added these lines to proxy.conf 

<pre>
ProxyPreserveHost On

ProxyPass /websats/SatStream ws://boot:8080/websats/SatStream
ProxyPass /websats http://boot:8080/websats
</pre>

For DC/OS you could run Apache on the Public Agent(s) and websats would now be accessible via the "public" DNS Name or IP.  

**NOTE** You must add the ws proxy before the http proxy.

### Nginx

In /etc/nginx/nginx.conf I added these items to the server block.

<pre>
        location /websats/SatStream/subscribe {
            proxy_pass http://localhost:9090/websats/SatStream/subscribe;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }

        location /websats {
                proxy_set_header X-Forwarded-Host $host;
                proxy_set_header X-Forwarded-Server $host;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header Host $host;
                proxy_pass http://localhost:9090/websats;
        }

</pre>

I got http working; have not tried to configure https yet.

I also noted an issue with the Tomcat version I was using. The Stream Service would fail if more than one client subscribed to the stream. I changed to [Glassfish](https://glassfish.java.net/) or [Jetty](http://www.eclipse.org/jetty/download.html) and I did not have this problem. I think the latest version of Tomcat works too.

If selinux is enabled (default); then you'll need to add a rule to allow nginx to connect to localhost:8080.

<pre>
setsebool -P httpd_can_network_connect true
</pre>


## Configure WebSocket for ArcGIS 

Added a servlet SatStream.java which returns a schema which is expected for the Esri Javascript Client.

For the Stream Servlet I set the path to be /SatStream/subscribe which is defined in the schema. 

With these changes I was able to create a Esri Javascript client that consumes the websocket.  







