/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import jakarta.enterprise.inject.spi.CDI
import net.atos.zac.flowable.delegate.log.DocumentSigningEvent
import net.atos.zac.flowable.delegate.log.DocumentsToSignFound
import nl.info.zac.enkelvoudiginformatieobject.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.flowable.FlowableHelper
import nl.info.zac.flowable.ZaakVariabelenService
import nl.info.zac.log.ZacLogger
import nl.info.zac.policy.assertPolicy
import nl.info.zac.websocket.event.ScreenEventType
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.UUID

/**
 * Flowable BPMN delegate to sign one or multiple documents.
 *
 * This class may be used in existing BPMN process definitions, so be careful renaming or moving it to another package
 * because that will break all zaken and tasks that were created with (previous versions of) the related BPMN process.
 */
class SignDocumentDelegate : AbstractDelegate() {

    // Set by Flowable. Can be either FixedValue or JuelExpression. Defaults to DEFAULT_DOCUMENTS_TO_SIGN_ZAAK_DATA_KEY if not set.
    var documentenKey: Expression? = null

    companion object {
        private val LOG = ZacLogger.getLogger(SignDocumentDelegate::class)
        private const val DEFAULT_DOCUMENTS_TO_SIGN_ZAAK_DATA_KEY = "ZAAK_Documenten_Te_Ondertekenen"
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val enkelvoudigInformatieObjectUpdateService =
            CDI.current().select(EnkelvoudigInformatieObjectUpdateService::class.java).get()
        val zaakUuid = execution.parent.getVariable(ZaakVariabelenService.VAR_ZAAK_UUID) as UUID
        val zaak = flowableHelper.zrcClientService.readZaakByID(getZaakIdentificatie(execution))
        val documentsToSignZaakDataKey = documentenKey?.resolveValueAsString(execution)?.takeUnless { it.isBlank() }
            ?: DEFAULT_DOCUMENTS_TO_SIGN_ZAAK_DATA_KEY
        val documentsToSign = flowableHelper.zaakVariabelenService.readZaakdata(zaakUuid)
            .filter { (key, _) -> key.startsWith(documentsToSignZaakDataKey) }
            .values
            .asSequence()
            .filterIsInstance<List<*>>()
            .flatten()
            .filterIsInstance<Map<*, *>>()
            .filter { it["selected"] == true }
            .mapNotNull { it["uuid"] as? String }
            .map { UUID.fromString(it) }
            .distinct()
            .toList()

        LOG.fine(
            DocumentsToSignFound(
                zaakIdentificatie = zaak.identificatie,
                activityName = execution.currentActivityName,
                zaakdataKey = documentsToSignZaakDataKey,
                documentCount = documentsToSign.size
            )
        )

        documentsToSign.forEach { uuid ->
            val enkelvoudigInformatieobject = flowableHelper.drcClientService.readEnkelvoudigInformatieobject(uuid)
            assertPolicy(
                flowableHelper.policyService.readDocumentRechten(enkelvoudigInformatieobject, zaak).canOndertekenen,
                LOG,
                DocumentSigningEvent.DocumentSigningDenied(
                    informatieobjectIdentificatie = enkelvoudigInformatieobject.identificatie,
                    zaakIdentificatie = zaak.identificatie
                )
            )

            if (enkelvoudigInformatieobject.ondertekening?.datum != null) {
                LOG.warning(
                    DocumentSigningEvent.DocumentAlreadySigned(
                        informatieobjectIdentificatie = enkelvoudigInformatieobject.identificatie,
                        zaakIdentificatie = zaak.identificatie
                    )
                )
                return@forEach
            }

            LOG.fine(
                DocumentSigningEvent.DocumentSigningStarted(
                    informatieobjectIdentificatie = enkelvoudigInformatieobject.identificatie,
                    zaakIdentificatie = zaak.identificatie
                )
            )
            enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(uuid)

            flowableHelper.eventingService.send(
                ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(enkelvoudigInformatieobject)
            )
        }
    }
}
