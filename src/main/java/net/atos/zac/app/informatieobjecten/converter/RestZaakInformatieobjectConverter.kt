/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.converter

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.zac.app.informatieobjecten.model.RestZaakInformatieobject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
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
        val restZaakInformatieobject = RestZaakInformatieobject()
        restZaakInformatieobject.zaakIdentificatie = zaak.getIdentificatie()
        restZaakInformatieobject.zaakRechten = zaakrechten.toRestZaakRechten()
        if (zaakrechten.lezen) {
            restZaakInformatieobject.zaakStartDatum = zaak.getStartdatum()
            restZaakInformatieobject.zaakEinddatumGepland = zaak.getEinddatumGepland()
            restZaakInformatieobject.zaaktypeOmschrijving = zaaktype.getOmschrijving()
            zaak.getStatus()?.let { statusUri ->
                val status = zrcClientService.readStatus(statusUri)
                val statustype = ztcClientService.readStatustype(status.getStatustype())
                restZaakInformatieobject.zaakStatus = toRestZaakStatus(status, statustype)
            }
        }
        return restZaakInformatieobject
    }
}
