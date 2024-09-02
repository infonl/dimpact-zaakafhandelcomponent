package net.atos.zac.zoeken

class IndexingException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
}
