/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.generated.ZaakSub
import java.lang.reflect.Field

/**
 * Extension function on [Zaak] to create a [ZaakSub] by copying all relevant information.
 * Uses reflection for read-only fields (url, uuid, einddatum, laatstGemuteerd, etc.).
 */
fun Zaak.toZaakSub(): ZaakSub {
    val zaakSub = ZaakSub().apply {
        identificatie = this@toZaakSub.identificatie
        bronorganisatie = this@toZaakSub.bronorganisatie
        omschrijving = this@toZaakSub.omschrijving
        toelichting = this@toZaakSub.toelichting
        zaaktype = this@toZaakSub.zaaktype
        registratiedatum = this@toZaakSub.registratiedatum
        verantwoordelijkeOrganisatie = this@toZaakSub.verantwoordelijkeOrganisatie
        startdatum = this@toZaakSub.startdatum
        einddatumGepland = this@toZaakSub.einddatumGepland
        uiterlijkeEinddatumAfdoening = this@toZaakSub.uiterlijkeEinddatumAfdoening
        publicatiedatum = this@toZaakSub.publicatiedatum
        laatstGeopend = this@toZaakSub.laatstGeopend
        communicatiekanaal = this@toZaakSub.communicatiekanaal
        communicatiekanaalNaam = this@toZaakSub.communicatiekanaalNaam
        productenOfDiensten = this@toZaakSub.productenOfDiensten
        vertrouwelijkheidaanduiding = this@toZaakSub.vertrouwelijkheidaanduiding
        betalingsindicatie = this@toZaakSub.betalingsindicatie
        laatsteBetaaldatum = this@toZaakSub.laatsteBetaaldatum
        zaakgeometrie = this@toZaakSub.zaakgeometrie
        verlenging = this@toZaakSub.verlenging
        opschorting = this@toZaakSub.opschorting
        selectielijstklasse = this@toZaakSub.selectielijstklasse
        hoofdzaak = this@toZaakSub.hoofdzaak
        relevanteAndereZaken = this@toZaakSub.relevanteAndereZaken
        kenmerken = this@toZaakSub.kenmerken
        archiefnominatie = this@toZaakSub.archiefnominatie
        archiefstatus = this@toZaakSub.archiefstatus
        archiefactiedatum = this@toZaakSub.archiefactiedatum
        opdrachtgevendeOrganisatie = this@toZaakSub.opdrachtgevendeOrganisatie
        processobjectaard = this@toZaakSub.processobjectaard
        startdatumBewaartermijn = this@toZaakSub.startdatumBewaartermijn
        processobject = this@toZaakSub.processobject
    }

    // Use reflection for read-only fields
    setReadOnlyField(zaakSub, "url", this.url)
    setReadOnlyField(zaakSub, "uuid", this.uuid)
    setReadOnlyField(zaakSub, "einddatum", this.einddatum)
    setReadOnlyField(zaakSub, "laatstGemuteerd", this.laatstGemuteerd)
    setReadOnlyField(zaakSub, "betalingsindicatieWeergave", this.betalingsindicatieWeergave)
    setReadOnlyField(zaakSub, "deelzaken", this.deelzaken)
    setReadOnlyField(zaakSub, "eigenschappen", this.eigenschappen)
    setReadOnlyField(zaakSub, "rollen", this.rollen)
    setReadOnlyField(zaakSub, "status", this.status)
    setReadOnlyField(zaakSub, "zaakinformatieobjecten", this.zaakinformatieobjecten)
    setReadOnlyField(zaakSub, "zaakobjecten", this.zaakobjecten)
    setReadOnlyField(zaakSub, "resultaat", this.resultaat)

    return zaakSub
}

/**
 * Helper function to set a read-only field using reflection.
 */
private fun setReadOnlyField(target: Any, fieldName: String, value: Any?) {
    try {
        val field: Field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    } catch (e: NoSuchFieldException) {
        // Field doesn't exist, skip it
    }
}
