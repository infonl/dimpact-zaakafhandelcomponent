/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.besluit

import jakarta.inject.Inject
import net.atos.client.zgw.brc.BrcClientService
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
class BesluitService @Inject constructor(
    private val brcClientService: BrcClientService
)
