# Commit ranges: v5.0.0 → v5.4.4

Raw `git log` output (non-merge commits) for three tag ranges, generated on 2026-08-28.
Format: `<short hash> <commit date> <subject>`. Library/dependency-bump commits (Renovate
`chore(deps)`/`fix(deps)` prefixes) are broken out into four tables at the bottom of each
section, showing only the library and the version it was bumped to. Commits that don't
explicitly name a library and a target version (lock file maintenance, digest-only repins,
vague `(minor)`/`(patch)` bumps) are omitted.

Docker-image bumps are split by which file(s) they touch: an image bumped only in the Helm
chart (`charts/zac/`) is counted as production; an image bumped in `docker-compose.yaml`
is counted as platform, even if the same commit also updates the Helm chart (several images —
e.g. gotenberg, nginx-unprivileged, OPA, the OTel collector image, solr — are pinned in both
places and bumped together in one commit).

## v5.0.0 → v5.0.2 (50 commits)

### Changes (24)

```
aca48bca5 2026-08-13 feat: make BRP verwerkingregister extension with zaaktype configurable (#6757)
e1580c57c 2026-06-04 chore: Reschedule e2e-tests to run at 1:00h UTC (#6125)
11f43e733 2026-06-04 fix(app): Postpone dialog + Zaak create changes (#6120)
15af196ce 2026-06-03 fix(ci): restore --helm-extra-args in helm-chart-testing install step (#6124)
164ada4b9 2026-06-03 fix: helm configurations (#6119)
e04a21b3a 2026-06-03 chore: Removed temporary logging (#6112)
d8cfb6712 2026-06-03 chore: make 1password opt-out in docker compose script (#6113)
20ba2e5a3 2026-06-03 fix: Unlinked documents can now be unlocked (#6117)
36c04d98b 2026-06-03 fix: Betrokkenheid of klanten was not matched correctly (#6111)
2d2f222f5 2026-06-02 feat: Fix e2e tests (#6110)
e4b5326c0 2026-06-02 fix: set profile null when initiator is changed (#6109)
8100e5964 2026-06-01 feature: add aanvullende informatie to fase check (#6108)
9dc15d898 2026-06-01 fix: Form shows employee id and group id instead of full name (#6107)
9e7cf83fd 2026-06-01 feat(brp): make BRP protocollering headers fully configurable (#6069)
c1edce220 2026-06-01 feature: show adres type with correct formatting (#6068)
96f66ecc2 2026-05-29 fix(formio): BPMN task submit hide post-submit (#6065)
303a828b6 2026-05-28 fix: BPMN tasks used group/user full names instead of id (#6094)
d540abcd9 2026-05-28 fix: required bug and refactored taken-vrijgeven-dialog.component.ts… (#6093)
52976809f 2026-05-28 chore: support productaanvraag flow using Open Formulieren in local Docker Compose setup (#6075)
03f16c4d4 2026-05-28 fix(initiator): hide aanvraagspecifiek panel when empty and show hint on bedrijfsgegevens (#6072)
70c307309 2026-05-28 fix: use disabled fields values for creating documents (#6074)
8258f832d 2026-05-28 feat: Limit open-forms to podiumD supported versions in renovate config (#6088)
b56caa0f5 2026-05-27 fix: fix mail template creation JSON deserialization issue. (#6073)
02c06461c 2026-05-27 fix(dev-seed): align Test zaaktype 1/3 doorlooptijd_behandeling with TEST/PROD (#6060)
```

### Library updates — production code (5)

Includes Helm-only image bumps; excludes anything that also touches docker-compose.

| Library | Version |
|---|---|
| tanstack-query | v5.100.14 |
| typescript-eslint | v8.59.4 |
| com.diffplug.spotless | v8.6.0 |
| busybox | v1.38.0 |
| dimpact-zaakafhandelcomponent | v5 |

### Library updates — test code (0)

| Library | Version |
|---|---|

### Library updates — CI (GitHub Actions) (8)

| Library | Version |
|---|---|
| github/codeql-action | v4.36.1 |
| actions/checkout | v6.0.3 |
| umbrelladocs/action-linkspector | v1.5.2 |
| friedinger/deletebranchcaches | v2.4.2 |
| docker/setup-buildx-action | v4.1.0 |
| docker/login-action | v4.2.0 |
| docker/build-push-action | v7.2.0 |
| github/codeql-action | v4.36.0 |

### Library updates — platform images (3)

Docker-compose image bumps, including images that are also referenced from the Helm chart.

| Library | Version |
|---|---|
| gotenberg/gotenberg | v8.33.0 |
| nginxinc/nginx-unprivileged | v1.31.1 |
| rabbitmq | v4.2.7 |

## v5.4.0 → v5.4.4 (38 commits)

### Changes (15)

```
85dda9fb5 2026-08-25 fix(document): add missing import for createZaakInformatieobjectForReads
c43642153 2026-08-20 fix(document): keep documents that are already linked to a zaak out of the inbox (#6852)
956ce709e 2026-08-12 fix(admin): match resultaattypen by omschrijving when copying zaakbeeindig gegevens (#6763)
bc0787bff 2026-08-13 feat: make BRP verwerkingregister extension with zaaktype configurable (#6757)
bde2afe2c 2026-08-06 fix: use dd-mm-yyyy in date range picker (#6738)
ab0b9c2be 2026-08-03 feat(bpmn): datagrid-based document selection and signing (#6645)
054fbf9c7 2026-07-28 feat(app): prevent multiple submits on mailtemplate and document forms, and migrate document/zaak link actions to TanStack (#6615)
90241fd15 2026-07-27 feat(app): Goedkeuren task - filter out signed documents and sort task menu list (#6610)
bc5ac3443 2026-07-24 fix: fix flaky e2e test where zaakdata was fetched before ZAC has had a change to add all variables to zaakdata (#6614)
a98d5ae2b 2026-07-23 chore: upgrade to Open Object 4.0.2 in Docker Compose (#6611)
338edab1b 2026-07-23 feat(app): Prevent multiple submit clicks on location, case-link and besluit-create forms (#6589)
44e9915bd 2026-07-23 fix: fix Trivy Renovate config (#6609)
aa2827d50 2026-07-23 fix: reduce width taken by checkboxes in werklijsten (#6607)
96cac4701 2026-07-23 feat(app): Align and fix contact/initiator email retrieval (#6599)
200dd962c 2026-07-22 chore: update Renovate Objects API component version to 4.0.x (#6596)
```

### Library updates — production code (9)

Includes Helm-only image bumps; excludes anything that also touches docker-compose.

| Library | Version |
|---|---|
| com.diffplug.spotless | v8.9.0 |
| less | v4.8.0 |
| tanstack-query | v5.101.4 |
| typescript-eslint | v8.65.0 |
| tanstack-query | v5.101.3 |
| io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations | v2.30.0 |
| nl.jacobras:human-readable | v1.13.1 |
| itextpdf | v9.7.1 |
| nl.info.webdav:webdav-servlet | v1.2.313 |

### Library updates — test code (2)

| Library | Version |
|---|---|
| @playwright/test | v1.62.0 |
| @cucumber/cucumber | v13.2.0 |

### Library updates — CI (GitHub Actions) (3)

| Library | Version |
|---|---|
| docker/login-action | v4.5.2 |
| docker/login-action | v4.5.1 |
| docker/login-action | v4.5.0 |

### Library updates — platform images (5)

Docker-compose image bumps, including images that are also referenced from the Helm chart.

| Library | Version |
|---|---|
| openformulieren/open-forms | v3.5.6 |
| redis | v8.6.5 |
| pabc | v1.1.1 |
| pabc | v1.1.1 |
| otel/opentelemetry-collector-contrib | v0.157.0 |

## v5.0.0 → v5.4.0 (450 commits)

### Changes (182)

```
b6cd7e7e0 2026-07-22 fix: filter on zaaktype omschrijving in stead of uuid so we support multiple zaaktype versions (#6593)
98bc6b221 2026-07-22 chore: remove unused contactgegevens OpenAPI spec (#6591)
f3cce4f37 2026-07-21 chore: improve code in SearchService and some renaming (#6587)
af84f9ce9 2026-07-21 refactor: a more manageable setup for the styling of werklijsten (#6547)
823e1aa6a 2026-07-21 feat(app): Move dialog/action-row wrapper out of ZacFormActions component (#6578)
a481179eb 2026-07-21 feat: filter on einddatum when searching for linkable zaken (#6576)
740a3ce52 2026-07-21 feat: filter on startdatum while searching for linkable zaken (#6573)
ea4160a2f 2026-07-21 chore: update OpenAPI specs for `contactgegevens` and `klantinteracties` (#6575)
4e6421fd7 2026-07-20 feat: find zaak to link - filter on zaaktype (#6555)
b7b07bc61 2026-07-20 chore: sync WildFly-provided dependency versions using BOMs (#6565)
22e209ccb 2026-07-20 chore: Added objecten-api-celery to docker-compose.arm64-override.yaml (#6564)
0cb6bfb2e 2026-07-17 chore: convert net.atos.zac.util.time package to Kotlin (#6554)
b38c3b34d 2026-07-16 chore: delete post function from ZTC client (#6553)
90f9c9ad3 2026-07-16 feat: find zaak to link - search for omschrijving (#6541)
fb33398a7 2026-07-16 feat: Backend afleidingswijze brondatum eigenschap (#6543)
148cff0d8 2026-07-15 feature: changed naam to name for displaying communicatiekanalen (#6546)
a69b38b5a 2026-07-15 chore: remove duplicate BPMN configuration beheer service unit test (#6542)
7bd259bf3 2026-07-15 fix(werklijsten): give the Zaaknummer column only the width it needs (#6540)
9e424ed60 2026-07-15 chore: move parameters for findLinkableZaken to class (#6535)
5edb33b98 2026-07-15 chore: use lowercase Kotest BDD style (#6538)
cfc306218 2026-07-14 chore: upgrade to Kotlin 2.4.0 (#6537)
4e38ee893 2026-07-14 Docker-compose: documentation update (#6522)
d5fd29f64 2026-07-14 chore: convert zgw util package to Kotlin (#6536)
e8adbcc25 2026-07-14 fix: validate datasource connections on match to survive dropped PostgreSQL connections (#6524)
436b83bd5 2026-07-14 chore: remove redundant tests (#6531)
53f1de66a 2026-07-14 fix: enable PKCE in local ZAC setup (#6534)
03ab705a8 2026-07-14 feat: add configurable PKCE support for the OIDC authorization code flow (#6490)
9e4e9214a 2026-07-14 fix(werklijsten): keep sticky action columns opaque while paginating (#6530)
b8341f2bd 2026-07-14 chore: use doctoc to update and check table of contents in ZAC manuals (#6525)
211e526d9 2026-07-14 chore: Remove obsolete PoC-code (#6523)
d683eee3b 2026-07-13 feat(admin): Reference table - enforce max lengths in backend API (#6499)
d1092dced 2026-07-13 fix(forms): prevent double submit on the remaining forms (#6502)
bfdbf221d 2026-07-13 refactor(shared): remove dead MFB dialog and orphaned form-field builders (#6519)
1f03ecee6 2026-07-13 refactor(zaak-betrokkenen): migrate betrokkene-ontkoppelen dialog off MFB (#6516)
b02d20ca3 2026-07-10 Update TOC for IAM (#6503)
6a99034d0 2026-07-10 docker-compose: Linux fixed for host-user with uid != 1000 (#6505)
eabad6830 2026-07-09 fix: only clear title and file in the 'another document' flow (#6496)
c56d61534 2026-07-09 fix: fixed Open Zaak database setup script for zaaktype eigenschap-specificaties (#6497)
32f7c6e96 2026-07-09 docs: improve some GitHub docs (#6498)
d0659a70e 2026-07-09 feature: fixed bsn not showing bug and extracted zaak-betrokkene to new standalone component (#6491)
b77c23200 2026-07-09 feat(app): Admin - Reference table V2 - increase table value max length to 1000 (#6487)
e1de4e74d 2026-07-09 feat(app/admin): Admin - Remove old reference table page, cookie, and ATOS MFB edit field PLUS all admin routes lazy loading (#6468)
1f430be94 2026-07-09 refactor(dialog): replace ATOS MFB shared dialog with native generic dialog (#6473)
b8e3256df 2026-07-09 feat: Updated open-zaak to 1.29.1 (#6479)
4fb67879b 2026-07-09 feat: continue with productaanvraag handling when attached document cannot be retrieved (#6488)
65432f0e0 2026-07-07 feature: create ZaakHistoryComponent and fetch history on tab click (#6464)
17103a88e 2026-07-07 feature: tighten security on cronjob. fixes trivy findings (#6474)
feb044b0c 2026-07-07 refactor: consolidate zaak koppelen decision logic (#6459)
73c92c43b 2026-07-07 feat(pipeline): Exclude FE routes from tests (#6469)
468d714e0 2026-07-07 docs(openspec): archive prevent-duplicate-zaak-behandelaar-assignment (#6470)
881e492fe 2026-07-07 chore: Update renovate.json for podiumd alignment (#6465)
7cdb35e75 2026-07-07 feature(app/admin): Admin - Reference table V2- Adjust button into primary and text + minor language label  (#6467)
0d4d70a62 2026-07-07 feat: related zaken can be ontkoppeld regardless of whether they are open or closed (#6463)
7043bc5ce 2026-07-06 feat(app/admin): Admin Referentietabel - Refactor ATOS MFB forms using angular form and fields (#6462)
559403916 2026-07-06 docs: improvements in CONTRIBUTING.md (#6461)
06e5990da 2026-07-06 chore: update access control policies for zaak koppelen (#6460)
843c1e759 2026-07-06 feat: zaak relateren ongeacht open/gesloten (#6433)
6975fbc33 2026-07-06 feature: show gerelateerde zaak zaakidentificatie in zaak history (#6419)
94ff34537 2026-07-06 docs: small improvements in publiccode.yaml and CONTRIBUTING.md (#6440)
a21d8e3b4 2026-07-03 feature: use new endpoint for autorisation of multiple zaaktypes in frontend zaken/taken verdelen dialog (#6412)
11b00f033 2026-07-02 chore: add more tests for zaak koppelen (#6438)
0c58bcc9b 2026-07-02 feat(app): Zaak Create + Zaak Wijzigen - prevent duplicate submissions on forms (#6424)
fc34e43c6 2026-07-02 fix: disallow multiple parallel zaak assignment calls for the same zaak in the backend (#6423)
6bb807ced 2026-07-01 fix: consistent field alignment, dividers and spacing in sidebar forms (#6404)
94a59d1f2 2026-07-01 docs: update developer docs for Java 25 upgrade (#6420)
f80ce8198 2026-07-01 feature: aggregated status for helm bump/test/docs so it can be added as a required status check (#6417)
244a45ad5 2026-07-01 chore: upgrade source and target compatibility to Java 25 (#6418)
7caca9837 2026-06-30 feature(app): Angular v20 upgrade - Revert button letter-spacing override (legacy pre-M3 token) (#6415)
ab2b8a31b 2026-06-30 feature: automerge weekly lockfile maintenance (#6407)
36198ca2a 2026-06-30 feat(app): Angular v20 upgrade -  Remove dead material css selectors (#6405)
acb57d988 2026-06-30 feat(app): Angular v20 upgrade - migrate renamed Material 20 theme tokens (--mdc-* → --mat-*) (#6403)
6161fc942 2026-06-30 fix(besluit): persist cleared optional date fields on besluit edit (#6385)
c0489e7cd 2026-06-30 feat(app-deps): Upgrade Angular 19.2 to 20.3 (#6395)
ee2ce843a 2026-06-29 feat: Update openforms allowedVersions to '< 3.6.0' in renovate.json (#6398)
70038fbce 2026-06-29 feat(app): Documentenlijst - Load lists via TanStack Query & disable toggle while loading (#6379)
20a9b731e 2026-06-29 chore: added missing functional role mapping in PABC Docker Compose for raadpleger2newiam test user (#6397)
bb794bd18 2026-06-29 chore: updated Objecten 3.6.1 OpenAPI spec (#6394)
a1489c234 2026-06-29 feat: add list authorised behandelaar groups for multiple zaaktypes endpoint (#6374)
fa2d8db5b 2026-06-29 chore: remove hand-authored Bruno test suite and related artifacts (#6376)
290da95fd 2026-06-26 feature: Enable automerge for patch-level updates (#6378)
a9c5039cb 2026-06-25 feature: fix HIGH severity and report all others too (#6377)
dd7fa9fbb 2026-06-25 feature: show gerelateerde zaken documenten and make slider active (#6370)
758f5f8ec 2026-06-25 feat: autorize brp zoeken binnengemeentelijk (#6265)
9f472e76f 2026-06-25 feature(app): Specs prep changes to upgrade to Angular v20 (#6367)
ca607c826 2026-06-25 feature: set readonlyfilesystem to true in helm pods (#6358)
3cfb462a5 2026-06-25 refactor(material-form-builder): remove orphaned ATOS (MFB) date/divider/paragraph form components (#6366)
33345faee 2026-06-25 fix: resolve incompatible Vite version breaking local frontend on port 4200 (#6365)
138591575 2026-06-25 refactor(besluit): migrate besluit-edit off MFB to explicit form components (#6275)
ff0f5b3f3 2026-06-25 refactor(material-form-builder): remove orphaned medewerker-groep field (#6342)
f95023b67 2026-06-25 refactor(taken): Taak View - Refactor to Angular form components and remove obsolete MFB code (#6341)
60797c4b0 2026-06-25 feature: keep nvd database cache in docker image (#6364)
e4d88ce6c 2026-06-24 feature: Improvements to the E2E tests (#6353)
63e26b711 2026-06-24 chore: add unit tests for Hibernate Validator validations (#6351)
8d216edc1 2026-06-24 chore: improve frontend CI unit tests performance by adding Jest transform cache (#6349)
5740c2f84 2026-06-23 feature: fix small security findings in Dockerfile and chart (#6347)
edf3fe194 2026-06-23 chore: add a simple docs/readme and simplify some backend code (#6346)
bfbe2a315 2026-06-23 Docs:PZ-9840 (#6343)
bcc4a8bed 2026-06-23 feature: introduce trivy iac scanner (#6339)
bfded4862 2026-06-23 chore: add backend unit test coverage (#6328)
db51c386e 2026-06-23 feature: Keep trivy up-to-date using renovate and improve visibility on reporting issues (#6336)
66cc8046e 2026-06-23 feature: restore enums (#6331)
3a262626f 2026-06-22 chore: add more unit tests for converters (#6315)
ccc78ba88 2026-06-22 feature: removed dead code from relevante zaken (#6307)
55096dd6e 2026-06-22 chore: fix Gradle and compiler warnings (#6310)
5b44f4807 2026-06-22 test: remove extraneous space issue in e2e tests (#6302)
6aa9bfd02 2026-06-22 feature: some lowhanging fruit changes for optimizing the dependency security scan workflow (#6300)
f3514d2c2 2026-06-22 chore: add more unit test coverage for backend converter classes (#6299)
e31b8b400 2026-06-18 chore: convert admin package to Kotlin (#6288)
9bde0a2f2 2026-06-18 chore: update documentation to reflect INFO.nl ICATT branding and improve clarity (#6283)
39250ac59 2026-06-18 chore(app-security): patch low-hanging frontend npm CVEs via pinned overrides (#6280)
7a764b7a5 2026-06-18 chore: use GitHub artifacts for cross-job data (#6278)
6acaaef1e 2026-06-18 chore: refactor zaakrestservice and unit tests (#6269)
f03f714a4 2026-06-18 feature: update maxLength of organisatorischeEenheidIdentificatie & medewerkerIdentificatie (#6274)
af3aa998f 2026-06-18 feat(app): resolve strict TypeScript errors in shared model classes (#6268)
95a935d52 2026-06-18 refactor(material-form-builder): remove obsolete ATOS MFB components message and hidden input  (#6266)
0443f7403 2026-06-17 refactor(besluit): migrate besluit-view off MFB to static display + intrekken dialog (#6241)
1180081fc 2026-06-17 feat: improve SECURITY.md file with timeline, supported releases and preferred channels (#6263)
444565c69 2026-06-17 fix: Field gerelateerdeZaken cannot be set to null, but must be an empty list (#6262)
00ca5c2a5 2026-06-17 feat: Renovate - Pin Angular Renovate group to v20.x (#6259)
1e119f4bc 2026-06-17 chore: add explicit targets for spotless tasks  (#6256)
c4cc9e65f 2026-06-17 test: improve human task retrieval logic in goedkeuren integration tests to make them less flaky (#6255)
e482fb3c8 2026-06-17 chore: exclude unused Solrj modules from dependencies (#6250)
00b33c6d3 2026-06-17 docs: Add documentation for brp_zoeken (#6239)
3067eb926 2026-06-16 chore: upgrade WildFly to 40.0 (#6246)
db3bae6bb 2026-06-16 fix(app): resolve 52 strict TypeScript errors in zoek-object models (#6244)
8ddd6c194 2026-06-16 feat: Add support for gerelateerde zaken (#6235)
92175e189 2026-06-16 chore: rename decision in backend to besluit (#6240)
8d17b5978 2026-06-16 chore: fix compiler warnings (#6237)
b68ded4fd 2026-06-16 feat(brp): introduce brp_zoeken role to control BRP person search (#6215)
7b90586be 2026-06-16 fix: add missing authorisation checks for download process diagram and list afzenders for zaak (#6229)
dad595b1d 2026-06-16 feat: Add authorization checks to BPMN processes (#6162)
f07e1c6ad 2026-06-16 refactor(taken): remove dead ATOS form-builder code (#6228)
aa524b507 2026-06-16 chore: document two gotchas for wsl development (#6234)
1f483bcc3 2026-06-16 chore: update env example to use host.docker.internal (#6230)
b82e2b468 2026-06-16 feat(deps): group all Angular packages in renovate config (#6223)
d050f2473 2026-06-16 fix: add authorisation check for list besluiten for zaak endpoint in backend (#6225)
e203005af 2026-06-16 refactor(informatie-objecten): InformatieObjectVerzendenComponent - migrate MFB form to explicit form components (#6182)
f6d4765d8 2026-06-15 fix: revert RabbitMQ Docker image to 4.2.7 (#6219)
235808795 2026-06-15 chore: revert host file check in docker compose script (#6212)
36907a211 2026-06-15 chore: log a warning in the docker compose script if host.docker.internal does not point to localhost (#6180)
6060e60d6 2026-06-15 feat: Update Open Zaak OpenAPI specs for version 2.7.1 (#6211)
0c057b52f 2026-06-11 refactor(plan-items): HumanTaskDoComponent - Remove MFB form logic in and make component standalone (#6179)
00c686d31 2026-06-11 fix(documenten): validate document uploads by extension only (#6172)
88cc7e37f 2026-06-09 fix(documenten): accept OS/browser media-type variants for document uploads (#6167)
0c64e85ea 2026-06-09 chore: alter E2E test and add documentation (#6165)
4c99afdf2 2026-06-09 chore(app): Angular v19 migration to standalone - ZacForm (#6166)
94eac1ca1 2026-06-08 chore(app): Angular v19 migration to standalone - ZacFile, ZacDocuments, ZacHtmlEditor (#6147)
a3c908982 2026-06-08 chore: allow public access to favicon and PWA static files (#6155)
cda0165b4 2026-06-08 feat(security): move file type allowlist to backend and validate uploads (#6071)
09fa696aa 2026-06-08 chore: fix lint sentinel file created in project root instead of build dir (#6151)
e0d36f6eb 2026-06-08 feature: create custom functions in formio (#6118)
a03ad0096 2026-06-05 feat: NVD database caching and increase rate limit (#6137)
6f65cab6f 2026-06-05 chore(app): Remove unused ATOS material-form-builder components (#6128)
571acce8c 2026-06-05 fix: Separate lint + testbump steps — the testbump step runs with if: always() and set +e, so it correctly detects version bump needs even after a lint failure (#6130)
d59c92562 2026-06-05 chore(app): Favicon (#6129)
f987b8f97 2026-06-05 feat(task-forms): migrate DOCUMENT_VERZENDEN_POST to Angular task form (#6051)
6a9ccbbeb 2026-06-05 feat(app): Angular v19 migration to standalone components - Dashboard + Toolbar (#6127)
54523b1dd 2026-06-05 feat(app): Zaak-data - styling +  copy-to-clipboard (AKA The Maurits change) (#6126)
280912b52 2026-06-05 feat(task-forms): migrate DEFAULT_TAAKFORMULIER to Angular task form (#6050)
e1580c57c 2026-06-04 chore: Reschedule e2e-tests to run at 1:00h UTC (#6125)
11f43e733 2026-06-04 fix(app): Postpone dialog + Zaak create changes (#6120)
15af196ce 2026-06-03 fix(ci): restore --helm-extra-args in helm-chart-testing install step (#6124)
164ada4b9 2026-06-03 fix: helm configurations (#6119)
e04a21b3a 2026-06-03 chore: Removed temporary logging (#6112)
d8cfb6712 2026-06-03 chore: make 1password opt-out in docker compose script (#6113)
20ba2e5a3 2026-06-03 fix: Unlinked documents can now be unlocked (#6117)
36c04d98b 2026-06-03 fix: Betrokkenheid of klanten was not matched correctly (#6111)
2d2f222f5 2026-06-02 feat: Fix e2e tests (#6110)
e4b5326c0 2026-06-02 fix: set profile null when initiator is changed (#6109)
8100e5964 2026-06-01 feature: add aanvullende informatie to fase check (#6108)
9dc15d898 2026-06-01 fix: Form shows employee id and group id instead of full name (#6107)
9e7cf83fd 2026-06-01 feat(brp): make BRP protocollering headers fully configurable (#6069)
c1edce220 2026-06-01 feature: show adres type with correct formatting (#6068)
96f66ecc2 2026-05-29 fix(formio): BPMN task submit hide post-submit (#6065)
303a828b6 2026-05-28 fix: BPMN tasks used group/user full names instead of id (#6094)
d540abcd9 2026-05-28 fix: required bug and refactored taken-vrijgeven-dialog.component.ts… (#6093)
52976809f 2026-05-28 chore: support productaanvraag flow using Open Formulieren in local Docker Compose setup (#6075)
03f16c4d4 2026-05-28 fix(initiator): hide aanvraagspecifiek panel when empty and show hint on bedrijfsgegevens (#6072)
70c307309 2026-05-28 fix: use disabled fields values for creating documents (#6074)
8258f832d 2026-05-28 feat: Limit open-forms to podiumD supported versions in renovate config (#6088)
b56caa0f5 2026-05-27 fix: fix mail template creation JSON deserialization issue. (#6073)
02c06461c 2026-05-27 fix(dev-seed): align Test zaaktype 1/3 doorlooptijd_behandeling with TEST/PROD (#6060)
```

### Library updates — production code (98)

Includes Helm-only image bumps; excludes anything that also touches docker-compose.

| Library | Version |
|---|---|
| less | v4.7.0 |
| flyway | v13 |
| nl.info.webdav:webdav-servlet | v1.2.311 |
| org.openapi.generator | v7.24.0 |
| wildfly/wildfly | v41 |
| typescript-eslint | v8.64.0 |
| org.keycloak:keycloak-admin-client | v26.0.11 |
| nl.info.webdav:webdav-servlet | v1.2.309 |
| nl.info.webdav:webdav-servlet | v1.2.307 |
| eslint | v9.39.5 |
| npm | v11 |
| com.auth0:java-jwt | v4.6.0 |
| typescript-eslint | v8.63.0 |
| opentelemetry-java | v1.64.0 |
| opentelemetry-collector | v0.165.0 |
| nl.info.webdav:webdav-servlet | v1.2.306 |
| @types/node | v22.20.1 |
| io.smallrye.openapi | v4.3.5 |
| flyway | v12.11.0 |
| opentelemetry-collector | v0.164.1 |
| itextpdf | v9.7.0 |
| opentelemetry-collector | v0.164.0 |
| http-proxy-middleware | v4.2.0 |
| com.itextpdf:html2pdf | v6.3.3 |
| opentelemetry-collector | v0.163.0 |
| jackson | v2.22.1 |
| nl.info.webdav:webdav-servlet | v1.2.305 |
| webpack-dev-server | v6 |
| webpack-dev-server | v5.2.6 |
| angular | v20.3.31 |
| serialize-javascript | v7.0.7 |
| typescript-eslint | v8.62.1 |
| nl.info.webdav:webdav-servlet | v1.2.304 |
| opentelemetry-collector | v0.162.0 |
| opentelemetry-collector | v0.160.0 |
| eclipse-temurin | v25 |
| tanstack-query | v5.101.2 |
| nl.info.webdav:webdav-servlet | v1.2.303 |
| org.flywaydb:flyway-core | v12.10.0 |
| org.keycloak:keycloak-admin-client | v26.0.10 |
| com.diffplug.spotless | v8.8.0 |
| opentelemetry-collector | v0.159.2 |
| opentelemetry-collector | v0.159.1 |
| gradle | v9.6.1 |
| tanstack-query | v5.101.1 |
| less | v4.6.7 |
| typescript-eslint | v8.62.0 |
| @formio/angular | 10 |
| busybox | v1.38.0 |
| curlimages/curl | v8.21.0 |
| node.js | v22.23.1 |
| @types/node | v22.20.0 |
| opentelemetry-collector | v0.159.0 |
| webdav.servlet | v1.2.300 |
| opentelemetry.instrumentation | v2.29.0 |
| gradle | v9.6.0 |
| http-proxy-middleware | v4 |
| vite | v8 |
| typescript-eslint | v8.61.1 |
| vite | v7 |
| flyway | v12.9.0 |
| node.js | v22.23.0 |
| docker/dockerfile | v1.25.0 |
| kotlin.csv | v2 |
| webdav.servlet | v1.2.298 |
| less | v4.6.6 |
| opentelemetry-collector | v0.158.2 |
| webdav.servlet | v1.2.297 |
| flowable | v8 |
| spotless | v8.7.0 |
| webdav.servlet | v1.2.296 |
| @types/node | v22.19.21 |
| kotlin.csv | v1.11.0 |
| infinispan | v16.2.1 |
| opentelemetry-java | v1.63.0 |
| okhttp | v5.4.0 |
| openapi.generator | v7.23.0 |
| openapi | v4.3.4 |
| tanstack-query | v5.101.0 |
| flyway | v12.8.1 |
| typescript-eslint | v8.61.0 |
| proj4 | v2.20.9 |
| webdav.servlet | v1.2.295 |
| @types/node | v22.19.20 |
| opentelemetry-collector | v0.158.1 |
| webdav.servlet | v1.2.293 |
| angular | v19.2.25 |
| opentelemetry-collector | v0.156.2 |
| angular-cli | v19.2.27 |
| eslint-plugin-prettier | v5.5.6 |
| dimpact-zaakafhandelcomponent | v5.0.1 |
| jackson | v2.21.4 |
| angular | v19.2.24 |
| tanstack-query | v5.100.14 |
| typescript-eslint | v8.59.4 |
| com.diffplug.spotless | v8.6.0 |
| busybox | v1.38.0 |
| dimpact-zaakafhandelcomponent | v5 |

### Library updates — test code (16)

| Library | Version |
|---|---|
| kotest | v6.2.3 |
| @cucumber/cucumber | v13.1.1 |
| multiple-cucumber-html-reporter | v4.1.0 |
| kotest | v6.2.2 |
| expect-type | v1.4.0 |
| @playwright/test | v1.61.1 |
| playwright-bdd | v9.2.0 |
| kotest | v6.2.1 |
| playwright | v1.61.0 |
| @cucumber/cucumber | v13 |
| playwright-bdd | v9 |
| kotest | v6.2.0 |
| multiple-cucumber-html-reporter | v4 |
| mockk | v1.14.11 |
| jacoco | v0.8.15 |
| github.kotlin.logging | v8.0.4 |

### Library updates — CI (GitHub Actions) (49)

| Library | Version |
|---|---|
| github/codeql-action | v4.37.3 |
| github/codeql-action | v4.37.2 |
| actions/checkout | v7.0.1 |
| actions/setup-java | v5.6.0 |
| github/codeql-action | v4.37.1 |
| slackapi/slack-github-action | v4 |
| actions/setup-node | v7 |
| slackapi/slack-github-action | v3.0.5 |
| slackapi/slack-github-action | v3.0.4 |
| helm cli | v4.2.3 |
| github/codeql-action | v4.37.0 |
| actions/setup-java | v5.5.0 |
| docker/login-action | v4.4.0 |
| friedinger/deletebranchcaches | v2.4.3 |
| dorny/paths-filter | v4.0.2 |
| docker/setup-buildx-action | v4.2.0 |
| github/codeql-action | v4.36.3 |
| docker/login-action | v4.3.0 |
| docker/build-push-action | v7.3.0 |
| java-jre | v25 |
| aquasecurity/trivy | v0.72.0 |
| actions/cache | v6.1.0 |
| actions/setup-java | v5.4.0 |
| actions/cache | v6 |
| azure/setup-helm | v5.0.1 |
| github artifact actions | v7 |
| github artifact actions | v8 |
| github artifact actions | v6 |
| github artifact actions | v5 |
| actions/checkout | v7 |
| helm cli | v4.2.2 |
| actions/setup-java | v5.3.0 |
| enricomi/publish-unit-test-result-action | v2.24.0 |
| helm cli | v4.2.1 |
| gradle/actions | v6.2.0 |
| umbrelladocs/action-linkspector | v1.5.4 |
| codecov/codecov-action | v7 |
| umbrelladocs/action-linkspector | v1.5.3 |
| gradle/actions | v6.1.1 |
| codecov/codecov-action | v6.0.2 |
| github/codeql-action | v4.36.2 |
| github/codeql-action | v4.36.1 |
| actions/checkout | v6.0.3 |
| umbrelladocs/action-linkspector | v1.5.2 |
| friedinger/deletebranchcaches | v2.4.2 |
| docker/setup-buildx-action | v4.1.0 |
| docker/login-action | v4.2.0 |
| docker/build-push-action | v7.2.0 |
| github/codeql-action | v4.36.0 |

### Library updates — platform images (46)

Docker-compose image bumps, including images that are also referenced from the Helm chart.

| Library | Version |
|---|---|
| grafana/grafana | v13.1.1 |
| openformulieren/open-forms | v3.5.5 |
| ghcr.io/infonl/objects-api | v3.6.2 |
| maykinmedia/objects-api | v3.6.2 |
| nginxinc/nginx-unprivileged | v1.31.3 |
| greenmail/standalone | v2.1.11 |
| greenmail/standalone | v2.1.10 |
| quay.io/keycloak/keycloak | v26.6.4 |
| prom/prometheus | v3.13.1 |
| open-archiefbeheer | v2 |
| otel/opentelemetry-collector-contrib | v0.156.0 |
| openzaak/open-zaak | v1.27.3 |
| openpolicyagent/opa | v1.18.2 |
| prom/prometheus | v3.13.0 |
| openformulieren/open-forms | v3.5.4 |
| openpolicyagent/opa | v1.18.1 |
| objects-api | v3.6.1 |
| openpolicyagent/opa | v1.18.0 |
| otel/opentelemetry-collector-contrib | v0.155.0 |
| ghcr.io/brp-api/personen-mock | v2.7.0-202606230850 |
| open-forms | v3.4.10 |
| grafana/tempo | v3 |
| nginxinc/nginx-unprivileged | v1.31.2 |
| open-zaak | v1.27.2 |
| ghcr.io/brp-api/personen-mock | v2.7.0-202606151541 |
| greenmail | v2.1.9 |
| open-notificaties | v1.15.0 |
| redis | v8.6.4 |
| grafana/tempo | v2.10.7 |
| prom/prometheus | v3.12.0 |
| gotenberg/gotenberg | v8.34.0 |
| open-forms | v3.4.9 |
| otel/opentelemetry-collector-contrib | v0.154.0 |
| ghcr.io/brp-api/personen-mock | v2.7.0-202606121007 |
| grafana/tempo | v2.10.6 |
| openpolicyagent/opa | v1.17.1 |
| redis | v8.4.4 |
| rabbitmq | v4.3.1 |
| ghcr.io/brp-api/personen-mock | v2.7.0-202606080929 |
| grafana | v13.0.2 |
| openpolicyagent/opa | v1.17.0 |
| ghcr.io/brp-api/personen-mock | v2.7.0-202606041530 |
| objecttypes-api | v3.4.2 |
| gotenberg/gotenberg | v8.33.0 |
| nginxinc/nginx-unprivileged | v1.31.1 |
| rabbitmq | v4.2.7 |

