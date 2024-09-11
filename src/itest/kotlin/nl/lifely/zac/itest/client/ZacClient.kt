/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.lifely.zac.itest.config.ItestConfiguration.COMMUNICATIEKANAAL_TEST_1
import nl.lifely.zac.itest.config.ItestConfiguration.DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
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
            requestBodyAsString = "{\n" +
                "  \"humanTaskParameters\": [\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"AANVULLENDE_INFORMATIE\",\n" +
                "        \"id\": \"AANVULLENDE_INFORMATIE\",\n" +
                "        \"naam\": \"Aanvullende informatie\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"AANVULLENDE_INFORMATIE\",\n" +
                "      \"referentieTabellen\": [],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"GOEDKEUREN\",\n" +
                "        \"id\": \"GOEDKEUREN\",\n" +
                "        \"naam\": \"Goedkeuren\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"GOEDKEUREN\",\n" +
                "      \"referentieTabellen\": [],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"ADVIES\",\n" +
                "        \"id\": \"ADVIES_INTERN\",\n" +
                "        \"naam\": \"Advies intern\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"ADVIES\",\n" +
                "      \"referentieTabellen\": [\n" +
                "        {\n" +
                "          \"veld\": \"ADVIES\",\n" +
                "          \"tabel\": {\n" +
                "            \"aantalWaarden\": 5,\n" +
                "            \"code\": \"ADVIES\",\n" +
                "            \"id\": 1,\n" +
                "            \"naam\": \"Advies\",\n" +
                "            \"systeem\": true\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"EXTERN_ADVIES_VASTLEGGEN\",\n" +
                "        \"id\": \"ADVIES_EXTERN\",\n" +
                "        \"naam\": \"Advies extern\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"EXTERN_ADVIES_VASTLEGGEN\",\n" +
                "      \"referentieTabellen\": [],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"planItemDefinition\": {\n" +
                "        \"defaultFormulierDefinitie\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "        \"id\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "        \"naam\": \"Document verzenden\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      \"defaultGroepId\": null,\n" +
                "      \"formulierDefinitieId\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "      \"referentieTabellen\": [],\n" +
                "      \"actief\": true,\n" +
                "      \"doorlooptijd\": null\n" +
                "    }\n" +
                "  ],\n" +
                "  \"mailtemplateKoppelingen\": [],\n" +
                "  \"userEventListenerParameters\": [\n" +
                "    {\n" +
                "      \"id\": \"INTAKE_AFRONDEN\",\n" +
                "      \"naam\": \"Intake afronden\",\n" +
                "      \"toelichting\": null\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"ZAAK_AFHANDELEN\",\n" +
                "      \"naam\": \"Zaak afhandelen\",\n" +
                "      \"toelichting\": null\n" +
                "    }\n" +
                "  ],\n" +
                "  \"valide\": false,\n" +
                "  \"zaakAfzenders\": [],\n" +
                "  \"zaakbeeindigParameters\": [],\n" +
                "  \"zaaktype\": {\n" +
                "    \"beginGeldigheid\": \"2023-09-21\",\n" +
                "    \"doel\": \"$zaakTypeDescription\",\n" +
                "    \"identificatie\": \"$zaakTypeIdentificatie\",\n" +
                "    \"nuGeldig\": true,\n" +
                "    \"omschrijving\": \"$zaakTypeDescription\",\n" +
                "    \"servicenorm\": false,\n" +
                "    \"uuid\": \"$zaakTypeUuid\",\n" +
                "    \"versiedatum\": \"2023-09-21\",\n" +
                "    \"vertrouwelijkheidaanduiding\": \"openbaar\"\n" +
                "  },\n" +
                "  \"intakeMail\": \"BESCHIKBAAR_UIT\",\n" +
                "  \"afrondenMail\": \"BESCHIKBAAR_UIT\",\n" +
                "  \"caseDefinition\": {\n" +
                "    \"humanTaskDefinitions\": [\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"AANVULLENDE_INFORMATIE\",\n" +
                "        \"id\": \"AANVULLENDE_INFORMATIE\",\n" +
                "        \"naam\": \"Aanvullende informatie\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"GOEDKEUREN\",\n" +
                "        \"id\": \"GOEDKEUREN\",\n" +
                "        \"naam\": \"Goedkeuren\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"ADVIES\",\n" +
                "        \"id\": \"ADVIES_INTERN\",\n" +
                "        \"naam\": \"Advies intern\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"EXTERN_ADVIES_VASTLEGGEN\",\n" +
                "        \"id\": \"ADVIES_EXTERN\",\n" +
                "        \"naam\": \"Advies extern\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "        \"id\": \"DOCUMENT_VERZENDEN_POST\",\n" +
                "        \"naam\": \"Document verzenden\",\n" +
                "        \"type\": \"HUMAN_TASK\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"key\": \"generiek-zaakafhandelmodel\",\n" +
                "    \"naam\": \"Generiek zaakafhandelmodel\",\n" +
                "    \"userEventListenerDefinitions\": [\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"DEFAULT_TAAKFORMULIER\",\n" +
                "        \"id\": \"INTAKE_AFRONDEN\",\n" +
                "        \"naam\": \"Intake afronden\",\n" +
                "        \"type\": \"USER_EVENT_LISTENER\"\n" +
                "      },\n" +
                "      {\n" +
                "        \"defaultFormulierDefinitie\": \"DEFAULT_TAAKFORMULIER\",\n" +
                "        \"id\": \"ZAAK_AFHANDELEN\",\n" +
                "        \"naam\": \"Zaak afhandelen\",\n" +
                "        \"type\": \"USER_EVENT_LISTENER\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"domein\": null,\n" +
                "  \"defaultGroepId\": \"test-group-a\",\n" +
                "  \"defaultBehandelaarId\": null,\n" +
                "  \"einddatumGeplandWaarschuwing\": null,\n" +
                "  \"uiterlijkeEinddatumAfdoeningWaarschuwing\": null,\n" +
                "  \"productaanvraagtype\": \"$productaanvraagType\",\n" +
                "  \"zaakNietOntvankelijkResultaattype\": {\n" +
                "    \"archiefNominatie\": \"VERNIETIGEN\",\n" +
                "    \"archiefTermijn\": \"5 jaren\",\n" +
                "    \"besluitVerplicht\": false,\n" +
                "    \"id\": \"dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6\",\n" +
                "    \"naam\": \"Geweigerd\",\n" +
                "    \"naamGeneriek\": \"Geweigerd\",\n" +
                "    \"toelichting\": \"Het door het orgaan behandelen van een aanvraag, melding of verzoek om " +
                "toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen\",\n" +
                "    \"vervaldatumBesluitVerplicht\": false\n" +
                "  }\n" +
                "}\n"
        )
    }

    @Suppress("LongMethod", "LongParameterList")
    fun createZaak(
        zaakTypeUUID: UUID,
        groupId: String,
        groupName: String,
        description: String? = "dummyOmschrijving",
        startDate: ZonedDateTime,
        communicatiekanaal: String? = COMMUNICATIEKANAAL_TEST_1,
        vertrouwelijkheidaanduiding: String? = DOCUMENT_VERTROUWELIJKHEIDS_AANDUIDING_OPENBAAR
    ): Response {
        logger.info {
            "Creating zaak with group id: $groupId and group name: $groupName"
        }
        return itestHttpClient.performJSONPostRequest(
            url = "${ZAC_API_URI}/zaken/zaak",
            requestBodyAsString = "{\n" +
                "  \"zaak\": {\n" +
                "    \"zaaktype\": {\n" +
                "      \"uuid\": \"$zaakTypeUUID\"\n" +
                "    },\n" +
                "    \"initiatorIdentificatie\": null,\n" +
                "    \"startdatum\": \"$startDate\",\n" +
                "    \"groep\": {\n" +
                "      \"id\": \"$groupId\",\n" +
                "      \"naam\": \"$groupName\"\n" +
                "    },\n" +
                "    \"communicatiekanaal\": \"$communicatiekanaal\",\n" +
                "    \"vertrouwelijkheidaanduiding\": \"$vertrouwelijkheidaanduiding\",\n" +
                "    \"omschrijving\": \"$description\",\n" +
                "    \"toelichting\": null\n" +
                "  },\n" +
                "  \"bagObjecten\": []\n" +
                "}"
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
