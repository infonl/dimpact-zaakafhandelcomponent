# Installation guide

This guide describes how to install ZAC in a Kubernetes environment as a system administrator.
For developer documentation on how to build, install and run ZAC locally, please see [INSTALL.md](../../development/INSTALL.md). 

For an architectural overview, see the: [ZAC deployment model](../../solution-architecture/deploymentModel.md).

## Install ZAC in Kubernetes

To deploy ZAC in a Kubernetes cluster you can use the [ZAC Helm chart](../../../charts/zac/Chart.yaml) and fill-in all values that do not have a 
default value (they have an empty object, array or string).

It is best to check the [ZAC Helm Chart README.md](../../../charts/zac/README.md) for installation instructions and information about the 
values that can or have to be provided on installation.

## Upgrade to Keycloak 26.8.2 or later

As of version 26.8.2, the Keycloak Admin REST API only accepts tokens that carry the `kc-admin-api` client scope. The Keycloak upgrade
creates this scope in every realm, but only assigns it to its own clients `admin-cli` and `security-admin-console`. Without the scope, ZAC
gets a `401 Unauthorized` response when it reads users and groups from Keycloak.

Before or directly after you upgrade Keycloak, do these steps in the Keycloak admin console, in the ZAC realm:

1. Go to **Clients** and open the ZAC admin client (the `keycloak.adminClient.id` value of the Helm chart).
2. Go to **Client scopes** > **Add client scope**, select `kc-admin-api` and choose **Add** > **Default**. It must be a default scope,
   because ZAC does not request scopes by name.
3. Do the same for the PABC admin client, if you use PABC.

If the `401` errors continue, restart ZAC to remove its current token.
See the [Keycloak upgrading guide](https://www.keycloak.org/docs/latest/upgrading/index.html) for more information.
