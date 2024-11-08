/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model

import net.atos.zac.shared.model.SorteerRichting

data class Sortering(val sorteerVeld: SorteerVeld, val richting: SorteerRichting)
