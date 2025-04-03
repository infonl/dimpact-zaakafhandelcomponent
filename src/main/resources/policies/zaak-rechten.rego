#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.zaak

import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.coordinator
import data.net.atos.zac.rol.recordmanager
import input.zaak
import input.user

zaak_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen,
    "toekennen": toekennen,
    "behandelen": behandelen,
    "afbreken": afbreken,
    "heropenen": heropenen,
    "bekijken_zaakdata": bekijken_zaakdata,
    "wijzigen_doorlooptijd": wijzigen_doorlooptijd,
    "verlengen": verlengen,
    "opschorten": opschorten,
    "hervatten": hervatten,
    "creeeren_document": creeeren_document,
    "toevoegen_document": toevoegen_document,
    "koppelen": koppelen,
    "versturen_email": versturen_email,
    "versturen_ontvangstbevestiging": versturen_ontvangstbevestiging,
    "toevoegen_initiator_persoon": toevoegen_initiator_persoon,
    "toevoegen_initiator_bedrijf": toevoegen_initiator_bedrijf,
    "verwijderen_initiator": verwijderen_initiator,
    "toevoegen_betrokkene_persoon": toevoegen_betrokkene_persoon,
    "toevoegen_betrokkene_bedrijf": toevoegen_betrokkene_bedrijf,
    "verwijderen_betrokkene": verwijderen_betrokkene,
    "toevoegen_bag_object": toevoegen_bag_object,
    "starten_taak": starten_taak,
    "vastleggen_besluit": vastleggen_besluit,
    "verlengen_doorlooptijd": verlengen_doorlooptijd
}

default zaaktype_allowed := false
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    zaak.zaaktype in user.zaaktypen
}

default lezen := false
lezen if {
    raadpleger.rol in user.rollen
    zaaktype_allowed
}

default wijzigen := false
wijzigen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

wijzigen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toekennen := false
toekennen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default behandelen := false
behandelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default afbreken := false
afbreken if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default heropenen := false
heropenen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default bekijken_zaakdata := false
bekijken_zaakdata if {
    beheerder.rol in user.rollen
    zaaktype_allowed
}

default wijzigen_doorlooptijd := false
wijzigen_doorlooptijd if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default verlengen := false
verlengen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
    not zaak.verlengd
}

default opschorten := false
opschorten if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
}

default hervatten := false
hervatten if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default creeeren_document := false
creeeren_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default toevoegen_document := false
toevoegen_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_document if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default koppelen := false
koppelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

koppelen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default versturen_email := false
versturen_email if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default versturen_ontvangstbevestiging := false
versturen_ontvangstbevestiging if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default toevoegen_initiator_persoon := false
toevoegen_initiator_persoon if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_initiator_persoon if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toevoegen_initiator_bedrijf := false
toevoegen_initiator_bedrijf if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_initiator_bedrijf if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default verwijderen_initiator := false
verwijderen_initiator if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

verwijderen_initiator if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toevoegen_betrokkene_persoon := false
toevoegen_betrokkene_persoon if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_betrokkene_persoon if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toevoegen_betrokkene_bedrijf := false
toevoegen_betrokkene_bedrijf if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_betrokkene_bedrijf if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default verwijderen_betrokkene := false
verwijderen_betrokkene if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

verwijderen_betrokkene if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default toevoegen_bag_object := false
toevoegen_bag_object if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_bag_object if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default starten_taak := false
starten_taak if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

default vastleggen_besluit := false
vastleggen_besluit if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.intake
    zaak.besloten
}

default verlengen_doorlooptijd := false
verlengen_doorlooptijd if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
