package net.atos.zac.aanvraag

import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Point2D
import net.atos.zac.aanvraag.model.generated.Geometry

fun Geometry.convertToZgwPoint() = Point(Point2D(this.coordinates[0], this.coordinates[1]))
