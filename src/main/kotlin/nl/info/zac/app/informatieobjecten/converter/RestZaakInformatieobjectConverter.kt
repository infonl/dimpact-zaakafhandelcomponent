/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.informatieobjecten.model.RestZaakInformatieobject
import nl.info.zac.app.policy.model.toRestZaakRechten
import nl.info.zac.app.zaak.model.toRestZaakStatus
import nl.info.zac.policy.PolicyService

class RestZaakInformatieobjectConverter @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val zrcClientService: ZrcClientService,
    private val policyService: PolicyService
) {
    fun toRestZaakInformatieobject(zaakInformatieobject: ZaakInformatieobject): RestZaakInformatieobject {
        val zaak = zrcClientService.readZaak(zaakInformatieobject.zaak)
        val zaaktype = ztcClientService.readZaaktype(zaak.getZaaktype())
        val zaakrechten = policyService.readZaakRechten(zaak, zaaktype)
        return RestZaakInformatieobject(
            zaakIdentificatie = zaak.getIdentificatie(),
            zaakRechten = zaakrechten.toRestZaakRechten(),
            zaakStartDatum = takeIf { zaakrechten.lezen }?.let { zaak.getStartdatum() },
            zaakEinddatumGepland = takeIf { zaakrechten.lezen }?.let { zaak.getEinddatumGepland() },
            zaaktypeOmschrijving = takeIf { zaakrechten.lezen }?.let { zaaktype.getOmschrijving() },
            zaakStatus = takeIf { zaakrechten.lezen }?.let {
                zaak.getStatus()?.let { statusUri ->
                    val status = zrcClientService.readStatus(statusUri)
                    val statustype = ztcClientService.readStatustype(status.getStatustype())
                    toRestZaakStatus(statustype, status)
                }
            }
        )
    }
}
