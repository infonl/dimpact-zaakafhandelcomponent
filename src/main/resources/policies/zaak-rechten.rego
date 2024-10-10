#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.zaak

import future.keywords
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
zaaktype_allowed {
    not user.zaaktypen
}
zaaktype_allowed {
    zaak.zaaktype in user.zaaktypen
}

default lezen := false
lezen {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
}

default wijzigen := false
wijzigen {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
wijzigen {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default toekennen := false
toekennen {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
}

default behandelen := false
behandelen {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
}

default afbreken := false
afbreken {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
}

default heropenen := false
heropenen {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default bekijken_zaakdata := false
bekijken_zaakdata {
    beheerder.rol in user.rollen
    zaaktype_allowed == true
}

default wijzigen_doorlooptijd := false
wijzigen_doorlooptijd {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
}

default verlengen := false
verlengen {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
    zaak.heropend == false
    zaak.opgeschort == false
    zaak.verlengd == false
}

default opschorten := false
opschorten {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
    zaak.heropend == false
    zaak.opgeschort == false
}

default hervatten := false
hervatten {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
}

default creeeren_document := false
creeeren_document {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}

default toevoegen_document := false
toevoegen_document {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
toevoegen_document {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default koppelen := false
koppelen {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
koppelen {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default versturen_email := false
versturen_email {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}

default versturen_ontvangstbevestiging := false
versturen_ontvangstbevestiging {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}

default toevoegen_initiator_persoon := false
toevoegen_initiator_persoon {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
toevoegen_initiator_persoon {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default toevoegen_initiator_bedrijf := false
toevoegen_initiator_bedrijf {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
toevoegen_initiator_bedrijf {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default verwijderen_initiator := false
verwijderen_initiator {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
verwijderen_initiator {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default toevoegen_betrokkene_persoon := false
toevoegen_betrokkene_persoon {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
toevoegen_betrokkene_persoon {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default toevoegen_betrokkene_bedrijf := false
toevoegen_betrokkene_bedrijf {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
toevoegen_betrokkene_bedrijf {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default verwijderen_betrokkene := false
verwijderen_betrokkene {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
verwijderen_betrokkene {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default toevoegen_bag_object := false
toevoegen_bag_object {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
toevoegen_bag_object {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
}

default starten_taak := false
starten_taak {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}

default vastleggen_besluit := false
vastleggen_besluit {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
    zaak.intake == false
    zaak.besloten == true
}

default verlengen_doorlooptijd := false
verlengen_doorlooptijd {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    zaak.open == true
}
