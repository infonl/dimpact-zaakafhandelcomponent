/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail.model

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import net.atos.client.zgw.zrc.model.Zaak
import org.flowable.task.api.TaskInfo

class Bronnen private constructor(
    val zaak: Zaak?,
    val document: EnkelvoudigInformatieObject?,
    val taskInfo: TaskInfo?
) {
    class Builder {
        private var zaak: Zaak? = null
        private var document: EnkelvoudigInformatieObject? = null
        private var taskInfo: TaskInfo? = null

        fun add(zaak: Zaak) = apply { this.zaak = zaak }
        fun add(document: EnkelvoudigInformatieObject) = apply { this.document = document }
        fun add(taskInfo: TaskInfo) = apply { this.taskInfo = taskInfo }
        fun build() = Bronnen(zaak, document, taskInfo)
    }
}

fun Zaak.getBronnenFromZaak() = Bronnen.Builder().add(this).build()
