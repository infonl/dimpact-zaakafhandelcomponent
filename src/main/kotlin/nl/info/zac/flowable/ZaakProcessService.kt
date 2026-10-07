/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.exception.ZaaktypeConfigurationNotFoundException
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.ZaaktypeProcessBinding
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID

/**
 * Runs the process operations of a zaak in the engine that its zaaktype configuration is bound to.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
class ZaakProcessService @Inject constructor(
    private val processBindingInstances: Instance<ProcessBinding>
) {
    private val processBindingsByEngine: Map<ProcessEngine, ProcessBinding> by lazy {
        processBindingInstances.groupBy { it.processEngine }.let { bindingsByEngine ->
            ProcessEngine.entries.associateWith { processEngine ->
                bindingsByEngine[processEngine]?.singleOrNull() ?: error(
                    "Expected one process binding for engine '$processEngine', " +
                        "found ${bindingsByEngine[processEngine].orEmpty().size}"
                )
            }
        }
    }

    fun start(
        zaaktypeConfiguration: ZaaktypeConfiguration,
        zaak: Zaak,
        zaaktype: ZaakType,
        processStartData: ProcessStartData
    ) = readProcessBinding(zaaktypeConfiguration).let {
        bindingFor(it.processEngine).start(
            zaak = zaak,
            zaaktype = zaaktype,
            definitionKey = it.definitionKey,
            processStartData = processStartData
        )
    }

    /**
     * Whether the zaaktype in Open Zaak is set up the way the engine of its configuration needs to create its zaken.
     */
    fun isZaaktypeReady(zaaktypeConfiguration: ZaaktypeConfiguration, zaaktypeUri: URI) =
        zaaktypeConfiguration.processBinding?.let { bindingFor(it.processEngine).isZaaktypeReady(zaaktypeUri) } ?: false

    /**
     * Terminates the process of the zaak. A configuration without a process binding cannot start a zaak, so its
     * zaken have no process to terminate.
     */
    fun terminate(zaaktypeConfiguration: ZaaktypeConfiguration, zaakUuid: UUID) {
        zaaktypeConfiguration.processBinding?.let { bindingFor(it.processEngine).terminate(zaakUuid) }
    }

    /**
     * Deletes the process of the zaak in every engine. A deleted zaak no longer has a zaaktype to look up its
     * engine in.
     */
    fun deleteInAllEngines(zaakUuid: UUID) = processBindingsByEngine.values.forEach { it.delete(zaakUuid) }

    /**
     * Asks every engine, because only the runtime of an engine knows whether the process of a zaak still runs: a
     * closed or reopened zaak keeps its zaaktype configuration but has no running process.
     */
    fun hasActiveProcess(zaakUuid: UUID) = processBindingsByEngine.values.any { it.hasActiveProcess(zaakUuid) }

    /**
     * Updates the process of the zaak in every engine, each of which updates only a process that still runs, so the
     * caller needs only the zaak UUID and not its zaaktype configuration.
     */
    fun updateAssignment(zaakUuid: UUID, groupId: String?, behandelaarId: String?) =
        processBindingsByEngine.values.forEach { it.updateAssignment(zaakUuid, groupId, behandelaarId) }

    fun updateCommunicatiekanaal(zaakUuid: UUID, communicatiekanaal: String) =
        processBindingsByEngine.values.forEach { it.updateCommunicatiekanaal(zaakUuid, communicatiekanaal) }

    private fun bindingFor(processEngine: ProcessEngine) = processBindingsByEngine.getValue(processEngine)

    private fun readProcessBinding(zaaktypeConfiguration: ZaaktypeConfiguration): ZaaktypeProcessBinding =
        zaaktypeConfiguration.processBinding ?: throw ZaaktypeConfigurationNotFoundException(
            "Zaaktype configuration for zaaktype '${zaaktypeConfiguration.zaaktypeUuid}' is not bound to a process"
        )
}
