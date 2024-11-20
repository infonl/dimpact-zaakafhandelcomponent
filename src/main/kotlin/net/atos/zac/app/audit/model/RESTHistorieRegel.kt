/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.audit.model

import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.shared.util.HistorieUtil
import java.time.LocalDate
import java.time.ZonedDateTime

class RESTHistorieRegel(val attribuutLabel: String, val oudeWaarde: String?, val nieuweWaarde: String?) {
    var datumTijd: ZonedDateTime? = null
    var door: String? = null
    var applicatie: String? = null
    var toelichting: String? = null
    var actie: RESTHistorieActie? = null

    constructor(attribuutLabel: String, oudeWaarde: LocalDate?, nieuweWaarde: LocalDate?) : this(
        attribuutLabel,
        HistorieUtil.toWaarde(oudeWaarde),
        HistorieUtil.toWaarde(nieuweWaarde)
    )

    constructor(attribuutLabel: String, oudeWaarde: ZonedDateTime?, nieuweWaarde: ZonedDateTime?) : this(
        attribuutLabel,
        HistorieUtil.toWaarde(oudeWaarde),
        HistorieUtil.toWaarde(nieuweWaarde)
    )

    constructor(attribuutLabel: String, oudeWaarde: Boolean?, nieuweWaarde: Boolean?) : this(
        attribuutLabel,
        HistorieUtil.toWaarde(oudeWaarde),
        HistorieUtil.toWaarde(nieuweWaarde)
    )

    constructor(
        attribuutLabel: String,
        oudeWaarde: StatusEnum?,
        nieuweWaarde: StatusEnum?
    ) : this(attribuutLabel, HistorieUtil.toWaarde(oudeWaarde), HistorieUtil.toWaarde(nieuweWaarde))

    constructor(
        attribuutLabel: String,
        oudeWaarde: VertrouwelijkheidaanduidingEnum?,
        nieuweWaarde: VertrouwelijkheidaanduidingEnum?
    ) : this(attribuutLabel, HistorieUtil.toWaarde(oudeWaarde), HistorieUtil.toWaarde(nieuweWaarde))
}
