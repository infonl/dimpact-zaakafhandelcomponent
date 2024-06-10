/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.signalering

import jakarta.inject.Inject
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.flowable.FlowableTaskService
import org.flowable.task.api.TaskInfo
import java.util.UUID

class SignaleringZACHelper @Inject constructor(
    private val zrcClientService: ZRCClientService,
    private val flowableTaskService: FlowableTaskService,
    private val drcClientService: DRCClientService
) {
    fun getZaak(zaakUUID: String): Zaak = zrcClientService.readZaak(UUID.fromString(zaakUUID))

    fun getTaak(taakID: String): TaskInfo = flowableTaskService.readTask(taakID)

    fun getDocument(documentUUID: String): EnkelvoudigInformatieObject =
        drcClientService.readEnkelvoudigInformatieobject(UUID.fromString(documentUUID))
}
