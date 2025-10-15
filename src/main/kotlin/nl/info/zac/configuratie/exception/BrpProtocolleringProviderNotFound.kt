package nl.info.zac.configuratie.exception

import jakarta.ws.rs.NotFoundException

class BrpProtocolleringProviderNotFound(message: String) : NotFoundException(message)
