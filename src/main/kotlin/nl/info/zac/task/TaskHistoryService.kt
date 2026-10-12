/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.zac.flowable.task.model.ValueChangeData
import net.atos.zac.util.JsonbUtil.FIELD_VISIBILITY_STRATEGY
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import org.flowable.engine.HistoryService
import org.flowable.task.api.TaskInfo

@ApplicationScoped
@AllOpen
@NoArgConstructor
class TaskHistoryService @Inject constructor(
    private val historyService: HistoryService,
    private val identityService: IdentityService
) {
    companion object {
        const val USER_TASK_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ADDED =
            "USER_TASK_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ADDED"
    }

    fun addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(task: TaskInfo, zaak: Zaak, medewerkerId: String) {
        historyService.createHistoricTaskLogEntryBuilder(task)
            .type(USER_TASK_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ADDED)
            .data(
                FIELD_VISIBILITY_STRATEGY.toJson(
                    ValueChangeData(
                        "",
                        identityService.readUser(medewerkerId).getFullName(),
                        ZaakspecifiekeAutorisatieService.zaakspecifiekGeautoriseerdeMedewerkerToelichting(zaak)
                    )
                )
            )
            .create()
    }
}
