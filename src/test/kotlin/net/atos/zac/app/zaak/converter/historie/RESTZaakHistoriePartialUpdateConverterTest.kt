package net.atos.zac.app.zaak.converter.historie

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.zac.app.audit.model.RESTHistorieActie
import java.math.BigDecimal
import java.time.ZonedDateTime

class RESTZaakHistoriePartialUpdateConverterTest : BehaviorSpec({
    val zrcClientService = mockk<ZrcClientService>()
    val auditTrail = mockk<ZRCAuditTrailRegel>()
    val actie = mockk<RESTHistorieActie>()
    val creationDate = ZonedDateTime.now()
    val userView = "view"
    val description = "description"
    val restZaakHistoriePartialUpdateConverter = RESTZaakHistoriePartialUpdateConverter(zrcClientService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
           An audit trail with changes for start, completion and target dates, zaak geometry,
           and a dummy key-value pair
        """.trimIndent()
    ) {
        every { auditTrail.aanmaakdatum } returns creationDate
        every { auditTrail.gebruikersWeergave } returns userView
        every { auditTrail.toelichting } returns description

        val newValues = mapOf(
            "startdatum" to "2024-10-30",
            "uiterlijkeEinddatumAfdoening" to "2024-11-30",
            "einddatumGepland" to "2024-12-30",
            "zaakgeometrie" to mapOf(
                "type" to "Point",
                "coordinates" to listOf(
                    BigDecimal("53.602182801494195"),
                    BigDecimal("5.363728969647492")
                )
            ),
            "dummyKey" to "dummyValue"
        )

        When("history is requested") {
            val history = restZaakHistoriePartialUpdateConverter.convertPartialUpdate(
                auditTrail,
                actie,
                emptyMap<String, String>(),
                newValues
            )

            Then("it is converted correctly") {
                history.size shouldBe 5
                with(history[0]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userView
                    toelichting shouldBe description
                    attribuutLabel shouldBe "startdatum"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "30-10-2024"
                }
                with(history[1]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userView
                    toelichting shouldBe description
                    attribuutLabel shouldBe "uiterlijkeEinddatumAfdoening"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "30-11-2024"
                }
                with(history[2]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userView
                    toelichting shouldBe description
                    attribuutLabel shouldBe "einddatumGepland"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "30-12-2024"
                }
                with(history[3]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userView
                    toelichting shouldBe description
                    attribuutLabel shouldBe "zaakgeometrie"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "POINT(53.602182801494195 5.363728969647492)"
                }
                with(history[4]) {
                    datumTijd shouldBe creationDate
                    door shouldBe userView
                    toelichting shouldBe description
                    attribuutLabel shouldBe "dummyKey"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "dummyValue"
                }
            }
        }
    }
})
