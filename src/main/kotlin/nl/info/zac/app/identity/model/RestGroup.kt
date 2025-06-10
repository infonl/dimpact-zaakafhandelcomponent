/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.model

import jakarta.validation.constraints.Size
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie.IDENTIFICATIE_MAX_LENGTH
import nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie.NAAM_MAX_LENGTH
import nl.info.zac.identity.model.Group
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestGroup(
    /**
     * Since this is used for the 'identificatie' field in
     * [nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie],
     * we need to make sure it adheres to the same constraints.
     */
    @field:Size(max = IDENTIFICATIE_MAX_LENGTH)
    var id: String,

    /**
     * Since this is used for the 'naam' field in
     * [nl.info.client.zgw.zrc.model.generated.OrganisatorischeEenheidIdentificatie],
     * we need to make sure it adheres to the same constraints.
     */
    @field:Size(max = NAAM_MAX_LENGTH)
    var naam: String
)

fun Group.toRestGroup(): RestGroup =
    RestGroup(this.id, this.name)

fun List<Group>.toRestGroups(): List<RestGroup> =
    this.map { it.toRestGroup() }
