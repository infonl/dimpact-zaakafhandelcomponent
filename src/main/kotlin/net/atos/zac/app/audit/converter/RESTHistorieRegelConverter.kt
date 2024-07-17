/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.audit.converter

import jakarta.enterprise.inject.Any
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.zac.app.audit.model.RESTHistorieRegel

class RESTHistorieRegelConverter {

    @Inject
    @Any
    lateinit var wijzigingConverterInstance: Instance<AbstractAuditWijzigingConverter<out AuditWijziging<*>>>

    fun convert(auditTrail: List<AuditTrailRegel>): List<RESTHistorieRegel> =
        auditTrail.sortedByDescending { it.aanmaakdatum }
            .flatMap { convert(it) }
            .toList()

    private fun convert(auditTrailRegel: AuditTrailRegel): List<RESTHistorieRegel> =
        convertWijziging(auditTrailRegel.wijzigingen)
            .map { convertAuditTrailBasis(it, auditTrailRegel) }

    private fun convertWijziging(wijziging: AuditWijziging<*>): List<RESTHistorieRegel> {
        for (wijzigingConverter in wijzigingConverterInstance) {
            if (wijzigingConverter.supports(wijziging.objectType)) {
                return wijzigingConverter.convert(wijziging)
            }
        }
        return emptyList()
    }

    private fun convertAuditTrailBasis(historieRegel: RESTHistorieRegel, auditTrailRegel: AuditTrailRegel) =
        historieRegel.apply {
            datumTijd = auditTrailRegel.aanmaakdatum
            door = auditTrailRegel.gebruikersWeergave
            applicatie = auditTrailRegel.applicatieWeergave
            toelichting = auditTrailRegel.toelichting
        }
}
