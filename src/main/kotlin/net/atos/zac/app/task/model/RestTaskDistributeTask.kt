/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.task.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestTaskDistributeTask(
    var taakId: String,

    var zaakUuid: UUID
)
