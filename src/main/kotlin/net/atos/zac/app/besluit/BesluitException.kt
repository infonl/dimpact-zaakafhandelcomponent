package net.atos.zac.app.besluit

import jakarta.ws.rs.BadRequestException

class BesluitException(message: String) : BadRequestException(message)
