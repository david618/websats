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

## Deploy

Download and install [Tomcat](http://apache.org/dist//tomcat/tomcat-8/)

*** Windows ***
Download the zip file. Unzip.
- Downloaded zip (e.g. apache-tomcat-8.0.43.zip)
- Unzip to folder (e.g. C:\ Tomcat will be in  C:\apache-tomcat-8.0.43)


*** Linux ***
Download the tar.gz. 

<pre>
$ curl -O http://apache.org/dist//tomcat/tomcat-8/v8.0.43/bin/apache-tomcat-8.0.43.tar.gz 
$ tar xvzf apache-tomcat-8.0.43.tar.gz 
</pre>

From the target folder of websats project you cloned earlier; copy the war file (e.g. websats-1.0-SNAPSHOT.war) to the "webapps" folder of tomcat. I usually rename the war file to "websats.war"

Start Tomcat (Scripts are in tomcat bin folder).

The websats will autodeploy.

You should now be able to access websats.  (e.g.  http://localhost:8080/websats/) 

The accuracy of the satellite positions depends on the currency of the TLE data in &lt;webapps&gt;/websats/WEB-INF/classes/sats.tle file. There is a bash shell script in websats/scripts folder (update_tle.sh) that can be configured as a crontab task to update the sats.tle.

You can do this manually by downloading the TLE's from [Celestrack](https://www.celestrak.com/NORAD/elements/).  The script downloads several different sets and concatenates them into the one file (sats.tle).





