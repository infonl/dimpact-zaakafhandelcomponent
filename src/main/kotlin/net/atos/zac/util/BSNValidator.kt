package net.atos.zac.util

object BSNValidator {

    private const val BSN_LENGTH = 9
    private const val BSN_MULTIPLE = 11

    fun validateBSN(bsn: String) {
        require(bsn.length == BSN_LENGTH) { "BSN '$bsn' length must be $BSN_LENGTH" }
        val sum = bsnSum(bsn)
        require(sum % BSN_MULTIPLE == 0) { "Invalid BSN '$bsn'" }
    }

    private fun bsnSum(bsn: String) =
        bsn.mapIndexed { index, bsnChar ->
            require(bsnChar.isDigit()) { "Character on index $index in BSN '$bsn' is not a digit" }
            if (index == BSN_LENGTH - 1) {
                - bsnChar.digitToInt()
            } else {
                (BSN_LENGTH - index) * bsnChar.digitToInt()
            }
        }.sum()
}
