/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import net.atos.zac.util.SerializableByYasson
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class OverigeRechten @JsonbCreator constructor(
    @param:JsonbProperty("starten_zaak") val startenZaak: Boolean,
    @param:JsonbProperty("beheren") val beheren: Boolean,
    @param:JsonbProperty("zoeken") val zoeken: Boolean
) : SerializableByYasson
