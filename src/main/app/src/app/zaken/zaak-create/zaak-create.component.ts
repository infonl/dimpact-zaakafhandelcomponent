/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, ViewChild } from "@angular/core";
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from "@angular/forms";
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
import { Zaak } from "../model/zaak";
import { ZaakAanmaakGegevens } from "../model/zaak-aanmaak-gegevens";
import { Zaaktype } from "../model/zaaktype";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-create",
  templateUrl: "./zaak-create.component.html",
})
export class ZaakCreateComponent {
  static KANAAL_E_FORMULIER = "E-formulier";

  @ViewChild('mat-sidenav') protected readonly actionsSidenav!: MatSidenav;

  protected activeSideAction: string | null = null;

  private readonly inboxProductaanvraag: InboxProductaanvraag;

  private initiator: Klant | null = null;
  protected bagObjects: BAGObject[] = [];
  protected groups: GeneratedType<"RestGroup">[] = [];
  protected users: GeneratedType<"RestUser">[] = [];
  protected zaaktypes: Zaaktype[] = [];
  protected communicationChannels: string[] = [];
  protected confidentialityNotices = this.utilService.getEnumAsSelectList(
    "vertrouwelijkheidaanduiding",
    Vertrouwelijkheidaanduiding,
  );

  private readonly bagObjectenFormControl = new FormControl<string | null>(
    null,
  );
  private readonly initiatorFormControl = new FormControl<
    string | null | undefined
  >(null);
  private readonly commentFormControl = new FormControl<string | null>(null);
  private readonly userFormControl = new FormControl<
    GeneratedType<"RestUser"> | null | undefined
  >(null);
  private readonly confidentialityNoticeFormControl = new FormControl<
    (typeof this.confidentialityNotices)[number] | null | undefined
  >(null, [Validators.required]);
  private readonly groupFormControl = new FormControl<
    GeneratedType<"RestGroup"> | null | undefined
  >(null, [Validators.required]);

  protected form: FormGroup<{
    zaaktype: FormControl<Zaaktype | null>;
    startdatum: FormControl<moment.Moment | null>;
    group: typeof ZaakCreateComponent.prototype.groupFormControl;
    initiator: typeof ZaakCreateComponent.prototype.initiatorFormControl;
    communicatiekanaal: FormControl<string | null>;
    vertrouwelijkheidaanduiding: typeof ZaakCreateComponent.prototype.confidentialityNoticeFormControl;
    description: FormControl<string | null>;
    bagObjecten: typeof ZaakCreateComponent.prototype.bagObjectenFormControl;
    toelichting: typeof ZaakCreateComponent.prototype.commentFormControl;
    user: typeof ZaakCreateComponent.prototype.userFormControl;
  }>;

  constructor(
    private readonly zakenService: ZakenService,
    private readonly router: Router,
    private readonly navigation: NavigationService,
    private readonly klantenService: KlantenService,
    referentieTabelService: ReferentieTabelService,
    private readonly translateService: TranslateService,
    private readonly utilService: UtilService,
    formBuilder: FormBuilder,
    identityService: IdentityService,
  ) {
    this.inboxProductaanvraag =
      router.getCurrentNavigation()?.extras?.state?.inboxProductaanvraag;

    zakenService.listZaaktypes().subscribe((zaaktypes) => {
      this.zaaktypes = zaaktypes;
    });
    identityService.listGroups().subscribe((groups) => {
      this.groups = groups ?? [];
    });
    referentieTabelService
      .listCommunicatiekanalen(this.inboxProductaanvraag != null)
      .subscribe((channels) => {
        // TODO if the list of communicatiekanalen includes E-formulier, it should be set as default
        this.communicationChannels = channels;
      });

    this.groupFormControl.valueChanges.subscribe((value) => {
      if (!value) {
        this.userFormControl.setValue(null);
        this.userFormControl.disable();
        return;
      }
      identityService.listUsersInGroup(value.id).subscribe((users) => {
        console.log({ users });
        this.users = users ?? [];
        this.userFormControl.enable();
      });
    });

    const caseTypeFormControl = new FormControl<Zaaktype | null>(null, [
      Validators.required,
    ]);
    caseTypeFormControl.valueChanges.subscribe(
      this.zaaktypeGeselecteerd.bind(this),
    );

    this.userFormControl.disable();

    this.bagObjectenFormControl.valueChanges.subscribe((value) => {
      if (value) return;
      this.bagObjects = [];
    });

    this.initiatorFormControl.valueChanges.subscribe((value) => {
      if (value) return;
      this.initiator = null;
    });

    this.form = formBuilder.group({
      zaaktype: caseTypeFormControl,
      startdatum: new FormControl<moment.Moment | null>(null, [
        Validators.required,
      ]),
      group: this.groupFormControl,
      user: this.userFormControl,
      initiator: this.initiatorFormControl,
      communicatiekanaal: new FormControl<string | null>(null, [
        Validators.required,
      ]),
      vertrouwelijkheidaanduiding: this.confidentialityNoticeFormControl,
      description: new FormControl<string | null>(null, [
        Validators.required,
        Validators.maxLength(80),
      ]),
      bagObjecten: this.bagObjectenFormControl,
      toelichting: this.commentFormControl,
    });

    utilService.setTitle("title.zaak.aanmaken");

    this.verwerkInboxProductaanvraagGegevens(this.inboxProductaanvraag);
  }

  formSubmit(form: FormGroup): void {
    const zaak = new Zaak();
    Object.keys(form.controls).forEach((key) => {
      switch (key) {
        case "vertrouwelijkheidaanduiding":
          zaak[key] = form.controls[key].value?.value;
          break;
        case "initiatorIdentificatie":
          if (this.initiator != null) {
            zaak["initiatorIdentificatieType"] =
              this.initiator.identificatieType;
            zaak[key] = this.initiator.identificatie;
          }
          break;
        case "bagObjecten":
          // skip
          break;
        case "toekenning":
          if (this.userFormControl.value) {
            zaak.behandelaar = this.userFormControl.value;
          }
          if (this.groupFormControl.value) {
            zaak.groep = this.groupFormControl.value;
          }
          break;
        default:
          zaak[key] = form.controls[key].value;
          break;
      }
    });
    this.zakenService
      .createZaak(
        new ZaakAanmaakGegevens(
          zaak,
          this.inboxProductaanvraag,
          this.bagObjects,
        ),
      )
      .pipe(
        catchError(() => {
          this.form.reset();
          return of();
        }),
      )
      .subscribe(({ identificatie }) =>
        this.router.navigate(["/zaken/", identificatie]),
      );
  }

  async initiatorSelected(user: Klant) {
    this.initiator = user;
    this.initiatorFormControl.setValue(user?.naam);
    await this.actionsSidenav.close();
  }

  zaaktypeGeselecteerd(caseType?: Zaaktype | null): void {
    if (!caseType) return;

    const {
      zaakafhandelparameters: { defaultGroepId, defaultBehandelaarId },
      vertrouwelijkheidaanduiding,
    } = caseType;

    this.groupFormControl.setValue(
      this.groups.find(({ id }) => id === defaultGroepId),
    );
    this.userFormControl.setValue(
      this.users.find(({ id }) => id === defaultBehandelaarId),
    );

    this.confidentialityNoticeFormControl.setValue(
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

  private verwerkInboxProductaanvraagGegevens(
    productAanvraag?: InboxProductaanvraag,
  ) {
    const bsnLength = 9;
    const vestigingsnummerLength = 12;

    this.commentFormControl.setValue(
      "Vanuit productaanvraag van type " + productAanvraag?.type,
    );

    if (!productAanvraag?.initiatorID) return;

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
      this.initiatorFormControl.setValue(result.naam);
    });
  }

  bagGeselecteerd(): void {
    const value = this.bagObjects
      .map(({ omschrijving }) => omschrijving)
      .join(" | ");

    if (value.length <= 100) {
      this.bagObjectenFormControl.setValue(value);
      return;
    }

    this.translateService
      .get("msg.aantal.bagObjecten.geselecteerd", {
        aantal: this.bagObjects.length,
      })
      .subscribe(this.bagObjectenFormControl.setValue.bind(this));
  }
}
