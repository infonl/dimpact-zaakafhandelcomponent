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
        // Unlike a CMMN case, a BPMN process can use the initiator and the klantcontact of the zaak, for example to
        // send the confirmation of receipt email that ZAC itself sends for a CMMN case. So ZAC sets up the zaak before
        // it starts the process, and marks the productaanvraag as done only after that.
        // See docs/solution-architecture/productRequestSupport.md.
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
        productaanvraagClaimRepository.markDone(productaanvraagObject.uuid)
    }
}
