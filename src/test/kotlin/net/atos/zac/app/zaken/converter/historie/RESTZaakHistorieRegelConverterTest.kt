package net.atos.zac.app.zaken.converter.historie

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import net.atos.client.vrl.VrlClientService
import net.atos.client.zgw.shared.model.Bron
import net.atos.client.zgw.shared.model.audit.createZRCAuditTrailRegel
import net.atos.client.zgw.zrc.model.generated.Wijzigingen
import net.atos.client.zgw.ztc.ZtcClientService
import java.net.URI

class RESTZaakHistorieRegelConverterTest : BehaviorSpec({

    Given("Audit trail has resource zaak with action created") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val zaakIdentificatie = "ZAAK-2024-0000000003"
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.ZAKEN_API,
            actie = "create",
            actieWeergave = "Object aangemaakt",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "zaak",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "zaak created",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf("identificatie" to zaakIdentificatie)
            }
        )
        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "zaak"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe zaakIdentificatie
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe null
                    applicatie shouldBe null
                    toelichting shouldBe "zaak created"
                }
            }
        }
    }

    Given("Audit trail has resource rol with action updated") {
        val ztcClientService = mockk<ZtcClientService>()
        val vrlClientService = mockk<VrlClientService>()

        val naam = "dummyVoorletters dummyAchternaam"
        val zrcAuditTrailRegel = createZRCAuditTrailRegel(
            bron = Bron.AUTORISATIES_API,
            actie = "update",
            actieWeergave = "Update",
            resultaat = 201,
            hoofdObject = URI("https://example.com/somePath"),
            resource = "rol",
            resourceUrl = URI("https://example.com/somePath"),
            toelichting = "rol updated",
            wijzigingen = Wijzigingen().apply {
                nieuw = mapOf(
                    "roltoelichting" to "",
                    "omschrijving" to "dummyOmschrijving",
                    "betrokkeneIdentificatie" to mapOf(
                        "achternaam" to "dummyAchternaam",
                        "identificatie" to "dummyIdentificatie",
                        "voorletters" to "dummyVoorletters"
                    ),
                    "betrokkeneType" to "medewerker",
                    "zaak" to "https://example.com/zaak",
                    "identificatienummer" to "dummyIdentificatie",
                    "naam" to naam,
                    "omschrijvingGeneriek" to "initiator"
                )
            }
        )
        val restZaakHistorieRegelConverter = RESTZaakHistorieRegelConverter(
            ztcClientService,
            RESTZaakHistoriePartialUpdateConverter(vrlClientService)
        )

        When("converted to REST historie regel") {
            val listRestRegel = restZaakHistorieRegelConverter.convertZaakRESTHistorieRegel(zrcAuditTrailRegel)

            Then("it should return correct data") {
                listRestRegel.size shouldBe 1
                with(listRestRegel.first()) {
                    attribuutLabel shouldBe "rol"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe naam
                    datumTijd shouldBe zrcAuditTrailRegel.aanmaakdatum
                    door shouldBe null
                    applicatie shouldBe null
                    toelichting shouldBe "rol updated"
                }
            }
        }
    }
})
