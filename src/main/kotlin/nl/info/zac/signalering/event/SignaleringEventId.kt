/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering.event

data class SignaleringEventId<ID : Any>(val resource: ID, val detail: ID?) {
    override fun toString() = detail?.let { "$resource;$it" } ?: resource.toString()
}
