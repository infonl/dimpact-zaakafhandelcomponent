package nl.lifely.zac.itest.client

import java.net.URLEncoder

fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8.name())
