# Identity and Access Management (IAM)

## Current ('old') ZAC IAM architecture

The current ('old') IAM (Identity and Access Management) architecture of ZAC is illustrated in the following diagram:

```mermaid
C4Context
    title Current ZAC IAM architecture

    Person(Employee, "Employee", "An employee of a municipality")
    
    Enterprise_Boundary(b1, "ZAC") {
        System(ZAC, "ZAC", "Zaakafhandelcomponent")
        System(OPA, "Open Policy Agent")
    }

    Enterprise_Boundary(b2, "IAM components") {
        System(Keycloak, "Keycloak")
    }

    Rel(Employee, ZAC, "Uses")
    Rel(Employee, Keycloak, "Authenticates", "OIDC")
    Rel(ZAC, Keycloak, "Authenticates,<br/>Gets groups and users", "OIDC, Keycloak API")
    Rel(ZAC, OPA, "Manage policies", "REST")

    UpdateElementStyle(ZAC, $bgColor="red", $borderColor="red")
    UpdateElementStyle(OPA, $bgColor="red", $borderColor="red")
    
    UpdateRelStyle(Employee, ZAC, $offsetX="-40", $offsetY="-30")
    UpdateRelStyle(Employee, Keycloak, $offsetX="-50", $offsetY="-40")
    UpdateRelStyle(ZAC, Keycloak, $offsetX="-50", $offsetY="20")

    UpdateLayoutConfig($c4ShapeInRow="1", $c4BoundaryInRow="2")
```

The following components are part of the current ZAC IAM architecture:

| Component                                   | Description                                         | ZAC usage                                                                                                                                                                                |
|---------------------------------------------|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [OPA](https://www.openpolicyagent.org//)    | Open Policy Agent. Policy engine that manages security policies. | ZAC manages all low-level security policies (= role-permission mappings) in OPA. The OPA server is used by ZAC only and is part of the overall ZAC application package.                  |
| [Keycloak](https://www.keycloak.org/)       | Open Source Identity and Access Management product. | - ZAC authenticates a user to Keycloak using OIDC (OpenID Connect).<br/> - ZAC also uses the Keycloak API to retrieve groups and the users in a group, for example to be able to assign zaken. |

For details about the OPA access control policies and roles used by ZAC please see: [access control policies](accessControlPolicies.md).

Two completely disjunct principles are used for authorisation in the current ZAC architecture:

- Authorised zaaktypes
- ZAC application roles

Authorised zaaktypes are managed using 'domains', where domains exist both as special Keycloak roles (prepended with `domein_`) and also as configuration parameters in 
the ZAC zaakafhandelparameters which forms the link to a specific zaaktype.

ZAC offers a set of available application roles and the mapping from users (optionally through groups) to these application roles is done in Keycloak.

A logged-in user has one or more ZAC application roles and, completely disjunct from this, can also have one or more authorised zaaktypes. 
Typically, a combination of these two, but sometimes only the application role, is used for authorisation checks in ZAC (using OPA).
Some authorisation checks are independent of a particular zaaktype, and for these authorisations only the ZAC application roles are used.

## New ZAC IAM architecture

ZAC is in the transition to move to a new IAM architecture. This is currently 'hidden' for normal ZAC usage by the `FEATURE_FLAG_PABC_INTEGRATION` feature flag.
When this feature flag is disabled (which is the default), ZAC uses the current IAM architecture as described above.

The new ZAC IAM architecture is illustrated in the following diagram:

```mermaid
C4Context
    title New ZAC IAM architecture

    Person(Employee, "Employee", "An employee of a municipality")
    
    Enterprise_Boundary(b1, "ZAC") {
        System(ZAC, "ZAC", "Zaakafhandelcomponent")
        System(OPA, "Open Policy Agent")
    }

    Enterprise_Boundary(b2, "IAM components") {
        System(Keycloak, "Keycloak")
        System(PABC, "PABC", "Platform Autorisatie Beheer Component")
    }
    
    Rel(Employee, ZAC, "Uses")
    Rel(Employee, Keycloak, "Authenticates", "OIDC")
    Rel(ZAC, Keycloak, "Authenticates,<br/>Gets groups and users", "OIDC, Keycloak API")
    Rel(ZAC, PABC, "Get authorised entity types<br/>and application roles", "PABC API")
    Rel(PABC, Keycloak, "Get groups and functional roles per group", "Keycloak API")
    Rel(ZAC, OPA, "Get low-level policies", "REST")

    UpdateElementStyle(ZAC, $bgColor="red", $borderColor="red")

    UpdateRelStyle(Employee, ZAC, $offsetX="-40", $offsetY="-30")
    UpdateRelStyle(Employee, Keycloak, $offsetX="-50", $offsetY="-40")
    UpdateRelStyle(ZAC, Keycloak, $offsetX="-50", $offsetY="20")
    UpdateRelStyle(ZAC, OPA, $offsetX="-115", $offsetY="10")

    UpdateLayoutConfig($c4ShapeInRow="1", $c4BoundaryInRow="2")
```

The following components are part of the new ZAC IAM architecture:

| Component                                                        | Description                                                                             | Data managed                                                                                      | Usage in ZAC IAM architecture                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
|------------------------------------------------------------------|-----------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Keycloak](https://www.keycloak.org/)                            | Open Source Identity and Access Management product.                                     | Users, groups, functional roles and mappings between them                                         | ZAC uses Keycloak for authentication and user-to-functional-role mappings (via groups). ZAC authenticates to Keycloak using OIDC (OpenID Connect). ZAC also uses Keycloak to retrieve (unauthorised) groups and users in a group.                                                                                                                                                                                                                                                                                                                                                           |
| [PABC](https://github.com/Platform-Autorisatie-Beheer-Component) | Platform Autorisatie Beheer Component                                                   | Domains and authorisation mappings (= functional roles to zaaktypes + application roles mappings) | ZAC uses the PABC to retrieve authorisation mappings for the logged-in user's functional roles and to retrieve authorised groups. Authorised groups are those groups that through the groups' functional roles are authorised for a certain application role and a certain entity type. Authorised groups are used in ZAC to show a list of groups that may be assigned to a given zaak for the application role 'behandelaar'.<br/><br/> The PABC also integrates with Keycloak to retrieve groups and the groups' functional roles, in order to return authorised groups to applications. |
| ZAC                                                              | The Zaakafhandelcomponent application.                                                  | Application roles                                                                                 | ZAC uses the authorisation mappings retrieved from PABC to perform authorisation policy checks using OPA.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| - [OPA](https://www.openpolicyagent.org/)                        | Open Policy Agent. Policy engine that manages low-level security policies for ZAC only. | ZAC low-level security policies (= ZAC application roles to permission mappings)                  | ZAC manages all low-level security policies (= application role - permission mappings) in OPA. OPA is used by ZAC only and is part of the overall ZAC application package. ZAC does not function without OPA.                                                                                                                                                                                                                                                                                                                                                                               |

The new IAM architecture is quite different from the old IAM architecture in that it allows different application roles _per_ zaaktype per user.
This is implemented using the following concepts which are managed in the PABC:

* authorisation mappings from functional roles to a set of the following:
    * A combination of:
        * Authorised zaaktype
        * ZAC application roles applicable to this zaaktype
    * ZAC application roles that are not zaaktype-specific

In the PABC the concept of `zaaktype` is abstracted as a generic `entity type` concept, allowing for the possibility to use the PABC for authorisations on other entity types in the future.

Internally in the PABC these `entity types` are grouped using `domains`. In the new IAM architecture domains are a PABC-internal concept only. 
ZAC, nor Keycloak, have any knowledge of these domains.

The functional roles and the mappings of groups to functional roles as well as the users themselves are managed in Keycloak.

### IAM components

The relationship between the three main components in the new IAM architecture (Keycloak, PABC, ZAC) is illustrated in the following diagram. 
Note that this is a simplified overview and will change in the future when the new IAM architecture will be extended further.

- A '*' indicates that the current component is the source of a certain type of IAM related data.
- The arrows indicate dependencies between components.

```mermaid
block-beta
    columns 1
    block:ZAC
        columns 1
        zacBlockTitle("ZAC"):1
        ApplicationRolesSource("Application roles*")
        Policies("Authorisation policies ('permissions')")
    end
    space
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
            AuthorisationMappings("Authorisation mappings*")
            Applications("Applications*")
        end
    end

    ZAC -- "Unauthorised groups and<br/>users per group\n(Keycloak API)" -.-> Keycloak
    ZAC -- "Functional role<br/>authorisation mappings\nand authorised groups \n(PABC API)" -.-> PABC
    PABC -- "Groups and<br/>groups' functional roles\n(Keycloak API)" -.-> Keycloak

    %% workaround to make sure titles of sub-blocks are vertically aligned
    %% see: https://github.com/mermaid-js/mermaid/issues/5423
    class IAM BT
    class zacBlockTitle BT
    class keycloakBlockTitle BT
    class pabcBlockTitle BT
    classDef BT stroke:transparent,fill:transparent

    style ZAC fill:red,stroke:#333,stroke-width:4px
    style Keycloak fill:#2969B9,stroke:#333,stroke-width:4px
    style PABC fill:#2969B9,stroke:#333,stroke-width:4px
```

The following table summarises the main IAM concepts used in the new ZAC IAM architecture:

| Concept            | Managed in  | Description                                                                                                                                                                                                    |
|--------------------|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Application role  | ZAC         | A ZAC application role that represents a set of permissions in ZAC. Authorisation policies in ZAC are defined per application role (using OPA).                                                                |
| Authorisation policy | ZAC         | A policy that represents a set of permissions in ZAC. Authorisation policies are defined per application role (using OPA).                                                                                     |
| User               | Keycloak    | An individual employee of a municipality. Note that citizens are not part of the IAM architecture.                                                                                                             |
| Group              | Keycloak    | A group of users. Groups are used to manage functional role assignments to users.                                                                                                                              |
| Functional role    | Keycloak    | A role that represents a certain function or responsibility within the context of one or more domains. Users are assigned functional roles via groups. Technically functional roles are Keycloak Domain Roles. |
| Domain             | PABC        | A grouping of entity types (e.g. zaaktypes) in the PABC. Domains are a PABC-internal concept only. They do not necessarily represent organisational domains within a council.                                  |
| Entity type        | PABC        | A data type on which you can assign authorisation. Currently only the 'zaaktype' entity type is supported in ZAC. Authorisation mappings are defined per entity type.                                          |
| Authorisation mapping | PABC        | A mapping from a functional role to a set of entity types and application-specific application roles.                                                                                                          |
| Application         | PABC       | An application for which authorisation mappings can be defined. In this case: ZAC.                                                                                                                             |

### Scenarios

The following sequence diagram illustrates the scenario of a ZAC user logging in and retrieving the authorisation mappings from the PABC:

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
    ZAC->>PABC: Retrieve authorisation mappings for functional roles
    PABC-->>ZAC: Returns authorisation mappings
    deactivate ZAC
    Employee->>ZAC: Performs functionality in ZAC user interface for which authorisation is required
    activate ZAC
    ZAC->>OPA: Performs policy-based authorisation checks using authorisation mappings
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
