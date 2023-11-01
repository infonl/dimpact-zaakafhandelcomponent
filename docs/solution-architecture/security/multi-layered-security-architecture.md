# Zac multi-layered security architecture

ZAC has a multi-layered security architecture. This document will explain the different layers and how they interact with each other.

## Authenticatie en Autorisatie
This can also be referred to as: Identity and Access Management. The concept is divided into:

* **Authentication** = Identity Management: This involves registering, managing, and verifying the identities of individuals, organizations (or parts of them), and systems.

* **Authorization** = Access Management: This involves registering, managing, and applying access rights that allow individuals, organizations, or systems to access functionalities and/or data from systems. In the diagram mentioned, the direction of the action always goes from top to bottom. The top layer sends a request (for instance, for access to an application, function, or data), and the lower layer either denies or grants this with a potentially asynchronous response. Such actions are never initiated by the lower layers towards a higher layer. However, APIs can call other APIs.

![Authenticatie en Autorisatie](./../attachments/images/Authenticatie%20en%20Autorisatie%202021-12-16.png)

### 5. Interaction Layer / User Interface connects with 4. Process Layer / Logic
| Level 5 to 4 | Users to Applications | 
| :-- | :-- | 
| **Description** | The top layer deals with individual users, fulfilled by task applications and/or the municipality. They define a user's permissions and the actions they can perform within an application. The application must implement its own authorization model to ensure it doesn't make API calls not allowed for that user. Each individual user is known by name within the application and has one or more application roles assigned. These roles determine the processes they can or cannot execute, and which specific data they can or cannot create, view, modify, or delete. This is the most granular level of authorization among the five layers. |
| **Authentication** | OpenID Connect and KeyCloak |
| **Authorization** | To be determined within the applications |
| **Connection Protocol** | HTTPS |
| **Security/Encryption** | TLS |

### 4. Process Layer / Logic, typically connects directly with 2. Service Layer / APIs
| Level 4 to 2 | Applications to Services (via APIs) | 
| :-- | :-- | 
| **Description** | Task applications integrate with the authentication mechanism used by a municipality. They are then responsible for generating a JSON Web Token (JWT) which grants the task application access to the API. In a ZGW context, it cannot be the case that any application from an organization can query all data from a ZRC (or other components). At this layer, it's determined which applications are authorized for which data, for example, which operations are allowed for a subset of case types. |
| **Authentication** | JSON Web Token (JWT) is a mechanism for exchanging stateless claims. A generated token is sent via an HTTP Header in a request to the API. The API must then ensure the token's integrity, applies the claims in the payload, and translates these claims to the corresponding scopes (rights). |
| **Authorization** | The APIs recognize scopes - these are sets of permissions grouped in a generic form. They are adjusted according to typical use. Organizations' task applications communicate with the APIs based on these scopes. The APIs are not aware of the actual end user using a task application. (However, the user can be included in the aforementioned JWT token so that the Service can use this information for keeping track of audit logs, indicating who did what) |
| **Connection Protocol** | HTTP / HTTPS |
| **Security/Encryption** | Network access between the business processes and the services can be set up using Kubernetes network policies. |


### 2. Service Layer / APIs, connects with 1. Data Layer / Database
| Level | Services to Data | 
| :-- | :-- | 
| **Description** | Services have access to data. |
| **Authentication** | Userid / password |
| **Authorization** | Each service has access to all data objects (tables, etc.) and data instances (rows, etc.) necessary for the service (API) to function correctly. By configuring different database 'users', it can be determined which databases and database tables each 'user' can access. For good separation, for example, each API should have one database or one user. Note: the 'user' here is the service (API), not the users from layer 5. However, usernames from layer 5 are passed on for audit logging purposes but not for data access rights. |
| **Connection Protocol** | PostgreSQL uses a message-based protocol for communication between frontends and backends (clients and servers). This protocol is supported via TCP/IP as well as Unix-domain sockets. |
| **Security/Encryption** | PostgreSQL has built-in support for using SSL connections to encrypt client/server communication. |

### Optional: 3. Integration Layer / NLX
| Level | Applications to Services | 
| :-- | :-- | 
| **Description** | Organizations are authorized to connect with other organizations through NLX out- and inways. |
| **Authentication** | TLS client verification using PKI government certificates. Outway and inway nodes identify themselves using their signed certificate. NLX acts as an intermediary for authentication from layer 2 to layer 4: The organization's auth service provides a JWT to the application, which is sent to the API via the NLX outway & inway. |
| **Authorization** | Each application has access to all processes necessary for the application to function correctly. This is a federated system - if you can connect with an organization, you can, in principle, access all APIs that the inway provides. |
| **Connection Protocol** | REST/JSON over HTTP |
| **Security/Encryption** | The connection between an NLX-Inway and an NLX-Outway is secured via both client and server certificates, known as two-way TLS authentication. The certificates used by the NLX-Inway and NLX-Outway components can be NLX-Manager issued NLX Certificates and, perhaps in the future, externally issued PKIo certificates. |
