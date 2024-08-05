/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.klant;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.klant.exception.KlantRuntimeExceptionMapper;
import net.atos.client.klant.model.Actor;
import net.atos.client.klant.model.ActorKlantcontact;
import net.atos.client.klant.model.Betrokkene;
import net.atos.client.klant.model.Bijlage;
import net.atos.client.klant.model.Categorie;
import net.atos.client.klant.model.CategorieRelatie;
import net.atos.client.klant.model.DigitaalAdres;
import net.atos.client.klant.model.ExpandKlantcontact;
import net.atos.client.klant.model.ExpandPartij;
import net.atos.client.klant.model.InterneTaak;
import net.atos.client.klant.model.Klantcontact;
import net.atos.client.klant.model.Onderwerpobject;
import net.atos.client.klant.model.PaginatedActorKlantcontactList;
import net.atos.client.klant.model.PaginatedActorList;
import net.atos.client.klant.model.PaginatedBetrokkeneList;
import net.atos.client.klant.model.PaginatedBijlageList;
import net.atos.client.klant.model.PaginatedCategorieList;
import net.atos.client.klant.model.PaginatedCategorieRelatieList;
import net.atos.client.klant.model.PaginatedDigitaalAdresList;
import net.atos.client.klant.model.PaginatedExpandKlantcontactList;
import net.atos.client.klant.model.PaginatedExpandPartijList;
import net.atos.client.klant.model.PaginatedInterneTaakList;
import net.atos.client.klant.model.PaginatedOnderwerpobjectList;
import net.atos.client.klant.model.PaginatedPartijIdentificatorList;
import net.atos.client.klant.model.PaginatedRekeningnummerList;
import net.atos.client.klant.model.PaginatedVertegenwoordigdenList;
import net.atos.client.klant.model.Partij;
import net.atos.client.klant.model.PartijIdentificator;
import net.atos.client.klant.model.PatchedActor;
import net.atos.client.klant.model.PatchedActorKlantcontact;
import net.atos.client.klant.model.PatchedBetrokkene;
import net.atos.client.klant.model.PatchedBijlage;
import net.atos.client.klant.model.PatchedCategorie;
import net.atos.client.klant.model.PatchedCategorieRelatie;
import net.atos.client.klant.model.PatchedDigitaalAdres;
import net.atos.client.klant.model.PatchedInterneTaak;
import net.atos.client.klant.model.PatchedKlantcontact;
import net.atos.client.klant.model.PatchedOnderwerpobject;
import net.atos.client.klant.model.PatchedPartij;
import net.atos.client.klant.model.PatchedPartijIdentificator;
import net.atos.client.klant.model.PatchedRekeningnummer;
import net.atos.client.klant.model.PatchedVertegenwoordigden;
import net.atos.client.klant.model.Rekeningnummer;
import net.atos.client.klant.model.Vertegenwoordigden;
import net.atos.client.klant.util.KlantClientHeadersFactory;

/**
 * Klanten API
 * <p>
 * Een API om zowel klanten te registreren als op te vragen.
 * Een klant is een natuurlijk persoon, niet-natuurlijk persoon (bedrijf) of vestiging waarbij het gaat om niet geverifieerde gegevens.
 */
@RegisterRestClient(configKey = "Klanten-API-Client")
@RegisterClientHeaders(KlantClientHeadersFactory.class)
@RegisterProvider(KlantRuntimeExceptionMapper.class)
@Path("/klantinteracties/api/v1")
public interface KlantClient {

    @POST
    @Path("/actoren")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Actor actorenCreate(Actor actor) throws ProcessingException;

    @DELETE
    @Path("/actoren/{uuid}")
    void actorenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/actoren")
    @Produces({"application/json"})
    PaginatedActorList actorenList(
            @QueryParam("actoridentificatorCodeObjecttype") String actoridentificatorCodeObjecttype,
            @QueryParam("actoridentificatorCodeRegister") String actoridentificatorCodeRegister,
            @QueryParam("actoridentificatorCodeSoortObjectId") String actoridentificatorCodeSoortObjectId,
            @QueryParam("actoridentificatorObjectId") String actoridentificatorObjectId,
            @QueryParam("indicatieActief") Boolean indicatieActief,
            @QueryParam("naam") String naam,
            @QueryParam("page") Integer page,
            @QueryParam("soortActor") String soortActor
    ) throws ProcessingException;

    @PATCH
    @Path("/actoren/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Actor actorenPartialUpdate(@PathParam("uuid") UUID uuid, PatchedActor patchedActor) throws ProcessingException;

    @GET
    @Path("/actoren/{uuid}")
    @Produces({"application/json"})
    Actor actorenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/actoren/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Actor actorenUpdate(@PathParam("uuid") UUID uuid, Actor actor) throws ProcessingException;

    @POST
    @Path("/actorklantcontacten")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    ActorKlantcontact actorklantcontactenCreate(ActorKlantcontact actorKlantcontact) throws ProcessingException;

    @DELETE
    @Path("/actorklantcontacten/{uuid}")
    void actorklantcontactenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/actorklantcontacten")
    @Produces({"application/json"})
    PaginatedActorKlantcontactList actorklantcontactenList(
            @QueryParam("actor__url") String actorUrl,
            @QueryParam("actor__uuid") UUID actorUuid,
            @QueryParam("klantcontact__url") String klantcontactUrl,
            @QueryParam("klantcontact__uuid") UUID klantcontactUuid,
            @QueryParam("page") Integer page
    ) throws ProcessingException;

    @PATCH
    @Path("/actorklantcontacten/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    ActorKlantcontact actorklantcontactenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedActorKlantcontact patchedActorKlantcontact
    ) throws ProcessingException;

    @GET
    @Path("/actorklantcontacten/{uuid}")
    @Produces({"application/json"})
    ActorKlantcontact actorklantcontactenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/actorklantcontacten/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    ActorKlantcontact actorklantcontactenUpdate(
            @PathParam("uuid") UUID uuid,
            ActorKlantcontact actorKlantcontact
    ) throws ProcessingException;

    @POST
    @Path("/betrokkenen")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Betrokkene betrokkenenCreate(Betrokkene betrokkene) throws ProcessingException;

    @DELETE
    @Path("/betrokkenen/{uuid}")
    void betrokkenenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/betrokkenen")
    @Produces({"application/json"})
    PaginatedBetrokkeneList betrokkenenList(
            @QueryParam("contactnaamAchternaam") String contactnaamAchternaam,
            @QueryParam("contactnaamVoorletters") String contactnaamVoorletters,
            @QueryParam("contactnaamVoornaam") String contactnaamVoornaam,
            @QueryParam("contactnaamVoorvoegselAchternaam") String contactnaamVoorvoegselAchternaam,
            @QueryParam("hadKlantcontact__nummer") String hadKlantcontactNummer,
            @QueryParam("hadKlantcontact__url") String hadKlantcontactUrl,
            @QueryParam("hadKlantcontact__uuid") String hadKlantcontactUuid,
            @QueryParam("organisatienaam") String organisatienaam,
            @QueryParam("page") Integer page,
            @QueryParam("verstrektedigitaalAdres__adres") String verstrektedigitaalAdresAdres,
            @QueryParam("verstrektedigitaalAdres__url") String verstrektedigitaalAdresUrl,
            @QueryParam("verstrektedigitaalAdres__uuid") String verstrektedigitaalAdresUuid,
            @QueryParam("wasPartij__nummer") String wasPartijNummer,
            @QueryParam("wasPartij__url") String wasPartijUrl,
            @QueryParam("wasPartij__uuid") String wasPartijUuid
    ) throws ProcessingException;

    @PATCH
    @Path("/betrokkenen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Betrokkene betrokkenenPartialUpdate(@PathParam("uuid") UUID uuid, PatchedBetrokkene patchedBetrokkene) throws ProcessingException;

    @GET
    @Path("/betrokkenen/{uuid}")
    @Produces({"application/json"})
    Betrokkene betrokkenenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/betrokkenen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Betrokkene betrokkenenUpdate(@PathParam("uuid") UUID uuid, Betrokkene betrokkene) throws ProcessingException;

    @POST
    @Path("/bijlagen")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Bijlage bijlagenCreate(Bijlage bijlage) throws ProcessingException;

    @DELETE
    @Path("/bijlagen/{uuid}")
    void bijlagenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/bijlagen")
    @Produces({"application/json"})
    PaginatedBijlageList bijlagenList(
            @QueryParam("bijlageidentificatorCodeObjecttype") String bijlageidentificatorCodeObjecttype,
            @QueryParam("bijlageidentificatorCodeRegister") String bijlageidentificatorCodeRegister,
            @QueryParam("bijlageidentificatorCodeSoortObjectId") String bijlageidentificatorCodeSoortObjectId,
            @QueryParam("bijlageidentificatorObjectId") String bijlageidentificatorObjectId,
            @QueryParam("page") Integer page
    ) throws ProcessingException;

    @PATCH
    @Path("/bijlagen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Bijlage bijlagenPartialUpdate(@PathParam("uuid") UUID uuid, PatchedBijlage patchedBijlage) throws ProcessingException;

    @GET
    @Path("/bijlagen/{uuid}")
    @Produces({"application/json"})
    Bijlage bijlagenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/bijlagen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Bijlage bijlagenUpdate(@PathParam("uuid") UUID uuid, Bijlage bijlage) throws ProcessingException;

    @POST
    @Path("/categorieen")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Categorie categorieenCreate(Categorie categorie) throws ProcessingException;

    @DELETE
    @Path("/categorieen/{uuid}")
    void categorieenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/categorieen")
    @Produces({"application/json"})
    PaginatedCategorieList categorieenList(@QueryParam("page") Integer page) throws ProcessingException;

    @PATCH
    @Path("/categorieen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Categorie categorieenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedCategorie patchedCategorie
    ) throws ProcessingException;

    @GET
    @Path("/categorieen/{uuid}")
    @Produces({"application/json"})
    Categorie categorieenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/categorieen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Categorie categorieenUpdate(
            @PathParam("uuid") UUID uuid,
            Categorie categorie
    ) throws ProcessingException;

    @POST
    @Path("/categorie-relaties")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    CategorieRelatie categorieRelatiesCreate(CategorieRelatie categorieRelatie) throws ProcessingException;

    @DELETE
    @Path("/categorie-relaties/{uuid}")
    void categorieRelatiesDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/categorie-relaties")
    @Produces({"application/json"})
    PaginatedCategorieRelatieList categorieRelatiesList(
            @QueryParam("beginDatum") LocalDate beginDatum,
            @QueryParam("categorie__naam") String categorieNaam,
            @QueryParam("categorie__url") String categorieUrl,
            @QueryParam("categorie__uuid") String categorieUuid,
            @QueryParam("eindDatum") LocalDate eindDatum,
            @QueryParam("page") Integer page,
            @QueryParam("partij__nummer") String partijNummer,
            @QueryParam("partij__url") String partijUrl,
            @QueryParam("partij__uuid") String partijUuid
    ) throws ProcessingException;

    @PATCH
    @Path("/categorie-relaties/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    CategorieRelatie categorieRelatiesPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedCategorieRelatie patchedCategorieRelatie
    ) throws ProcessingException;

    @GET
    @Path("/categorie-relaties/{uuid}")
    @Produces({"application/json"})
    CategorieRelatie categorieRelatiesRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/categorie-relaties/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    CategorieRelatie categorieRelatiesUpdate(
            @PathParam("uuid") UUID uuid,
            CategorieRelatie categorieRelatie
    ) throws ProcessingException;

    @POST
    @Path("/digitaleadressen")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    DigitaalAdres digitaleadressenCreate(DigitaalAdres digitaalAdres) throws ProcessingException;

    @DELETE
    @Path("/digitaleadressen/{uuid}")
    void digitaleadressenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/digitaleadressen")
    @Produces({"application/json"})
    PaginatedDigitaalAdresList digitaleadressenList(@QueryParam("page") Integer page) throws ProcessingException;

    @PATCH
    @Path("/digitaleadressen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    DigitaalAdres digitaleadressenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedDigitaalAdres patchedDigitaalAdres
    ) throws ProcessingException;

    @GET
    @Path("/digitaleadressen/{uuid}")
    @Produces({"application/json"})
    DigitaalAdres digitaleadressenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/digitaleadressen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    DigitaalAdres digitaleadressenUpdate(
            @PathParam("uuid") UUID uuid,
            DigitaalAdres digitaalAdres
    ) throws ProcessingException;

    @POST
    @Path("/internetaken")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    InterneTaak internetakenCreate(InterneTaak interneTaak) throws ProcessingException;

    @DELETE
    @Path("/internetaken/{uuid}")
    void internetakenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/internetaken")
    @Produces({"application/json"})
    PaginatedInterneTaakList internetakenList(
            @QueryParam("aanleidinggevendKlantcontact__url") String aanleidinggevendKlantcontactUrl,
            @QueryParam("aanleidinggevendKlantcontact__uuid") UUID aanleidinggevendKlantcontactUuid,
            @QueryParam("actor__naam") String actorNaam,
            @QueryParam("klantcontact__nummer") String klantcontactNummer,
            @QueryParam("klantcontact__uuid") UUID klantcontactUuid,
            @QueryParam("nummer") String nummer,
            @QueryParam("page") Integer page,
            @QueryParam("status") String status,
            @QueryParam("toegewezenAanActor__url") String toegewezenAanActorUrl,
            @QueryParam("toegewezenAanActor__uuid") UUID toegewezenAanActorUuid,
            @QueryParam("toegewezenOp") OffsetDateTime toegewezenOp
    ) throws ProcessingException;

    @PATCH
    @Path("/internetaken/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    InterneTaak internetakenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedInterneTaak patchedInterneTaak
    ) throws ProcessingException;

    @GET
    @Path("/internetaken/{uuid}")
    @Produces({"application/json"})
    InterneTaak internetakenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/internetaken/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    InterneTaak internetakenUpdate(
            @PathParam("uuid") UUID uuid,
            InterneTaak interneTaak
    ) throws ProcessingException;

    @POST
    @Path("/klantcontacten")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Klantcontact klantcontactenCreate(Klantcontact klantcontact) throws ProcessingException;

    @DELETE
    @Path("/klantcontacten/{uuid}")
    void klantcontactenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/klantcontacten")
    @Produces({"application/json"})
    PaginatedExpandKlantcontactList klantcontactenList(
            @QueryParam("expand") String expand,
            @QueryParam("hadBetrokkene__url") String hadBetrokkeneUrl,
            @QueryParam("hadBetrokkene__uuid") UUID hadBetrokkeneUuid,
            @QueryParam("indicatieContactGelukt") Boolean indicatieContactGelukt,
            @QueryParam("inhoud") String inhoud,
            @QueryParam("kanaal") String kanaal,
            @QueryParam("nummer") String nummer,
            @QueryParam("onderwerp") String onderwerp,
            @QueryParam("onderwerpobject__onderwerpobjectidentificatorCodeObjecttype") String onderwerpobjectOnderwerpobjectidentificatorCodeObjecttype,
            @QueryParam("onderwerpobject__onderwerpobjectidentificatorCodeRegister") String onderwerpobjectOnderwerpobjectidentificatorCodeRegister,
            @QueryParam("onderwerpobject__onderwerpobjectidentificatorCodeSoortObjectId") String onderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId,
            @QueryParam("onderwerpobject__onderwerpobjectidentificatorObjectId") String onderwerpobjectOnderwerpobjectidentificatorObjectId,
            @QueryParam("onderwerpobject__url") String onderwerpobjectUrl,
            @QueryParam("onderwerpobject__uuid") UUID onderwerpobjectUuid,
            @QueryParam("page") Integer page,
            @QueryParam("plaatsgevondenOp") OffsetDateTime plaatsgevondenOp,
            @QueryParam("vertrouwelijk") Boolean vertrouwelijk,
            @QueryParam("wasOnderwerpobject__onderwerpobjectidentificatorCodeObjecttype") String wasOnderwerpobjectOnderwerpobjectidentificatorCodeObjecttype,
            @QueryParam("wasOnderwerpobject__onderwerpobjectidentificatorCodeRegister") String wasOnderwerpobjectOnderwerpobjectidentificatorCodeRegister,
            @QueryParam("wasOnderwerpobject__onderwerpobjectidentificatorCodeSoortObjectId") String wasOnderwerpobjectOnderwerpobjectidentificatorCodeSoortObjectId,
            @QueryParam("wasOnderwerpobject__onderwerpobjectidentificatorObjectId") String wasOnderwerpobjectOnderwerpobjectidentificatorObjectId,
            @QueryParam("wasOnderwerpobject__url") String wasOnderwerpobjectUrl,
            @QueryParam("wasOnderwerpobject__uuid") UUID wasOnderwerpobjectUuid
    ) throws ProcessingException;

    @PATCH
    @Path("/klantcontacten/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Klantcontact klantcontactenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedKlantcontact patchedKlantcontact
    ) throws ProcessingException;

    @GET
    @Path("/klantcontacten/{uuid}")
    @Produces({"application/json"})
    ExpandKlantcontact klantcontactenRetrieve(
            @PathParam("uuid") UUID uuid,
            @QueryParam("expand") String expand
    ) throws ProcessingException;

    @PUT
    @Path("/klantcontacten/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Klantcontact klantcontactenUpdate(
            @PathParam("uuid") UUID uuid,
            Klantcontact klantcontact
    ) throws ProcessingException;

    @POST
    @Path("/onderwerpobjecten")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Onderwerpobject onderwerpobjectenCreate(Onderwerpobject onderwerpobject) throws ProcessingException;

    @DELETE
    @Path("/onderwerpobjecten/{uuid}")
    void onderwerpobjectenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/onderwerpobjecten")
    @Produces({"application/json"})
    PaginatedOnderwerpobjectList onderwerpobjectenList(
            @QueryParam("onderwerpobjectidentificatorCodeObjecttype") String onderwerpobjectidentificatorCodeObjecttype,
            @QueryParam("onderwerpobjectidentificatorCodeRegister") String onderwerpobjectidentificatorCodeRegister,
            @QueryParam("onderwerpobjectidentificatorCodeSoortObjectId") String onderwerpobjectidentificatorCodeSoortObjectId,
            @QueryParam("onderwerpobjectidentificatorObjectId") String onderwerpobjectidentificatorObjectId,
            @QueryParam("page") Integer page
    ) throws ProcessingException;

    @PATCH
    @Path("/onderwerpobjecten/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Onderwerpobject onderwerpobjectenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedOnderwerpobject patchedOnderwerpobject
    ) throws ProcessingException;

    @GET
    @Path("/onderwerpobjecten/{uuid}")
    @Produces({"application/json"})
    Onderwerpobject onderwerpobjectenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/onderwerpobjecten/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Onderwerpobject onderwerpobjectenUpdate(
            @PathParam("uuid") UUID uuid,
            Onderwerpobject onderwerpobject
    ) throws ProcessingException;

    @POST
    @Path("/partijen")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Partij partijenCreate(Partij partij) throws ProcessingException;

    @DELETE
    @Path("/partijen/{uuid}")
    void partijenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/partijen")
    @Produces({"application/json"})
    PaginatedExpandPartijList partijenList(
            @QueryParam("bezoekadresAdresregel1") String bezoekadresAdresregel1,
            @QueryParam("bezoekadresAdresregel2") String bezoekadresAdresregel2,
            @QueryParam("bezoekadresAdresregel3") String bezoekadresAdresregel3,
            @QueryParam("bezoekadresLand") String bezoekadresLand,
            @QueryParam("bezoekadresNummeraanduidingId") String bezoekadresNummeraanduidingId,
            @QueryParam("categorierelatie__categorie__naam") String categorierelatieCategorieNaam,
            @QueryParam("correspondentieadresAdresregel1") String correspondentieadresAdresregel1,
            @QueryParam("correspondentieadresAdresregel2") String correspondentieadresAdresregel2,
            @QueryParam("correspondentieadresAdresregel3") String correspondentieadresAdresregel3,
            @QueryParam("correspondentieadresLand") String correspondentieadresLand,
            @QueryParam("correspondentieadresNummeraanduidingId") String correspondentieadresNummeraanduidingId,
            @QueryParam("expand") String expand,
            @QueryParam("indicatieActief") Boolean indicatieActief,
            @QueryParam("indicatieGeheimhouding") Boolean indicatieGeheimhouding,
            @QueryParam("nummer") String nummer,
            @QueryParam("page") Integer page,
            @QueryParam("partijIdentificator__codeObjecttype") String partijIdentificatorCodeObjecttype,
            @QueryParam("partijIdentificator__codeRegister") String partijIdentificatorCodeRegister,
            @QueryParam("partijIdentificator__codeSoortObjectId") String partijIdentificatorCodeSoortObjectId,
            @QueryParam("partijIdentificator__objectId") String partijIdentificatorObjectId,
            @QueryParam("soortPartij") String soortPartij,
            @QueryParam("vertegenwoordigdePartij__url") String vertegenwoordigdePartijUrl,
            @QueryParam("vertegenwoordigdePartij__uuid") UUID vertegenwoordigdePartijUuid
    ) throws ProcessingException;

    @PATCH
    @Path("/partijen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Partij partijenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedPartij patchedPartij
    ) throws ProcessingException;

    @GET
    @Path("/partijen/{uuid}")
    @Produces({"application/json"})
    ExpandPartij partijenRetrieve(
            @PathParam("uuid") UUID uuid,
            @QueryParam("expand") String expand
    ) throws ProcessingException;

    @PUT
    @Path("/partijen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Partij partijenUpdate(
            @PathParam("uuid") UUID uuid,
            Partij partij
    ) throws ProcessingException;

    @POST
    @Path("/partij-identificatoren")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    PartijIdentificator partijIdentificatorenCreate(PartijIdentificator partijIdentificator) throws ProcessingException;

    @DELETE
    @Path("/partij-identificatoren/{uuid}")
    void partijIdentificatorenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/partij-identificatoren")
    @Produces({"application/json"})
    PaginatedPartijIdentificatorList partijIdentificatorenList(
            @QueryParam("anderePartijIdentificator") String anderePartijIdentificator,
            @QueryParam("page") Integer page,
            @QueryParam("partijIdentificatorCodeObjecttype") String partijIdentificatorCodeObjecttype,
            @QueryParam("partijIdentificatorCodeRegister") String partijIdentificatorCodeRegister,
            @QueryParam("partijIdentificatorCodeSoortObjectId") String partijIdentificatorCodeSoortObjectId,
            @QueryParam("partijIdentificatorObjectId") String partijIdentificatorObjectId
    ) throws ProcessingException;

    @PATCH
    @Path("/partij-identificatoren/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    PartijIdentificator partijIdentificatorenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedPartijIdentificator patchedPartijIdentificator
    ) throws ProcessingException;

    @GET
    @Path("/partij-identificatoren/{uuid}")
    @Produces({"application/json"})
    PartijIdentificator partijIdentificatorenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/partij-identificatoren/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    PartijIdentificator partijIdentificatorenUpdate(
            @PathParam("uuid") UUID uuid,
            PartijIdentificator partijIdentificator
    ) throws ProcessingException;

    @POST
    @Path("/rekeningnummers")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Rekeningnummer rekeningnummersCreate(Rekeningnummer rekeningnummer) throws ProcessingException;

    @DELETE
    @Path("/rekeningnummers/{uuid}")
    void rekeningnummersDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/rekeningnummers")
    @Produces({"application/json"})
    PaginatedRekeningnummerList rekeningnummersList(
            @QueryParam("bic") String bic,
            @QueryParam("iban") String iban,
            @QueryParam("page") Integer page,
            @QueryParam("uuid") UUID uuid
    ) throws ProcessingException;

    @PATCH
    @Path("/rekeningnummers/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Rekeningnummer rekeningnummersPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedRekeningnummer patchedRekeningnummer
    ) throws ProcessingException;

    @GET
    @Path("/rekeningnummers/{uuid}")
    @Produces({"application/json"})
    Rekeningnummer rekeningnummersRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/rekeningnummers/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Rekeningnummer rekeningnummersUpdate(
            @PathParam("uuid") UUID uuid,
            Rekeningnummer rekeningnummer
    ) throws ProcessingException;

    @POST
    @Path("/vertegenwoordigingen")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Vertegenwoordigden vertegenwoordigingenCreate(Vertegenwoordigden vertegenwoordigden) throws ProcessingException;

    @DELETE
    @Path("/vertegenwoordigingen/{uuid}")
    void vertegenwoordigingenDestroy(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @GET
    @Path("/vertegenwoordigingen")
    @Produces({"application/json"})
    PaginatedVertegenwoordigdenList vertegenwoordigingenList(
            @QueryParam("page") Integer page,
            @QueryParam("vertegenwoordigdePartij__url") String vertegenwoordigdePartijUrl,
            @QueryParam("vertegenwoordigdePartij__uuid") UUID vertegenwoordigdePartijUuid,
            @QueryParam("vertegenwoordigendePartij__url") String vertegenwoordigendePartijUrl,
            @QueryParam("vertegenwoordigendePartij__uuid") UUID vertegenwoordigendePartijUuid
    ) throws ProcessingException;

    @PATCH
    @Path("/vertegenwoordigingen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Vertegenwoordigden vertegenwoordigingenPartialUpdate(
            @PathParam("uuid") UUID uuid,
            PatchedVertegenwoordigden patchedVertegenwoordigden
    ) throws ProcessingException;

    @GET
    @Path("/vertegenwoordigingen/{uuid}")
    @Produces({"application/json"})
    Vertegenwoordigden vertegenwoordigingenRetrieve(@PathParam("uuid") UUID uuid) throws ProcessingException;

    @PUT
    @Path("/vertegenwoordigingen/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    Vertegenwoordigden vertegenwoordigingenUpdate(
            @PathParam("uuid") UUID uuid,
            Vertegenwoordigden vertegenwoordigden
    ) throws ProcessingException;
}
