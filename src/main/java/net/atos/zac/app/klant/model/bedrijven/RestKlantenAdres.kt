/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

class RestKlantenAdres(
    /** Correspondentieadres en/of bezoekadres  */
    var type: String,
    var afgeschermd: Boolean,
    var volledigAdres: String
)
