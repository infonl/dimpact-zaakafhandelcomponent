package net.atos.zac.app.mail.model

fun createRESTMailGegevens(
    verzender: String = "from@example.com",
    ontvanger: String = "to@example.com",
) = RESTMailGegevens().apply {
    this.verzender = verzender
    this.ontvanger = ontvanger
}
