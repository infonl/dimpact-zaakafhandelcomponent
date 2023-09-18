package net.atos.zac.identity.model

fun createGroup(
    id: String = "dummyId",
    name: String = "dummyName",
    email: String = "dummy-group@example.com"
) = Group(
    id,
    name,
    email
)
