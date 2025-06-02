/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model

import jakarta.json.bind.annotation.JsonbNillable
import nl.info.client.zgw.zrc.model.generated.Zaak
import java.net.URI

/**
 * Extension of [Zaak] to be able to delete the hoofdzaak of a zaak.
 * As per the ZGW ZRC API, to remove the hoofdzaak from a zaak, the hoofdzaak needs to be set to `null`.
 */
class HoofdzaakZaakPatch(@field:JsonbNillable private val hoofdzaak: URI?) : Zaak() {
    override fun getHoofdzaak(): URI? {
        return hoofdzaak
    }
}
