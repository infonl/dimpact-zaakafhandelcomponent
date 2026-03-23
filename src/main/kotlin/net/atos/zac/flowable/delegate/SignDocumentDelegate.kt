/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.delegate

import jakarta.enterprise.inject.spi.CDI
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.flowable.FlowableHelper
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import org.flowable.common.engine.api.delegate.Expression
import org.flowable.engine.delegate.DelegateExecution
import java.util.UUID
import java.util.logging.Logger

class SignDocumentDelegate : AbstractDelegate() {

    // Set by Flowable. Can be either FixedValue or JuelExpression
    lateinit var documentenKey: Expression

    companion object {
        private val LOG = Logger.getLogger(SignDocumentDelegate::class.java.name)
    }

    override fun execute(execution: DelegateExecution) {
        val flowableHelper = FlowableHelper.getInstance()
        val drcClientService = CDI.current().select(DrcClientService::class.java).get()
        val enkelvoudigInformatieObjectUpdateService =
            CDI.current().select(EnkelvoudigInformatieObjectUpdateService::class.java).get()

        val zaakUuid = execution.parent.getVariable(ZaakVariabelenService.VAR_ZAAK_UUID) as UUID
        val documentsToSign = flowableHelper.zaakVariabelenService.readZaakdata(zaakUuid)
            .filter { (key, _) -> key.startsWith(documentenKey.toString()) }
            .values
            .filterIsInstance<List<*>>()
            .flatten()
            .filterIsInstance<String>()
            .map { UUID.fromString(it) }

        LOG.fine(
            "Found ${documentsToSign.size} document(s) to sign " +
                "from activity '${execution.currentActivityName}'"
        )

        documentsToSign.forEach { uuid ->
            val enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(uuid)

            if (enkelvoudigInformatieobject.ondertekening?.datum != null) {
                LOG.warning("Document '${enkelvoudigInformatieobject.identificatie}' is already signed, skipping")
                return@forEach
            }

            LOG.fine("Signing document '${enkelvoudigInformatieobject.identificatie}'")
            enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(uuid)
        }
    }
}
