/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_FILE_TITLE
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_STATUS_IN_BEWERKING
import nl.info.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.info.zac.itest.config.ItestConfiguration.FAKE_AUTHOR_NAME
import nl.info.zac.itest.config.ItestConfiguration.FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
import nl.info.zac.itest.config.ItestConfiguration.INFORMATIE_OBJECT_TYPE_BIJLAGE_UUID
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_BODY
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME
import nl.info.zac.itest.config.ItestConfiguration.MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_SUBJECT
import nl.info.zac.itest.config.ItestConfiguration.ZAAK_OMSCHRIJVING
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.TestGroup
import nl.info.zac.itest.config.TestUser
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Suppress("TooManyFunctions")
class ZacClient(
    val itestHttpClient: ItestHttpClient = ItestHttpClient()
) {
    private val logger = KotlinLogging.logger {}

    @Suppress("LongParameterList")
    fun createEnkelvoudigInformatieobjectForZaak(
        zaakUUID: UUID,
        fileName: String,
        title: String = DOCUMENT_FILE_TITLE,
        authorName: String = FAKE_AUTHOR_NAME,
        fileMediaType: String,
        vertrouwelijkheidaanduiding: String,
        testUser: TestUser
    ): ResponseContent {
        val createEnkelvoudigInformatieobjectEndpointURI =
            "$ZAC_API_URI/informatieobjecten/informatieobject/$zaakUUID/$zaakUUID"
        val file = Thread.currentThread().contextClassLoader.getResource(fileName).let {
            File(URLDecoder.decode(it!!.path, Charsets.UTF_8))
        }
        val requestBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("bestandsnaam", fileName)
                .addFormDataPart("titel", title)
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
                .addFormDataPart("auteur", authorName)
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
            requestBody = requestBody,
            testUser = testUser
        )
    }

    @Suppress("LongParameterList")
    fun createZaaktypeBpmnConfiguration(
        zaakTypeUuid: UUID,
        zaakTypeDescription: String,
        bpmnProcessDefinitionKey: String,
        productaanvraagType: String,
        defaultGroupName: String,
        brpDoelbindingenZoekWaarde: String = "BRPACT-ZoekenAlgemeen",
        brpDoelbindingenRaadpleegWaarde: String = "BRPACT-AlgemeneTaken",
        brpVerwerkingWaarde: String = "Algemeen",
        testUser: TestUser? = null
    ): ResponseContent {
        logger.info {
            "Creating a zaaktype BPMN configuration in ZAC for zaaktype with description: $zaakTypeDescription " +
                "and UUID: $zaakTypeUuid"
        }
        return itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/zaaktype-bpmn-configuration/$bpmnProcessDefinitionKey",
            requestBodyAsString = """{ 
              "zaaktypeUuid": "$zaakTypeUuid",
              "zaaktypeOmschrijving": "$zaakTypeDescription",
              "productaanvraagtype": "$productaanvraagType",
              "groepNaam": "$defaultGroupName",
              "betrokkeneKoppelingen": {
                "brpKoppelen": true,
                "kvkKoppelen": true
              },
              "brpDoelbindingen": {
                "zoekWaarde": "$brpDoelbindingenZoekWaarde",
                "raadpleegWaarde": "$brpDoelbindingenRaadpleegWaarde",
                "verwerkingWaarde": "$brpVerwerkingWaarde"
              }
            }
            """.trimIndent(),
            testUser = testUser
        )
    }

    @Suppress("LongMethod", "LongParameterList")
    fun createZaaktypeCmmnConfiguration(
        zaakTypeIdentificatie: String,
        zaakTypeUuid: UUID,
        zaakTypeDescription: String,
        productaanvraagType: String,
        domein: String? = null,
        brpDoelbindingenZoekWaarde: String = "BRPACT-ZoekenAlgemeen",
        brpDoelbindingenRaadpleegWaarde: String = "BRPACT-AlgemeneTaken",
        brpVerwerkingWaarde: String = "Algemeen",
        automaticEmailConfirmationSender: String = "sender@example.com",
        automaticEmailConfirmationReply: String = "reply@example.com",
        testUser: TestUser? = null
    ): ResponseContent {
        logger.info {
            "Creating a zaaktype CMMN configuration in ZAC for zaaktype with identificatie: $zaakTypeIdentificatie " +
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
              "mailtemplateKoppelingen": [
                {
                  "mailtemplate": {
                    "body": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_BODY",
                    "defaultMailtemplate": true,
                    "id": 2,
                    "mail": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_MAIL",
                    "mailTemplateNaam": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_NAME",
                    "onderwerp": "$MAIL_TEMPLATE_ZAAK_NIET_ONTVANKELIJK_SUBJECT",
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
              "defaultGroepId": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
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
                "brpKoppelen": true,
                "kvkKoppelen": true
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
            """.trimIndent(),
            testUser = testUser
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
        vertrouwelijkheidaanduiding: String? = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR,
        testUser: TestUser
    ): ResponseContent {
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
            """.trimIndent(),
            testUser = testUser
        )
    }

    fun retrieveZaak(zaakUUID: UUID, testUser: TestUser): ResponseContent {
        logger.info {
            "Retrieving zaak with UUID: $zaakUUID"
        }
        return itestHttpClient.performGetRequest(
            url = "${ZAC_API_URI}/zaken/zaak/$zaakUUID",
            testUser = testUser
        )
    }

    fun retrieveZaak(id: String, testUser: TestUser): ResponseContent {
        logger.info {
            "Retrieving zaak with id: $id"
        }
        return itestHttpClient.performGetRequest(
            url = "${ZAC_API_URI}/zaken/zaak/id/$id",
            testUser = testUser
        )
    }

    fun getHumanTaskPlanItemsForZaak(zaakUUID: UUID, testUser: TestUser): ResponseContent {
        logger.info {
            "Retrieving human task plan items for zaak with UUID: $zaakUUID"
        }
        return itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/planitems/zaak/$zaakUUID/humanTaskPlanItems",
            testUser = testUser
        )
    }

    @Suppress("LongParameterList")
    fun startHumanTaskPlanItem(
        planItemInstanceId: String,
        fatalDate: LocalDate,
        groupId: String,
        groupName: String,
        sendMail: Boolean = false,
        testUser: TestUser
    ): ResponseContent {
        logger.info {
            "Starting human task plan item with plan item instance id: $planItemInstanceId, " +
                "fatal date: $fatalDate, group id: $groupId, group name: $groupName, send mail: $sendMail"
        }
        return itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
            requestBodyAsString = """{
                    "planItemInstanceId": "$planItemInstanceId",
                    "fataledatum": "${fatalDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
                    "taakStuurGegevens": { "sendMail": $sendMail },
                    "groep": { "id": "$groupId", "naam": "$groupName" },
                    "taakdata":{}
                }
            """.trimIndent(),
            testUser = testUser
        )
    }

    /**
     * Starts the "Aanvullende Informatie" human task plan item (i.e. task) for the given zaak.
     */
    fun startAanvullendeInformatieTaskForZaak(
        zaakUUID: UUID,
        fatalDate: LocalDate,
        group: TestGroup,
        sendMail: Boolean = false,
        testUser: TestUser
    ): ResponseContent {
        val aanvullendeInformatieHumanTaskPlanItemId = getHumanTaskPlanItemsForZaak(
            zaakUUID,
            testUser
        ).let { response ->
            JSONArray(response.bodyAsString)
                .map { it as JSONObject }
                .firstOrNull {
                    it.getString("formulierDefinitie") == FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
                }?.getString("id") ?: error(
                "No human task plan item with formulier definitie '$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE' " +
                    "found for zaak with UUID: '$zaakUUID'"
            )
        }
        return startHumanTaskPlanItem(
            planItemInstanceId = aanvullendeInformatieHumanTaskPlanItemId,
            fatalDate = fatalDate,
            groupId = group.name,
            groupName = group.description,
            sendMail = sendMail,
            testUser = testUser
        )
    }

    fun searchForTasks(zaakIdentificatie: String, taskName: String, testUser: TestUser): String =
        itestHttpClient.performPutRequest(
            url = "${ZAC_API_URI}/zoeken/list",
            requestBodyAsString = """
                {
                  "rows": 10,
                  "page": 0,
                  "alleenMijnZaken": false,
                  "alleenOpenstaandeZaken": false,
                  "alleenAfgeslotenZaken": false,
                  "alleenMijnTaken": false,
                  "datums": {},
                  "zoeken": {
                    "TAAK_ZAAK_ID": "$zaakIdentificatie"
                  },
                  "filters": {
                    "TAAK_NAAM": {
                      "values": [ "$taskName" ],
                      "inverse": "false"
                    }
                  },
                  "sorteerRichting": "",
                  "type": "TAAK"
                }
            """.trimIndent(),
            testUser = testUser
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HttpURLConnection.HTTP_OK
            responseBody
        }

    fun submitFormData(bpmnZaakUuid: UUID, taakData: String, testUser: TestUser): String {
        val takenCreateResponse = itestHttpClient.performGetRequest(
            url = "${ZAC_API_URI}/taken/zaak/$bpmnZaakUuid",
            testUser = testUser
        ).let {
            val responseBody = it.bodyAsString
            logger.info { "Response: $responseBody" }
            it.code shouldBe HttpURLConnection.HTTP_OK
            responseBody
        }

        val patchedTakenData = takenCreateResponse.replace(""""taakdata":{}""", """"taakdata": $taakData""")
        logger.info { "Patched request: $patchedTakenData" }

        return itestHttpClient.performPatchRequest(
            url = "${ZAC_API_URI}/taken/complete",
            requestBodyAsString = patchedTakenData,
            testUser = testUser
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HttpURLConnection.HTTP_OK
            responseBody
        }
    }
}
