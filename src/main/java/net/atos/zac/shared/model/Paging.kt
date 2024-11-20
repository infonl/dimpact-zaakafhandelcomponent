package net.atos.zac.shared.model

class Paging(val page: Int, val maxResults: Int) {
    val firstResult: Int
        get() = page * maxResults
}
