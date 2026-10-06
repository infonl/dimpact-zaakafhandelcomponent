/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CmmnService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.healthcheck.HealthCheckService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.util.UUID

@ApplicationScoped
@NoArgConstructor
@AllOpen
class CmmnProcessBinding @Inject constructor(
    private val cmmnService: CmmnService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val healthCheckService: HealthCheckService
) : ProcessBinding {
    override val processEngine = ProcessEngine.CMMN

    override fun start(zaak: Zaak, zaaktype: ZaakType, definitionKey: String, processStartData: ProcessStartData) =
        cmmnService.startCase(
            zaak = zaak,
            zaaktype = zaaktype,
            caseDefinitionKey = definitionKey,
            zaakData = processStartData.caseData.takeIf { it.isNotEmpty() }
        )

    /**
     * The zaaktype check calls Open Zaak several times, so callers only ask for it after the ZAC configuration of
     * the zaaktype has proven to be valid.
     */
    override fun isZaaktypeReady(zaaktypeUri: URI) = healthCheckService.controleerZaaktype(zaaktypeUri).isValide

    override fun terminate(zaakUuid: UUID) {
        cmmnService.terminateCase(zaakUuid)
    }

    override fun delete(zaakUuid: UUID) {
        cmmnService.deleteCase(zaakUuid)
        zaakVariabelenService.deleteAllCaseVariables(zaakUuid)
    }
}
