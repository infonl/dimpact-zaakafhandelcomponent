/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.gebruikersvoorkeuren.model

import java.time.ZonedDateTime

@Suppress("LongParameterList")
fun createZoekopdracht(
    id: Long? = 1L,
    creationDate: ZonedDateTime = ZonedDateTime.now(),
    name: String = "fakeZoekopdracht",
    lijstID: Werklijst = Werklijst.MIJN_ZAKEN,
    medewerkerID: String = "testMedewerker",
    actief: Boolean = false
) = Zoekopdracht().apply {
    this.id = id
    this.creatiedatum = creationDate
    this.naam = name
    this.lijstID = lijstID
    this.medewerkerID = medewerkerID
    this.isActief = actief
}

fun createDashboardCardInstelling(
    id: Long? = 1L,
    medewerkerId: String = "testMedewerker",
    cardId: DashboardCardId = DashboardCardId.MIJN_TAKEN,
    kolom: Int = 1,
    volgorde: Int = 0
) = DashboardCardInstelling().apply {
    this.id = id
    this.medewerkerId = medewerkerId
    this.cardId = cardId
    this.kolom = kolom
    this.volgorde = volgorde
}

fun createTabelInstellingen(
    lijstID: Werklijst = Werklijst.MIJN_ZAKEN,
    medewerkerID: String = "testMedewerker",
    aantalPerPagina: Int = TabelInstellingen.AANTAL_PER_PAGINA_DEFAULT
) = TabelInstellingen().apply {
    this.lijstID = lijstID
    this.medewerkerID = medewerkerID
    this.aantalPerPagina = aantalPerPagina
}
