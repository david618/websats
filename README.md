# websats simulator

The simulator outputs satellite positions, tracks, or footprints for approximately 700 satellites based on starting point (Celestrack ephemeris) and a propagation model based on my implementation of math found in "Fundamentals of Astrodynamics" by by Roger R. Bate (Author), Donald D. Mueller (Author), Jerry E. White (Author), William W. Saylor (Author).

While the data is interesting; the purpose of this application is to generate data in various formats to support testing.

## Building

### Install some development software Mac

```
brew install openjdk@17
brew install git
brew install maven
```

### Clone and Install geotools

First the [geotools](https://github.com/david618/geotools) project
```
git clone https://github.com/david618/geotools
cd sat
mvn install
```

### Clone and Install sat

First the [sat](https://github.com/david618/sat) project
```
git clone https://github.com/david618/sat
cd sat
mvn install
```

### Clone and Install websat

Now the [websats](https://github.com/david618/websats)
```
git clone https://github.com/david618/websats
cd websats
mvn install
```

### Build Docker Image

```
TAG=v2.0
docker buildx build --platform=linux/amd64 -t david62243/websats:${TAG} .
docker push david62243/websats:${TAG}
```

## Run Locally using Docker

```
docker run -it --rm -p 8080:8080 david62243/websats:v2.0
```

From browser

```
http://localhost:8080/websats
```

## Deploy on Kubernetes

### Create Namespace

```
NAMESPACE=websats
kubectl create ns ${NAMESPACE}
```

### Create Deployment

```
kubectl -n ${NAMESPACE} apply -f k8s/deployment.yaml
```

### Create LB Service (optional)

Makes the service available from http via IP or DNS.

```
kubectl -n ${NAMESPACE} apply -f k8s/service-lb.yaml
```

The creates a LoadBalancer type service. For AKS this creates a Public IP.  You can access with the IP or set DNS name (e.g. http://websats.westus2.cloudapp.azure.com).

### Create Service using Ingress Controller

Creates a Public IP and configures it to automatically maintain valid SSL certificate using LetsEncrypt.

#### cert-manager

```
helm repo add jetstack https://charts.jetstack.io
helm repo update
```

```
helm install \
  cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.13.1 \
  --set installCRDs=true
```

#### Open Port

To allow LetsEncrypt/Certmanager create a certificate; add port 8089 to the Network Security Group (NSG) in the Managed cluster.

#### Nginx Ingress


https://docs.nginx.com/nginx-ingress-controller/

This places the nginx-controller in the same namespace as websats.

```
helm -n websats install --set controller.ingressClass.name=websats nginx-websats oci://ghcr.io/nginxinc/charts/nginx-ingress
```

Find and update the Public IP:

```
kubectl apply -f k8s/issuer.yaml
kubectl apply -f k8s/ingress.yaml
```

#### Allow http Access

The above configuration forces all requests to https and ssl.

The following creates a NodePort service.

```
kubectl apply -f k8s/service-nodeport.yaml
```

Then in the Managed Cluster allow access from anyone on port 30001.

Then add a Load Balancing rule to allow
- Frontend IP for websats2
- Backend kubernetes (That's the Kubernetes Nodes)
- Configure Port 30001 from Public IP to be be passed to 30001 of the nodes

```
http://websats2.westus2.cloudapp.azure.com:30001/websats/
```

### Update Two Line Elements (TLE)

The accuracy of the satellite positions depends on the currency of the TLE data in /websats/WEB-INF/classes/sats.tle file. There is a bash shell script in websats/scripts folder (update_tle.sh) that can be configured as a crontab task to update the sats.tle.

You can do this manually by downloading the TLE's from [Celestrack](https://www.celestrak.com/NORAD/elements/).  The script downloads several different sets and concatenates them into the one file (sats.tle).

Run the script from the script folder.

```
cd scripts
bash update_tle.sh
```

Check the sats.tle should have repeated lines looking like.

```
SALSAT
1 46495U 20068K   23305.43341904  .00007151  00000+0  45027-3 0  9999
2 46495  97.7859 255.1334 0015682  20.8972 339.2894 15.09392571169767
IXPE
1 49954U 21121A   23304.37853396  .00006299  00000+0  48565-3 0  9990
2 49954   0.2314 295.3817 0009988 114.1419 310.5636 14.95291507103431
```

## Configured WebSocket for ArcGIS

Added a servlet SatStream.java which returns a schema which is expected for the Esri Javascript Client.

For the Stream Servlet I set the path to be /SatStream/subscribe which is defined in the schema.

With these changes I was able to create a Esri Javascript client that consumes the websocket.


wss://websats2.westus2.cloudapp.azure.com/websats/SatStream/subscribe

Returns lines of Json

```
{"geometry":{"x":-1.0068799458653178E7,"y":465.0199682922589,"spatialReference":{"wkid":102100}},"attributes":{"dtg":"2-NOV-2023 0:44:23.69","num":"54244","name":"GALAXY 32 (G-32)","alt":35797.9165006124,"lon":-90.4495644770158,"lat":0.004177345446274193,"timestamp":1698885863688}}
{"geometry":{"x":-1.3393847655426364E7,"y":1760.4615748861313,"spatialReference":{"wkid":102100}},"attributes":{"dtg":"2-NOV-2023 0:44:23.69","num":"54243","name":"GALAXY 31 (G-31)","alt":35793.30253118031,"lon":-120.31898063712774,"lat":0.015814495199646835,"timestamp":1698885863688}}
```



## Added geotools and sats as git submodule

This makes it easier access these projects.  They need to be built before you can build websats.

```
git submodule add  https://github.com/david618/geotools
```

```
git submodule add  https://github.com/david618/sat
```


## Taskfile

Installed task extension for vsCode.

Installed taskfile.  https://taskfile.dev/installation/

Created a couple of tasks.  Perhaps this is better than README's with instructions.






