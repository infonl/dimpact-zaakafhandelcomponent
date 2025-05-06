# Identity and Access Management (IAM)

The IAM (Identify and Access Management) architecture of ZAC is illustrated in the following diagram:

```mermaid
C4Context
    title ZAC IAM architecture

    Person(Employee, "Employee", "An employee of a municipality")

    Enterprise_Boundary(b0, "ZAC and related Common Ground components") {
        System(OPA, "Open Policy Agent")
        System(ZAC, "ZAC", "Zaakafhandelcomponent")
    }

    Enterprise_Boundary(b1, "Centralized services") {
        System(Keycloak, "Keycloak")
    }

    Rel(Employee, ZAC, "Uses")
    Rel(Employee, Keycloak, "Authenticates", "OIDC")
    Rel(ZAC, Keycloak, "Uses", "OIDC")
    Rel(ZAC, Keycloak, "Get users and groups", "Keycloak API")
    Rel(ZAC, OPA, "Manage policies", "REST")

    UpdateElementStyle(ZAC, $bgColor="red", $borderColor="red")
    UpdateElementStyle(Keycloak, $bgColor="darkgrey", $borderColor="darkgrey")

    UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="4")
```

The following components are part of the ZAC IAM architecture:

| Component                                   | Description                                         | ZAC usage                                                                                                                                                                                                       |
|---------------------------------------------|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [OPA](https://www.openpolicyagent.org//)    | Open Policy Agent. Policy engine that manages security policies. | ZAC manages all security policies (= role-permission mappings) in OPA.                                                                                                                                          |
| [Keycloak](https://www.keycloak.org/)       | Open Source Identity and Access Management product. | ZAC uses Keycloak for authentication and authorization. ZAC authenticates to Keycloak using OIDC (OpenID Connect). ZAC also uses Keycloak to retrieve users and groups, for example to be able to assign zaken. |

For details about the OPA access control policies and roles used by ZAC please see: [access control policies](accessControlPolicies.md).

## Internal endpoints

ZAC exposes some 'internal' endpoints (on `/rest/internal/*`) which are not called by the ZAC user interface but rather by other components in the ZAC architecture,
such as Kubernetes cron jobs or scripts which can be run manually.

Because in these system integrations there is no end-user available, these endpoints are secured using a simple built-in API key mechanism. This works as follows:

- The API key is a simple string which needs to be passed with all internal HTTP(S) requests in the `X-API-KEY` HTTP header.
- If the API key is not provided the internal endpoint will return a 401 Unauthorized response.
- On deployment ZAC needs to be configured with a value for this API key. This value is also used to configure specific ZAC Kubernetes cron jobs that use these 
internal endpoints. Please see the [ZAC Helm Chart](../../charts/zac) for details.
