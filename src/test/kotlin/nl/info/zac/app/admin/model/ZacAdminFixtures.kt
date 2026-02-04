package nl.info.zac.app.admin.model

import net.atos.zac.app.admin.model.RestZaakbeeindigReden
import net.atos.zac.app.admin.model.RestZaakbeeindigParameter
import nl.info.zac.app.admin.createRestResultaattype
import nl.info.zac.app.admin.createRestZaakbeeindigReden
import nl.info.zac.app.zaak.model.RestResultaattype

fun createRestZaakbeeindigParameter(
    id: Long = 1234L,
    restZaakbeeindigReden: RestZaakbeeindigReden = createRestZaakbeeindigReden(),
    restResultaattype: RestResultaattype = createRestResultaattype()
) = RestZaakbeeindigParameter().apply {
    this.id = id
    this.zaakbeeindigReden = restZaakbeeindigReden
    this.resultaattype = restResultaattype
}
