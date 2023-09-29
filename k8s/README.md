
# Deploy on Kuberentes

## Create Deployment and LB Service

```
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service-lb.yaml
```

## Change Azure LB Config

You can set a DNS Name. 

From Azure Portal go to Manage Cluster Resource Group (e.g. MC_velocitysimulators_sim_westus2)

Find the "Public IP" and select Configuration.

You can set the subdomain (e.g. websats2).  The full DNS will include Azure Region. (e.g. websats2.westus2.cloudapp.azure.com)

The name has to be unique for Region. 



## Create Local Service (optional)

Useful if you want to call the service from within the cluster.



## Additional Tweaks When using Istio
kubectl label namespace simulators istio-injection=enabled

kubectl label namespace simulators istio.io/rev=1-17-2
