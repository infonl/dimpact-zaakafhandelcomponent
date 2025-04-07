/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.identity.model

import jakarta.validation.constraints.Size
import nl.info.zac.identity.model.Group
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestGroup(
    /**
     * Since this is used for the 'identificatie' field in
     * [net.atos.client.zgw.zrc.model.OrganisatorischeEenheid]
     * we need to make sure it adheres to the same constraints.
     */
    @field:Size(max = 24)
    var id: String,

    /**
     * Since this is used for the 'naam' field in
     * [net.atos.client.zgw.zrc.model.OrganisatorischeEenheid]
     * we need to make sure it adheres to the same constraints.
     */
    @field:Size(max = 50)
    var naam: String
)

fun Group.toRestGroup(): RestGroup =
    RestGroup(this.id, this.name)

fun List<Group>.toRestGroups(): List<RestGroup> =
    this.map { it.toRestGroup() }
