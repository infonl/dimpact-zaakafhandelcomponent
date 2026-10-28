/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.model

enum class SignaleringTarget {
    GROUP,
    USER;

    data class Mail(val naam: String, val emailadres: String)
}
