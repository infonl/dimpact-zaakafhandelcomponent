package net.atos.zac.app.audit.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.model.createBesluit
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.createBesluitInformatieObject
import net.atos.client.zgw.shared.model.Bron
import net.atos.client.zgw.shared.model.ObjectType
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.client.zgw.shared.model.audit.besluiten.BesluitInformatieobjectWijziging
import net.atos.client.zgw.shared.model.audit.createAuditTrailRegel
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.audit.converter.documenten.AuditBesluitInformatieobjectConverter
import net.atos.zac.app.audit.converter.documenten.AuditEnkelvoudigInformatieobjectConverter
import net.atos.zac.app.audit.model.RESTHistorieActie
import java.net.URI

class RESTHistorieRegelConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val brcClientService = mockk<BrcClientService>()
    val besluitWijziging = mockk<AuditWijziging<Besluit>>()
    val besluitInformatieobjectWijziging = mockk<BesluitInformatieobjectWijziging>()

    val auditEnkelvoudigInformatieobjectConverter = AuditEnkelvoudigInformatieobjectConverter(ztcClientService)
    val auditBesluitInformatieobjectConverter = AuditBesluitInformatieobjectConverter(brcClientService)

    val converter = RESTHistorieRegelConverter(
        auditEnkelvoudigInformatieobjectConverter,
        auditBesluitInformatieobjectConverter
    )

    Given("Besluit audit trail list") {
        val auditTrailRegel = listOf(
            createAuditTrailRegel(
                bron = Bron.BESLUITEN_API,
                actie = "create",
                actieWeergave = "Object aangemaakt",
                resultaat = 201,
                hoofdObject = URI("https://example.com/hoofd"),
                resource = "besluit",
                resourceUrl = URI("https://example.com/besluit"),
                toelichting = "description",
                wijzigingen = besluitWijziging
            ),
            createAuditTrailRegel(
                bron = Bron.BESLUITEN_API,
                actie = "create",
                actieWeergave = "Object aangemaakt",
                resultaat = 201,
                hoofdObject = URI("https://example.com/hoofd"),
                resource = "besluit",
                resourceUrl = URI("https://example.com/besluit"),
                toelichting = "123",
                wijzigingen = besluitInformatieobjectWijziging
            )
        )

        every { besluitWijziging.oud } returns null
        every { besluitWijziging.nieuw } returns createBesluit()
        every { besluitWijziging.objectType } returns ObjectType.BESLUIT

        every { besluitInformatieobjectWijziging.oud } returns null
        every { besluitInformatieobjectWijziging.nieuw } returns createBesluitInformatieObject()
        every { besluitInformatieobjectWijziging.objectType } returns ObjectType.BESLUIT_INFORMATIEOBJECT

        every { brcClientService.readBesluit(any()) } returns createBesluit()

        When("converted to historie regel") {
            val restHistorieRegel = converter.convert(auditTrailRegel)

            Then("it should return a correct regel list") {
                restHistorieRegel.size shouldBe 2
                with(restHistorieRegel[1]) {
                    actie shouldBe RESTHistorieActie.GEKOPPELD
                    attribuutLabel shouldBe "Besluit"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "dummyIdentificatie"
                    door shouldBe "Test User"
                    applicatie shouldBe "ZAC"
                    toelichting shouldBe "description"
                }
                with(restHistorieRegel[0]) {
                    actie shouldBe RESTHistorieActie.GEKOPPELD
                    attribuutLabel shouldBe "informatieobject"
                    oudeWaarde shouldBe null
                    nieuweWaarde shouldBe "dummyIdentificatie"
                    door shouldBe "Test User"
                    applicatie shouldBe "ZAC"
                    toelichting shouldBe "123"
                }
            }
        }
    }
})
