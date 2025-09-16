# Identity and Access Management (IAM)

## Current ('legacy') ZAC IAM architecture

The current IAM (Identity and Access Management) architecture of ZAC is illustrated in the following diagram:

```mermaid
C4Context
    title Current ZAC IAM architecture

    Enterprise_Boundary(b0, "Users") {
        Person(Employee, "Employee", "An employee of a municipality")
    }
    
    Enterprise_Boundary(b1, "ZAC") {
        System(ZAC, "ZAC", "Zaakafhandelcomponent")
        System(OPA, "Open Policy Agent")
    }

    Enterprise_Boundary(b2, "External services") {
        System(Keycloak, "Keycloak")
    }

    Rel(Employee, ZAC, "Uses")
    Rel(Employee, Keycloak, "Authenticates", "OIDC")
    Rel(ZAC, Keycloak, "Uses", "OIDC")
    Rel(ZAC, Keycloak, "Get users and groups", "Keycloak API")
    Rel(ZAC, OPA, "Manage policies", "REST")

    UpdateElementStyle(ZAC, $bgColor="red", $borderColor="red")
    UpdateElementStyle(Keycloak, $bgColor="darkgrey", $borderColor="darkgrey")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="2")
```

The following components are part of the current ZAC IAM architecture:

| Component                                   | Description                                         | ZAC usage                                                                                                                                                                                                       |
|---------------------------------------------|-----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [OPA](https://www.openpolicyagent.org//)    | Open Policy Agent. Policy engine that manages security policies. | ZAC manages all security policies (= role-permission mappings) in OPA.                                                                                                                                          |
| [Keycloak](https://www.keycloak.org/)       | Open Source Identity and Access Management product. | ZAC uses Keycloak for authentication and authorization. ZAC authenticates to Keycloak using OIDC (OpenID Connect). ZAC also uses Keycloak to retrieve users and groups, for example to be able to assign zaken. |

For details about the OPA access control policies and roles used by ZAC please see: [access control policies](accessControlPolicies.md).

Two completely disjunct principles are used for authorization in the current ZAC architecture:

- Authorised zaaktypes
- ZAC application roles

Authorised zaaktypes are managed using 'domains', where domains exist both as special Keycloak roles (prepended with `domein_`) and also as configuration parameters in 
the ZAC zaakafhandelparameters which forms the link to a specific zaaktype.

ZAC offers a set of available application roles and the mapping from users (optionally through groups) to these application roles is done in Keycloak.

A logged-in user has one or more ZAC application roles and, completely disjunct from this, can also have one or more authorised zaaktypes. 
Typically, a combination of these two, but sometimes only the application role, is used for authorization checks in ZAC (using OPA).
Some authorization checks are independent of a particular zaaktype, and for these authorizations only the ZAC application roles are used.

## New ZAC IAM architecture

ZAC is in the transition to move to a new IAM architecture. This is currently 'hidden' for normal ZAC usage by the `FEATURE_FLAG_PABC_INTEGRATION` feature flag.
When this feature flag is disabled (which is the default), ZAC uses the current IAM architecture as described above.

The new ZAC IAM architecture is illustrated in the following diagram:

```mermaid
C4Context
    title New ZAC IAM architecture

    Enterprise_Boundary(b0, "Users") {
        Person(Employee, "Employee", "An employee of a municipality")
    }
    
    Enterprise_Boundary(b1, "ZAC") {
        System(ZAC, "ZAC", "Zaakafhandelcomponent")
        System(OPA, "Open Policy Agent")
    }

    Enterprise_Boundary(b2, "External services") {
        System(Keycloak, "Keycloak")
        System(PABC, "PABC", "Platform Autorisatie Beheer Component")
    }
    
    Rel(Employee, ZAC, "Uses")
    Rel(Employee, Keycloak, "Authenticates", "OIDC")
    Rel(ZAC, Keycloak, "Uses", "OIDC")
    Rel(ZAC, Keycloak, "Get users and groups", "Keycloak API")
    Rel(ZAC, PABC, "Get authorised entity types and application roles", "PABC API")
    Rel(ZAC, OPA, "Manage policies", "REST")

    UpdateElementStyle(ZAC, $bgColor="red", $borderColor="red")
    UpdateElementStyle(Keycloak, $bgColor="darkgrey", $borderColor="darkgrey")

    UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="2")
```

The following components are part of the new ZAC IAM architecture:

| Component                                                        | Description                                                      | Data managed                                                                                      | Usage in ZAC IAM architecture                                                                                                                                                                                                                    |
|------------------------------------------------------------------|------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Keycloak](https://www.keycloak.org/)                            | Open Source Identity and Access Management product.              | Users, groups, functional roles and mappings between them                                         | ZAC uses Keycloak for authentication and user-to-functional-role mappings (via groups). ZAC authenticates to Keycloak using OIDC (OpenID Connect). ZAC also uses Keycloak to retrieve users and groups, for example to be able to assign zaken. |
| [PABC](https://github.com/Platform-Autorisatie-Beheer-Component) | Platform Autorisatie Beheer Component                            | Domains and authorization mappings (= functional roles to zaaktypes + application roles mappings) | ZAC uses the PABC to retrieve authorization mappings for the user's functional roles. In a future version PABC will also integrate with Keycloak.                                                                                                |
| ZAC                                                              | The Zaakafhandelcomponent application.                           | Application roles                                                                                 | ZAC uses the authorization mappings retrieved from PABC to perform authorization policy checks using OPA.                                                                                                                                        |
| - [OPA](https://www.openpolicyagent.org/)                        | Open Policy Agent. Policy engine that manages security policies. | ZAC policies (= application roles to permission mappings)                                         | ZAC manages all security policies (= application role - permission mappings) in OPA.                                                                                                                                                             |

The new IAM architecture is quite different from the old IAM architecture in that it allows different application roles _per_ zaaktype per user.
This is implemented using the following concepts which are managed in the PABC:

* Authorization mappings from functional roles to a set of the following:
    * A combination of:
        * Authorised zaaktype
        * ZAC application roles applicable to this zaaktype
    * ZAC application roles that are not zaaktype-specific

In the PABC the concept of `zaaktype` is abstracted as a generic `entity type` concept, allowing for the possibility to use the PABC for authorisations on other entity types in the future.

Internally in the PABC these `entity types` are grouped using `domains`. In the new IAM architecture `domains` are a PABC-internal concept only. 
ZAC, nor Keycloak, have any knowledge of these `domains`.

The functional roles and mappings from users (typically through groups) to functional roles are managed in Keycloak.

### Main IAM components and used data types

The relationship between the three main components in the new IAM architecture (Keycloak, PABC, ZAC) as well as the main 
data types used within each component is illustrated in the following diagram. 
Note that this is a simplified overview.

- A '*' indicates that the current component is the source of a data type.
- The arrows indicate the flow of data between the components, not dependencies.

```mermaid
block-beta
    columns 1
    block:IAM
        block:Keycloak
            columns 1
            keycloakBlockTitle("Keycloak"):1
            UsersSource("Users*")
            GroupsSource("Groups*")
            FunctionalRolesSource("Functional roles*")
        end
        space
        block:PABC
            columns 1
            pabcBlockTitle("PABC"):1
            Domains("Domains*")
            FunctionalRoles("Functional roles")
            ApplicationRoles("Application roles")
            EntityTypes("Entity types (zaaktypes)*")
        end
    end
    space
    block:ZAC
        columns 1
        zacBlockTitle("ZAC"):1
        Users("Users")
        Groups("Groups")
        ApplicationRolesSource("Application roles*")
        Policies("Authorisation policies ('permissions')")
    end
    
    Keycloak --> ZAC
    PABC --> ZAC
    
    %% workaround to make sure titles of sub-blocks are vertically aligned
    %% see: https://github.com/mermaid-js/mermaid/issues/5423
    class keycloakBlockTitle BT
    class zacBlockTitle BT
    class pabcBlockTitle BT
    classDef BT stroke:transparent,fill:transparent

    style Keycloak fill:darkgrey,stroke:#333,stroke-width:4px
    style ZAC fill:red,stroke:#333,stroke-width:4px
    style PABC fill:darkslateblue,stroke:#333,stroke-width:4px
```

### Scenarios

The following sequence diagram illustrates the scenario of a ZAC user logging in and retrieving the authorization mappings from the PABC:

```mermaid
sequenceDiagram
    actor Employee
    participant Keycloak
    participant ZAC
    participant PABC

    Employee->>ZAC: Requests ZAC user interface
    activate ZAC
    ZAC->>Keycloak: Redirects to Keycloak for authentication
    deactivate ZAC
    Employee->>Keycloak: Logs in
    activate Keycloak
    Keycloak-->>ZAC: Pass on functional roles in JWT OIDC token
    deactivate Keycloak
    activate ZAC
    ZAC->>PABC: Retrieve authorization mappings for functional roles
    PABC-->>ZAC: Returns authorization mappings
    deactivate ZAC
    Employee->>ZAC: Performs functionality in ZAC user interface for which authorization is required
    activate ZAC
    ZAC->>OPA: Uses authorization mappings for authorization checks
    deactivate ZAC
```    

The following concrete example illustrates this scenario:
1. A user in Keycloak belongs to the group `behandelaars_domein_x`.
2. The group `behandelaars_domein_x` is mapped to the functional role `behandelaar_domein_x`.
3. The functional role `behandelaar_domein_x` is mapped in the PABC to: zaaktype `zaaktype_1` with the ZAC application role `behandelaar`.
4. The user logs in to ZAC and has the functional role `behandelaar_domein_x` in the Keycloak OIDC token.
5. ZAC retrieves the authorisation mappings for the functional role `behandelaar_domein_x` from the PABC, which includes the mapping to zaaktype `zaaktype_1` with the ZAC application role `behandelaar`.
6. The user can now perform actions in ZAC on zaken of `zaaktype_1` as a `behandelaar`.

## Internal endpoints

ZAC exposes some 'internal' endpoints (on `/rest/internal/*`) which are not called by the ZAC user interface but rather by other components in the ZAC architecture,
such as Kubernetes cron jobs or scripts which can be run manually.

Because in these system integrations there is no end-user available, these endpoints are secured using a simple built-in API key mechanism. This works as follows:

- The API key is a simple string which needs to be passed with all internal HTTP(S) requests in the `X-API-KEY` HTTP header.
- If the API key is not provided the internal endpoint will return a 401 Unauthorized response.
- On deployment ZAC needs to be configured with a value for this API key. This value is also used to configure specific ZAC Kubernetes cron jobs that use these 
internal endpoints. Please see the [ZAC Helm Chart](../../charts/zac) for details.
