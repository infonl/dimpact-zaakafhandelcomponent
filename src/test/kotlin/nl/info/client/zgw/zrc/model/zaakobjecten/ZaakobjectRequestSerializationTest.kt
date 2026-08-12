/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import jakarta.json.Json
import jakarta.json.JsonObject
import nl.info.client.zgw.util.JsonbConfiguration
import java.io.StringReader
import java.net.URI

private val jsonb = JsonbConfiguration().getContext(Any::class.java)

private fun toJsonObject(zaakobjectRequest: ZaakobjectRequest): JsonObject =
    Json.createReader(StringReader(jsonb.toJson(zaakobjectRequest))).readObject()

/**
 * Verifies that the `*Request` hierarchy serializes to the same POST body shape the combined
 * read/write `Zaakobject` hierarchy produced before it was split: same field names, same
 * `objectTypeOverige` marker values, and no `url`/`uuid` fields.
 */
class ZaakobjectRequestSerializationTest : BehaviorSpec({
    val zaakUri = URI("https://example.com/zaken/api/v1/zaken/fakeZaakUuid")

    given("a ZaakobjectAdresRequest with an inline adres") {
        val bagobjectUri = URI("https://example.com/bag/api/v1/adressen/fakeAdresUuid")
        val zaakobjectAdresRequest = ZaakobjectAdresRequest(
            zaak = zaakUri,
            bagobjectURI = bagobjectUri,
            adres = ObjectAdres(
                identificatie = "fakeIdentificatie",
                wplWoonplaatsNaam = "fakeWoonplaatsNaam",
                gorOpenbareRuimteNaam = "fakeOpenbareRuimteNaam",
                huisnummer = 10,
                huisletter = "A",
                postcode = "1234AB"
            )
        )

        `when`("it is serialized") {
            val json = toJsonObject(zaakobjectAdresRequest)

            then("it contains the shared fields, objectType 'adres' and the inline objectIdentificatie, but no url or uuid") {
                json.getString("zaak") shouldBe zaakUri.toString()
                json.getString("object") shouldBe bagobjectUri.toString()
                json.getString("objectType") shouldBe "adres"
                json.getJsonObject("objectIdentificatie").getString("identificatie") shouldBe "fakeIdentificatie"
                json.containsKey("url").shouldBeFalse()
                json.containsKey("uuid").shouldBeFalse()
            }
        }
    }

    given("a ZaakobjectAdresRequest without an inline adres, linked only via the object URI") {
        val bagobjectUri = URI("https://example.com/bag/api/v1/adressen/fakeAdresUuid")
        val zaakobjectAdresRequest = ZaakobjectAdresRequest(zaak = zaakUri, bagobjectURI = bagobjectUri, adres = null)

        `when`("it is serialized") {
            val json = toJsonObject(zaakobjectAdresRequest)

            then("the objectIdentificatie field is omitted entirely rather than serialized as null") {
                json.containsKey("objectIdentificatie").shouldBeFalse()
            }
        }
    }

    given("a ZaakobjectPandRequest") {
        val bagobjectUri = URI("https://example.com/bag/api/v1/panden/fakePandUuid")
        val zaakobjectPandRequest = ZaakobjectPandRequest(
            zaak = zaakUri,
            bagobjectUri = bagobjectUri,
            pand = ObjectPand(identificatie = "fakeIdentificatie")
        )

        `when`("it is serialized") {
            val json = toJsonObject(zaakobjectPandRequest)

            then("it has objectType 'pand' and the inline objectIdentificatie, but no url or uuid") {
                json.getString("zaak") shouldBe zaakUri.toString()
                json.getString("object") shouldBe bagobjectUri.toString()
                json.getString("objectType") shouldBe "pand"
                json.getJsonObject("objectIdentificatie").getString("identificatie") shouldBe "fakeIdentificatie"
                json.containsKey("url").shouldBeFalse()
                json.containsKey("uuid").shouldBeFalse()
            }
        }
    }

    given("a ZaakobjectOpenbareRuimteRequest") {
        val bagobjectUri = URI("https://example.com/bag/api/v1/openbareruimtes/fakeOpenbareRuimteUuid")
        val zaakobjectOpenbareRuimteRequest = ZaakobjectOpenbareRuimteRequest(
            zaak = zaakUri,
            bagobjectURI = bagobjectUri,
            objectOpenbareRuimte = ObjectOpenbareRuimte(identificatie = "fakeIdentificatie")
        )

        `when`("it is serialized") {
            val json = toJsonObject(zaakobjectOpenbareRuimteRequest)

            then("it has objectType 'openbare_ruimte' and the inline objectIdentificatie, but no url or uuid") {
                json.getString("zaak") shouldBe zaakUri.toString()
                json.getString("object") shouldBe bagobjectUri.toString()
                json.getString("objectType") shouldBe "openbare_ruimte"
                json.getJsonObject("objectIdentificatie").getString("identificatie") shouldBe "fakeIdentificatie"
                json.containsKey("url").shouldBeFalse()
                json.containsKey("uuid").shouldBeFalse()
            }
        }
    }

    given("a ZaakobjectWoonplaatsRequest") {
        val bagobjectUri = URI("https://example.com/bag/api/v1/woonplaatsen/fakeWoonplaatsUuid")
        val zaakobjectWoonplaatsRequest = ZaakobjectWoonplaatsRequest(
            zaak = zaakUri,
            bagobjectUri = bagobjectUri,
            woonplaats = ObjectWoonplaats(identificatie = "fakeIdentificatie", woonplaatsNaam = "fakeWoonplaatsNaam")
        )

        `when`("it is serialized") {
            val json = toJsonObject(zaakobjectWoonplaatsRequest)

            then("it has objectType 'woonplaats' and the inline objectIdentificatie, but no url or uuid") {
                json.getString("zaak") shouldBe zaakUri.toString()
                json.getString("object") shouldBe bagobjectUri.toString()
                json.getString("objectType") shouldBe "woonplaats"
                json.getJsonObject("objectIdentificatie").getString("identificatie") shouldBe "fakeIdentificatie"
                json.containsKey("url").shouldBeFalse()
                json.containsKey("uuid").shouldBeFalse()
            }
        }
    }

    given("a ZaakobjectNummeraanduidingRequest") {
        val bagobjectUri = URI("https://example.com/bag/api/v1/nummeraanduidingen/fakeNummeraanduidingUuid")
        val zaakobjectNummeraanduidingRequest = ZaakobjectNummeraanduidingRequest(
            zaak = zaakUri,
            bagObjectUri = bagobjectUri,
            nummeraanduiding = ObjectNummeraanduiding(identificatie = "fakeIdentificatie")
        )

        `when`("it is serialized") {
            val json = toJsonObject(zaakobjectNummeraanduidingRequest)

            then(
                "it has objectType 'overige', the nummeraanduiding objectTypeOverige marker and the " +
                    "double-wrapped inline objectIdentificatie, but no url or uuid"
            ) {
                json.getString("zaak") shouldBe zaakUri.toString()
                json.getString("object") shouldBe bagobjectUri.toString()
                json.getString("objectType") shouldBe "overige"
                json.getString("objectTypeOverige") shouldBe ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE_NUMMERAANDUIDING
                json.getJsonObject("objectIdentificatie").getJsonObject("overigeData").getString("identificatie") shouldBe
                    "fakeIdentificatie"
                json.containsKey("url").shouldBeFalse()
                json.containsKey("uuid").shouldBeFalse()
            }
        }
    }

    given("a ZaakobjectProductaanvraagRequest") {
        val productaanvraagUri = URI("https://example.com/objects/api/v2/objects/fakeProductaanvraagUuid")
        val zaakobjectProductaanvraagRequest = ZaakobjectProductaanvraagRequest(
            zaak = zaakUri,
            productaanvraag = productaanvraagUri
        )

        `when`("it is serialized") {
            val json = toJsonObject(zaakobjectProductaanvraagRequest)

            then(
                "it has objectType 'overige', the productaanvraag objectTypeOverige marker, no objectIdentificatie " +
                    "field, and no url or uuid"
            ) {
                json.getString("zaak") shouldBe zaakUri.toString()
                json.getString("object") shouldBe productaanvraagUri.toString()
                json.getString("objectType") shouldBe "overige"
                json.getString("objectTypeOverige") shouldBe ZaakobjectProductaanvraag.OBJECT_TYPE_OVERIGE_PRODUCTAANVRAAG
                json.containsKey("objectIdentificatie").shouldBeFalse()
                json.containsKey("url").shouldBeFalse()
                json.containsKey("uuid").shouldBeFalse()
            }
        }
    }
})
