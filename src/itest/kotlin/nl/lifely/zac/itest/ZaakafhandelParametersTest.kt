/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.provided.ProjectConfig
import io.kotest.provided.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID

private val logger = KotlinLogging.logger {}

class ZaakafhandelParametersTest : BehaviorSpec({
    given("ZAC Docker container is running and zaakafhandelparameters have been created") {
        When("the list zaakafhandelparameterts endpoint is called for our zaaktype under test") {
            then("the response should be ok and it should return the zaakafhandelparameters") {
                khttp.get(
                    url = "${ProjectConfig.zacContainer.apiUrl}/zaakafhandelParameters/$ZAAKTYPE_MELDING_KLEIN_EVENEMENT_UUID",
                    headers = mapOf("Authorization" to "Bearer ${ProjectConfig.accessToken}")
                ).apply {
                    logger.info { "Zaakafhandelparameters response: $text" }
                    // todo check contents
                    // {"afrondenMail":"BESCHIKBAAR_UIT","caseDefinition":{"humanTaskDefinitions":[{"defaultFormulierDefinitie":"AANVULLENDE_INFORMATIE","id":"AANVULLENDE_INFORMATIE","naam":"Aanvullende informatie","type":"HUMAN_TASK"},{"defaultFormulierDefinitie":"GOEDKEUREN","id":"GOEDKEUREN","naam":"Goedkeuren","type":"HUMAN_TASK"},{"defaultFormulierDefinitie":"ADVIES","id":"ADVIES_INTERN","naam":"Advies intern","type":"HUMAN_TASK"},{"defaultFormulierDefinitie":"EXTERN_ADVIES_VASTLEGGEN","id":"ADVIES_EXTERN","naam":"Advies extern","type":"HUMAN_TASK"},{"defaultFormulierDefinitie":"DOCUMENT_VERZENDEN_POST","id":"DOCUMENT_VERZENDEN_POST","naam":"Document verzenden","type":"HUMAN_TASK"}],"key":"melding-klein-evenement","naam":"Melding klein evenement","userEventListenerDefinitions":[{"defaultFormulierDefinitie":"DEFAULT_TAAKFORMULIER","id":"INTAKE_AFRONDEN","naam":"Intake afronden","type":"USER_EVENT_LISTENER"},{"defaultFormulierDefinitie":"DEFAULT_TAAKFORMULIER","id":"ZAAK_AFHANDELEN","naam":"Zaak afhandelen","type":"USER_EVENT_LISTENER"}]},"creatiedatum":"2023-10-27T07:15:42.020258551Z","defaultGroepId":"test-group-a","humanTaskParameters":[{"actief":true,"formulierDefinitieId":"AANVULLENDE_INFORMATIE","id":5,"planItemDefinition":{"defaultFormulierDefinitie":"AANVULLENDE_INFORMATIE","id":"AANVULLENDE_INFORMATIE","naam":"Aanvullende informatie","type":"HUMAN_TASK"},"referentieTabellen":[]},{"actief":true,"formulierDefinitieId":"GOEDKEUREN","id":2,"planItemDefinition":{"defaultFormulierDefinitie":"GOEDKEUREN","id":"GOEDKEUREN","naam":"Goedkeuren","type":"HUMAN_TASK"},"referentieTabellen":[]},{"actief":true,"formulierDefinitieId":"ADVIES","id":3,"planItemDefinition":{"defaultFormulierDefinitie":"ADVIES","id":"ADVIES_INTERN","naam":"Advies intern","type":"HUMAN_TASK"},"referentieTabellen":[{"id":1,"tabel":{"aantalWaarden":5,"code":"ADVIES","id":1,"naam":"Advies","systeem":true},"veld":"ADVIES"}]},{"actief":true,"formulierDefinitieId":"EXTERN_ADVIES_VASTLEGGEN","id":4,"planItemDefinition":{"defaultFormulierDefinitie":"EXTERN_ADVIES_VASTLEGGEN","id":"ADVIES_EXTERN","naam":"Advies extern","type":"HUMAN_TASK"},"referentieTabellen":[]},{"actief":true,"formulierDefinitieId":"DOCUMENT_VERZENDEN_POST","id":1,"planItemDefinition":{"defaultFormulierDefinitie":"DOCUMENT_VERZENDEN_POST","id":"DOCUMENT_VERZENDEN_POST","naam":"Document verzenden","type":"HUMAN_TASK"},"referentieTabellen":[]}],"id":1,"intakeMail":"BESCHIKBAAR_UIT","mailtemplateKoppelingen":[],"userEventListenerParameters":[{"id":"INTAKE_AFRONDEN","naam":"Intake afronden"},{"id":"ZAAK_AFHANDELEN","naam":"Zaak afhandelen"}],"valide":true,"zaakAfzenders":[{"defaultMail":false,"mail":"GEMEENTE","speciaal":true},{"defaultMail":false,"mail":"MEDEWERKER","speciaal":true}],"zaakNietOntvankelijkResultaattype":{"archiefNominatie":"VERNIETIGEN","archiefTermijn":"5 jaren","besluitVerplicht":false,"id":"dd2bcd87-ed7e-4b23-a8e3-ea7fe7ef00c6","naam":"Geweigerd","naamGeneriek":"Geweigerd","toelichting":"Het door het orgaan behandelen van een aanvraag, melding of verzoek om toestemming voor het doen of laten van een derde waar het orgaan bevoegd is om over te beslissen","vervaldatumBesluitVerplicht":false},"zaakbeeindigParameters":[],"zaaktype":{"beginGeldigheid":"2023-09-21","doel":"Melding evenement organiseren behandelen","identificatie":"melding-evenement-organiseren-behandelen","nuGeldig":true,"omschrijving":"Melding evenement organiseren behandelen","servicenorm":false,"uuid":"448356ff-dcfb-4504-9501-7fe929077c4f","versiedatum":"2023-09-21","vertrouwelijkheidaanduiding":"openbaar"}}
                    statusCode shouldBe HttpStatus.SC_OK
                }
            }
        }
    }
})
