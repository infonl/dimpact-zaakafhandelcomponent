# Testing Kubernetes Helm installs using Kind

## Prerequisites

### Kind
[Kind](https://kind.sigs.k8s.io/) is a tool for running a Kubernetes cluster locally. It can be installed using a 
package manager, like so:

For Macs, using Homebrew:
```shell
brew install kind
```

For Windows, using Chocolatery:
```commandline
choco install kind
```

Or check the [installation guide](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) for other ways of 
installing it on your machine.

### Helm
[Helm](https://helm.sh/) calls itself a package manager for Kubernetes. We have created a [Helm Chart](../../charts/zac/Chart.yaml) 
definition and a [Values file](../../charts/zac/values.yaml) with required settings for the installation of the 
Zaakafhandelsysteem, and it's dependencies.

So ensure you have Helm installed:

For Mac, using Homebrew:
```shell
brew install helm
```

For Windows, using Chocolatery:
```commandline
choco install helm
```

Or check the [installation guide](https://helm.sh/docs/intro/install/) for instructions to install it on your machine.

### Kubectl
We can use [Kubectl](https://kubernetes.io/docs/tasks/tools/) from the command line to check and control Kubernetes 
clusters. We will want to use that locally to connect with the Kind cluster to be able to check how it all behaves under
the hood.

We should ensure it is installed.

For Mac, using Homebrew:
```shell
brew install kubectl
```

For Windows, using Chocolatery:
```commandline
choco install kubernetes-cli
```

Or check the [installation guide](https://kubernetes.io/docs/tasks/tools/) for ways to install it on your machine.

### PodiumD
As ZAC is part of the PodiumD products, we may want to use the Helm Charts that are available for the podium to install
all components (or a sub-set) that make up the podium.

We can find the Helm Charts and Values for the podium in their [GitHub repository](https://github.com/Dimpact-Samenwerking/helm-charts/tree/main/charts/podiumd).
There we can find different released versions, or use the latest and greatest when you checkout this repository locally.

## Testing ZAC Helm Deployment

### Setup
In the [kind config file](kind-config.yaml) we define a cluster with a control-plane and worker nodes.

Using the command
```shell
kind create cluster --name podiumd-cluster --config kind-config.yaml
```
we create a cluster for podiumd, and then we can look at installing the helm chart to it.

Firstly, we have to ensure that helm knows about the repos that will be used by the helm charts, so let's add them to 
the repository list.

Check the current known repos:
```shell
helm repo list
```

Then ensure the following repos are made available:

```shell
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add dimpact https://Dimpact-Samenwerking.github.io/helm-charts/
helm repo add kiss-frontend https://raw.githubusercontent.com/Klantinteractie-Servicesysteem/KISS-frontend/main/helm
helm repo add kiss-adapter https://raw.githubusercontent.com/ICATT-Menselijk-Digitaal/podiumd-adapter/main/helm
helm repo add kiss-elastic https://raw.githubusercontent.com/Klantinteractie-Servicesysteem/.github/main/docs/scripts/elastic
helm repo add maykinmedia https://maykinmedia.github.io/charts
helm repo add wiremind https://wiremind.github.io/wiremind-helm-charts
```
( per [PodiumD Readme](https://github.com/Dimpact-Samenwerking/helm-charts/tree/main/charts/podiumd#add-used-chart-repositories) )

And check if the repos, specifically for ZAC are available:
```shell
helm repo add opentelemetry https://open-telemetry.github.io/opentelemetry-helm-charts
```
( per [ZAC Helm Chart Readme](../../charts/zac/README.md) )


Before installing the Helm Charts, we should create a separate namespace:
```shell
kubectl create namespace podiumd
```

### Install PodiumD
```shell
helm install podiumd dimpact/podiumd --namespace podiumd --version 3.2.0 --wait
```

