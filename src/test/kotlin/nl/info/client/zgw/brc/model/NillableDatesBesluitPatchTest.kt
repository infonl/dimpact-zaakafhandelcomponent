/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.brc.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import net.atos.zac.util.JsonbUtil
import nl.info.client.zgw.brc.model.generated.VervalredenEnum
import java.time.LocalDate

class NillableDatesBesluitPatchTest : BehaviorSpec({

    given("A patch whose optional date fields are cleared") {
        val besluitPatch = NillableDatesBesluitPatch(
            toelichting = "fakeToelichting",
            ingangsdatum = LocalDate.of(2026, 1, 1),
            vervaldatum = null,
            vervalreden = VervalredenEnum.EMPTY,
            publicatiedatum = null,
            uiterlijkeReactiedatum = null
        )

        `when`("it is serialized to JSON for a ZGW request") {
            val json = JsonbUtil.JSONB.toJson(besluitPatch)

            then("the cleared optional date fields are serialized as explicit null, so Open Zaak clears them") {
                json shouldContain "\"vervaldatum\":null"
                json shouldContain "\"publicatiedatum\":null"
                json shouldContain "\"uiterlijkeReactiedatum\":null"
            }

            then("the non-nullable vervalreden is serialized as the blank value rather than null") {
                json shouldNotContain "\"vervalreden\":null"
                json shouldContain "\"vervalreden\":\"\""
            }
        }
    }

    given("A patch whose optional date fields are populated") {
        val besluitPatch = NillableDatesBesluitPatch(
            toelichting = "fakeToelichting",
            ingangsdatum = LocalDate.of(2026, 1, 1),
            vervaldatum = LocalDate.of(2026, 11, 14),
            vervalreden = VervalredenEnum.TIJDELIJK,
            publicatiedatum = LocalDate.of(2026, 10, 14),
            uiterlijkeReactiedatum = LocalDate.of(2026, 11, 1)
        )

        `when`("it is serialized to JSON for a ZGW request") {
            val json = JsonbUtil.JSONB.toJson(besluitPatch)

            then("the populated date fields are serialized with their value rather than null") {
                json shouldContain "vervaldatum"
                json shouldNotContain "\"vervaldatum\":null"
                json shouldContain "publicatiedatum"
                json shouldNotContain "\"publicatiedatum\":null"
                json shouldContain "uiterlijkeReactiedatum"
                json shouldNotContain "\"uiterlijkeReactiedatum\":null"
            }
        }
    }
})
