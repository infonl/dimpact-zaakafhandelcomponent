/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from "@angular/forms";
import { MatCheckboxChange } from "@angular/material/checkbox";
import { MatSelectChange } from "@angular/material/select";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatTableDataSource } from "@angular/material/table";
import { ActivatedRoute } from "@angular/router";
import { forkJoin, Subject, Subscription, takeUntil } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { getBeschikbareMailtemplateKoppelingen } from "../model/mail-utils";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { SmartDocumentsFormComponent } from "./smart-documents-form/smart-documents-form.component";

@Component({
  templateUrl: "./parameter-edit.component.html",
  styleUrls: ["./parameter-edit.component.less"],
})
export class ParameterEditComponent
  extends AdminComponent
  implements OnInit, OnDestroy, AfterViewInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  @ViewChild("smartDocumentsFormRef")
  smartDocsFormGroup!: SmartDocumentsFormComponent;

  private readonly destroy$ = new Subject<void>();

  isSmartDocumentsStepValid: boolean = true;

  showDoelbindingen: boolean = true;

  parameters: GeneratedType<"RestZaakafhandelParameters"> = {
    humanTaskParameters: [],
    mailtemplateKoppelingen: [],
    zaakbeeindigParameters: [],
    smartDocuments: {
      enabledGlobally: false,
      enabledForZaaktype: false,
    },
    zaakAfzenders: [],
    userEventListenerParameters: [],
    zaaktype: {},
    betrokkeneKoppelingen: {
      brpKoppelen: false,
      kvkKoppelen: false,
    },
    brpDoelbindingen: {
      zoekWaarde: "",
      raadpleegWaarde: "",
      verwerkingsregisterWaarde: "",
    },
    productaanvraagtype: null,
    automaticEmailConfirmation: {
      enabled: false,
      templateName: null,
      emailSender: null,
      emailReply: null,
    },
  };

  humanTaskParameters: GeneratedType<"RESTHumanTaskParameters">[] = [];
  userEventListenerParameters: GeneratedType<"RESTUserEventListenerParameter">[] =
    [];
  zaakbeeindigParameters: GeneratedType<"RESTZaakbeeindigParameter">[] = [];
  selection = new SelectionModel<GeneratedType<"RESTZaakbeeindigParameter">>(
    true,
  );
  zaakAfzenders: string[] = [];
  zaakAfzendersDataSource = new MatTableDataSource<
    GeneratedType<"RestZaakAfzender">
  >();
  mailtemplateKoppelingen = getBeschikbareMailtemplateKoppelingen();

  algemeenFormGroup = this.formBuilder.group({
    caseDefinition:
      this.formBuilder.control<GeneratedType<"RESTCaseDefinition"> | null>(
        null,
        [Validators.required],
      ),
    domein: this.formBuilder.control<string | null>(null),
    defaultGroep: this.formBuilder.control<GeneratedType<"RestGroup"> | null>(
      null,
      [Validators.required],
    ),
    defaultBehandelaar:
      this.formBuilder.control<GeneratedType<"RestUser"> | null>(null),
    einddatumGeplandWaarschuwing: this.formBuilder.control<number | null>(
      null,
      [Validators.min(0), Validators.max(31)],
    ),
    uiterlijkeEinddatumAfdoeningWaarschuwing: this.formBuilder.control<
      number | null
    >(null, [Validators.min(0)]),
    productaanvraagtype: this.formBuilder.control<string | null>(null),
  });

  humanTasksFormGroup = new FormGroup({});
  userEventListenersFormGroup = new FormGroup({});
  mailFormGroup = new FormGroup({
    intakeMail: new FormControl(),
    afrondenMail: new FormControl(),
  });
  brpProtocoleringFormGroup = new FormGroup({
    zoekWaarde: new FormControl(""),
    raadpleegWaarde: new FormControl(""),
    verwerkingsregisterWaarde: new FormControl(""),
  });

  zaakbeeindigFormGroup = new FormGroup({});
  smartDocumentsEnabledForm = new FormGroup({
    enabledForZaaktype: new FormControl<boolean | undefined>(false),
  });
  betrokkeneKoppelingen = new FormGroup({
    brpKoppelen: new FormControl(false),
    kvkKoppelen: new FormControl(false),
  });

  automatischeOntvangstbevestigingFormGroup = this.formBuilder.group({
    enabled: this.formBuilder.control(false),
    templateName:
      this.formBuilder.control<GeneratedType<"RESTMailtemplate"> | null>(null),
    emailSender: this.formBuilder.control<GeneratedType<"RESTReplyTo"> | null>(
      null,
    ),
    emailReply: this.formBuilder.control<GeneratedType<"RESTReplyTo"> | null>(
      null,
    ),
  });

  mailOpties: {
    label: `statusmail.optie.${GeneratedType<"ZaakafhandelparametersStatusMailOption">}`;
    value: GeneratedType<"ZaakafhandelparametersStatusMailOption">;
  }[] = [
    { label: "statusmail.optie.BESCHIKBAAR_AAN", value: "BESCHIKBAAR_AAN" },
    { label: "statusmail.optie.BESCHIKBAAR_UIT", value: "BESCHIKBAAR_UIT" },
    { label: "statusmail.optie.NIET_BESCHIKBAAR", value: "NIET_BESCHIKBAAR" },
  ];

  protected caseDefinitions =
    this.zaakafhandelParametersService.listCaseDefinitions();
  protected domeinen = this.referentieTabelService.listDomeinen();
  protected groepen = this.identityService.listGroups();
  protected medewerkers: GeneratedType<"RestLoggedInUser">[] = [];
  resultaattypes: GeneratedType<"RestResultaattype">[] = [];
  formulierDefinities: GeneratedType<"RESTTaakFormulierDefinitie">[] = [];
  referentieTabellen: GeneratedType<"RestReferenceTable">[] = [];
  zaakbeeindigRedenen: GeneratedType<"RESTZaakbeeindigReden">[] = [];
  mailtemplates: GeneratedType<"RESTMailtemplate">[] = [];
  replyTos: GeneratedType<"RESTReplyTo">[] = [];
  loading = false;
  subscriptions$: Subscription[] = [];
  brpConsultingValues: string[] = [];
  brpSearchValues: string[] = [];
  brpProcessingValues: string[] = [];

  constructor(
    public readonly utilService: UtilService,
    public readonly zaakafhandelParametersService: ZaakafhandelParametersService,
    public readonly configuratieService: ConfiguratieService,
    private readonly identityService: IdentityService,
    private readonly route: ActivatedRoute,
    private readonly referentieTabelService: ReferentieTabelService,
    mailtemplateBeheerService: MailtemplateBeheerService,
    private readonly formBuilder: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
  ) {
    super(utilService, configuratieService);
    this.route.data.subscribe((data) => {
      this.parameters = data.parameters;
      this.parameters.intakeMail = this.parameters.intakeMail
        ? this.parameters.intakeMail
        : "BESCHIKBAAR_UIT";
      this.parameters.afrondenMail = this.parameters.afrondenMail
        ? this.parameters.afrondenMail
        : "BESCHIKBAAR_UIT";
      this.userEventListenerParameters =
        this.parameters.userEventListenerParameters;
      this.humanTaskParameters = this.parameters.humanTaskParameters;

      forkJoin([
        zaakafhandelParametersService.listFormulierDefinities(),
        referentieTabelService.listReferentieTabellen(),
        referentieTabelService.listAfzenders(),
        zaakafhandelParametersService.listReplyTos(),
        zaakafhandelParametersService.listZaakbeeindigRedenen(),
        mailtemplateBeheerService.listKoppelbareMailtemplates(),
        zaakafhandelParametersService.listResultaattypes(
          this.parameters.zaaktype.uuid ?? "",
        ),
        referentieTabelService.listBrpSearchValues(),
        referentieTabelService.listBrpViewValues(),
        referentieTabelService.listBrpProcessingValues(),
      ]).subscribe(
        async ([
          formulierDefinities,
          referentieTabellen,
          afzenders,
          replyTos,
          zaakbeeindigRedenen,
          mailtemplates,
          resultaattypes,
          brpSearchValues,
          brpViewValues,
          brpProcessingValues,
        ]) => {
          this.formulierDefinities = formulierDefinities;
          this.referentieTabellen = referentieTabellen;
          this.zaakbeeindigRedenen = zaakbeeindigRedenen;
          this.mailtemplates = mailtemplates;
          this.zaakAfzenders = afzenders;
          this.replyTos = replyTos;
          this.resultaattypes = resultaattypes;
          this.brpSearchValues = brpSearchValues;
          this.brpConsultingValues = brpViewValues;
          this.brpProcessingValues = brpProcessingValues;
          await this.createForm();
        },
      );
    });
  }

  ngOnInit(): void {
    this.setupMenu("title.parameters.wijzigen");
  }

  ngAfterViewInit(): void {
    this.cdr.detectChanges();
    if (this.smartDocsFormGroup) {
      this.smartDocsFormGroup.saveSmartDocumentsMapping();
    }
  }

  private async readHumanTaskParameters(
    caseDefinition: GeneratedType<"RESTCaseDefinition">,
  ) {
    this.humanTaskParameters = [];
    const caseDefinitions = await this.caseDefinitions.toPromise();
    caseDefinitions
      ?.find(({ key }) => key === caseDefinition?.key)
      ?.humanTaskDefinitions?.forEach((humanTaskDefinition) => {
        this.humanTaskParameters.push({
          planItemDefinition: humanTaskDefinition,
          defaultGroepId: this.parameters.defaultGroepId ?? undefined,
          formulierDefinitieId: humanTaskDefinition.defaultFormulierDefinitie,
          referentieTabellen: [],
          actief: true,
        });
      });
    this.createHumanTasksForm();
  }

  private async readUserEventListenerParameters(
    caseDefinition: GeneratedType<"RESTCaseDefinition">,
  ) {
    this.userEventListenerParameters = [];
    const caseDefinitions = await this.caseDefinitions.toPromise();

    caseDefinitions
      ?.find(({ key }) => key === caseDefinition?.key)
      ?.userEventListenerDefinitions?.forEach(({ id, naam }) => {
        this.userEventListenerParameters.push({ id, naam });
      });
    this.createUserEventListenerForm();
  }

  protected getHumanTaskControl(
    parameter: GeneratedType<"RESTHumanTaskParameters">,
    field: string,
  ): FormControl {
    const formGroup = this.humanTasksFormGroup.get(
      parameter.planItemDefinition?.id ?? "",
    ) as FormGroup;
    return formGroup.get(field) as FormControl;
  }

  getMailtemplateKoppelingControl(
    koppeling: GeneratedType<"Mail">,
    field: string,
  ) {
    const formGroup = this.mailFormGroup.get(koppeling);
    return formGroup?.get(field);
  }

  async createForm() {
    this.algemeenFormGroup.patchValue(this.parameters, { emitEvent: true });

    const { defaultGroepId, defaultBehandelaarId } = this.parameters;

    this.algemeenFormGroup.controls.defaultGroep.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((group) => {
        if (!group) return;

        this.identityService.listUsersInGroup(group.id).subscribe((users) => {
          this.medewerkers = users;
          const pickedUserId =
            this.algemeenFormGroup.controls.defaultBehandelaar.value?.id;
          const defaultUser = users.find(
            ({ id }) => id === (pickedUserId ?? defaultBehandelaarId),
          );
          this.algemeenFormGroup.controls.defaultBehandelaar.setValue(
            defaultUser ?? null,
          );
        });
      });

    if (defaultGroepId) {
      const groups = await this.groepen.toPromise();
      const defaultGroup = groups?.find(({ id }) => id === defaultGroepId);
      this.algemeenFormGroup.controls.defaultGroep.setValue(
        defaultGroup ?? null,
      );
    }

    this.algemeenFormGroup.controls.caseDefinition.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((caseDefinition) => {
        if (!caseDefinition) return;
        this.readHumanTaskParameters(caseDefinition);
        this.readUserEventListenerParameters(caseDefinition);
      });

    this.createHumanTasksForm();
    this.createUserEventListenerForm();
    this.createMailForm();
    this.createZaakbeeindigForm();
    this.createSmartDocumentsEnabledForm();
    this.createBetrokkeneKoppelingenForm();
    this.createBrpDoelbindingForm();
    this.createAutomatischeOntvangstbevestigingForm();
    this.checkProtocollering();
  }

  checkProtocollering() {
    this.configuratieService
      .readProtocolleringProvider()
      .subscribe((provider: string) => {
        const cleanProvider = provider.replace(/['"\n\r\t\s]+/g, "").trim();

        this.showDoelbindingen = cleanProvider === "iConnect";
        console.log(
          "Protocollering provider:",
          cleanProvider,
          " - showDoelbindingen:",
          this.showDoelbindingen,
        );
      });
  }

  protected isHumanTaskParameterValid(
    humanTaskParameter: GeneratedType<"RESTHumanTaskParameters">,
  ) {
    return (
      this.humanTasksFormGroup.get(
        humanTaskParameter.planItemDefinition?.id ?? "",
      )?.status === "VALID"
    );
  }

  private createHumanTasksForm() {
    this.humanTasksFormGroup = this.formBuilder.group({});
    this.humanTaskParameters.forEach((parameter) => {
      this.humanTasksFormGroup.addControl(
        parameter.planItemDefinition?.id ?? "",
        this.getHumanTaskFormGroup(parameter),
      );
    });
  }

  private getHumanTaskFormGroup(
    humanTaskParameters: GeneratedType<"RESTHumanTaskParameters">,
  ): FormGroup {
    const humanTaskFormGroup: FormGroup = this.formBuilder.group({
      formulierDefinitie: [
        humanTaskParameters.formulierDefinitieId,
        [Validators.required],
      ],
      defaultGroep: [humanTaskParameters.defaultGroepId],
      doorlooptijd: [humanTaskParameters.doorlooptijd, [Validators.min(0)]],
      actief: [humanTaskParameters.actief],
    });

    if (humanTaskParameters.formulierDefinitieId) {
      for (const veld of this.getVeldDefinities(
        humanTaskParameters.formulierDefinitieId,
      ) ?? []) {
        humanTaskFormGroup.addControl(
          "referentieTabel" + veld.naam,
          this.formBuilder.control(
            this.getReferentieTabel(humanTaskParameters, veld),
            Validators.required,
          ),
        );
      }
    }

    const doorlooptijdControl = humanTaskFormGroup.get("doorlooptijd");
    if (doorlooptijdControl) {
      this.subscriptions$.push(
        doorlooptijdControl.valueChanges
          .pipe(takeUntil(this.destroy$))
          .subscribe((value) => {
            doorlooptijdControl.setValue(this.sanitizeNumericInput(value), {
              emitEvent: false,
            });
          }),
      );
    }

    return humanTaskFormGroup;
  }

  private getReferentieTabel(
    humanTaskParameters: GeneratedType<"RESTHumanTaskParameters">,
    veldDefinitie: GeneratedType<"RESTTaakFormulierVeldDefinitie">,
  ) {
    const humanTaskReferentieTabel =
      humanTaskParameters.referentieTabellen?.find(
        ({ veld }) => veld === veldDefinitie.naam,
      );
    return (
      humanTaskReferentieTabel?.tabel ??
      this.referentieTabellen.find(({ code }) => code === veldDefinitie.naam)
    );
  }

  private createUserEventListenerForm() {
    this.userEventListenersFormGroup = this.formBuilder.group({});
    this.userEventListenerParameters.forEach((parameter) => {
      const formGroup = this.formBuilder.group({
        toelichting: [parameter.toelichting],
      });
      this.userEventListenersFormGroup.addControl(
        parameter.id ?? "",
        formGroup,
      );
    });
  }

  private createMailForm() {
    this.mailFormGroup = this.formBuilder.group({
      intakeMail: [this.parameters.intakeMail, [Validators.required]],
      afrondenMail: [this.parameters.afrondenMail, [Validators.required]],
    });
    this.mailtemplateKoppelingen.forEach((beschikbareKoppeling) => {
      const mailtemplate = this.parameters.mailtemplateKoppelingen.find(
        (mailtemplateKoppeling) =>
          mailtemplateKoppeling.mailtemplate?.mail === beschikbareKoppeling,
      )?.mailtemplate;
      const formGroup = this.formBuilder.group({
        mailtemplate: mailtemplate?.id,
      });
      // @ts-expect-error TODO: add proper type to formGroup
      this.mailFormGroup.addControl(beschikbareKoppeling, formGroup);
    });
    this.initZaakAfzenders();
  }

  private createZaakbeeindigForm() {
    this.zaakbeeindigFormGroup = this.formBuilder.group({});
    this.addZaakbeeindigParameter(
      this.getZaaknietontvankelijkParameter(this.parameters),
    );
    for (const reden of this.zaakbeeindigRedenen) {
      this.addZaakbeeindigParameter(this.getZaakbeeindigParameter(reden));
    }
  }

  private createBetrokkeneKoppelingenForm() {
    this.betrokkeneKoppelingen = this.formBuilder.group({
      kvkKoppelen: [
        this.parameters.betrokkeneKoppelingen?.kvkKoppelen ?? false,
      ],
      brpKoppelen: [
        this.parameters.betrokkeneKoppelingen?.brpKoppelen ?? false,
      ],
    });

    this.betrokkeneKoppelingen.controls.brpKoppelen.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        this.brpProtocoleringFormGroup.controls.raadpleegWaarde.setValidators(
          value ? [Validators.required] : [],
        );
        this.brpProtocoleringFormGroup.controls.zoekWaarde.setValidators(
          value ? [Validators.required] : [],
        );
        this.brpProtocoleringFormGroup.controls.verwerkingsregisterWaarde.setValidators(
          value ? [Validators.required] : [],
        );

        this.brpProtocoleringFormGroup.updateValueAndValidity({
          emitEvent: false,
        });
        if (value) return;

        this.brpProtocoleringFormGroup.reset();
      });
  }

  private createBrpDoelbindingForm() {
    this.brpProtocoleringFormGroup = this.formBuilder.group({
      raadpleegWaarde: [
        this.parameters.brpDoelbindingen.raadpleegWaarde ?? "",
        this.betrokkeneKoppelingen.controls.brpKoppelen.value
          ? [Validators.required]
          : [],
      ],
      zoekWaarde: [
        this.parameters.brpDoelbindingen.zoekWaarde ?? "",
        this.betrokkeneKoppelingen.controls.brpKoppelen.value
          ? [Validators.required]
          : [],
      ],
      verwerkingsregisterWaarde: [
        this.parameters.brpDoelbindingen.verwerkingsregisterWaarde ?? "",
        this.betrokkeneKoppelingen.controls.brpKoppelen.value
          ? [Validators.required]
          : [],
      ],
    });
  }

  private createAutomatischeOntvangstbevestigingForm() {
    const { automaticEmailConfirmation } = this.parameters;

    this.automatischeOntvangstbevestigingFormGroup.controls.enabled.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((enabled) => {
        const validators = enabled ? [Validators.required] : [];
        this.automatischeOntvangstbevestigingFormGroup.controls.templateName.setValidators(
          validators,
        );
        this.automatischeOntvangstbevestigingFormGroup.controls.emailSender.setValidators(
          validators,
        );
        this.automatischeOntvangstbevestigingFormGroup.updateValueAndValidity();
      });

    this.automatischeOntvangstbevestigingFormGroup.patchValue({
      templateName: this.getBeschikbareMailtemplates(
        "TAAK_ONTVANGSTBEVESTIGING",
      ).find(
        ({ mailTemplateNaam }) =>
          mailTemplateNaam === automaticEmailConfirmation.templateName,
      ),
      emailSender: this.replyTos.find(
        ({ mail }) => mail === automaticEmailConfirmation.emailSender,
      ),
      emailReply: this.replyTos.find(
        ({ mail }) => mail === automaticEmailConfirmation.emailReply,
      ),
      enabled: automaticEmailConfirmation.enabled ?? false,
    });
  }

  private createSmartDocumentsEnabledForm() {
    this.smartDocumentsEnabledForm = this.formBuilder.group({
      enabledForZaaktype: this.parameters.smartDocuments.enabledForZaaktype,
    });
  }

  protected isZaaknietontvankelijkParameter(
    parameter: GeneratedType<"RESTZaakbeeindigParameter">,
  ) {
    return parameter.zaakbeeindigReden === undefined;
  }

  private addZaakbeeindigParameter(
    parameter: GeneratedType<"RESTZaakbeeindigParameter">,
  ): void {
    this.zaakbeeindigParameters.push(parameter);
    this.zaakbeeindigFormGroup.addControl(
      parameter.zaakbeeindigReden?.id + "__beeindigResultaat",
      new FormControl(parameter.resultaattype),
    );
    this.updateZaakbeeindigForm(parameter);
  }

  private getZaaknietontvankelijkParameter(
    zaakafhandelParameters: GeneratedType<"RestZaakafhandelParameters">,
  ) {
    const parameter: GeneratedType<"RESTZaakbeeindigParameter"> = {
      resultaattype: zaakafhandelParameters.zaakNietOntvankelijkResultaattype,
    };
    this.selection.select(parameter);
    return parameter;
  }

  private getZaakbeeindigParameter(
    reden: GeneratedType<"RESTZaakbeeindigReden">,
  ) {
    let parameter: GeneratedType<"RESTZaakbeeindigParameter"> | null = null;
    for (const item of this.parameters.zaakbeeindigParameters) {
      if (this.compareObject(item.zaakbeeindigReden, reden)) {
        parameter = item;
        this.selection.select(parameter);
        break;
      }
    }
    if (parameter === null) {
      parameter = { zaakbeeindigReden: reden };
    }

    return parameter;
  }

  private updateZaakbeeindigForm(
    parameter: GeneratedType<"RESTZaakbeeindigParameter">,
  ) {
    const control = this.getZaakbeeindigControl(parameter, "beeindigResultaat");
    if (this.selection.isSelected(parameter)) {
      control?.addValidators([Validators.required]);
    } else {
      control?.clearValidators();
    }
    control?.updateValueAndValidity({ emitEvent: false });
  }

  protected changeSelection(
    $event: MatCheckboxChange,
    parameter: GeneratedType<"RESTZaakbeeindigParameter">,
  ): void {
    if ($event) {
      this.selection.toggle(parameter);
      this.updateZaakbeeindigForm(parameter);
    }
  }

  private initZaakAfzenders() {
    let i = 0;
    for (const zaakAfzender of this.parameters.zaakAfzenders) {
      (zaakAfzender as { index: number }).index = i++;
      this.addZaakAfzenderControl(zaakAfzender);
    }
    this.loadZaakAfzenders();
    this.initAfzenders();
  }

  private loadZaakAfzenders() {
    this.zaakAfzendersDataSource.data = this.parameters.zaakAfzenders
      .slice()
      .sort((a, b) => {
        return a.speciaal !== b.speciaal
          ? a.speciaal
            ? -1
            : 1
          : (a.mail?.localeCompare(b.mail ?? "") ?? 0);
      });
  }

  protected addZaakAfzender(afzender: string): void {
    const zaakAfzender: GeneratedType<"RestZaakAfzender"> & { index: number } =
      {
        speciaal: false,
        defaultMail: false,
        mail: afzender,
        replyTo: undefined,
        index: 0,
      };
    for (const bestaand of this.parameters.zaakAfzenders) {
      if (zaakAfzender.index <= (bestaand as { index: number }).index) {
        zaakAfzender.index = (bestaand as { index: number }).index + 1;
      }
    }
    this.addZaakAfzenderControl(zaakAfzender);
    this.parameters.zaakAfzenders.push(zaakAfzender);
    this.loadZaakAfzenders();
    this.removeAfzender(afzender);
  }

  protected updateZaakAfzenders(afzender: string): void {
    for (const zaakAfzender of this.parameters.zaakAfzenders) {
      zaakAfzender.defaultMail = zaakAfzender.mail === afzender;
    }
  }

  protected removeZaakAfzender(afzender: string): void {
    for (let i = 0; i < this.parameters.zaakAfzenders.length; i++) {
      const zaakAfzender = this.parameters.zaakAfzenders[i];
      if (zaakAfzender.mail === afzender) {
        this.parameters.zaakAfzenders.splice(i, 1);
      }
    }
    this.loadZaakAfzenders();
    this.addAfzender(afzender);
  }

  private addZaakAfzenderControl(
    zaakAfzender: GeneratedType<"RestZaakAfzender">,
  ) {
    // @ts-expect-error TODO: add proper type to `mailFormGroup`
    this.mailFormGroup.addControl(
      "afzender" + (zaakAfzender as { index: number }).index + "__replyTo",
      new FormControl(zaakAfzender.replyTo),
    );
  }

  protected getZaakAfzenderControl(
    zaakAfzender: GeneratedType<"RestZaakAfzender"> & { index?: number },
    field: string,
  ) {
    return this.mailFormGroup.get(`afzender${zaakAfzender.index}__${field}`);
  }

  private initAfzenders() {
    for (const zaakAfzender of this.parameters.zaakAfzenders) {
      if (zaakAfzender.mail) {
        this.removeAfzender(zaakAfzender.mail);
      }
    }
    this.loadAfzenders();
  }

  private loadAfzenders(): void {
    this.zaakAfzenders.sort((a, b) => a.localeCompare(b));
  }

  private addAfzender(afzender: string): void {
    this.zaakAfzenders.push(afzender);
    this.loadAfzenders();
  }

  private removeAfzender(afzender: string): void {
    for (let i = 0; i < this.zaakAfzenders.length; i++) {
      if (this.zaakAfzenders[i] === afzender) {
        this.zaakAfzenders.splice(i, 1);
      }
    }
  }

  protected getZaakbeeindigControl(
    parameter: GeneratedType<"RESTZaakbeeindigParameter">,
    field: string,
  ) {
    return this.zaakbeeindigFormGroup.get(
      `${parameter.zaakbeeindigReden?.id}__${field}`,
    );
  }

  protected isValid(): boolean {
    return (
      this.algemeenFormGroup.valid &&
      this.humanTasksFormGroup.valid &&
      this.zaakbeeindigFormGroup.valid &&
      this.automatischeOntvangstbevestigingFormGroup.valid &&
      this.betrokkeneKoppelingen.valid &&
      this.brpProtocoleringFormGroup.valid &&
      this.isSmartDocumentsStepValid
    );
  }

  protected opslaan() {
    this.loading = true;
    this.parameters = {
      ...this.parameters,
      ...this.algemeenFormGroup.value,
      defaultGroepId: this.algemeenFormGroup.value.defaultGroep?.id,
      defaultBehandelaarId: this.algemeenFormGroup.value.defaultBehandelaar?.id,
    };

    this.humanTaskParameters.forEach((param) => {
      param.formulierDefinitieId = this.getHumanTaskControl(
        param,
        "formulierDefinitie",
      ).value;
      param.defaultGroepId = this.getHumanTaskControl(
        param,
        "defaultGroep",
      ).value;
      param.actief = this.getHumanTaskControl(param, "actief").value;
      param.doorlooptijd = this.getHumanTaskControl(
        param,
        "doorlooptijd",
      ).value;
      const bestaandeHumanTaskParameter =
        this.parameters.humanTaskParameters.find(
          ({ planItemDefinition }) =>
            planItemDefinition?.id === param.planItemDefinition?.id,
        );
      const bestaandeReferentietabellen = bestaandeHumanTaskParameter
        ? bestaandeHumanTaskParameter.referentieTabellen
        : [];
      param.referentieTabellen = [];
      this.getVeldDefinities(param.formulierDefinitieId ?? "")?.forEach(
        (value) => {
          const bestaandeHumanTaskReferentieTabel =
            bestaandeReferentietabellen?.find((o) => o.veld === value.naam);
          const tabel =
            bestaandeHumanTaskReferentieTabel ??
            ({} satisfies GeneratedType<"RestHumanTaskReferenceTable">);
          tabel.veld = value.naam;
          tabel.tabel = this.getHumanTaskControl(
            param,
            "referentieTabel" + tabel.veld,
          ).value;
          param.referentieTabellen?.push(tabel);
        },
      );
    });
    this.parameters.humanTaskParameters = this.humanTaskParameters;
    this.userEventListenerParameters.forEach((param) => {
      param.toelichting = this.userEventListenersFormGroup
        ?.get(param.id ?? "")
        ?.get("toelichting")?.value;
    });
    this.parameters.userEventListenerParameters =
      this.userEventListenerParameters;

    this.parameters.intakeMail = this.mailFormGroup.get("intakeMail")?.value;
    this.parameters.afrondenMail =
      this.mailFormGroup.get("afrondenMail")?.value;

    const parameterMailtemplateKoppelingen: GeneratedType<"RESTMailtemplateKoppeling">[] =
      [];
    this.mailtemplateKoppelingen.forEach((koppeling) => {
      const mailtemplateKoppeling: GeneratedType<"RESTMailtemplateKoppeling"> =
        {
          mailtemplate: this.mailtemplates.find(
            (mailtemplate) =>
              mailtemplate.id ===
              this.mailFormGroup.get(koppeling)?.get("mailtemplate")?.value,
          ),
        };

      if (mailtemplateKoppeling.mailtemplate) {
        parameterMailtemplateKoppelingen.push(mailtemplateKoppeling);
      }
    });
    this.parameters.mailtemplateKoppelingen = parameterMailtemplateKoppelingen;

    this.parameters.zaakbeeindigParameters = [];
    this.selection.selected.forEach((param) => {
      if (this.isZaaknietontvankelijkParameter(param)) {
        this.parameters.zaakNietOntvankelijkResultaattype =
          this.getZaakbeeindigControl(param, "beeindigResultaat")?.value;
      } else {
        param.resultaattype = this.getZaakbeeindigControl(
          param,
          "beeindigResultaat",
        )?.value;
        this.parameters.zaakbeeindigParameters.push(param);
      }
    });

    const index: string[] = [];
    for (const afzender of this.parameters.zaakAfzenders) {
      if (afzender.mail) {
        index[(afzender as { index: number }).index] = afzender.mail;
      }
      afzender.replyTo = this.getZaakAfzenderControl(
        afzender,
        "replyTo",
      )?.value;
    }

    this.parameters.smartDocuments.enabledForZaaktype = Boolean(
      this.smartDocumentsEnabledForm.value.enabledForZaaktype,
    );

    this.parameters.betrokkeneKoppelingen = {
      kvkKoppelen: Boolean(
        this.betrokkeneKoppelingen.controls.kvkKoppelen.value,
      ),
      brpKoppelen: Boolean(
        this.betrokkeneKoppelingen.controls.brpKoppelen.value,
      ),
    };

    this.parameters.brpDoelbindingen = this.brpProtocoleringFormGroup.value;

    const { templateName, emailSender, emailReply, enabled } =
      this.automatischeOntvangstbevestigingFormGroup.value;
    this.parameters.automaticEmailConfirmation = {
      templateName: templateName?.mailTemplateNaam,
      emailReply: emailReply?.mail,
      emailSender: emailSender?.mail,
      enabled: Boolean(enabled),
    };

    this.zaakafhandelParametersService
      .updateZaakafhandelparameters(this.parameters)
      .subscribe({
        next: (data) => {
          this.loading = false;
          this.utilService.openSnackbar(
            "msg.zaakafhandelparameters.opgeslagen",
          );
          this.parameters = data;
          for (const afzender of this.parameters.zaakAfzenders) {
            for (let i = 0; i < index.length; i++) {
              if (index[i] === afzender.mail) {
                (
                  afzender as GeneratedType<"RestZaakAfzender"> & {
                    index: number;
                  }
                ).index = i;
                break;
              }
            }
          }
        },
        error: () => {
          this.loading = false;
        },
      });

    if (
      this.parameters.smartDocuments.enabledGlobally &&
      this.parameters.smartDocuments.enabledForZaaktype
    ) {
      this.smartDocsFormGroup?.saveSmartDocumentsMapping().subscribe();
    }
  }

  protected compareObject = (a: unknown, b: unknown) =>
    this.utilService.compare(a, b);

  protected formulierDefinitieChanged(
    $event: MatSelectChange,
    humanTaskParameter: GeneratedType<"RESTHumanTaskParameters">,
  ): void {
    humanTaskParameter.formulierDefinitieId = $event.value;
    this.humanTasksFormGroup.setControl(
      humanTaskParameter.planItemDefinition?.id ?? "",
      this.getHumanTaskFormGroup(humanTaskParameter),
    );
  }

  protected getVeldDefinities(formulierDefinitieId: string) {
    if (formulierDefinitieId) {
      return this.formulierDefinities.find((f) => f.id === formulierDefinitieId)
        ?.veldDefinities;
    } else {
      return [];
    }
  }

  getBeschikbareMailtemplates(mailtemplate: GeneratedType<"Mail">) {
    return this.mailtemplates.filter(
      (template) => template.mail === mailtemplate,
    );
  }

  private sanitizeNumericInput(value: number) {
    return parseInt(value?.toString(), 10);
  }

  protected replyToDisplayValue(replyTo: GeneratedType<"RESTReplyTo">) {
    return replyTo.speciaal
      ? "gegevens.mail.afzender." + replyTo.mail
      : replyTo.mail;
  }

  protected filterOutMedewerkerMail(
    replyTos: GeneratedType<"RESTReplyTo">[],
  ): GeneratedType<"RESTReplyTo">[] {
    return replyTos.filter(
      (replyTo: GeneratedType<"RESTReplyTo">) =>
        !(replyTo.speciaal && replyTo.mail === "MEDEWERKER"),
    );
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
    for (const subscription of this.subscriptions$) {
      subscription.unsubscribe();
    }
  }
}
