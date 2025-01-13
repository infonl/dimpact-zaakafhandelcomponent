/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Response
import java.time.ZonedDateTime
import java.util.UUID

class ZacClient {
    private val logger = KotlinLogging.logger {}
    private var itestHttpClient = ItestHttpClient()

    @Suppress("LongMethod")
    fun createZaakAfhandelParameters(
        zaakTypeIdentificatie: String,
        zaakTypeUuid: UUID,
        zaakTypeDescription: String,
        productaanvraagType: String
    ): Response {
        logger.info {
            "Creating zaakafhandelparameters in ZAC for zaaktype with identificatie: $zaakTypeIdentificatie " +
                "and UUID: $zaakTypeUuid"
        }
        return itestHttpClient.performPutRequest(
            url = "$ZAC_API_URI/zaakafhandelparameters",
            requestBodyAsString = """{
              "humanTaskParameters": [
                {
                  "planItemDefinition": {
                    "defaultFormulierDefinitie": "AANVULLENDE_INFORMATIE",
                    "id": "AANVULLENDE_INFORMATIE",
                    "naam": "Aanvullende informatie",
                    "type": "HUMAN_TASK"
                  },
                  "defaultGroepId": null,
                  "formulierDefinitieId": "AANVULLENDE_INFORMATIE",
                  "referentieTabellen": [],
                  "actief": true,
                  "doorlooptijd": null
                },
                {
                  "planItemDefinition": {
                    "defaultFormulierDefinitie": "GOEDKEUREN",
                    "id": "GOEDKEUREN",
                    "naam": "Goedkeuren",
                    "type": "HUMAN_TASK"
                  },
                  "defaultGroepId": null,
                  "formulierDefinitieId": "GOEDKEUREN",
                  "referentieTabellen": [],
                  "actief": true,
                  "doorlooptijd": null
                },
                {
                  "planItemDefinition": {
                    "defaultFormulierDefinitie": "ADVIES",
                    "id": "ADVIES_INTERN",
                    "naam": "Advies intern",
                    "type": "HUMAN_TASK"
                  },
                  "defaultGroepId": null,
                  "formulierDefinitieId": "ADVIES",
                  "referentieTabellen": [
                    {
                      "veld": "ADVIES",
                      "tabel": {
                        "aantalWaarden": 5,
                        "code": "ADVIES",
                        "id": 1,
                        "naam": "Advies",
                        "systeem": true
                      }
                    }
                  ],
                  "actief": true,
                  "doorlooptijd": null
                },
                {
                  "planItemDefinition": {
                    "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                    "id": "ADVIES_EXTERN",
                    "naam": "Advies extern",
                    "type": "HUMAN_TASK"
                  },
                  "defaultGroepId": null,
                  "formulierDefinitieId": "EXTERN_ADVIES_VASTLEGGEN",
                  "referentieTabellen": [],
                  "actief": true,
                  "doorlooptijd": null
                },
                {
                  "planItemDefinition": {
                    "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                    "id": "DOCUMENT_VERZENDEN_POST",
                    "naam": "Document verzenden",
                    "type": "HUMAN_TASK"
                  },
                  "defaultGroepId": null,
                  "formulierDefinitieId": "DOCUMENT_VERZENDEN_POST",
                  "referentieTabellen": [],
                  "actief": true,
                  "doorlooptijd": null
                }
              ],
              "mailtemplateKoppelingen": [],
              "userEventListenerParameters": [
                {
                  "id": "INTAKE_AFRONDEN",
                  "naam": "Intake afronden",
                  "toelichting": null
                },
                {
                  "id": "ZAAK_AFHANDELEN",
                  "naam": "Zaak afhandelen",
                  "toelichting": null
                }
              ],
              "valide": false,
              "zaakAfzenders": [],
              "zaakbeeindigParameters": [],
              "zaaktype": {
                "beginGeldigheid": "2023-09-21",
                "doel": "$zaakTypeDescription",
                "identificatie": "$zaakTypeIdentificatie",
                "nuGeldig": true,
                "omschrijving": "$zaakTypeDescription",
                "servicenorm": false,
                "uuid": "$zaakTypeUuid",
                "versiedatum": "2023-09-21",
                "vertrouwelijkheidaanduiding": "openbaar"
              },
              "intakeMail": "BESCHIKBAAR_UIT",
              "afrondenMail": "BESCHIKBAAR_UIT",
              "caseDefinition": {
                "humanTaskDefinitions": [
                  {
                    "defaultFormulierDefinitie": "AANVULLENDE_INFORMATIE",
                    "id": "AANVULLENDE_INFORMATIE",
                    "naam": "Aanvullende informatie",
                    "type": "HUMAN_TASK"
                  },
                  {
                    "defaultFormulierDefinitie": "GOEDKEUREN",
                    "id": "GOEDKEUREN",
                    "naam": "Goedkeuren",
                    "type": "HUMAN_TASK"
                  },
                  {
                    "defaultFormulierDefinitie": "ADVIES",
                    "id": "ADVIES_INTERN",
                    "naam": "Advies intern",
                    "type": "HUMAN_TASK"
                  },
                  {
                    "defaultFormulierDefinitie": "EXTERN_ADVIES_VASTLEGGEN",
                    "id": "ADVIES_EXTERN",
                    "naam": "Advies extern",
                    "type": "HUMAN_TASK"
                  },
                  {
                    "defaultFormulierDefinitie": "DOCUMENT_VERZENDEN_POST",
                    "id": "DOCUMENT_VERZENDEN_POST",
                    "naam": "Document verzenden",
                    "type": "HUMAN_TASK"
                  }
                ],
                "key": "generiek-zaakafhandelmodel",
                "naam": "Generiek zaakafhandelmodel",
                "userEventListenerDefinitions": [
                  {
                    "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                    "id": "INTAKE_AFRONDEN",
                    "naam": "Intake afronden",
                    "type": "USER_EVENT_LISTENER"
                  },
                  {
                    "defaultFormulierDefinitie": "DEFAULT_TAAKFORMULIER",
                    "id": "ZAAK_AFHANDELEN",
                    "naam": "Zaak afhandelen",
                    "type": "USER_EVENT_LISTENER"
                  }
                ]
              },
              "domein": null,
              "defaultGroepId": "test-group-a",
              "defaultBehandelaarId": null,
              "einddatumGeplandWaarschuwing": null,
              "uiterlijkeEinddatumAfdoeningWaarschuwing": null,
              "productaanvraagtype": "$productaanvraagType",
              "zaakNietOntvankelijkResultaattype": {
                "archiefNominatie": "VERNIETIGEN",
                "archiefTermijn": "5 jaren",
                "besluitVerplicht": false,
                "id": "dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6",
                "naam": "Geweigerd",
                "naamGeneriek": "Geweigerd",
                "toelichting": "Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen",
                "vervaldatumBesluitVerplicht": false
              },
              "smartDocuments": {
                 "enabledForZaaktype": true
              }
            }
            """.trimIndent()
        )
    }

    @Suppress("LongMethod", "LongParameterList")
    fun createZaak(
        zaakTypeUUID: UUID,
        groupId: String,
        groupName: String,
        description: String? = "dummyOmschrijving",
        toelichting: String? = null,
        startDate: ZonedDateTime,
        communicatiekanaal: String? = COMMUNICATIEKANAAL_TEST_1,
        vertrouwelijkheidaanduiding: String? = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
    ): Response {
        logger.info {
            "Creating zaak with group id: $groupId and group name: $groupName"
        }
        return itestHttpClient.performJSONPostRequest(
            url = "${ZAC_API_URI}/zaken/zaak",
            requestBodyAsString = """{
              "zaak": {
                "zaaktype": {
                  "uuid": "$zaakTypeUUID"
                },
                "initiatorIdentificatie": null,
                "startdatum": "$startDate",
                "groep": {
                  "id": "$groupId",
                  "naam": "$groupName"
                },
                "communicatiekanaal": "$communicatiekanaal",
                "vertrouwelijkheidaanduiding": "$vertrouwelijkheidaanduiding",
                "omschrijving": "$description",
                "toelichting": "$toelichting"
              },
              "bagObjecten": []
            }"""
        )
    }

    fun retrieveZaak(zaakUUID: UUID): Response {
        logger.info {
            "Retrieving zaak with UUID: $zaakUUID"
        }
        return itestHttpClient.performGetRequest(
            url = "${ZAC_API_URI}/zaken/zaak/$zaakUUID"
        )
    }
}
