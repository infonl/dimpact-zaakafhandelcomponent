/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken.model

import java.util.Arrays

enum class TaakSortering {
    ID,
    TAAKNAAM,
    CREATIEDATUM,
    FATALEDATUM,
    BEHANDELAAR;

    companion object {
        fun fromValue(value: String): TaakSortering {
            return Arrays.stream(entries.toTypedArray())
                .filter { it.name.equals(value, ignoreCase = true) }
                .findAny()
                .orElseThrow {
                    IllegalArgumentException("Unsupported taak sortering value: '$value'")
                }
        }
    }
}
