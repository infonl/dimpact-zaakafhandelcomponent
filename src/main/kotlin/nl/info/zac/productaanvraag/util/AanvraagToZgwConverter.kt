/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.util

import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Point2D
import nl.info.zac.productaanvraag.model.generated.Geometry

fun Geometry.convertToZgwPoint() = Point(Point2D(this.coordinates[0], this.coordinates[1]))
