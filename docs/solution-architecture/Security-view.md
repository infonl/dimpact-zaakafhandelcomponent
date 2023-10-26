<_Richtlijn voor het invullen: Hoe elk gebruikerstype wordt geverifieerd. Autorisatiemechanismen voor verschillende delen van de site. Gegevensbeveiliging - als codering vereist is, welk type wordt gebruikt en hoe gevoelige informatie wordt behandeld. Netwerkcontroles en topologie ter ondersteuning van de beveiliging. Bijvoorbeeld  firewalls. HTTPS-beheer en beheer van SSL-certificaten. Beperking van DoS-aanvallen._>

## Authenticatie en Autorisatie
Ook wel genoemd: Identity en Access Management. Het concept valt uiteen in:
* Authenticatie = Identity management: het registreren, beheren en toepassen (controleren) van identiteiten van personen, organisaties (of onderdelen ervan) en systemen.
* Autorisatie = Access management: het registreren, beheren en toepassen van toegangsrechten die personen, organisatie of systemen toegang geven tot functionaliteiten en/of gegevens van systemen.
De richting van de actie is – in onderstaand diagram – altijd van boven naar beneden. De bovenliggende laag doet een verzoek (request) bijv. toegang tot een applicatie, functie of gegevens, en de onderliggende laag weigert of honoreert dit middels een - evt. asynchroon - antwoord (reply). Er worden nooit dergelijke acties geïnitieerd door de onderste lagen, richting een bovenliggende laag. API’s kunnen echter wel andere API’s aanroepen.

![Authenticatie en Autorisatie](./attachments/images/Authenticatie%20en%20Autorisatie%202021-12-16.png)

### 5. Interactie-laag / User interface, verbindt met 4. Proces-laag / Logica
| Niveau 5 naar 4 | Gebruikers naar Applicaties | 
| :-- | :-- | 
| Beschrijving | De bovenste laag houdt zich bezig met individuele gebruikers, en wordt ingevuld door de taakapplicaties en/of de gemeente. Deze leggen vast wat de permissies zijn van een gebruiker, welke acties deze in een applicatie mag uitvoeren. De applicatie dient zelf een autorisatiemodel te implementeren die voorkomt dat de applicatie API calls maakt die eigenlijk niet mogen voor deze gebruiker. Elke individuele gebruiker is in de applicatie bij naam bekend en heeft een of meerdere applicatierollen toegewezen waarmee hij/zij processen wel of niet kan uitvoeren en specifieke gegevens wel of niet mag aanmaken, bekijken, wijzigen of verwijderen. Dit is de meest fijnmazige autorisatie van de 5 lagen. | 
| Authenticatie | OpenID Connect en KeyCloak | 
| Autorisatie | <nader te bepalen> in de applicaties | 
| Verbinding protocol | HTTPS | 
| Beveiliging/Encryptie | TLS | 

### 4. Proces-laag / Logica, verbindt meestal *) rechtstreeks met 2. Service-laag / API’s
| Niveau 4 naar 2 | Applicaties naar Services (via API's) | 
| :-- | :-- | 
| Beschrijving | Taakapplicaties haken in op het authenticatiemechanisme dat bij een gemeente gebruikt wordt. Deze zijn vervolgens verantwoordelijk voor het genereren van een JSON Web Token (JWT) wat toegang verleent tot de API aan de taakapplicatie. In een ZGW-context kan het niet zo zijn dat eender welke applicatie van een organisatie alle data kan opvragen uit een ZRC (of andere componenten). Op deze laag wordt bepaald welke applicaties geautoriseerd zijn op welke gegevens, bijvoorbeeld welke operaties toegelaten zijn voor een subset van zaaktypes. | 
| Authenticatie | JSON Web Token (JWT) is een mechanisme om stateless claims te kunnen uitwisselen. Een gegenereerd token wordt via een HTTP Header meegestuurd in een request naar de API. De API moet vervolgens verzekeren dat de integriteit van het token in orde is, past daarna de claims in de payload toe en vertaalt deze claims naar de bijbehorende scopes (rechten). | 
| Autorisatie | De APIs kennen scopes - dit zijn sets van permissies die gegroepeerd worden, in een generieke vorm. Deze worden afgestemd volgens typisch gebruik. De taakapplicaties van organisaties communiceren met de APIs op basis van deze scopes. De APIs hebben geen kennis van de feitelijke eindgebruiker die een taakapplicatie gebruikt. (De gebruiker kan wel meegegeven worden in het eerder genoemde JWT token zodat de Service deze informatie kan gebruiken voor het bijhouden van audit logs, wie heeft wat gedaan) | 
| Verbinding protocol | HTTP / HTTPS | 
| Beveiliging/Encryptie | Netwerk toegang tussen de bedrijfsprocessen en de services kan worden ingericht via Kubernetes network policies. | 

### 2. Service-laag / API’s, verbindt met 1. Data-laag / Database
| Niveau | Services naar Gegevens | 
| :-- | :-- | 
| Beschrijving | Services hebben toegang tot data. | 
| Authenticatie | Userid / password | 
| Autorisatie | Elke service heeft toegang tot alle data objecten (tabellen etc.) en data instanties (rijen etc.) die (maximaal) nodig zijn om de service (API) correct te laten werken. Door verschillende database 'users' te configureren kan per 'user' worden ingeregeld tot welke database en welke database tabellen deze 'user' toegang heeft. Voor een goede afscherming bijvoorbeeld per API 1 database of 1 user. N.B. de 'user' is hier de service (API), niet de gebruikers uit laag 5. De gebruikersnamen uit laag 5 worden wel doorgegeven t.b.v. audit logging, maar niet voor rechten op de data. | 
| Verbinding protocol | PostgreSQL gebruikt een op berichten gebaseerd protocol voor communicatie tussen frontends en backends (clients en servers). Het protocol wordt ondersteund via TCP/IP en ook via Unix-domein sockets. | 
| Beveiliging/Encryptie | PostgreSQL heeft ingebouwde ondersteuning voor het gebruik van SSL-verbindingen om client/server-communicatie te versleutelen. | 

### *) Optioneel is 3. Integratie-laag / NLX
| Niveau | Applicaties naar Services | 
| :-- | :-- | 
| Beschrijving | Organisaties zijn geautoriseerd om met andere organisaties te verbinden via NLX out- en inways.  | 
| Authenticatie | TLS-clientverificatie middels PKIoverheid-certificaten. Outway- en inway-knooppunten identificeren zichzelf met behulp van hun ondertekende certificaat. NLX fungeert als intermediair voor de authenticatie van laag 2 naar laag 4: De auth-service van de organisatie geeft een JWT aan de applicatie, dit wordt via de NLX outway & inway naar de API verstuurd.  | 
| Autorisatie | Elke applicatie heeft toegang tot alle processen die (maximaal) nodig zijn om de applicatie correct te laten werken. Dit is een gefederaliseerd systeem - als je met een organisatie mag verbinden, kan je in principe bij alle APIs die de inway ontsluit. | 
| Verbinding protocol | REST/JSON over HTTP | 
| Beveiliging/Encryptie | De verbinding tussen een NLX-Inway en een NLX-Outway wordt beveiligd via zowel client- als servercertificaten, zogenaamde tweeweg TLS authenticatie. De certificaten die door de NLX-Inway en NLX-Outway componenten worden gebruikt kunnen door de NLX-Beheerder uitgegeven NLX-Certificaten zijn en, in de toekomst wellicht, door een externe TSP uitgegeven PKIo-certificaten. | 
