/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.exception.KvkClientException
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.zac.util.joinNonBlank
import java.util.logging.Level
import java.util.logging.Logger
import nl.info.client.kvk.basisprofiel.model.generated.Adres as BasisprofielAdres
import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres as VestigingsprofielAdres

private const val KVK_ADRES_TYPE_CORRESPONDENTIEADRES = "correspondentieadres"
private const val KVK_ADRES_TYPE_BEZOEKADRES = "bezoekadres"

private val LOG = Logger.getLogger(KvkClientService::class.java.name)

/**
 * Returns the correspondentieadres, or else the bezoekadres, from the KVK vestigingsprofiel of a vestiging or from
 * the KVK basisprofiel of a rechtspersoon.
 * Returns `null` when KVK has neither address, and also when the profiel cannot be read, so that the caller can
 * continue without an address.
 */
fun KvkClientService.findCorrespondentieadresOrBezoekadres(resultaatItem: ResultaatItem): VestigingsprofielAdres? =
    try {
        findProfielCorrespondentieadresOrBezoekadres(resultaatItem)
    } catch (notFoundException: NotFoundException) {
        LOG.log(Level.INFO, "The KVK profiel of the bedrijf was not found, so its address is left out", notFoundException)
        null
    } catch (webApplicationException: WebApplicationException) {
        logKvkProfielFailure(webApplicationException)
    } catch (kvkClientException: KvkClientException) {
        logKvkProfielFailure(kvkClientException)
    } catch (processingException: ProcessingException) {
        logKvkProfielFailure(processingException)
    }

private fun KvkClientService.findProfielCorrespondentieadresOrBezoekadres(
    resultaatItem: ResultaatItem
): VestigingsprofielAdres? {
    val vestigingsnummer = resultaatItem.vestigingsnummer
    val kvkNummer = resultaatItem.kvkNummer
    return when {
        vestigingsnummer != null -> findVestigingsprofiel(vestigingsnummer)
            ?.adressen
            ?.findCorrespondentieadresOrBezoekadres { it.type }
        kvkNummer != null -> findBasisprofiel(kvkNummer)
            ?.embedded?.eigenaar?.adressen
            ?.findCorrespondentieadresOrBezoekadres { it.type }
            ?.toVestigingsprofielAdres()
        else -> null
    }
}

private fun logKvkProfielFailure(exception: RuntimeException): VestigingsprofielAdres? {
    LOG.log(Level.WARNING, "Could not read the KVK profiel of the bedrijf, so its address is left out", exception)
    return null
}

private fun <T> List<T>.findCorrespondentieadresOrBezoekadres(adresType: (T) -> String?) =
    firstOrNull { adresType(it) == KVK_ADRES_TYPE_CORRESPONDENTIEADRES }
        ?: firstOrNull { adresType(it) == KVK_ADRES_TYPE_BEZOEKADRES }

fun BasisprofielAdres.toVestigingsprofielAdres(): VestigingsprofielAdres =
    VestigingsprofielAdres()
        .type(type)
        .indAfgeschermd(indAfgeschermd)
        .volledigAdres(volledigAdres)
        .postbusnummer(postbusnummer)
        .straatHuisnummer(straatHuisnummer)
        .toevoegingAdres(toevoegingAdres)
        .postcodeWoonplaats(postcodeWoonplaats)
        .land(land)
        .straatnaam(straatnaam)
        .huisnummer(huisnummer)
        .huisletter(huisletter)
        .huisnummerToevoeging(huisnummerToevoeging)
        .postcode(postcode)
        .plaats(plaats)

fun VestigingsprofielAdres.toAddressString(wordSeparator: String = " ", huisnummerToevoegingSeparator: String = "") =
    when {
        postbusnummer != null -> listOf(
            "Postbus $postbusnummer",
            listOf(postcode, plaats).joinWords(wordSeparator)
        )
        straatHuisnummer != null -> listOf(
            listOf(straatHuisnummer, toevoegingAdres).joinWords(wordSeparator),
            listOf(postcodeWoonplaats).joinWords(wordSeparator),
            land
        )
        else -> listOf(
            listOf(
                straatnaam,
                listOf(
                    listOf(huisnummer?.toString(), huisletter).joinNonBlank(),
                    huisnummerToevoeging
                ).joinNonBlank(huisnummerToevoegingSeparator)
            ).joinWords(wordSeparator),
            listOf(postcode, plaats).joinWords(wordSeparator)
        )
    }.joinNonBlank(", ")

private fun List<String?>.joinWords(wordSeparator: String) =
    joinNonBlank(wordSeparator).replace(" ", wordSeparator)
