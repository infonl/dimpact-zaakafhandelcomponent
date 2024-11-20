package net.atos.zac.shared.model

import nl.lifely.zac.util.AllOpen

@AllOpen
class ListParameters {
    var sorting: Sorting? = null
    var paging: Paging? = null
}
