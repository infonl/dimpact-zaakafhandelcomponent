/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, ViewChild } from "@angular/core";
import { FormBuilder, FormControl, Validators } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { Observable, of } from "rxjs";
import { catchError } from "rxjs/operators";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { BAGObject } from "../../bag/model/bagobject";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { Vertrouwelijkheidaanduiding } from "../../informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { KlantenService } from "../../klanten/klanten.service";
import { Bedrijf } from "../../klanten/model/bedrijven/bedrijf";
import { Klant } from "../../klanten/model/klanten/klant";
import { InboxProductaanvraag } from "../../productaanvragen/model/inbox-productaanvraag";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { Zaaktype } from "../model/zaaktype";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-create",
  templateUrl: "./zaak-create.component.html",
})
export class ZaakCreateComponent {
  static KANAAL_E_FORMULIER = "E-formulier";

  @ViewChild("actionsSidenav") protected readonly actionsSidenav!: MatSidenav;

  protected activeSideAction: string | null = null;

  private readonly inboxProductaanvraag: InboxProductaanvraag;

  private initiator: Klant | null = null;
  protected bagObjects: BAGObject[] = [];
  protected groups: GeneratedType<"RestGroup">[] = [];
  protected users: GeneratedType<"RestUser">[] = [];
  protected caseTypes: Zaaktype[] = [];
  protected communicationChannels: string[] = [];
  protected confidentialityNotices = this.utilService.getEnumAsSelectList(
    "vertrouwelijkheidaanduiding",
    Vertrouwelijkheidaanduiding,
  );

  protected readonly form = this.formBuilder.group({
    zaaktype: new FormControl<Zaaktype | null>(null, [Validators.required]),
    startdatum: new FormControl<moment.Moment | null>(null, [
      Validators.required,
    ]),
    groep: new FormControl<GeneratedType<"RestGroup"> | null | undefined>(
      null,
      [Validators.required],
    ),
    behandelaar: new FormControl<GeneratedType<"RestUser"> | null | undefined>(
      null,
    ),
    initiator: new FormControl<string | null | undefined>(null),
    communicatiekanaal: new FormControl<string | null>(null, [
      Validators.required,
    ]),
    vertrouwelijkheidaanduiding: new FormControl<
      (typeof this.confidentialityNotices)[number] | null | undefined
    >(null, [Validators.required]),
    omschrijving: new FormControl<string | null>(null, [
      Validators.required,
      Validators.maxLength(80),
    ]),
    bagObjecten: new FormControl<string | null>(null),
    toelichting: new FormControl<string | null>(null),
  });

  constructor(
    private readonly zakenService: ZakenService,
    private readonly router: Router,
    private readonly klantenService: KlantenService,
    referentieTabelService: ReferentieTabelService,
    private readonly translateService: TranslateService,
    private readonly utilService: UtilService,
    private readonly formBuilder: FormBuilder,
    identityService: IdentityService,
    protected readonly navigationService: NavigationService,
  ) {
    this.inboxProductaanvraag =
      router.getCurrentNavigation()?.extras?.state?.inboxProductaanvraag;

    zakenService.listZaaktypes().subscribe((caseTypes) => {
      this.caseTypes = caseTypes;
    });
    identityService.listGroups().subscribe((groups) => {
      this.groups = groups ?? [];
    });
    referentieTabelService
      .listCommunicatiekanalen(Boolean(this.inboxProductaanvraag))
      .subscribe((communicationChannels) => {
        this.communicationChannels = communicationChannels;

        if (
          communicationChannels.includes(ZaakCreateComponent.KANAAL_E_FORMULIER)
        ) {
          this.form.controls.communicatiekanaal.setValue(
            ZaakCreateComponent.KANAAL_E_FORMULIER,
          );
        }
      });

    this.form.controls.zaaktype.valueChanges.subscribe((caseType) =>
      this.caseTypeSelected(caseType),
    );

    this.form.controls.groep.valueChanges.subscribe((value) => {
      if (!value) {
        this.form.controls.behandelaar.setValue(null);
        this.form.controls.behandelaar.disable();
        return;
      }
      identityService.listUsersInGroup(value.id).subscribe((users) => {
        this.users = users ?? [];
        this.form.controls.behandelaar.enable();
        this.form.controls.behandelaar.setValue(
          this.users.find(
            ({ id }) =>
              id ===
              this.form.controls.zaaktype.value?.zaakafhandelparameters
                .defaultBehandelaarId,
          ),
        );
      });
    });

    this.form.controls.behandelaar.disable();

    this.form.controls.bagObjecten.valueChanges.subscribe((value) => {
      if (value) return;
      this.bagObjects = [];
    });

    this.form.controls.initiator.valueChanges.subscribe((value) => {
      if (value) return;
      this.initiator = null;
    });

    utilService.setTitle("title.zaak.aanmaken");

    this.handleProductRequest(this.inboxProductaanvraag);
  }

  formSubmit(form: typeof this.form): void {
    const { bagObjecten, initiator, vertrouwelijkheidaanduiding, ...zaak } =
      form.value;

    this.zakenService
      .createZaak({
        zaak: {
          ...zaak,
          initiatorIdentificatie: this.initiator?.identificatie,
          initiatorIdentificatieType: this.initiator?.identificatieType,
          vertrouwelijkheidaanduiding: vertrouwelijkheidaanduiding?.value,
        } as any as GeneratedType<"RESTZaakAanmaakGegevens">["zaak"],
        bagObjecten: this.bagObjects,
        inboxProductaanvraag: this.inboxProductaanvraag,
      })
      .pipe(
        catchError(() => {
          this.form.reset();
          return of();
        }),
      )
      .subscribe((zaak) =>
        this.router.navigate(["/zaken/", zaak?.identificatie]),
      );
  }

  async initiatorSelected(user: Klant) {
    this.initiator = user;
    this.form.controls.initiator.setValue(user?.naam);
    await this.actionsSidenav.close();
  }

  caseTypeSelected(caseType?: Zaaktype | null): void {
    if (!caseType) return;

    const {
      zaakafhandelparameters: { defaultGroepId, defaultBehandelaarId },
      vertrouwelijkheidaanduiding,
    } = caseType;

    this.form.controls.groep.setValue(
      this.groups.find(({ id }) => id === defaultGroepId),
    );
    this.form.controls.behandelaar.setValue(
      this.users.find(({ id }) => id === defaultBehandelaarId),
    );

    this.form.controls.vertrouwelijkheidaanduiding.setValue(
      this.confidentialityNotices.find(
        ({ value }) => value === vertrouwelijkheidaanduiding,
      ),
    );
  }

  protected openSideNav(action: string) {
    return async () => {
      this.activeSideAction = action;
      await this.actionsSidenav.open();
    };
  }

  private handleProductRequest(productAanvraag?: InboxProductaanvraag) {
    const bsnLength = 9;
    const vestigingsnummerLength = 12;

    if (!productAanvraag?.initiatorID) return;

    this.form.controls.toelichting.setValue(
      "Vanuit productaanvraag van type " + productAanvraag?.type,
    );

    let observable:
      | Observable<GeneratedType<"RestPersoon"> | Bedrijf>
      | undefined = undefined;

    switch (productAanvraag.initiatorID.length) {
      case bsnLength:
        observable = this.klantenService.readPersoon(
          productAanvraag.initiatorID,
        );
        break;
      case vestigingsnummerLength:
        observable = this.klantenService.readVestiging(
          productAanvraag.initiatorID,
        );
        break;
    }

    observable?.subscribe((result) => {
      this.initiator = result as Klant;
      this.form.controls.initiator.setValue(result.naam);
    });
  }

  bagGeselecteerd(): void {
    const value = this.bagObjects
      .map(({ omschrijving }) => omschrijving)
      .join(" | ");

    if (value.length <= 100) {
      this.form.controls.bagObjecten.setValue(value);
      return;
    }

    this.translateService
      .get("msg.aantal.bagObjecten.geselecteerd", {
        aantal: this.bagObjects.length,
      })
      .subscribe((translation) => {
        this.form.controls.bagObjecten.setValue(translation);
      });
  }
}
