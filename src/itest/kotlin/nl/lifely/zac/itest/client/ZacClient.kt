/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import khttp.responses.Response
import nl.lifely.zac.itest.config.ItestConfiguration.PRODUCT_AANVRAAG_TYPE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.zaakUUID

private val logger = KotlinLogging.logger {}

@Suppress("LongMethod")
fun createZaakAfhandelParameters(): Response {
    logger.info {
        "Creating zaakafhandelparameters in ZAC for zaaktype with identificatie: $ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE " +
            "and UUID: $ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
    }

    val response = khttp.put(
        url = "$ZAC_API_URI/zaakafhandelParameters",
        headers = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
        ),
        data = "{\n" +
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
            "    \"doel\": \"$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION\",\n" +
            "    \"identificatie\": \"$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE\",\n" +
            "    \"nuGeldig\": true,\n" +
            "    \"omschrijving\": \"$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION\",\n" +
            "    \"servicenorm\": false,\n" +
            "    \"uuid\": \"$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID\",\n" +
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
            "  \"productaanvraagtype\": \"$PRODUCT_AANVRAAG_TYPE\",\n" +
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
    logger.info { "PUT zaakafhandelParameters response: ${response.text}" }
    return response
}

@Suppress("LongMethod")
fun createZaak(groupId: String, groupName: String): Response {
    logger.info {
        "Creating zaak with group name that is too long"
    }
    val response = khttp.post(
        url = "${ZAC_API_URI}/zaken/zaak",
        headers = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
        ),
        data = "{\n" +
            "  \"zaak\": {\n" +
            "    \"zaaktype\": {\n" +
            "      \"omschrijving\": \"$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION\",\n" +
            "      \"uuid\": \"$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID\"\n" +
            "    },\n" +
            "    \"initiatorIdentificatie\": null,\n" +
            "    \"startdatum\": \"2023-12-07T12:43:01+01:00\",\n" +
            "    \"groep\": {\n" +
            "      \"id\": \"$groupId\",\n" +
            "      \"naam\": \"$groupName\"\n" +
            "    },\n" +
            "    \"communicatiekanaal\": {\n" +
            "      \"naam\": \"E-mail\",\n" +
            "      \"uuid\": \"f5de7d7f-8440-4ce7-8f27-f934ad0c2ea6\"\n" +
            "    },\n" +
            "    \"vertrouwelijkheidaanduiding\": \"openbaar\",\n" +
            "    \"omschrijving\": \"dummyOmschrijving\",\n" +
            "    \"toelichting\": null\n" +
            "  },\n" +
            "  \"bagObjecten\": []\n" +
            "}"
    )
    logger.info { "POST zaak create response: ${response.text}" }
    return response
}

@Suppress("LongMethod")
fun assignZaakToGroup(groupId: String): Response {
    logger.info {
        "Assigning a zaak to a group with group name that is too long"
    }
    // note that this HTTP request currently requires the following environment variable
    // to be set when running this test: JAVA_TOOL_OPTIONS=--add-opens=java.base/java.net=ALL-UNNAMED
    // see: https://github.com/lojewalo/khttp/issues/88
    val response = khttp.patch(
        url = "${ZAC_API_URI}/zaken/toekennen",
        headers = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${KeycloakClient.requestAccessToken()}"
        ),
        data = "{\n" +
            "  \"zaakUUID\": \"$zaakUUID\",\n" +
            "  \"groepId\": \"$groupId\",\n" +
            "  \"reden\": \"dummyReason\"\n" +
            "}"
    )
    logger.info { "POST zaak toekennen response: ${response.text}" }
    return response
}
