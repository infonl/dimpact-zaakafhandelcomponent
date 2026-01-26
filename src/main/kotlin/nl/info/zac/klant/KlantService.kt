/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.klant

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.zgw.zrc.model.Rol
import nl.info.client.brp.exception.BrpPersonNotFoundException
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.NATUURLIJK_PERSOON
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.NIET_NATUURLIJK_PERSOON
import nl.info.client.zgw.zrc.model.generated.BetrokkeneTypeEnum.VESTIGING
import nl.info.client.zgw.zrc.model.generated.NatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.NietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.zrc.model.generated.VestigingIdentificatie
import nl.info.zac.app.klant.model.klant.IdentificatieType
import nl.info.zac.app.zaak.model.BetrokkeneIdentificatie
import nl.info.zac.sensitive.SensitiveDataService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@NoArgConstructor
@AllOpen
class KlantService @Inject constructor(
    private val sensitiveDataService: SensitiveDataService
) {
    companion object {
        private val LOG = Logger.getLogger(KlantService::class.java.name)
    }

    fun createBetrokkeneIdentificatieForInitiatorRole(initiatorRole: Rol<*>): BetrokkeneIdentificatie? {
        val betrokkeneIdentificatie = initiatorRole.betrokkeneIdentificatie
        val initiatorIdentificatieType = getInitiatorIdentificationType(initiatorRole, betrokkeneIdentificatie)
        return initiatorIdentificatieType?.let {
            BetrokkeneIdentificatie(
                type = it,
                personId = (betrokkeneIdentificatie as? NatuurlijkPersoonIdentificatie)?.inpBsn?.let {
                    replaceBsnWithKey(it)
                },
                kvkNummer = (betrokkeneIdentificatie as? NietNatuurlijkPersoonIdentificatie)?.kvkNummer,
                rsin = (betrokkeneIdentificatie as? NietNatuurlijkPersoonIdentificatie)?.innNnpId,
                vestigingsnummer = (betrokkeneIdentificatie as? NietNatuurlijkPersoonIdentificatie)?.vestigingsNummer
                    // we also support the legacy type of vestiging role
                    ?: (betrokkeneIdentificatie as? VestigingIdentificatie)?.vestigingsNummer
            )
        }
    }

    private fun getInitiatorIdentificationType(
        initiatorRole: Rol<*>,
        betrokkeneIdentificatie: Any
    ): IdentificatieType? =
        when (val betrokkeneType = initiatorRole.betrokkeneType) {
            NATUURLIJK_PERSOON -> IdentificatieType.BSN
            VESTIGING -> IdentificatieType.VN
            // the 'niet_natuurlijk_persoon' rol type is used both for rechtspersonen ('RSIN') as well as vestigingen
            NIET_NATUURLIJK_PERSOON -> (betrokkeneIdentificatie as? NietNatuurlijkPersoonIdentificatie)?.let {
                when {
                    // we support 'legacy' RSIN-type initiators with only an RSIN (no KVK nor vestigings number)
                    it.innNnpId?.isNotBlank() == true ||
                        (it.kvkNummer?.isNotBlank() == true && it.vestigingsNummer.isNullOrBlank()) -> IdentificatieType.RSIN
                    // as well as new 'RSIN-type' initiators with only a KVK number (but no vestigingsnummer)
                    it.vestigingsNummer?.isNotBlank() == true -> IdentificatieType.VN
                    else -> {
                        LOG.warning(
                            "Unsupported identification fields for betrokkene type: '$betrokkeneType' " +
                                "for role with UUID: '${initiatorRole.uuid}'"
                        )
                        null
                    }
                }
            }
            // betrokkeneType may be null (sadly enough)
            null -> null
            else -> {
                LOG.warning(
                    "Unsupported betrokkene type: '$betrokkeneType' for role with UUID: '${initiatorRole.uuid}'"
                )
                null
            }
        }

    fun replaceBsnWithKey(bsn: String): UUID = sensitiveDataService.put(bsn)

    fun replaceKeyWithBsn(key: UUID): String = sensitiveDataService.get(key)
        ?: throw BrpPersonNotFoundException("Geen persoon gevonden voor id '$key'")
}
