package net.atos.client.zgw.shared.model

import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject

fun createResultsOfZaakObjecten(
    list: List<Zaakobject> = emptyList(),
    count: Int = 0
): Results<Zaakobject> = Results(
    list,
    count
)
