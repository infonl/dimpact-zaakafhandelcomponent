/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.audit.converter

import jakarta.inject.Inject
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.drc.model.generated.Gebruiksrechten
import net.atos.client.zgw.shared.model.ObjectType.BESLUIT
import net.atos.client.zgw.shared.model.ObjectType.BESLUIT_INFORMATIEOBJECT
import net.atos.client.zgw.shared.model.ObjectType.ENKELVOUDIG_INFORMATIEOBJECT
import net.atos.client.zgw.shared.model.ObjectType.GEBRUIKSRECHTEN
import net.atos.client.zgw.shared.model.ObjectType.OBJECT_INFORMATIEOBJECT
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.zac.app.audit.converter.besluiten.AuditBesluitConverter
import net.atos.zac.app.audit.converter.documenten.AuditEnkelvoudigInformatieobjectConverter
import net.atos.zac.app.audit.converter.documenten.AuditGebruiksrechtenWijzigingConverter
import net.atos.zac.app.audit.model.RESTHistorieRegel

class RESTHistorieRegelConverter @Inject constructor(
    private val auditEnkelvoudigInformatieobjectConverter: AuditEnkelvoudigInformatieobjectConverter
) {

    fun convert(auditTrail: List<AuditTrailRegel>): List<RESTHistorieRegel> =
        auditTrail.sortedByDescending { it.aanmaakdatum }
            .flatMap { it.toRestHistorieRegelList() }
            .toList()

    @Suppress("UNCHECKED_CAST")
    private fun AuditTrailRegel.toRestHistorieRegelList(): List<RESTHistorieRegel> =
        with(this.wijzigingen) {
            when (this.objectType) {
                BESLUIT ->
                    AuditBesluitConverter.convert(this as AuditWijziging<Besluit>)
                GEBRUIKSRECHTEN ->
                    AuditGebruiksrechtenWijzigingConverter.convert(this as AuditWijziging<Gebruiksrechten>)
                ENKELVOUDIG_INFORMATIEOBJECT, OBJECT_INFORMATIEOBJECT, BESLUIT_INFORMATIEOBJECT ->
                    auditEnkelvoudigInformatieobjectConverter.convert(
                        this as AuditWijziging<EnkelvoudigInformatieObject>
                    )
                else -> emptyList()
            }
        }.map { convertAuditTrailBasis(it, this) }

    private fun convertAuditTrailBasis(historieRegel: RESTHistorieRegel, auditTrailRegel: AuditTrailRegel) =
        historieRegel.apply {
            datumTijd = auditTrailRegel.aanmaakdatum
            door = auditTrailRegel.gebruikersWeergave
            applicatie = auditTrailRegel.applicatieWeergave
            toelichting = auditTrailRegel.toelichting
        }
}
