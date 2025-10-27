/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.info.zac.app.admin.model.RestBetrokkeneKoppelingen
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.net.URLDecoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ZacClient {
    private val logger = KotlinLogging.logger {}
    private var itestHttpClient = ItestHttpClient()

    fun createEnkelvoudigInformatieobjectForZaak(
        zaakUUID: UUID,
        fileName: String,
        fileMediaType: String,
        vertrouwelijkheidaanduiding: String
    ): Response {
        val createEnkelvoudigInformatieobjectEndpointURI =
            "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakUUID/$zaakUUID"
        val file = Thread.currentThread().contextClassLoader.getResource(fileName).let {
            File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
        }
        val requestBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("bestandsnaam", fileName)
                .addFormDataPart("titel", DOCUMENT_FILE_TITLE)
                .addFormDataPart("bestandsomvang", file.length().toString())
                .addFormDataPart("formaat", fileMediaType)
                .addFormDataPart(
                    "file",
                    fileName,
                    file.asRequestBody(fileMediaType.toMediaType())
                )
                .addFormDataPart("informatieobjectTypeUUID", INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID)
                .addFormDataPart(
                    "vertrouwelijkheidaanduiding",
                    vertrouwelijkheidaanduiding
                )
                .addFormDataPart("status", DOCUMENT_STATUS_IN_BEWERKING)
                .addFormDataPart(
                    "creatiedatum",
                    DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd'T'HH:mm+01:00"
                    ).format(ZonedDateTime.now())
                )
                .addFormDataPart("auteur", TEST_USER_1_NAME)
                .addFormDataPart("taal", "dut")
                .build()
        return itestHttpClient.performPostRequest(
            url = createEnkelvoudigInformatieobjectEndpointURI,
            headers = Headers.headersOf(
                "Accept",
                "application/json",
                "Content-Type",
                "multipart/form-data"
            ),
            requestBody = requestBody
        )
    }

    fun getZaaktypeCmmnConfiguration(zaakTypeUuid: UUID,): Response {
        logger.info { "Creating zaaktypeCmmnConfiguration in ZAC for zaaktype with UUID: $zaakTypeUuid" }
        return itestHttpClient.performGetRequest(url = "$ZAC_API_URI/zaakafhandelparameters/$zaakTypeUuid")
    }

    @Suppress("LongMethod", "LongParameterList")
    fun createZaaktypeCmmnConfiguration(
        zaakTypeIdentificatie: String,
        zaakTypeUuid: UUID,
        zaakTypeDescription: String,
        productaanvraagType: String,
        domein: String? = null,
        brpKoppelen: Boolean? = true,
        kvkKoppelen: Boolean? = true,
        brpDoelbindingenZoekWaarde: String = "BRPACT-ZoekenAlgemeen",
        brpDoelbindingenRaadpleegWaarde: String = "BRPACT-Totaal",
        brpVerwerkingWaarde: String = "Algemeen",
        automaticEmailConfirmationSender: String = "sender@info.nl",
        automaticEmailConfirmationReply: String = "reply@info.nl"
    ): Response {
        logger.info {
            "Creating zaaktypeCmmnConfiguration in ZAC for zaaktype with identificatie: $zaakTypeIdentificatie " +
                "and UUID: $zaakTypeUuid"
        }
        return changeZaaktypeCmmnConfiguration("""{
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
              "mailtemplateKoppelingen": [
                {
                  "mailtemplate": {
                    "body": "<p>Beste {ZAAK_INITIATOR},</p><p></p><p>Uw verzoek over {ZAAK_TYPE} met zaaknummer {ZAAK_NUMMER} wordt niet in behandeling genomen. Voor meer informatie gaat u naar Mijn Loket.</p><p></p><p>Met vriendelijke groet,</p><p></p><p>Gemeente Dommeldam</p>",
                    "defaultMailtemplate": true,
                    "id": 2,
                    "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                    "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME",
                    "onderwerp": "<p>Wij hebben uw verzoek niet in behandeling genomen (zaaknummer: {ZAAK_NUMMER})</p>",
                    "variabelen": [
                      "GEMEENTE",
                      "ZAAK_NUMMER",
                      "ZAAK_TYPE",
                      "ZAAK_STATUS",
                      "ZAAK_REGISTRATIEDATUM",
                      "ZAAK_STARTDATUM",
                      "ZAAK_STREEFDATUM",
                      "ZAAK_FATALEDATUM",
                      "ZAAK_OMSCHRIJVING",
                      "ZAAK_TOELICHTING",
                      "ZAAK_INITIATOR",
                      "ZAAK_INITIATOR_ADRES"
                    ]
                  }
                }
              ],
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
              "domein": ${domein?.let { "\"$it\"" }},
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
              },
              "betrokkeneKoppelingen": {
                "brpKoppelen": $brpKoppelen,
                "kvkKoppelen": $kvkKoppelen
              },
              "brpDoelbindingen": {
                "zoekWaarde": "$brpDoelbindingenZoekWaarde",
                "raadpleegWaarde": "$brpDoelbindingenRaadpleegWaarde",
                "verwerkingWaarde": "$brpVerwerkingWaarde"
              },
              "automaticEmailConfirmation": {
                "enabled": true,
                "templateName": "Ontvangstbevestiging",
                "emailSender": "$automaticEmailConfirmationSender",
                "emailReply": "$automaticEmailConfirmationReply"
              }
            }
            """.trimIndent()
        )
    }

    fun changeZaaktypeCmmnConfiguration(body: String): Response {
        return itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zaakafhandelparameters",
                requestBodyAsString = body
        )
    }

    @Suppress("LongMethod", "LongParameterList")
    fun createZaak(
        zaakTypeUUID: UUID,
        groupId: String,
        groupName: String,
        behandelaarId: String? = null,
        description: String? = ZAAK_OMSCHRIJVING,
        toelichting: String? = null,
        startDate: ZonedDateTime,
        communicatiekanaal: String? = COMMUNICATIEKANAAL_TEST_1,
        vertrouwelijkheidaanduiding: String? = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
    ): Response {
        logger.info {
            "Creating zaak with group id: $groupId and group name: $groupName"
        }
        val behandelaarString = behandelaarId?.let {
            """
                "behandelaar": {
                    "id": "$it"
                },
            """
        } ?: ""
        return itestHttpClient.performJSONPostRequest(
            url = "${ZAC_API_URI}/zaken/zaak",
            requestBodyAsString = """
            {
                "zaak": {
                    "zaaktype": {
                        "uuid": "$zaakTypeUUID"
                    },
                    "startdatum": "$startDate",
                    "groep": {
                        "id": "$groupId",
                        "naam": "$groupName"
                    },
                    $behandelaarString
                    "communicatiekanaal": "$communicatiekanaal",
                    "vertrouwelijkheidaanduiding": "$vertrouwelijkheidaanduiding",
                    "omschrijving": "$description",
                    "toelichting": "$toelichting"
                },
                "bagObjecten": [] 
            }
            """.trimIndent()
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

    fun retrieveZaak(id: String): Response {
        logger.info {
            "Retrieving zaak with id: $id"
        }
        return itestHttpClient.performGetRequest(
            url = "${ZAC_API_URI}/zaken/zaak/id/$id"
        )
    }
}
