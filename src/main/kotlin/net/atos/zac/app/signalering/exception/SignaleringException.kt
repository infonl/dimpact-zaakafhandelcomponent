package net.atos.zac.app.signalering.exception

import jakarta.ws.rs.BadRequestException

class SignaleringException(message: String) : BadRequestException(message)
