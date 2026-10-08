/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.model

import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.createGroup
import nl.info.zac.identity.model.createUser

fun createZaakAssignment(
    group: Group? = createGroup(),
    user: User? = createUser()
) = ZaakAssignment(group = group, user = user)
