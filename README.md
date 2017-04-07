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
- Install [Git](https://git-scm.com/download/win) 
  -- You can use defaults during install
- Install [Maven](https://maven.apache.org/download.cgi)
  -- Unzip the Binary zip archive and unzip
  -- Update your Path (e.g.  D:\apache-maven-3.3.9\bin)

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

## Deploy

Download and install [Tomcat](http://apache.org/dist//tomcat/tomcat-8/)

*** Windows ***
Download the zip file. Unzip.

*** Linux ***
Download the tar.gz. 

<pre>
$ curl -O http://apache.org/dist//tomcat/tomcat-8/v8.0.43/bin/apache-tomcat-8.0.43.tar.gz 
$ tar xvzf apache-tomcat-8.0.43.tar.gz 
</pre>

From the target folder of webapps copy the war file (e.g. websats-1.0-SNAPSHOT.war) to the "webapps" folder of tomcat. I usually rename the war file to "websats.war"

Start Tomcat (Scripts are in tomcat bin folder).

The websats will autodeploy.

You should now be able to access websats.  (e.g.  http://localhost:8080/websats/) 

The accuracy of the satellite positions depends on the currency of the TLE data in <webapps>/websats/WEB-INF/classes/sats.tle file. There is a bash shell script in websats/scripts folder (update_tle.sh) that can be configured as a crontab task to update the sats.tle.

You can do this manually by downloading the TLE's from [Celestrack](https://www.celestrak.com/NORAD/elements/).  The script downloads several different sets and concatenates them into the one file (sats.tle).





