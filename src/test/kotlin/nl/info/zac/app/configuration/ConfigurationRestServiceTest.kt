/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.configuration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.util.JsonbUtil
import nl.info.zac.app.configuration.model.createTaal
import nl.info.zac.configuration.BrpConfiguration
import nl.info.zac.configuration.BrpConfigurationProvider
import nl.info.zac.configuration.ConfigurationService

class ConfigurationRestServiceTest : BehaviorSpec({
    val configurationService = mockk<ConfigurationService>()
    val configurationRestService = ConfigurationRestService(configurationService)

    given("Multiple languages are available") {
        val taal1 = createTaal(1L, "nl", "Nederlands", "Dutch", "nl_NL")
        val taal2 = createTaal(2L, "en", "Engels", "English", "en_US")
        val talen = listOf(taal1, taal2)

        every { configurationService.listTalen() } returns talen

        `when`("listTalen is called") {
            val result = configurationRestService.listTalen()

            then("it should return a list of RestTaal objects") {
                result.size shouldBe 2
                result[0].id shouldBe "1"
                result[0].code shouldBe "nl"
                result[0].naam shouldBe "Nederlands"
                result[0].name shouldBe "Dutch"
                result[0].local shouldBe "nl_NL"
                result[1].id shouldBe "2"
                result[1].code shouldBe "en"
                result[1].naam shouldBe "Engels"
                result[1].name shouldBe "English"
                result[1].local shouldBe "en_US"
            }
        }
    }

    given("No languages are available") {
        every { configurationService.listTalen() } returns emptyList()

        `when`("listTalen is called") {
            val result = configurationRestService.listTalen()

            then("it should return an empty list") {
                result shouldBe emptyList()
            }
        }
    }

    given("A default language is available") {
        val defaultTaal = createTaal(1L, "nl", "Nederlands", "Dutch", "nl_NL")
        every { configurationService.findDefaultTaal() } returns defaultTaal

        `when`("readDefaultTaal is called") {
            val result = configurationRestService.readDefaultTaal()

            then("it should return the default RestTaal") {
                result?.id shouldBe "1"
                result?.code shouldBe "nl"
                result?.naam shouldBe "Nederlands"
                result?.name shouldBe "Dutch"
                result?.local shouldBe "nl_NL"
            }
        }
    }

    given("No default language is available") {
        every { configurationService.findDefaultTaal() } returns null

        `when`("readDefaultTaal is called") {
            val result = configurationRestService.readDefaultTaal()

            then("it should return null") {
                result shouldBe null
            }
        }
    }

    given("A maximum file size is configured") {
        every { configurationService.readMaxFileSizeMB() } returns 999L

        `when`("readMaxFileSizeMB is called") {
            val result = configurationRestService.readMaxFileSizeMB()

            then("it should return the configured file size") {
                result shouldBe 999L
            }
        }
    }

    given("The allowed file types endpoint is queried") {
        `when`("listAllowedFileTypes is called") {
            val result = configurationRestService.listAllowedFileTypes()

            then("it returns the full canonical allowlist as extension/media-type pairs") {
                result.map { it.extension } shouldContainExactlyInAnyOrder listOf(
                    ".avi", ".bmp", ".doc", ".docx", ".eml", ".flv", ".gif",
                    ".jpeg", ".jpg", ".mkv", ".mov", ".mp4", ".mpeg", ".msg",
                    ".ods", ".odt", ".pdf", ".png", ".ppt", ".pptx", ".rtf",
                    ".txt", ".vsd", ".wmv", ".xls", ".xlsx"
                )
                result.first { it.extension == ".pdf" }.mediaType shouldBe "application/pdf"
                result.first { it.extension == ".jpg" }.mediaType shouldBe "image/jpeg"
            }
        }
    }

    given("A gemeente code is configured") {
        val gemeenteCode = "fakeGemeenteCode"
        every { configurationService.readGemeenteCode() } returns gemeenteCode

        `when`("readGemeenteCode is called") {
            val result = configurationRestService.readGemeenteCode()

            then("it should return the gemeente code as JSON") {
                result shouldBe JsonbUtil.JSONB.toJson(gemeenteCode)
            }
        }
    }

    given("A gemeente name is configured") {
        val gemeenteNaam = "fakeGemeenteNaam"
        every { configurationService.readGemeenteNaam() } returns gemeenteNaam

        `when`("readGemeenteNaam is called") {
            val result = configurationRestService.readGemeenteNaam()

            then("it should return the gemeente name as JSON") {
                result shouldBe JsonbUtil.JSONB.toJson(gemeenteNaam)
            }
        }
    }

    given("doelbindingPerZaaktype is true") {
        val brpConfiguration = mockk<BrpConfigurationProvider>()
        every { brpConfiguration.isDoelbindingPerZaaktypeEnabled() } returns true
        every { configurationService.readBrpConfiguration() } returns brpConfiguration

        `when`("readBrpDoelbindingSetupEnabled is called") {
            val result = configurationRestService.readBrpDoelbindingSetupEnabled()

            then("it should return true") {
                result shouldBe true
            }
        }
    }

    given("doelbindingPerZaaktype is false") {
        val brpConfiguration = mockk<BrpConfiguration>()
        every { brpConfiguration.isDoelbindingPerZaaktypeEnabled() } returns false
        every { configurationService.readBrpConfiguration() } returns brpConfiguration

        `when`("readBrpDoelbindingSetupEnabled is called") {
            val result = configurationRestService.readBrpDoelbindingSetupEnabled()

            then("it should return false") {
                result shouldBe false
            }
        }
    }
})
