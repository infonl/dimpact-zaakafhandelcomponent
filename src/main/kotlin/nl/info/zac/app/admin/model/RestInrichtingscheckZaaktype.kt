/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.generated.ZaakType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

/**
 * The fields of a zaaktype that the Inrichtingscheck page shows.
 */
@NoArgConstructor
@AllOpen
data class RestInrichtingscheckZaaktype(
    var uuid: UUID,
    var omschrijving: String,
    var doel: String? = null,
    var beginGeldigheid: LocalDate
)

fun ZaakType.toRestInrichtingscheckZaaktype() = RestInrichtingscheckZaaktype(
    uuid = url.extractUuid(),
    omschrijving = omschrijving,
    doel = doel,
    beginGeldigheid = beginGeldigheid
)
