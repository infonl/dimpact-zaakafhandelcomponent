package nl.info.zac.productaanvraag.exception

import jakarta.ws.rs.NotFoundException

class MailTemplateNotFoundException(message: String) : NotFoundException(message)

