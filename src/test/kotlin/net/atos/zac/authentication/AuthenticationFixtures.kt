package net.atos.zac.authentication

import net.atos.zac.app.zaken.model.ZAAK_TYPE_1_OMSCHRIJVING
import net.atos.zac.app.zaken.model.ZAAK_TYPE_2_OMSCHRIJVING

fun createLoggedInUser() = LoggedInUser(
    "dummyId",
    "dummyFirstName",
    "dummyLastName",
    "dummyDisplayName",
    "dummy@example.com",
    setOf("dummyRole1", "dummyRole2"),
    setOf("dummyGroup1", "dummyGroup2"),
    setOf(ZAAK_TYPE_1_OMSCHRIJVING, ZAAK_TYPE_2_OMSCHRIJVING)
)
