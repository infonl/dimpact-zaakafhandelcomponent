# Installation guide

This guide describes how to install ZAC in a Kubernetes environment as a system administrator.
For developer documentation on how to build, install and run ZAC locally, please see [INSTALL.md](../../development/INSTALL.md). 

## Install ZAC in Kubernetes

To deploy ZAC in a Kubernetes cluster you can use the [ZAC Helm chart](../../../charts/zac/Chart.yaml) and fill-in all values that do not have a 
default value (they have an empty object, array or string).

It is best to check the [ZAC Helm Chart README.md](../../../charts/zac/README.md) for installation instructions and information about the 
values that can or have to be provided on installation.
