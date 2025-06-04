/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren

import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.task.model.RestTask

class ResolveDefaultValueContext(
    val task: RestTask,
    zrcClientService: ZrcClientService,
    zaakVariabelenService: ZaakVariabelenService
) {
    var zaak: Zaak = zrcClientService.readZaak(task.zaakUuid)
    var zaakData: Map<String, Any> = zaakVariabelenService.readProcessZaakdata(task.zaakUuid)
}
