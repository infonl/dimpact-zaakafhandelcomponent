package net.atos.client.sd.model

import net.atos.client.sd.model.wizard.WizardResponse

fun createWizardResponse(
    ticket: String = "dummyTicket",
) = WizardResponse().apply {
    this.ticket = ticket
}
