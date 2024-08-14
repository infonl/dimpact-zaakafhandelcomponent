package net.atos.zac.app.identity.model

fun createRESTUser(
    id: String = "dummyId",
    name: String = "dummyUserName"
) = RestUser(
    id = id,
    naam = name
)
