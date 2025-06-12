/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.gebruikersvoorkeuren.model

import java.time.ZonedDateTime

fun createZoekopdracht(
    id: Long = 1L,
    creationDate: ZonedDateTime = ZonedDateTime.now(),
    name: String = "fakeZoekopdracht"
) = Zoekopdracht().apply {
    this.id = id
    this.creatiedatum = creationDate
    this.naam = name
}
