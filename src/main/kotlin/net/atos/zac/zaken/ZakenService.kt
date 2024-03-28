package net.atos.zac.zaken

import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.zrc.model.Medewerker
import net.atos.client.zgw.zrc.model.OrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.zoeken.IndexeerService
import net.atos.zac.zoeken.model.index.ZoekObjectType
import java.util.UUID

class ZakenService @Inject constructor(
    private val zrcClientService: ZRCClientService,
    private val ztcClientService: ZTCClientService,
    private val indexeerService: IndexeerService
) {
    companion object {
        private val defaultCoroutineScope = CoroutineScope(Dispatchers.Default)
    }

    /**
     * Asynchronously assigns a list of zaken to a group and/or user and updates the search index on the fly.
     */
    fun assignZakenAsync(zaakUUIDs: List<UUID>, explanation: String? = null, group: Group? = null, user: User? = null) =
        defaultCoroutineScope.launch {
            zaakUUIDs.forEach { zaakUUID ->
                val zaak = zrcClientService.readZaak(zaakUUID)
                group?.let {
                    zrcClientService.updateRol(
                        zaak,
                        bepaalRolGroep(it, zaak),
                        explanation
                    )
                }
                user?.let {
                    zrcClientService.updateRol(
                        zaak,
                        bepaalRolMedewerker(it, zaak),
                        explanation
                    )
                }
                indexeerService.indexeerDirect(
                    zaakUUID.toString(),
                    ZoekObjectType.ZAAK
                )
            }
        }

    fun bepaalRolGroep(group: Group, zaak: Zaak) =
        RolOrganisatorischeEenheid(
            zaak.url,
            ztcClientService.readRoltype(
                RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaak.zaaktype
            ),
            "Behandelend groep van de zaak",
            OrganisatorischeEenheid().apply {
                identificatie = group.id
                naam = group.name
            }
        )

    fun bepaalRolMedewerker(user: User, zaak: Zaak) =
        RolMedewerker(
            zaak.url,
            ztcClientService.readRoltype(
                RolType.OmschrijvingGeneriekEnum.BEHANDELAAR,
                zaak.zaaktype
            ),
            "Behandelaar van de zaak",
            Medewerker().apply {
                identificatie = user.id
                voorletters = user.firstName
                achternaam = user.lastName
            }
        )
}
