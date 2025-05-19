/*
 * SPDX-FileCopyrightText: 2022 Atos
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
data class DocumentRechten @JsonbCreator constructor(
    @param:JsonbProperty("lezen") val lezen: Boolean,
    @param:JsonbProperty("wijzigen") val wijzigen: Boolean,
    @param:JsonbProperty("verwijderen") val verwijderen: Boolean,
    @param:JsonbProperty("vergrendelen") val vergrendelen: Boolean,
    @param:JsonbProperty("ontgrendelen") val ontgrendelen: Boolean,
    @param:JsonbProperty("ondertekenen") val ondertekenen: Boolean,
    @param:JsonbProperty("toevoegen_nieuwe_versie") val toevoegenNieuweVersie: Boolean,
    @param:JsonbProperty("verplaatsen") val verplaatsen: Boolean,
    @param:JsonbProperty("ontkoppelen") val ontkoppelen: Boolean,
    @param:JsonbProperty("downloaden") val downloaden: Boolean
) : SerializableByYasson
