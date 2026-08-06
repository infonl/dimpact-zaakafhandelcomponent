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

test_lezen_succeeds_with_behandelaar_role if {
    lezen with input.user.rollen as [ "behandelaar" ]
}

test_lezen_succeeds_with_coordinator_role if {
    lezen with input.user.rollen as [ "coordinator" ]
}

test_lezen_succeeds_with_recordmanager_role if {
    lezen with input.user.rollen as [ "recordmanager" ]
}

test_lezen_succeeds_with_beheerder_role if {
    lezen with input.user.rollen as [ "beheerder" ]
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

test_wijzigen_succeeds_with_coordinator_role if {
    wijzigen with input.user.rollen as [ "coordinator" ]
}

test_wijzigen_succeeds_with_recordmanager_role if {
    wijzigen with input.user.rollen as [ "recordmanager" ]
}

test_wijzigen_succeeds_with_beheerder_role if {
    wijzigen with input.user.rollen as [ "beheerder" ]
}

test_wijzigen_fails if {
    not wijzigen with input.user.rollen as [ "fakeRole" ]
}

test_wijzigen_fails_with_raadpleger_role if {
    not wijzigen with input.user.rollen as [ "raadpleger" ]
}
