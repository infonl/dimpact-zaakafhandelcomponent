/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import jakarta.enterprise.inject.spi.CDI
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.UUID
import java.util.logging.Logger

/**
 * Flowable BPMN delegate to sign one or multiple documents.
 *
 * This class may be used in existing BPMN process definitions, so be careful renaming or moving it to another package
 * because that will break all zaken and tasks that were created with (previous versions of) the related BPMN process.
 */
class SignDocumentDelegate : AbstractDelegate() {

    // Set by Flowable. Can be either FixedValue or JuelExpression. Defaults to DEFAULT_DOCUMENTEN_KEY if not set.
    var documentenKey: Expression? = null

    companion object {
        private val LOG = Logger.getLogger(SignDocumentDelegate::class.java.name)
        private const val DEFAULT_DOCUMENTEN_KEY = "ZAAK_Documenten_Ondertekenen_Selectie"
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val enkelvoudigInformatieObjectUpdateService =
            CDI.current().select(EnkelvoudigInformatieObjectUpdateService::class.java).get()
        val zaakUuid = execution.parent.getVariable(ZaakVariabelenService.VAR_ZAAK_UUID) as UUID
        val zaakDataKey = documentenKey?.resolveValueAsString(execution)?.takeUnless { it.isBlank() }
            ?: DEFAULT_DOCUMENTEN_KEY
        LOG.fine("Signing documents with key '$zaakDataKey' from activity '${execution.currentActivityName}'")
        val documentsToSign = flowableHelper.zaakVariabelenService.readZaakdata(zaakUuid)
            .filter { (key, _) -> key.startsWith(zaakDataKey) }
            .values
            .asSequence()
            .filterIsInstance<List<*>>()
            .flatten()
            .filterIsInstance<String>()
            .map { UUID.fromString(it) }
            .distinct()
            .toList()

        LOG.fine(
            "Found ${documentsToSign.size} document(s) to sign " +
                "from activity '${execution.currentActivityName}'"
        )

        documentsToSign.forEach { uuid ->
            val enkelvoudigInformatieobject = flowableHelper.drcClientService.readEnkelvoudigInformatieobject(uuid)

            if (enkelvoudigInformatieobject.ondertekening?.datum != null) {
                LOG.warning("Document '${enkelvoudigInformatieobject.identificatie}' is already signed, skipping")
                return@forEach
            }

            LOG.fine("Signing document '${enkelvoudigInformatieobject.identificatie}'")
            enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(uuid)

            // Open Zaak does not send a notification for this. So we send the ScreenEvent ourselves!
            flowableHelper.eventingService.send(
                ScreenEventType.ENKELVOUDIG_INFORMATIEOBJECT.updated(enkelvoudigInformatieobject)
            )
        }
    }
}
