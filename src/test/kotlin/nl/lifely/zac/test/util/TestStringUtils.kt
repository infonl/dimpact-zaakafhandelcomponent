package nl.lifely.zac.test.util

fun createRandomStringWithAlphanumericCharacters(stringLength: Int) = List(stringLength) {
    (('a'..'z') + ('A'..'Z') + ('0'..'9')).random()
}.joinToString("")
