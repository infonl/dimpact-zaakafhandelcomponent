/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, OnDestroy, ViewChild } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { MatSidenav } from "@angular/material/sidenav";
import { Router } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { EMPTY, Observable, of, Subject, takeUntil } from "rxjs";
import { catchError } from "rxjs/operators";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { Vertrouwelijkheidaanduiding } from "../../informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { KlantenService } from "../../klanten/klanten.service";
import { NavigationService } from "../../shared/navigation/navigation.service";
import {
  BSN_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "../../shared/utils/constants";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-create",
  templateUrl: "./zaak-create.component.html",
})
export class ZaakCreateComponent implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  static DEFAULT_CHANNEL = "E-formulier";

  @ViewChild(MatSidenav) protected readonly actionsSidenav!: MatSidenav;

  protected activeSideAction: string | null = null;
  public initiatornaam = "dax";

  private readonly inboxProductaanvraag: GeneratedType<"RESTInboxProductaanvraag">;

  protected groups: Observable<GeneratedType<"RestGroup">[]> = of([]);
  protected users: GeneratedType<"RestUser">[] = [];
  protected caseTypes = this.zakenService.listZaaktypes();
  protected communicationChannels: string[] = [];
  protected confidentialityNotices = this.utilService.getEnumAsSelectList(
    "vertrouwelijkheidaanduiding",
    Vertrouwelijkheidaanduiding,
  );

  protected readonly form = this.formBuilder.group({
    zaaktype: this.formBuilder.control<GeneratedType<"RestZaaktype"> | null>(
      null,
      [Validators.required],
    ),
    initiator: this.formBuilder.control<
      GeneratedType<"RestPersoon" | "RestBedrijf"> | null | undefined
    >(null),
    startdatum: this.formBuilder.control(moment(), [Validators.required]),
    bagObjecten: this.formBuilder.control<GeneratedType<"RESTBAGObject">[]>([]),
    groep: this.formBuilder.control<
      GeneratedType<"RestGroup"> | null | undefined
    >(null, [Validators.required]),
    behandelaar: this.formBuilder.control<
      GeneratedType<"RestUser"> | null | undefined
    >(null),
    communicatiekanaal: this.formBuilder.control("", [Validators.required]),
    vertrouwelijkheidaanduiding: this.formBuilder.control<
      (typeof this.confidentialityNotices)[number] | null | undefined
    >(null, [Validators.required]),
    omschrijving: this.formBuilder.control("", [
      Validators.maxLength(80),
      Validators.required,
    ]),
    toelichting: this.formBuilder.control("", [Validators.maxLength(1000)]),
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
    this.form.controls.groep.disable();
    this.form.controls.behandelaar.disable();
    this.form.controls.initiator.disable();

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

    this.form.controls.zaaktype.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((caseType) => {
        this.caseTypeSelected(caseType);

        if (!this.canAddInitiator()) {
          this.form.controls.initiator.setValue(null);
          this.form.controls.initiator.disable();
          return;
        }

        this.form.controls.initiator.enable();
      });
    this.form.controls.groep.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
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
                  ?.defaultBehandelaarId,
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
        } as unknown as GeneratedType<"RESTZaakAanmaakGegevens">["zaak"],
        bagObjecten: bagObjecten,
        inboxProductaanvraag: this.inboxProductaanvraag,
      })
      .pipe(
        catchError(() => {
          this.form.reset();
          return EMPTY;
        }),
      )
      .subscribe((zaak) =>
        this.router.navigate(["/zaken/", zaak?.identificatie]),
      );
  }

  async initiatorSelected(user: GeneratedType<"RestPersoon" | "RestBedrijf">) {
    this.form.controls.initiator.setValue(user);
    await this.actionsSidenav.close();
  }

  caseTypeSelected(caseType?: GeneratedType<"RestZaaktype"> | null): void {
    if (!caseType) return;
    const { zaakafhandelparameters, vertrouwelijkheidaanduiding } = caseType;
    this.form.controls.groep.enable();
    this.groups = this.identityService.listGroups(caseType.uuid);

    this.groups.subscribe((groups) => {
      this.form.controls.groep.setValue(
        groups?.find(({ id }) => id === zaakafhandelparameters?.defaultGroepId),
      );
    });

    this.form.controls.vertrouwelijkheidaanduiding.setValue(
      this.confidentialityNotices.find(
        ({ value }) => value === vertrouwelijkheidaanduiding,
      ),
    );

    if (
      !caseType.zaakafhandelparameters?.betrokkeneKoppelingen?.kvkKoppelen &&
      !caseType.zaakafhandelparameters?.betrokkeneKoppelingen?.brpKoppelen
    ) {
      this.form.controls.initiator.setValue(null);
    }
  }

  protected async openSideNav(action: string) {
    this.activeSideAction = action;
    await this.actionsSidenav.open();
  }

  private handleProductRequest(
    productRequest?: GeneratedType<"RESTInboxProductaanvraag">,
  ) {
    if (!productRequest?.initiatorID) return;

    this.form.controls.toelichting.setValue(
      `Vanuit productaanvraag van type ${productRequest.type}`,
    );

    let observable:
      | Observable<GeneratedType<"RestPersoon"> | GeneratedType<"RestBedrijf">>
      | undefined = undefined;

    const { initiatorID } = productRequest;
    switch (initiatorID.length) {
      case BSN_LENGTH:
        observable = this.klantenService.readPersoon(initiatorID, {
          context: "ZAAK_AANMAKEN",
          action: "find user",
        });
        break;
      case VESTIGINGSNUMMER_LENGTH:
        observable = this.klantenService.readBedrijf(initiatorID, null);
        break;
    }

    observable?.subscribe((result) => {
      this.form.controls.initiator.setValue(result);
    });
  }

  protected bagDisplayValue(bagObjects: GeneratedType<"RESTBAGObject">[]) {
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

  protected canAddInitiator() {
    const betrokkeneKoppelingen =
      this.form.controls.zaaktype.value?.zaakafhandelparameters
        ?.betrokkeneKoppelingen;
    if (!betrokkeneKoppelingen) return false;

    const { brpKoppelen, kvkKoppelen } = betrokkeneKoppelingen;

    return Boolean(brpKoppelen || kvkKoppelen);
  }

  hasInitiator(): boolean {
    return !!this.form.controls.initiator.value;
  }

  clearInitiator() {
    this.form.controls.initiator.setValue(null);
  }

  hasBagObject(): boolean {
    return (
      Array.isArray(this.form.controls.bagObjecten.value) &&
      this.form.controls.bagObjecten.value.length > 0
    );
  }

  clearBagObjecten() {
    this.form.controls.bagObjecten.setValue([]);
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
