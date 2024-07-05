package net.atos.client.zgw.ztc.exception

import jakarta.ws.rs.NotFoundException

class CatalogusNotFoundException(message: String) : NotFoundException(message)
