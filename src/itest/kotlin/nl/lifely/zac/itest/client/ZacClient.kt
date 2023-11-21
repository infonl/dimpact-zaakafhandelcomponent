/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.client

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.config.ItestConfiguration.PRODUCT_AANVRAAG_TYPE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI

private val logger = KotlinLogging.logger {}

@Suppress("LongMethod")
fun createZaakAfhandelParameters() {
    logger.info {
        "Creating zaakafhandelparameters in ZAC for zaaktype with UUID: $ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID"
    }

    khttp.put(
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
            "    \"doel\": \"Melding evenement organiseren behandelen\",\n" +
            "    \"identificatie\": \"$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_IDENTIFICATIE\",\n" +
            "    \"nuGeldig\": true,\n" +
            "    \"omschrijving\": \"Melding evenement organiseren behandelen\",\n" +
            "    \"servicenorm\": false,\n" +
            "    \"uuid\": \"448356ff-dcfb-4504-9501-7fe929077c4f\",\n" +
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
            "    \"key\": \"generiek-zaakafhandelmode\",\n" +
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
    ).apply {
        logger.info { "PUT zaakafhandelParameters response: $text" }
        // check contents
        statusCode shouldBe HttpStatus.SC_OK
    }
}
