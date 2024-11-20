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
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.client.zgw.shared.model.audit.besluiten.BesluitInformatieobjectWijziging
import net.atos.zac.app.audit.converter.besluiten.AuditBesluitConverter
import net.atos.zac.app.audit.converter.documenten.AuditBesluitInformatieobjectConverter
import net.atos.zac.app.audit.converter.documenten.AuditEnkelvoudigInformatieobjectConverter
import net.atos.zac.app.audit.converter.documenten.AuditGebruiksrechtenWijzigingConverter
import net.atos.zac.app.audit.model.RESTHistorieActie
import net.atos.zac.app.audit.model.RESTHistorieRegel

private const val CREATE = "create"
private const val DESTROY = "destroy"
private const val UPDATE = "update"
private const val PARTIAL_UPDATE = "partial_update"

class ZaakHistoryLineConverter @Inject constructor(
    private val auditEnkelvoudigInformatieobjectConverter: AuditEnkelvoudigInformatieobjectConverter,
    private val auditBesluitInformatieobjectConverter: AuditBesluitInformatieobjectConverter
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
                ENKELVOUDIG_INFORMATIEOBJECT ->
                    auditEnkelvoudigInformatieobjectConverter.convert(
                        this as AuditWijziging<EnkelvoudigInformatieObject>
                    )
                BESLUIT_INFORMATIEOBJECT ->
                    auditBesluitInformatieobjectConverter.convert(this as BesluitInformatieobjectWijziging)
                else -> emptyList()
            }
        }.map { convertAuditTrailBasis(it, this) }

    private fun convertAuditTrailBasis(historieRegel: RESTHistorieRegel, auditTrailRegel: AuditTrailRegel) =
        historieRegel.apply {
            actie = convertActie(auditTrailRegel.actie)
            applicatie = auditTrailRegel.applicatieWeergave
            datumTijd = auditTrailRegel.aanmaakdatum
            door = auditTrailRegel.gebruikersWeergave
            toelichting = auditTrailRegel.toelichting
        }

    private fun convertActie(auditTrailActie: String): RESTHistorieActie? =
        when (auditTrailActie) {
            CREATE -> RESTHistorieActie.GEKOPPELD
            UPDATE, PARTIAL_UPDATE -> RESTHistorieActie.GEWIJZIGD
            DESTROY -> RESTHistorieActie.ONTKOPPELD
            else -> null
        }
}
