package net.atos.zac.mailtemplates

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.client.brp.BrpClientService
import net.atos.client.kvk.KvkClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakStatus
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.model.createStatusType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.identity.IdentityService
import java.net.URI
import java.time.LocalDate

class MailTemplateHelperTest : BehaviorSpec({
    val brpClientService = mockk<BrpClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val identityService = mockk<IdentityService>()
    val kvkClientService = mockk<KvkClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val mailTemplateHelper = MailTemplateHelper(
        brpClientService,
        configuratieService,
        identityService,
        kvkClientService,
        zgwApiService,
        zrcClientService,
        ztcClientService
    )

    Given("A zaak") {
        val zaakType = createZaakType()
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            status = URI("https://example.com/dummyStatus"),
            startDate = LocalDate.of(2021, 10, 12)
        )
        val zaakStatus = createZaakStatus()
        val statusType = createStatusType(omschrijving = "dummyStatusTypeDescription")
        val zaakTonenURL = URI("https://example.com/dummyURL")
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns createZaakType()
        every { configuratieService.zaakTonenUrl(zaak.identificatie) } returns zaakTonenURL
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType

        When("I call the getMailTemplate method") {
            val resolvedText = mailTemplateHelper.resolveVariabelen(
                "dummyText, {ZAAK_NUMMER}, {ZAAK_URL}, {ZAAK_TYPE}, {ZAAK_STATUS}, {ZAAK_STARTDATUM}",
                zaak
            )

            Then("I should get the correct mail template") {
                resolvedText shouldBe "dummyText, ${zaak.identificatie}, $zaakTonenURL, ${zaakType.omschrijving}, " +
                    "${statusType.omschrijving}, 12-10-2021"
            }
        }
    }
})
