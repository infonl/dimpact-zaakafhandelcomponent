/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren

import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.zac.app.task.model.RestTask

class ResolveDefaultValueContext(
    val task: RestTask,
    private val zrcClientService: ZrcClientService,
    private val zaakVariabelenService: ZaakVariabelenService
) {
    var zaak: Zaak? = null
        get() {
            if (field == null) {
                field = zrcClientService.readZaak(task.zaakUuid)
            }
            return field
        }
        private set

    var zaakData: MutableMap<String?, Any?>? = null
        get() {
            if (field == null) {
                field = zaakVariabelenService.readProcessZaakdata(task.zaakUuid)
            }
            return field
        }
        private set
}
