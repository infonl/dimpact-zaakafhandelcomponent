#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.overig

import rego.v1

import data.net.atos.zac.overig.starten_zaak
import data.net.atos.zac.overig.beheren
import data.net.atos.zac.overig.zoeken

##############
# starten_zaak
##############
test_starten_zaak_with_behandelaar_role if {
    starten_zaak with input.user.rollen as [ "behandelaar" ]
}

test_starten_zaak_with_unknown_role_fails if {
    not starten_zaak with input.user.rollen as [ "basic" ]
}

test_starten_zaak_with_wrong_role_fails if {
    not starten_zaak with input.user.rollen as [ "beheerder" ]
}

#########
# beheren
#########
test_beheren_with_beheerder_role if {
    beheren with input.user.rollen as [ "beheerder" ]
}

test_beheren_with_unknown_role if {
    not beheren with input.user.rollen as [ "super" ]
}

test_beheren_with_wrong_role if {
    not beheren with input.user.rollen as [ "behandelaar" ]
}

########
# zoeken
########
test_zoeken_with_behandelaar_role if {
    zoeken with input.user.rollen as [ "raadpleger" ]
}

test_zoeken_with_unknown_role if {
    not zoeken with input.user.rollen as [ "plus" ]
}

test_zoeken_with_wrong_role if {
    not zoeken with input.user.rollen as [ "recordmanager" ]
}
