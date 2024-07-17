/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.audit.converter

import jakarta.enterprise.inject.Any
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import net.atos.zac.app.audit.model.RESTHistorieRegel
import java.util.stream.Collectors
import java.util.stream.Stream

class RESTHistorieRegelConverter {

    @Inject
    @Any
    lateinit var wijzigingConverterInstance: Instance<AbstractAuditWijzigingConverter<out AuditWijziging<*>>>

    fun convert(auditTrail: List<AuditTrailRegel>): List<RESTHistorieRegel> {
        return auditTrail.sortedByDescending { it.aanmaakdatum }.stream()
            .flatMap { auditTrailRegel: AuditTrailRegel -> this.convert(auditTrailRegel) }
            .collect(Collectors.toList())
    }

    private fun convert(auditTrailRegel: AuditTrailRegel): Stream<RESTHistorieRegel> =
        convertWijziging(auditTrailRegel.wijzigingen).peek {
            convertAuditTrailBasis(it, auditTrailRegel)
        }

    private fun convertWijziging(wijziging: AuditWijziging<*>): Stream<RESTHistorieRegel> {
        for (wijzigingConverter in wijzigingConverterInstance) {
            if (wijzigingConverter.supports(wijziging.objectType)) {
                return wijzigingConverter.convert(wijziging)
            }
        }
        return Stream.empty()
    }

    private fun convertAuditTrailBasis(historieRegel: RESTHistorieRegel, auditTrailRegel: AuditTrailRegel) =
        with(historieRegel) {
            datumTijd = auditTrailRegel.aanmaakdatum
            door = auditTrailRegel.gebruikersWeergave
            applicatie = auditTrailRegel.applicatieWeergave
            toelichting = auditTrailRegel.toelichting
        }
}
