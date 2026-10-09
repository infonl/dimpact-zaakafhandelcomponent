/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.client.or.objects.model.generated.ModelObject
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.flowable.ProcessStartData
import nl.info.zac.flowable.ZaakProcessService
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ProductaanvraagCmmnService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zaakProcessService: ZaakProcessService,
    private val productaanvraagZaakService: ProductaanvraagZaakService,
    private val productaanvraagEmailService: ProductaanvraagEmailService
) {
    fun createAndStartZaak(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        productaanvraagDimpact: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ) {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid)
        val zaak = productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
        val productaanvraagInitiator = productaanvraagZaakService.setUpZaakFromProductaanvraag(
            zaak = zaak,
            zaaktypeConfiguration = zaaktypeConfiguration,
            behandelaarId = productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak),
            productaanvraagDimpact = productaanvraagDimpact,
            productaanvraagObject = productaanvraagObject
        )
        zaakProcessService.start(
            zaaktypeConfiguration = zaaktypeConfiguration,
            zaak = zaak,
            zaaktype = zaaktype,
            processStartData = ProcessStartData(zaakData = productaanvraagObject.extractAanvraaggegevens())
        )
        productaanvraagEmailService.sendConfirmationOfReceiptEmailFromProductaanvraag(
            zaak = zaak,
            betrokkene = productaanvraagInitiator.betrokkene,
            productaanvraagSpecificEmailAddress = productaanvraagInitiator.klantcontactContactDetails
                ?.contactDetails?.emailAddress,
            zaaktypeConfiguration = zaaktypeConfiguration
        )
    }
}
