#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.notitie

import rego.v1

#######
# lezen
#######
test_lezen_succeeds if {
    lezen with input.user.rollen as [ "raadpleger" ]
}

test_lezen_fails if {
    not lezen with input.user.rollen as [ "fakeRole" ]
}

##########
# wijzigen
##########
test_wijzigen_succeeds if {
    wijzigen with input.user.rollen as [ "behandelaar" ]
}

test_wijzigen_fails if {
    not wijzigen with input.user.key as "fakeRole"
}
