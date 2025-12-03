/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.converter

import net.atos.zac.app.informatieobjecten.model.RestOndertekening
import nl.info.client.zgw.drc.model.generated.Ondertekening
import java.util.Locale

fun Ondertekening.toRestOndertekening() = RestOndertekening(
    soort = this@toRestOndertekening.getSoort().name.lowercase(Locale.getDefault()),
    datum = this@toRestOndertekening.getDatum()
)
