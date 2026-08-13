/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.json.Json
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.stream.JsonParser
import nl.info.client.zgw.zrc.exception.ZrcRuntimeException
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectProductaanvraag
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import java.lang.reflect.Type

class ZaakObjectJsonbDeserializerTest : BehaviorSpec({
    val deserializer = ZaakObjectJsonbDeserializer()
    val parser = mockk<JsonParser>()
    val deserializationContext = mockk<DeserializationContext>()
    val runtimeType = mockk<Type>()

    afterEach {
        checkUnnecessaryStub()
    }

    given("a JsonObject with objectType 'adres'") {
        every { parser.getObject() } returns Json.createObjectBuilder()
            .add("objectType", "adres")
            .add("objectTypeOverige", "")
            .add("zaak", "https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
            .build()

        `when`("the deserializer is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("it returns a ZaakobjectAdres") {
                result.shouldBeInstanceOf<ZaakobjectAdres>()
            }
        }
    }

    given("a JsonObject with objectType 'pand'") {
        every { parser.getObject() } returns Json.createObjectBuilder()
            .add("objectType", "pand")
            .add("objectTypeOverige", "")
            .add("zaak", "https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
            .build()

        `when`("the deserializer is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("it returns a ZaakobjectPand") {
                result.shouldBeInstanceOf<ZaakobjectPand>()
            }
        }
    }

    given("a JsonObject with objectType 'openbare_ruimte'") {
        every { parser.getObject() } returns Json.createObjectBuilder()
            .add("objectType", "openbare_ruimte")
            .add("objectTypeOverige", "")
            .add("zaak", "https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
            .build()

        `when`("the deserializer is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("it returns a ZaakobjectOpenbareRuimte") {
                result.shouldBeInstanceOf<ZaakobjectOpenbareRuimte>()
            }
        }
    }

    given("a JsonObject with objectType 'woonplaats'") {
        every { parser.getObject() } returns Json.createObjectBuilder()
            .add("objectType", "woonplaats")
            .add("objectTypeOverige", "")
            .add("zaak", "https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
            .build()

        `when`("the deserializer is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("it returns a ZaakobjectWoonplaats") {
                result.shouldBeInstanceOf<ZaakobjectWoonplaats>()
            }
        }
    }

    given("a JsonObject with objectType 'overige' and the productaanvraag marker") {
        every { parser.getObject() } returns Json.createObjectBuilder()
            .add("objectType", "overige")
            .add("objectTypeOverige", ZaakobjectProductaanvraag.OBJECT_TYPE_OVERIGE_PRODUCTAANVRAAG)
            .add("zaak", "https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
            .build()

        `when`("the deserializer is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("it returns a ZaakobjectProductaanvraag") {
                result.shouldBeInstanceOf<ZaakobjectProductaanvraag>()
            }
        }
    }

    given("a JsonObject with objectType 'overige' and the nummeraanduiding marker") {
        every { parser.getObject() } returns Json.createObjectBuilder()
            .add("objectType", "overige")
            .add("objectTypeOverige", ZaakobjectNummeraanduiding.OBJECT_TYPE_OVERIGE_NUMMERAANDUIDING)
            .add("zaak", "https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
            .build()

        `when`("the deserializer is called") {
            val result = deserializer.deserialize(parser, deserializationContext, runtimeType)

            then("it returns a ZaakobjectNummeraanduiding") {
                result.shouldBeInstanceOf<ZaakobjectNummeraanduiding>()
            }
        }
    }

    given("a JsonObject with objectType 'overige' and an unsupported marker") {
        every { parser.getObject() } returns Json.createObjectBuilder()
            .add("objectType", "overige")
            .add("objectTypeOverige", "fakeUnsupportedMarker")
            .add("zaak", "https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
            .build()

        `when`("the deserializer is called") {
            val zrcRuntimeException = shouldThrow<ZrcRuntimeException> {
                deserializer.deserialize(parser, deserializationContext, runtimeType)
            }

            then("it throws a ZrcRuntimeException") {
                zrcRuntimeException.message shouldBe "objectType 'overige' wordt niet ondersteund"
            }
        }
    }

    given("a JsonObject with an unsupported objectType") {
        every { parser.getObject() } returns Json.createObjectBuilder()
            .add("objectType", "besluit")
            .add("objectTypeOverige", "")
            .add("zaak", "https://example.com/zaken/api/v1/zaken/fakeZaakUuid")
            .build()

        `when`("the deserializer is called") {
            val zrcRuntimeException = shouldThrow<ZrcRuntimeException> {
                deserializer.deserialize(parser, deserializationContext, runtimeType)
            }

            then("it throws a ZrcRuntimeException") {
                zrcRuntimeException.message shouldBe "objectType 'besluit' wordt niet ondersteund"
            }
        }
    }
})
