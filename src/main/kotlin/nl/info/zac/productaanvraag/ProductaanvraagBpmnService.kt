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
class ProductaanvraagBpmnService @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zaakProcessService: ZaakProcessService,
    private val productaanvraagZaakService: ProductaanvraagZaakService,
    private val productaanvraagClaimRepository: ProductaanvraagClaimRepository
) {
    fun createAndStartZaak(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        productaanvraagDimpact: ProductaanvraagDimpact,
        productaanvraagObject: ModelObject
    ) {
        val zaaktype = ztcClientService.readZaaktype(zaaktypeConfiguration.zaaktypeUuid)
        val zaak = productaanvraagZaakService.createZaak(zaaktype, productaanvraagDimpact, productaanvraagObject)
        val behandelaarId = productaanvraagZaakService.findValidDefaultBehandelaarId(zaaktypeConfiguration, zaak)
        productaanvraagZaakService.setUpZaakFromProductaanvraag(
            zaak = zaak,
            zaaktypeConfiguration = zaaktypeConfiguration,
            behandelaarId = behandelaarId,
            productaanvraagDimpact = productaanvraagDimpact,
            productaanvraagObject = productaanvraagObject
        )
        zaakProcessService.start(
            zaaktypeConfiguration = zaaktypeConfiguration,
            zaak = zaak,
            zaaktype = zaaktype,
            processStartData = ProcessStartData(
                zaakData = productaanvraagObject.extractAanvraaggegevens(),
                groupId = zaaktypeConfiguration.groepID,
                behandelaarId = behandelaarId,
                communicatiekanaal = zaak.communicatiekanaalNaam
            )
        )
        // The claim boundary of docs/solution-architecture/productRequestSupport.md: only a productaanvraag whose
        // process has started is done, so that an interrupted handling is handled again after its claim times out.
        productaanvraagClaimRepository.markDone(productaanvraagObject.uuid)
    }
}
