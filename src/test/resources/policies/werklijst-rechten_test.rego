#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.overig

import rego.v1

import data.net.atos.zac.werklijst.inbox
import data.net.atos.zac.werklijst.ontkoppelde_documenten_verwijderen
import data.net.atos.zac.werklijst.inbox_productaanvragen_verwijderen
import data.net.atos.zac.werklijst.zaken_taken
import data.net.atos.zac.werklijst.zaken_taken_exporteren
import data.net.atos.zac.werklijst.zaken_taken_verdelen

#######
# inbox
#######
test_inbox if {
    inbox with input.user.rollen as [ "behandelaar" ]
}

test_inbox_wrong_role_fails if {
    not inbox with input.user.rollen as [ "functioneel" ]
}

test_inbox_missing_role_fails if {
    not inbox with input.user.key as "value"
}

####################################
# ontkoppelde_documenten_verwijderen
####################################
test_ontkoppelde_documenten_verwijderen if {
    ontkoppelde_documenten_verwijderen with input.user.rollen as [ "recordmanager" ]
}

test_ontkoppelde_documenten_verwijderen_wrong_role_fails if {
    not ontkoppelde_documenten_verwijderen with input.user.rollen as [ "functioneel" ]
}

test_ontkoppelde_documenten_verwijderen_missing_role_fails if {
    not ontkoppelde_documenten_verwijderen with input.user.key as "value"
}

####################################
# inbox_productaanvragen_verwijderen
####################################
test_inbox_productaanvragen_verwijderen if {
    inbox_productaanvragen_verwijderen with input.user.rollen as [ "recordmanager" ]
}

test_inbox_productaanvragen_verwijderen_wrong_role_fails if {
    not inbox_productaanvragen_verwijderen with input.user.rollen as [ "functioneel" ]
}

test_inbox_productaanvragen_verwijderen_missing_role_fails if {
    not inbox_productaanvragen_verwijderen with input.user.key as "value"
}

#############
# zaken_taken
#############
test_zaken_taken if {
    zaken_taken with input.user.rollen as [ "raadpleger" ]
}

test_zaken_taken_wrong_role_fails if {
    not zaken_taken with input.user.rollen as [ "functioneel" ]
}

test_zaken_taken_missing_role_fails if {
    not zaken_taken with input.user.key as "value"
}

######################
# zaken_taken_verdelen
######################
test_zaken_taken_verdelen if {
    zaken_taken_verdelen with input.user.rollen as [ "coordinator" ]
}

test_zaken_taken_verdelen_wrong_role_fails if {
    not zaken_taken_verdelen with input.user.rollen as [ "functioneel" ]
}

test_zaken_taken_verdelen_missing_role_fails if {
    not zaken_taken_verdelen with input.user.key as "value"
}

######################
# zaken_taken_exporteren
######################
test_zaken_taken_exporteren if {
    zaken_taken_exporteren with input.user.rollen as [ "beheerder" ]
}

test_zaken_taken_exporteren_wrong_role_fails if {
    not zaken_taken_exporteren with input.user.rollen as [ "functioneel" ]
}

test_zaken_taken_exporteren_missing_role_fails if {
    not zaken_taken_exporteren with input.user.key as "value"
}
