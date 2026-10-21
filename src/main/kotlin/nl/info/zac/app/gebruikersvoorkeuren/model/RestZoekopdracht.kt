/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.gebruikersvoorkeuren.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.gebruikersvoorkeuren.model.Werklijst
import nl.info.zac.gebruikersvoorkeuren.model.Zoekopdracht
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.time.ZonedDateTime

@NoArgConstructor
@AllOpen
data class RestZoekopdracht(
    var id: Long? = null,
    var lijstID: Werklijst,
    @get:JsonbProperty("actief")
    @set:JsonbProperty("actief")
    var isActief: Boolean = false,
    var creatiedatum: LocalDate? = null,
    var naam: String,
    var json: String
)

fun Zoekopdracht.toRestZoekopdracht() = RestZoekopdracht(
    id = id,
    lijstID = lijstID,
    isActief = isActief,
    creatiedatum = creatiedatum.toLocalDate(),
    naam = naam,
    json = json
)

fun RestZoekopdracht.toZoekopdracht(loggedInUser: LoggedInUser) =
    Zoekopdracht().apply {
        id = this@toZoekopdracht.id
        lijstID = this@toZoekopdracht.lijstID
        medewerkerID = loggedInUser.id
        naam = this@toZoekopdracht.naam
        isActief = this@toZoekopdracht.isActief
        creatiedatum = ZonedDateTime.now()
        json = this@toZoekopdracht.json
    }
