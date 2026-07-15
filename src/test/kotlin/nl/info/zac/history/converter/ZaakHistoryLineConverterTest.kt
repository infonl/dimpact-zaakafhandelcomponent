/*
 * SPDX-FileCopyrightText: 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.shared.model.Bron
import net.atos.client.zgw.shared.model.ObjectType
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.model.createBesluit
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.createBesluitInformatieObject
import nl.info.client.zgw.shared.model.audit.AuditWijziging
import nl.info.client.zgw.shared.model.audit.besluiten.BesluitInformatieobjectWijziging
import nl.info.client.zgw.shared.model.audit.createAuditTrailRegel
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.history.converter.documenten.AuditBesluitInformatieobjectConverter
import nl.info.zac.history.converter.documenten.AuditEnkelvoudigInformatieobjectConverter
import nl.info.zac.history.model.HistoryAction
import java.net.URI

class ZaakHistoryLineConverterTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val brcClientService = mockk<BrcClientService>()
    val besluitWijziging = mockk<AuditWijziging<Besluit>>()
    val besluitInformatieobjectWijziging = mockk<BesluitInformatieobjectWijziging>()

    val auditEnkelvoudigInformatieobjectConverter = AuditEnkelvoudigInformatieobjectConverter(ztcClientService)
    val auditBesluitInformatieobjectConverter = AuditBesluitInformatieobjectConverter(brcClientService)

    val converter = ZaakHistoryLineConverter(
        auditEnkelvoudigInformatieobjectConverter,
        auditBesluitInformatieobjectConverter
    )

    given("Besluit audit trail list") {
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

        `when`("converted to historie regel") {
            val restHistorieRegel = converter.convert(auditTrailRegel)

            then("it should return a correct regel list") {
                restHistorieRegel.size shouldBe 2
                with(restHistorieRegel[1]) {
                    action shouldBe HistoryAction.GEKOPPELD
                    attributeLabel shouldBe "Besluit"
                    oldValue shouldBe null
                    newValue shouldBe "fakeIdentificatie"
                    by shouldBe "Test User"
                    application shouldBe "ZAC"
                    explanation shouldBe "description"
                }
                with(restHistorieRegel[0]) {
                    action shouldBe HistoryAction.GEKOPPELD
                    attributeLabel shouldBe "informatieobject"
                    oldValue shouldBe null
                    newValue shouldBe "fakeIdentificatie"
                    by shouldBe "Test User"
                    application shouldBe "ZAC"
                    explanation shouldBe "123"
                }
            }
        }
    }
})
