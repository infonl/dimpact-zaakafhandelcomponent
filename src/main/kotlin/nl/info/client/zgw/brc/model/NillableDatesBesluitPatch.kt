/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.brc.model

import jakarta.json.bind.annotation.JsonbNillable
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import java.time.LocalDate

/**
 * Extension of [nl.info.client.zgw.brc.model.generated.Besluit] to be able to clear the optional
 * date fields of a besluit in ZGW JSON requests.
 * The date fields are annotated with [JsonbNillable] because JSON-B omits `null` properties by default,
 * which Open Zaak reads as "leave unchanged"; the annotation forces an explicit `null` to be written so
 * that a cleared date is actually persisted.
 * Note that this results in 'This property hides Java field XXXX thus making it inaccessible.' compiler warning.
 */
open class NillableDatesBesluitPatch(
    private val toelichting: String?,
    private val ingangsdatum: LocalDate?,
    @field:JsonbNillable
    private val vervaldatum: LocalDate?,
    private val vervalreden: VervalredenEnum?,
    @field:JsonbNillable
    private val publicatiedatum: LocalDate?,
    @field:JsonbNillable
    private val uiterlijkeReactiedatum: LocalDate?
) : Besluit() {
    override fun getToelichting(): String? = toelichting

    override fun getIngangsdatum(): LocalDate? = ingangsdatum

    override fun getVervaldatum(): LocalDate? = vervaldatum

    override fun getVervalreden(): VervalredenEnum? = vervalreden

    override fun getPublicatiedatum(): LocalDate? = publicatiedatum

    override fun getUiterlijkeReactiedatum(): LocalDate? = uiterlijkeReactiedatum
}
