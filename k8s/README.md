
# Deploy on Kuberentes

## Build Dependent Projects

```
https://github.com/david618/geotools
https://github.com/david618/sat
```

```
mvn install 
```

### Build websats

```
mvn install 
```

### Build Docker Image

```
docker build -t david62243/websats:v2.0 .
docker push david62243/websats:v2.0
```

## Set Context
```
export KUBECONFIG=/users/davi5017/simulators-vel2023.kubeconfig
```

## Create Namespace

```
NAMESPACE=websats
kubectl create ns ${NAMESPACE}
```

## Create Deployment and LB Service

```
kubectl -n ${NAMESPACE} apply -f k8s/deployment.yaml
kubectl -n ${NAMESPACE} apply -f k8s/service-lb.yaml
```

## Change Azure LB Config

You can set a DNS Name. 

From Azure Portal go to Manage Cluster Resource Group (e.g. MC_simulators_vel2023_westus2)

Find the "Public IP" and select Configuration should have public IP associated with the service.

```
kubectl -n websats  get svc
NAME         TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)        AGE
websats-lb   LoadBalancer   10.0.158.199   20.72.248.141   80:31806/TCP   12s
```

You can set the subdomain (e.g. websats2).  The full DNS will include Azure Region. (e.g. websats2.westus2.cloudapp.azure.com)

The name has to be unique for Region. 



## Create Local Service (optional)

Useful if you want to call the service from within the cluster.

```
kubectl -n ${NAMESPACE} apply -f k8s/service.yaml
```

From within this namespace the service is "websats" from other namespaces "websats.websats".

## Additional Tweaks when using Istio

```
kubectl label namespace simulators istio-injection=enabled

kubectl label namespace simulators istio.io/rev=1-17-2
```

When testing via istio; the websockets were not working.  Some additional configuration is needed.


## Istio and Cert Manager

The goal is to get https working in an automated way for websats, webplanes, and ultimately for Kafka TLS and TLS_SSL.

The Esri team is familiar with istio and cert-manager so this is the logically best solution; however, other solutions might be easier...

### cert-manager 

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

### nginx ingress controller


https://cert-manager.io/docs/tutorials/acme/nginx-ingress/

helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx

```
helm -n websats install websats ingress-nginx/ingress-nginx
```

```
NAME: websats
LAST DEPLOYED: Thu Oct  5 20:15:30 2023
NAMESPACE: default
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
The ingress-nginx controller has been installed.
It may take a few minutes for the LoadBalancer IP to be available.
You can watch the status by running 'kubectl --namespace default get services -o wide -w websats-ingress-nginx-controller'

An example Ingress that makes use of the controller:
  apiVersion: networking.k8s.io/v1
  kind: Ingress
  metadata:
    name: example
    namespace: foo
  spec:
    ingressClassName: nginx
    rules:
      - host: www.example.com
        http:
          paths:
            - pathType: Prefix
              backend:
                service:
                  name: exampleService
                  port:
                    number: 80
              path: /
    # This section is only required if TLS is to be enabled for the Ingress
    tls:
      - hosts:
        - www.example.com
        secretName: example-tls

If TLS is enabled for the Ingress, a Secret containing the certificate and key must also be provided:

  apiVersion: v1
  kind: Secret
  metadata:
    name: example-tls
    namespace: foo
  data:
    tls.crt: <base64 encoded cert>
    tls.key: <base64 encoded key>
  type: kubernetes.io/tls
```

Found and assigned the name: websats2.westus2.cloudapp.azure.com

nginx not working uninstalled and used the following

helm install nginx oci://ghcr.io/nginxinc/charts/nginx-ingress

Now it works

helm pull oci://ghcr.io/nginxinc/charts/nginx-ingress

## Move nginx to websats

This will give websats it's own public IP

```
helm -n websats install --set controller.ingressClass.name=websats nginx-websats oci://ghcr.io/nginxinc/charts/nginx-ingress
```

Find and update the Public IP:

kubectl apply -f k8s/issuer.yaml
kubectl apply -f k8s/ingress.yaml 

**NOTE:** I did open port 8089 in NSG for the cluster; This is the port used by ACME to validate the cert. 

### istio

**NOTE:** The following are notes I took went attempting to use istio for ingress.  Bottom line; installation was much more complex than nginx controller. 

```
helm repo add istio https://istio-release.storage.googleapis.com/charts
helm repo update
```

For velocity we used istioctl; however, the helm is fully supported now. 
https://istio.io/latest/docs/setup/install/helm/

```
kubectl create namespace istio-system
```

```
helm install istio-base istio/base -n istio-system --set defaultRevision=default
```

```
NAME: istio-base
LAST DEPLOYED: Thu Oct  5 18:50:26 2023
NAMESPACE: istio-system
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
Istio base successfully installed!

To learn more about the release, try:
  $ helm status istio-base
  $ helm get all istio-base
```

Skipped CNI Chart Step 4


```
helm install istiod istio/istiod -n istio-system --wait
```

```
kubectl create namespace istio-ingress
helm install istio-ingress istio/gateway -n istio-ingress --wait
```

```
kubectl -n istio-ingress  get svc
NAME            TYPE           CLUSTER-IP    EXTERNAL-IP    PORT(S)                                      AGE
istio-ingress   LoadBalancer   10.0.29.229   20.99.187.25   15021:30473/TCP,80:31864/TCP,443:32408/TCP   22s
```


## Create Service 

```
helm install istio-ingress istio/gateway -n websats --wait
```



