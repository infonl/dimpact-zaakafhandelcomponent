/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.search.model

import java.time.LocalDate

data class DatumRange(val van: LocalDate?, val tot: LocalDate?)
