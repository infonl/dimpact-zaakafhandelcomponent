package net.atos.zac.app.identity.model

fun createRESTUser(
    id: String = "dummyId",
    name: String = "dummyUserName"
) = RESTUser().apply {
    this.id = id
    this.naam = name
}
