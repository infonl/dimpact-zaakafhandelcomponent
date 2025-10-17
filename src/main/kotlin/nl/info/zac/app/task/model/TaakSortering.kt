/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.task.model

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
