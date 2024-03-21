/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

class RESTCoordinates {
    constructor()

    constructor(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    var x: Double = 0.0

    var y: Double = 0.0
}
