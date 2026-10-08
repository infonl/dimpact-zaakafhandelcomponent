/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_COMMUNICATIEKANAAL
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_GROUP
import net.atos.zac.flowable.ZaakVariabelenService.Companion.VAR_ZAAK_USER
import net.atos.zac.flowable.exception.CaseOrProcessNotFoundException
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.flowable.bpmn.BpmnService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class BpmnProcessBinding @Inject constructor(
    private val bpmnService: BpmnService,
    private val zaakVariabelenService: ZaakVariabelenService
) : ProcessBinding {
    companion object {
        private val LOG = Logger.getLogger(BpmnProcessBinding::class.java.name)
    }

    override val processEngine = ProcessEngine.BPMN

    override fun start(zaak: Zaak, zaaktype: ZaakType, definitionKey: String, processStartData: ProcessStartData) =
        bpmnService.startProcess(
            zaak = zaak,
            zaaktype = zaaktype,
            processDefinitionKey = definitionKey,
            zaakData = processStartData.zaakData + buildMap {
                processStartData.groupId?.let { put(VAR_ZAAK_GROUP, it) }
                processStartData.behandelaarId?.let { put(VAR_ZAAK_USER, it) }
                processStartData.communicatiekanaal?.let { put(VAR_ZAAK_COMMUNICATIEKANAAL, it) }
            }
        )

    /**
     * BPMN zaaktypen have no zaaktype check yet.
     */
    override fun isZaaktypeReady(zaaktypeUri: URI) = true

    override fun terminate(zaakUuid: UUID) {
        bpmnService.deleteProcessInstance(zaakUuid)
    }

    override fun delete(zaakUuid: UUID) = bpmnService.deleteProcessInstanceAndHistory(zaakUuid)

    override fun hasActiveProcess(zaakUuid: UUID) = bpmnService.isZaakProcessDriven(zaakUuid)

    override fun updateAssignment(zaakUuid: UUID, groupId: String?, behandelaarId: String?) {
        if (!hasActiveProcess(zaakUuid)) return
        try {
            groupId?.let { zaakVariabelenService.setGroup(zaakUuid, it) }
            behandelaarId?.let {
                zaakVariabelenService.setUser(zaakUuid, it)
            } ?: zaakVariabelenService.removeUser(zaakUuid)
        } catch (caseOrProcessNotFoundException: CaseOrProcessNotFoundException) {
            LOG.warning { caseOrProcessNotFoundException.message }
        }
    }

    override fun updateCommunicatiekanaal(zaakUuid: UUID, communicatiekanaal: String) {
        if (hasActiveProcess(zaakUuid)) zaakVariabelenService.setCommunicationChannel(zaakUuid, communicatiekanaal)
    }
}
