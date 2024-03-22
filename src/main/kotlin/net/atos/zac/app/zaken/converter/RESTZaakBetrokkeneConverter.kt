/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.converter

import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolNietNatuurlijkPersoon
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolVestiging
import net.atos.zac.app.zaken.model.RESTZaakBetrokkene
import java.util.stream.Stream

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
fun convertToRESTZaakBetrokkenen(rol: Rol<*>): RESTZaakBetrokkene =
    RESTZaakBetrokkene(
        rolid = rol.uuid.toString(),
        roltype = rol.omschrijving,
        roltoelichting = rol.roltoelichting,
        type = rol.betrokkeneType.name,
        identificatie = when (rol.betrokkeneType) {
            BetrokkeneType.NATUURLIJK_PERSOON -> (rol as RolNatuurlijkPersoon).betrokkeneIdentificatie.inpBsn
            BetrokkeneType.NIET_NATUURLIJK_PERSOON -> (rol as RolNietNatuurlijkPersoon).betrokkeneIdentificatie.innNnpId
            BetrokkeneType.VESTIGING -> (rol as RolVestiging).betrokkeneIdentificatie.vestigingsNummer
            BetrokkeneType.ORGANISATORISCHE_EENHEID -> (rol as RolOrganisatorischeEenheid).betrokkeneIdentificatie.naam
            BetrokkeneType.MEDEWERKER -> (rol as RolMedewerker).betrokkeneIdentificatie.identificatie
        }
    )

fun convertToRESTZaakBetrokkenen(rollen: Stream<Rol<*>>): List<RESTZaakBetrokkene> = rollen
    .map { rol -> convertToRESTZaakBetrokkenen(rol) }
    .toList()
