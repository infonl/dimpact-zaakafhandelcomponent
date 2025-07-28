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
data class TaakRechten @JsonbCreator constructor(
    @param:JsonbProperty("lezen") val lezen: Boolean,
    @param:JsonbProperty("wijzigen") val wijzigen: Boolean,
    @param:JsonbProperty("toekennen") val toekennen: Boolean,
    @param:JsonbProperty("creeren_document") val creerenDocument: Boolean,
    @param:JsonbProperty("toevoegen_document") val toevoegenDocument: Boolean
) : SerializableByYasson
