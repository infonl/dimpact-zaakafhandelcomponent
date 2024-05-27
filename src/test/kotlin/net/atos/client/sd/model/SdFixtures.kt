package net.atos.client.sd.model

fun createWizardResponse(
    ticket: String = "dummyTicket",
) = WizardResponse().apply {
    this.ticket = ticket
}
