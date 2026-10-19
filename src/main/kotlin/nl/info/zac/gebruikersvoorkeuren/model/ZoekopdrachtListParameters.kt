/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.gebruikersvoorkeuren.model

data class ZoekopdrachtListParameters(
    val lijstID: Werklijst,
    val medewerkerID: String
)
