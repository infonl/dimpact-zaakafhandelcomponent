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
import {
  BSN_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "../../shared/utils/constants";
import { Zaaktype } from "../model/zaaktype";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-create",
  templateUrl: "./zaak-create.component.html",
})
export class ZaakCreateComponent {
  static DEFAULT_CHANNEL = "E-formulier";

  @ViewChild(MatSidenav) protected readonly actionsSidenav!: MatSidenav;

  protected activeSideAction: string | null = null;

  private readonly inboxProductaanvraag: InboxProductaanvraag;

  protected groups = this.identityService.listGroups();
  protected users: GeneratedType<"RestUser">[] = [];
  protected caseTypes = this.zakenService.listZaaktypes();
  protected communicationChannels: string[] = [];
  protected confidentialityNotices = this.utilService.getEnumAsSelectList(
    "vertrouwelijkheidaanduiding",
    Vertrouwelijkheidaanduiding,
  );

  protected readonly form = this.formBuilder.group({
    zaaktype: new FormControl<Zaaktype | null>(null, [Validators.required]),
    initiator: new FormControl<Klant | null | undefined>(null),
    startdatum: new FormControl(moment(), [Validators.required]),
    bagObjecten: new FormControl<BAGObject[]>([]),
    groep: new FormControl<GeneratedType<"RestGroup"> | null | undefined>(
      null,
      [Validators.required],
    ),
    behandelaar: new FormControl<GeneratedType<"RestUser"> | null | undefined>(
      null,
    ),
    communicatiekanaal: new FormControl("", [Validators.required]),
    vertrouwelijkheidaanduiding: new FormControl<
      (typeof this.confidentialityNotices)[number] | null | undefined
    >(null, [Validators.required]),
    omschrijving: new FormControl("", [
      Validators.maxLength(80),
      Validators.required,
    ]),
    toelichting: new FormControl("", [Validators.maxLength(1000)]),
  });

  constructor(
    private readonly zakenService: ZakenService,
    private readonly router: Router,
    private readonly klantenService: KlantenService,
    referentieTabelService: ReferentieTabelService,
    private readonly translateService: TranslateService,
    private readonly utilService: UtilService,
    private readonly formBuilder: FormBuilder,
    private identityService: IdentityService,
    protected readonly navigationService: NavigationService,
  ) {
    utilService.setTitle("title.zaak.aanmaken");
    this.inboxProductaanvraag =
      router.getCurrentNavigation()?.extras?.state?.inboxProductaanvraag;
    this.form.controls.behandelaar.disable();

    referentieTabelService
      .listCommunicatiekanalen(Boolean(this.inboxProductaanvraag))
      .subscribe((channels) => {
        this.communicationChannels = channels;

        if (channels.includes(ZaakCreateComponent.DEFAULT_CHANNEL)) {
          this.form.controls.communicatiekanaal.setValue(
            ZaakCreateComponent.DEFAULT_CHANNEL,
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

    this.handleProductRequest(this.inboxProductaanvraag);
  }

  formSubmit(form: typeof this.form): void {
    const { bagObjecten, initiator, vertrouwelijkheidaanduiding, ...zaak } =
      form.value;

    this.zakenService
      .createZaak({
        zaak: {
          ...zaak,
          initiatorIdentificatie: initiator?.identificatie,
          initiatorIdentificatieType: initiator?.identificatieType,
          vertrouwelijkheidaanduiding: vertrouwelijkheidaanduiding?.value,
        } as any as GeneratedType<"RESTZaakAanmaakGegevens">["zaak"],
        bagObjecten: bagObjecten,
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
    this.form.controls.initiator.setValue(user);
    await this.actionsSidenav.close();
  }

  caseTypeSelected(caseType?: Zaaktype | null): void {
    if (!caseType) return;

    const {
      zaakafhandelparameters: { defaultGroepId },
      vertrouwelijkheidaanduiding,
    } = caseType;

    this.groups.subscribe((groups) => {
      this.form.controls.groep.setValue(
        groups?.find(({ id }) => id === defaultGroepId),
      );
    });

    this.form.controls.vertrouwelijkheidaanduiding.setValue(
      this.confidentialityNotices.find(
        ({ value }) => value === vertrouwelijkheidaanduiding,
      ),
    );
  }

  protected async openSideNav(action: string) {
    this.activeSideAction = action;
    await this.actionsSidenav.open();
  }

  private handleProductRequest(productRequest?: InboxProductaanvraag) {
    if (!productRequest?.initiatorID) return;

    this.form.controls.toelichting.setValue(
      `Vanuit productaanvraag van type ${productRequest.type}`,
    );

    let observable:
      | Observable<GeneratedType<"RestPersoon"> | Bedrijf>
      | undefined = undefined;

    const { initiatorID } = productRequest;
    switch (initiatorID.length) {
      case BSN_LENGTH:
        observable = this.klantenService.readPersoon(initiatorID);
        break;
      case VESTIGINGSNUMMER_LENGTH:
        observable = this.klantenService.readVestiging(initiatorID);
        break;
    }

    observable?.subscribe((result) => {
      this.form.controls.initiator.setValue(result as Klant);
    });
  }

  protected bagDisplayValue(bagObjects: BAGObject[]) {
    const value = bagObjects
      .map(({ omschrijving }) => omschrijving)
      .join(" | ");

    if (value.length <= 100) return value;

    return this.translateService.instant(
      "msg.aantal.bagObjecten.geselecteerd",
      {
        aantal: bagObjects.length,
      },
    );
  }

  // This is required for the `zac-bag-zoek` to work as expected
  protected bagObjectSelected() {}
}
