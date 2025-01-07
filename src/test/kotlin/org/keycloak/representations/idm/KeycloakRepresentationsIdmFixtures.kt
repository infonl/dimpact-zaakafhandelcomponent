package org.keycloak.representations.idm

fun createUserRepresentation(
    username: String = "dummyUsername",
    firstName: String = "dummyFirstName",
    lastName: String = "dummyLastName",
    email: String = "dummy@example.com"
) = UserRepresentation().apply {
    this.username = username
    this.firstName = firstName
    this.lastName = lastName
    this.email = email
}
