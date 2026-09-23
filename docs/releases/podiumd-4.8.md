# PodiumD 4.7 Release Notes

## Koppeling landelijk registers

- De BRP-verbindingsinstellingen, inclusief de headers die nodig zijn voor protocollering, zijn volledig configureerbaar per omgeving, waardoor het eenvoudig is om verbinding te maken met verschillende BRP-leveranciers zonder applicatiewijzigingen.
- Het detailniveau dat wordt gelogd voor BRP-verzoeken en -antwoorden is configureerbaar.

## KVK koppelvlak in ZAC

- Contactgegevens (e-mailadres en telefoonnummer) van rechtspersonen die via een KVK-nummer aan een zaak zijn gekoppeld, worden opgehaald uit Open Klant en weergegeven op zowel de zaakdetailpagina als de bedrijfsdetailpagina.
- Door op een gekoppelde rechtspersoon op de zaakdetailpagina te klikken, navigeer je naar de bedrijfsdetailpagina, waar het bedrijfstype wordt aangeduid als "Hoofdvestiging", "Nevenvestiging" of "Rechtspersoon".
- Bedrijfsadresgegevens worden getoond in de basisprofielweergave (bezoekadres) en de volledige profielweergave toont alle beschikbare adressen, elk aangeduid met het type (Bezoekadres of Correspondentieadres). De adresgegevens worden ververst wanneer een ander bedrijf als initiator wordt gekoppeld.
- Zaken die aan een vestiging zijn gekoppeld, worden opgehaald via zowel het vestigingsnummer als het KVK-nummer, zodat zaken zichtbaar blijven ook wanneer een bedrijf in de loop van de tijd van eigenaar wisselt.

## Automatische ontvangstbevestiging versturen vanuit ZAC

- Wanneer een productaanvraag aanvraagspecifieke contactgegevens bevat voor een geauthenticeerde aanvrager, worden deze gegevens weergegeven op de zaakdetailpagina en gebruikt voor de automatische ontvangstbevestiging — niet de algemene voorkeurscontactgegevens van de aanvrager.
- Contactgegevens die een aanvrager tijdens een productaanvraag als standaard instelt, worden behandeld als algemene voorkeursgegevens en niet als aanvraagspecifieke contactgegevens.
- Het label "Aanvraagspecifieke contactgegevens" wordt alleen weergegeven op de zaakdetailpagina wanneer er daadwerkelijk aanvraagspecifieke contactgegevens aanwezig zijn.
- Voor zaken met een bedrijf als initiator worden aanvraagspecifieke contactgegevens gemarkeerd met hetzelfde info-icoon als voor natuurlijke personen.

## BPMN in ZAC - Fase 3

- SmartDocuments-sjabloonkoppelingen kunnen per BPMN-zaaktype worden geconfigureerd in de Zaakafhandelparameters (ZAPS), op dezelfde manier als voor CMMN-zaaktypen. Deze configuratie wordt overgenomen wanneer een nieuwe versie van een zaaktype wordt gepubliceerd.
- Bij het aanmaken van een document vanuit een BPMN-zaak zijn alleen de SmartDocuments-sjablonen beschikbaar die voor dat zaaktype in ZAPS zijn geconfigureerd. Wanneer het proces een sjabloongroep of sjabloon vooraf selecteert, wordt deze automatisch geselecteerd in de documentaanmaakzijbalk.
- Automatische ontvangstbevestigingsmails kunnen worden getriggerd vanuit een BPMN-proces, met dezelfde logica als CMMN: aanvraagspecifieke contactgegevens hebben prioriteit boven de voorkeurscontactgegevens van de aanvrager uit Open Klant. De indicator voor verzonden ontvangstbevestiging wordt niet getoond voor BPMN-zaken, omdat dit door het proces zelf wordt afgehandeld.
- BPMN-taakformulieren tonen de volledige namen van de toegewezen medewerker en groep.

## IAM fase 2 vanuit - ZAC

- ZAC maakt uitsluitend gebruik van PABC voor identiteits- en toegangsbeheer. Autorisatie wordt volledig beheerd via PABC, inclusief rollen die gelden voor alle zaaktypen, zoals de beheerdersrol en BRP-toegangsrechten.
- Groepen kunnen in Keycloak als inactief worden gemarkeerd. Inactieve groepen worden uitgesloten van alle keuzelijsten bij het toewijzen van zaken of taken. Wanneer een zaak of taak al is toegewezen aan een inactieve groep, wordt een *(inactief)*-indicator getoond op de detailpagina en kan de toewijzing worden bijgewerkt naar een actieve groep zonder dat dit direct verplicht is.

## Security improvements

- De kantoorbestandenconverter is vervangen door Gotenberg, waarmee kritieke beveiligingskwetsbaarheden in multimedia-verwerkingsbibliotheken worden geëlimineerd die remote code execution via kwaadaardige documentuploads mogelijk maakten.

## Usage improvements

- Het taakformulier "Extern advies mail" vult automatisch het standaard e-mailadres van de afzender in wanneer dat beschikbaar is.
- Dashboardkaarten in dezelfde rij hebben een uniforme hoogte, waardoor de lay-out visueel consistent blijft terwijl inhoud wordt geladen.
- Alle kolommen op elke dashboardkaart zijn sorteerbaar. Een klik op een kolomkop sorteert oplopend, een tweede klik sorteert aflopend en een derde klik keert terug naar de standaardsorteervolgorde. Tijdens het ophalen van gegevens wordt een laadspinner getoond.
- Het dialoogvenster "Zoekopdracht opslaan" gebruikt dezelfde knopindeling en sluitknop als andere dialoogvensters in ZAC.

## Stability improvements

- ZAC herstelt automatisch wanneer de OpenZaak-catalogus tijdelijk niet beschikbaar is, en herstart zichzelf als de catalogus gedurende ongeveer 8 minuten onbereikbaar blijft.
- De poort van de kantoorbestandenconverter is configureerbaar via Helm-waarden, zodat de deployment kan worden aangepast bij gebruik van een andere converterafbeelding.
- Statische bestanden worden na het eerste bezoek gecachet in de browser, waardoor laadtijden bij volgende paginabezoeken worden verkort.
- De secties Documenten en Productaanvragen worden on demand geladen, waardoor de initiële opstarttijd van de applicatie wordt verkort.

## Bug fixes

- Signaleringen worden betrouwbaar verstuurd, ook wanneer de sessie van een gebruiker verloopt of de gebruiker uitlogt tijdens het proces.
- Het taakformulier "Extern advies mail" wordt correct ingediend.
- BPMN-taakformulieren tonen geen bevestigingsbericht na indiening.
- Het aanmaken van een nieuw e-mailsjabloon in ZAPS verloopt zonder foutmelding.
- Bij het toevoegen van een document met een ontvangstdatum wordt de documentstatus ingesteld op "Definitief" en gedraagt het statusveld zich zoals verwacht.
- Het veld "Reden" is verplicht (gemarkeerd met *) bij het vrijgeven van zaken of taken.
- De optie "Besluit vastleggen" is niet beschikbaar voor zaken in de intakefase, inclusief zaken met de status "Wacht op aanvullende informatie".
- Vergrendelde documenten die zijn ontkoppeld van een zaak, kunnen worden ontgrendeld en verwijderd uit de inbox voor ontkoppelde documenten.
- Het BAG-objectveld in het dialoogvenster voor het aanmaken van zaken is actief en BAG-objecten worden correct opgeslagen bij aanmaak. Het uitsteldialoogvenster gebruikt het label "Reden" voor het redenerveld.

## Documentation

- De BPMN-handleiding is bijgewerkt met documentatie over het activeren van een automatische ontvangstbevestiging vanuit een BPMN-proces en het gebruik van SmartDocuments-sjabloongroepen en -sjablonen die in ZAPS zijn geconfigureerd vanuit een BPMN-procesformulier.
- Gebruikershandleidingen zijn bijgewerkt om de op PABC gebaseerde IAM-architectuur te weerspiegelen.

## Library updates

| Frontend                | From version | To version |
|-------------------------|--------------|------------|
| Node.js                 | v22.22.2     | v22.22.3   |
| Angular monorepo        | v19.2.20     | v19.2.22   |
| TanStack Query monorepo | v5.99.0      | v5.100.14  |
| `@types/node`           | v22.19.17    | v22.19.19  |
| `angular-cli` monorepo  | v19.2.24     | v19.2.26   |
| `ol` (OpenLayers)       | v10.8.0      | v10.9.0    |

| Backend                                     | From version | To version |
|---------------------------------------------|--------------|------------|
| Java JRE                                    | v21.0.10     | v21.0.11   |
| Kotlin                                      | v2.3.10      | v2.3.21    |
| WildFly                                     | v39.0.0      | v39.0.1    |
| `org.keycloak:keycloak-admin-client`        | v26.0.8      | v26.0.9    |
| `io.smallrye.openapi`                       | v4.3.0       | v4.3.1     |
| `commons-io:commons-io`                     | v2.21.0      | v2.22.0    |
| Jackson monorepo                            | v2.21.2      | v2.21.3    |
| `com.auth0:java-jwt`                        | v4.5.1       | v4.5.2     |
| Infinispan                                  | v16.1.3      | v16.1.4    |
| Caffeine cache                              | v3.2.3       | v3.2.4     |
| `kotlinx-coroutines`                        | v1.10.2      | v1.11.0    |
| OpenTelemetry Java monorepo                 | v1.61.0      | v1.62.0    |
| SLF4J monorepo                              | v2.0.17      | v2.0.18    |
| `io.smallrye.openapi`                       | v4.3.1       | v4.3.3     |
| `kotlin-logging-jvm`                        | v8.0.01      | v8.0.03    |
| `nl.info.webdav:webdav-servlet`             | v1.2.263     | v1.2.287   |
| `opentelemetry-instrumentation-annotations` | v2.26.1      | v2.28.1    |
| Flyway                                      | v12.4.0      | v12.6.2    |

| Docker image                  | From version                 | To version                                    |
|-------------------------------|------------------------------|-----------------------------------------------|
| `eclipse-temurin`             | 21.0.11_10-jre-ubi10-minimal | 21.0.11_10-jre-ubi10-minimal (digest updated) |
| `gotenberg/gotenberg`         | v8.30.1                      | v8.33.0                                       |
| `curlimages/curl`             | v8.19.0                      | v8.20.0                                       |
| `openpolicyagent/opa`         | v1.15.2-static               | v1.16.2-static                                |
| `busybox`                     | v1.37.0-glibc                | v1.38.0-glibc                                 |
| `nginxinc/nginx-unprivileged` | v1.29.8                      | v1.31.1                                       |
| `solr`                        | 9.10.1-slim                  | 9.10.1-slim (digest updated)                  |

## CVE fixes

| CVE            | GitHub Security Advisory                                                 | Severity | Description                                                                                          |
|----------------|--------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------------------------|
| CVE-2026-44728 | [GHSA-fv7c-fp4j-7gwp](https://github.com/advisories/GHSA-fv7c-fp4j-7gwp) | high     | `@babel/plugin-transform-modules-systemjs` generates arbitrary code when compiling malicious input   |
| CVE-2026-6322  | [GHSA-v39h-62p7-jpjc](https://github.com/advisories/GHSA-v39h-62p7-jpjc) | high     | `fast-uri` vulnerable to host confusion via percent-encoded authority delimiters                     |
| CVE-2026-6321  | [GHSA-q3j6-qgpj-74h6](https://github.com/advisories/GHSA-q3j6-qgpj-74h6) | high     | `fast-uri` vulnerable to path traversal via percent-encoded dot segments                             |
| CVE-2026-41239 | [GHSA-crv5-9vww-q3g8](https://github.com/advisories/GHSA-crv5-9vww-q3g8) | medium   | `DOMPurify` has a SAFE_FOR_TEMPLATES bypass in RETURN_DOM mode                                       |
|                | [GHSA-39q2-94rc-95cp](https://github.com/advisories/GHSA-39q2-94rc-95cp) | medium   | `DOMPurify` ADD_TAGS function form bypasses FORBID_TAGS due to short-circuit evaluation              |
| CVE-2026-41240 | [GHSA-h7mw-gpvr-xq4m](https://github.com/advisories/GHSA-h7mw-gpvr-xq4m) | medium   | `DOMPurify` FORBID_TAGS bypassed by function-based ADD_TAGS predicate                                |
| CVE-2026-41238 | [GHSA-v9jr-rg53-9pgp](https://github.com/advisories/GHSA-v9jr-rg53-9pgp) | medium   | `DOMPurify` Prototype Pollution to XSS bypass via CUSTOM_ELEMENT_HANDLING fallback                   |
| CVE-2026-41305 | [GHSA-qx2v-qp2m-jg93](https://github.com/advisories/GHSA-qx2v-qp2m-jg93) | medium   | `PostCSS` XSS via unescaped `</style>` in CSS stringify output                                       |
| CVE-2026-40895 | [GHSA-r4q5-vmmm-2653](https://github.com/advisories/GHSA-r4q5-vmmm-2653) | medium   | `follow-redirects` leaks custom authentication headers to cross-domain redirect targets              |
| CVE-2026-42338 | [GHSA-v2v4-37r5-5v8g](https://github.com/advisories/GHSA-v2v4-37r5-5v8g) | medium   | `ip-address` XSS in Address6 HTML-emitting methods                                                   |
| CVE-2026-8723  | [GHSA-q8mj-m7cp-5q26](https://github.com/advisories/GHSA-q8mj-m7cp-5q26) | medium   | `qs` remotely triggerable DoS via `stringify` crash on null/undefined entries in comma-format arrays |
| CVE-2026-41907 | [GHSA-w5hq-g745-h8pq](https://github.com/advisories/GHSA-w5hq-g745-h8pq) | medium   | `uuid` missing buffer bounds check in v3/v5/v6 when `buf` is provided                                |
