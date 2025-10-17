/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.brp.model

import nl.info.client.brp.model.generated.NaamPersoon

fun createNaamPersoon(
    initials: String = "fakeFirstLetters",
    firstNames: String = "fakeFirstName",
    surname: String = "fakeLastName",
    prefix: String? = "fakePrefix",
    fullName: String = "fakeFullName",
) = NaamPersoon().apply {
    this.voorletters = initials
    this.voornamen = firstNames
    this.geslachtsnaam = surname
    this.voorvoegsel = prefix
    this.volledigeNaam = fullName
}
