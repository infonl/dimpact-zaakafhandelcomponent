/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createBetrokkeneKoppelingen
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding

class RestZaaktypeAanmaakInstellingenTest : BehaviorSpec({
    given(
        "A zaaktype with a configuration with a default groep, a default behandelaar and only the BRP koppeling switched on"
    ) {
        val zaaktype = createZaakType(
            omschrijving = "fakeOmschrijving",
            vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.ZAAKVERTROUWELIJK
        )
        val zaaktypeConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeUUID = zaaktype.url.extractUuid(),
            groupId = "fakeDefaultGroepId",
            defaultBehandelaarId = "fakeDefaultBehandelaarId",
            zaaktypeBetrokkeneParameters = createBetrokkeneKoppelingen(brpKoppelen = true, kvkKoppelen = false)
        )

        `when`("it is converted to the settings for zaak creation") {
            val restZaaktypeAanmaakInstellingen = zaaktype.toRestZaaktypeAanmaakInstellingen(zaaktypeConfiguration)

            then("the settings carry the zaaktype, its vertrouwelijkheidaanduiding and the settings of its configuration") {
                restZaaktypeAanmaakInstellingen shouldBe RestZaaktypeAanmaakInstellingen(
                    uuid = zaaktype.url.extractUuid(),
                    omschrijving = "fakeOmschrijving",
                    vertrouwelijkheidaanduiding = RestVertrouwelijkheidaanduiding.ZAAKVERTROUWELIJK,
                    defaultGroepId = "fakeDefaultGroepId",
                    defaultBehandelaarId = "fakeDefaultBehandelaarId",
                    isKvkKoppelenEnabled = false,
                    isBrpKoppelenEnabled = true
                )
            }
        }
    }

    given("A zaaktype with a configuration without a default behandelaar and without betrokkene koppelingen") {
        val zaaktype = createZaakType()
        val zaaktypeConfiguration = ZaaktypeConfiguration().apply {
            zaaktypeUuid = zaaktype.url.extractUuid()
            groepID = "fakeDefaultGroepId"
        }

        `when`("it is converted to the settings for zaak creation") {
            val restZaaktypeAanmaakInstellingen = zaaktype.toRestZaaktypeAanmaakInstellingen(zaaktypeConfiguration)

            then("there is no default behandelaar and neither koppeling is switched on") {
                with(restZaaktypeAanmaakInstellingen) {
                    defaultBehandelaarId shouldBe null
                    isKvkKoppelenEnabled shouldBe false
                    isBrpKoppelenEnabled shouldBe false
                }
            }
        }
    }
})
