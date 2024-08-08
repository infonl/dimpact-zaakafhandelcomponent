/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp.model

import net.atos.client.brp.model.generated.Persoon
import net.atos.client.brp.model.generated.RaadpleegMetBurgerservicenummerResponse

fun createPersoon(
    bsn: String = "123456789",
    age: Int = 18
) = Persoon().apply {
    burgerservicenummer = bsn
    leeftijd = age
}

fun createRaadpleegMetBurgerservicenummerResponse(
    persons: List<Persoon> = listOf(createPersoon())
) = RaadpleegMetBurgerservicenummerResponse().apply {
    personen = persons
}
