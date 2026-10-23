/*
 * SPDX-FileCopyrightText: 2021 - 2024 Dimpact, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatSidenav, MatSidenavModule } from "@angular/material/sidenav";
import { Router } from "@angular/router";
import { TranslatePipe, TranslateService } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import moment from "moment";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import { GeneratedType } from "src/app/shared/utils/generated-types";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { BagZoekComponent } from "../../bag/bag-zoek/bag-zoek.component";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { KlantKoppelComponent } from "../../klanten/koppel/klanten/klant-koppel/klant-koppel.component";
import { ZacAutoComplete } from "../../shared/form/auto-complete/auto-complete";
import { ZacDate } from "../../shared/form/date/date";
import { ZacInput } from "../../shared/form/input/input";
import { ZacSelect } from "../../shared/form/select/select";
import { ZacTextarea } from "../../shared/form/textarea/textarea";
import { injectMutation } from "../../shared/http/inject-mutation";
import { BackButtonDirective } from "../../shared/navigation/back-button.directive";
import {
  BSN_LENGTH,
  VESTIGINGSNUMMER_LENGTH,
} from "../../shared/utils/constants";
import { BetrokkeneIdentificatie } from "../model/betrokkeneIdentificatie";
import { ZakenService } from "../zaken.service";

@Component({
  selector: "zac-zaak-create",
  templateUrl: "./zaak-create.component.html",
  styleUrls: ["./zaak-create.component.less"],
  standalone: true,
  imports: [
    BackButtonDirective,
    ReactiveFormsModule,
    MatSidenavModule,
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    TranslatePipe,
    KlantKoppelComponent,
    BagZoekComponent,
    ZacAutoComplete,
    ZacDate,
    ZacInput,
    ZacSelect,
    ZacTextarea,
  ],
})
export class ZaakCreateComponent {
  private readonly zakenService = inject(ZakenService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly identityService = inject(IdentityService);
  private readonly translateService = inject(TranslateService);
  private readonly router = inject(Router);
  private readonly klantenService = inject(KlantenService);
  private readonly referentieTabelService = inject(ReferentieTabelService);

  private readonly queryClient = inject(QueryClient);
  static DEFAULT_CHANNEL = "E-formulier";

  protected readonly actionsSidenav = viewChild.required(MatSidenav);

  protected activeSideAction: string | null = null;

  private readonly inboxProductaanvraag: GeneratedType<"RestInboxProductaanvraag">;

  private readonly selectedCaseType = signal<
    GeneratedType<"RestZaaktype"> | undefined
  >(undefined);

  private readonly groupsQuery = injectQuery(() => ({
    ...this.identityService.listBehandelaarGroupsForZaaktype(
      this.selectedCaseType()?.omschrijving ?? "",
    ),
    enabled: !!this.selectedCaseType()?.omschrijving,
  }));

  protected readonly groups = computed(() => this.groupsQuery.data() ?? []);

  private readonly selectedGroupId = signal<string | undefined>(undefined);

  private readonly usersQuery = injectQuery(() => ({
    ...this.identityService.listUsersInGroup(this.selectedGroupId() ?? ""),
    enabled: !!this.selectedGroupId(),
  }));

  protected readonly users = computed(() => this.usersQuery.data() ?? []);

  private groupPrefilledForCaseType?: GeneratedType<"RestZaaktype">;
  private behandelaarPrefilledForGroupId?: string;
  protected caseTypes = this.zakenService.listZaaktypesForCreation();
  protected communicationChannels: string[] = [];
  protected confidentialityNotices =
    VertrouwelijkaanduidingToTranslationKeyPipe.selectList;

  protected createZaakMutation = injectMutation(
    () => this.zakenService.createZaak(),
    {
      onSuccess: ({ identificatie }) => {
        void this.router.navigate(["/zaken/", identificatie]);
      },
    },
  );

  protected readonly form = this.formBuilder.group({
    zaaktype: this.formBuilder.control<GeneratedType<"RestZaaktype"> | null>(
      null,
      [Validators.required],
    ),
    initiatorIdentificatie: this.formBuilder.control<
      GeneratedType<"BetrokkeneIdentificatie"> | null | undefined
    >(null),
    startdatum: this.formBuilder.control(moment(), [Validators.required]),
    bagObjecten: this.formBuilder.control<GeneratedType<"RestBagObject">[]>([]),
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

  constructor() {
    this.inboxProductaanvraag =
      this.router.getCurrentNavigation()?.extras?.state?.inboxProductaanvraag;
    this.form.controls.groep.disable();
    this.form.controls.behandelaar.disable();
    this.form.controls.initiatorIdentificatie.disable();

    this.referentieTabelService
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
      .pipe(takeUntilDestroyed())
      .subscribe((caseType) => {
        this.caseTypeSelected(caseType);

        if (!this.canAddInitiator()) {
          this.form.controls.initiatorIdentificatie.setValue(null);
          this.form.controls.initiatorIdentificatie.disable();
          return;
        }

        this.form.controls.initiatorIdentificatie.enable();
      });

    this.form.controls.groep.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((value) => {
        if (!value) {
          this.form.controls.behandelaar.setValue(null);
          this.form.controls.behandelaar.disable();
          this.selectedGroupId.set(undefined);
          this.behandelaarPrefilledForGroupId = undefined;
          return;
        }
        if (!value.id) return;

        this.selectedGroupId.set(value.id);
      });

    effect(() => {
      const caseType = this.selectedCaseType();
      const groups = this.groupsQuery.data();
      if (!caseType || !groups || this.groupPrefilledForCaseType === caseType)
        return;
      this.groupPrefilledForCaseType = caseType;

      const defaultGroup = groups.find(
        ({ id }) => id === caseType.zaakafhandelparameters?.defaultGroepId,
      );
      untracked(() => this.form.controls.groep.setValue(defaultGroup));
    });

    effect(() => {
      const groupId = this.selectedGroupId();
      const users = this.usersQuery.data();
      if (!groupId || !users) return;

      untracked(() => {
        this.form.controls.behandelaar.enable();
        if (this.behandelaarPrefilledForGroupId === groupId) return;
        this.behandelaarPrefilledForGroupId = groupId;

        const defaultBehandelaarId =
          this.form.controls.zaaktype.value?.zaakafhandelparameters
            ?.defaultBehandelaarId;
        this.form.controls.behandelaar.setValue(
          users.find(({ id }) => id === defaultBehandelaarId) ?? null,
        );
      });
    });

    void this.handleProductRequest(this.inboxProductaanvraag);
  }

  formSubmit() {
    const value = this.form.getRawValue();

    this.createZaakMutation.mutate({
      zaak: {
        ...value,
        initiatorIdentificatie: value.initiatorIdentificatie
          ? new BetrokkeneIdentificatie(value.initiatorIdentificatie)
          : null,
        vertrouwelijkheidaanduiding: value.vertrouwelijkheidaanduiding?.value,
        startdatum: value.startdatum?.toISOString(),
        omschrijving: value.omschrijving!,
        zaaktype: value.zaaktype!,
      },
      bagObjecten: value.bagObjecten,
      inboxProductaanvraag: this.inboxProductaanvraag,
    });
  }

  async initiatorSelected(user: GeneratedType<"RestPersoon" | "RestBedrijf">) {
    this.form.controls.initiatorIdentificatie.setValue({
      ...user,
      type: user.identificatieType!,
    });
    await this.actionsSidenav().close();
  }

  caseTypeSelected(caseType?: GeneratedType<"RestZaaktype"> | null) {
    if (!caseType) return;

    const { vertrouwelijkheidaanduiding } = caseType;
    this.form.controls.groep.enable();
    if (!caseType.omschrijving) return;

    this.selectedCaseType.set(caseType);

    this.form.controls.vertrouwelijkheidaanduiding.setValue(
      this.confidentialityNotices.find(
        ({ value }) => value === vertrouwelijkheidaanduiding,
      ),
    );

    if (
      !caseType.zaakafhandelparameters?.betrokkeneKoppelingen
        ?.isKvkKoppelenEnabled &&
      !caseType.zaakafhandelparameters?.betrokkeneKoppelingen
        ?.isBrpKoppelenEnabled
    ) {
      this.form.controls.initiatorIdentificatie.setValue(null);
    }
  }

  protected async openSideNav(action: string) {
    this.activeSideAction = action;
    await this.actionsSidenav().open();
  }

  private async handleProductRequest(
    productRequest?: GeneratedType<"RestInboxProductaanvraag">,
  ) {
    if (!productRequest?.initiatorID) return;

    this.form.controls.toelichting.setValue(
      `Vanuit productaanvraag van type ${productRequest.type}`,
    );

    const { initiatorID } = productRequest;
    switch (initiatorID.length) {
      case BSN_LENGTH: {
        const result = await this.queryClient.ensureQueryData(
          this.klantenService.readPersoon(initiatorID),
        );
        this.form.controls.initiatorIdentificatie.setValue({
          ...result,
          type: result.identificatieType!,
        });
        break;
      }
      case VESTIGINGSNUMMER_LENGTH: {
        const result = await this.queryClient.ensureQueryData(
          this.klantenService.readBedrijf(
            new BetrokkeneIdentificatie({
              vestigingsnummer: initiatorID,
              identificatieType: "VN",
            }),
          ),
        );
        this.form.controls.initiatorIdentificatie.setValue({
          ...result,
          type: result.identificatieType!,
        });
        break;
      }
    }
  }

  protected bagDisplayValue(bagObjects: GeneratedType<"RestBagObject">[]) {
    const value = bagObjects
      .map(({ omschrijving }) => omschrijving)
      .join(" | ");

    if (value.length <= 100) return value;

    return this.translateService.instant(
      "msg.aantal.bag-objecten.geselecteerd",
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

    const { isBrpKoppelenEnabled, isKvkKoppelenEnabled } =
      betrokkeneKoppelingen;

    return Boolean(isBrpKoppelenEnabled || isKvkKoppelenEnabled);
  }

  hasInitiator(): boolean {
    return !!this.form.controls.initiatorIdentificatie.value;
  }

  clearInitiator() {
    this.form.controls.initiatorIdentificatie.setValue(null);
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
}
