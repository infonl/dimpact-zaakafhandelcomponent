/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 Lifely
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
import { Subscription, forkJoin } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { Api } from "../../shared/utils/generated-types";
import { ZaakStatusmailOptie } from "../../zaken/model/zaak-statusmail-optie";
import { AdminComponent } from "../admin/admin.component";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { FormulierDefinitie } from "../model/formulier-definitie";
import { FormulierVeldDefinitie } from "../model/formulier-veld-definitie";
import { HumanTaskReferentieTabel } from "../model/human-task-referentie-tabel";
import {
  MailtemplateKoppelingMail,
  MailtemplateKoppelingMailUtil,
} from "../model/mailtemplate-koppeling-mail";
import { ReferentieTabel } from "../model/referentie-tabel";
import { ReplyTo } from "../model/replyto";
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

  isSmartDocumentsStepValid: boolean = true;

  parameters: Api<"RestZaakafhandelParameters"> = {
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
  };

  humanTaskParameters: Api<"RESTHumanTaskParameters">[] = [];
  userEventListenerParameters: Api<"RESTUserEventListenerParameter">[] = [];
  zaakbeeindigParameters: Api<"RESTZaakbeeindigParameter">[] = [];
  selection = new SelectionModel<Api<"RESTZaakbeeindigParameter">>(true);
  zaakAfzenders: string[] = [];
  zaakAfzendersDataSource = new MatTableDataSource<Api<"RESTZaakAfzender">>();
  mailtemplateKoppelingen =
    MailtemplateKoppelingMailUtil.getBeschikbareMailtemplateKoppelingen();

  algemeenFormGroup = new FormGroup({
    caseDefinition: new FormControl(),
    domein: new FormControl(),
    defaultGroepId: new FormControl(),
    defaultBehandelaarId: new FormControl(),
    einddatumGeplandWaarschuwing: new FormControl(),
    uiterlijkeEinddatumAfdoeningWaarschuwing: new FormControl(),
    productaanvraagtype: new FormControl(),
  });
  humanTasksFormGroup = new FormGroup({});
  userEventListenersFormGroup = new FormGroup({});
  mailFormGroup = new FormGroup({
    intakeMail: new FormControl(),
    afrondenMail: new FormControl(),
  });

  zaakbeeindigFormGroup = new FormGroup({});
  smartDocumentsEnabledForm = new FormGroup({
    enabledForZaaktype: new FormControl<boolean | undefined>(false),
  });
  betrokkeneKoppelingen = new FormGroup({
    brpKoppelen: new FormControl(false),
    kvkKoppelen: new FormControl(false),
  });

  mailOpties: { label: string; value: string }[] = [];

  caseDefinitions: Api<"RESTCaseDefinition">[] = [];
  domeinen: string[] = [];
  groepen: Api<"RestGroup">[] = [];
  medewerkers: Api<"RestLoggedInUser">[] = [];
  resultaattypes: Api<"RestResultaattype">[] = [];
  referentieTabellen: ReferentieTabel[] = [];
  formulierDefinities: FormulierDefinitie[] = [];
  zaakbeeindigRedenen: Api<"RESTZaakbeeindigReden">[] = [];
  mailtemplates: Api<"RESTMailtemplate">[] = [];
  replyTos: ReplyTo[] = [];
  loading = false;
  subscriptions$: Subscription[] = [];

  constructor(
    public utilService: UtilService,
    public zaakafhandelParametersService: ZaakafhandelParametersService,
    public configuratieService: ConfiguratieService,
    private identityService: IdentityService,
    private route: ActivatedRoute,
    referentieTabelService: ReferentieTabelService,
    mailtemplateBeheerService: MailtemplateBeheerService,
    private formBuilder: FormBuilder,
    private cdr: ChangeDetectorRef,
  ) {
    super(utilService, configuratieService);
    this.route.data.subscribe((data) => {
      this.parameters = data.parameters;
      this.parameters.intakeMail = this.parameters.intakeMail
        ? this.parameters.intakeMail
        : ZaakStatusmailOptie.BESCHIKBAAR_UIT;
      this.parameters.afrondenMail = this.parameters.afrondenMail
        ? this.parameters.afrondenMail
        : ZaakStatusmailOptie.BESCHIKBAAR_UIT;
      this.userEventListenerParameters =
        this.parameters.userEventListenerParameters;
      this.humanTaskParameters = this.parameters.humanTaskParameters;

      forkJoin([
        zaakafhandelParametersService.listCaseDefinitions(),
        zaakafhandelParametersService.listFormulierDefinities(),
        referentieTabelService.listReferentieTabellen(),
        referentieTabelService.listDomeinen(),
        referentieTabelService.listAfzenders(),
        zaakafhandelParametersService.listReplyTos(),
        identityService.listGroups(),
        zaakafhandelParametersService.listZaakbeeindigRedenen(),
        mailtemplateBeheerService.listKoppelbareMailtemplates(),
        zaakafhandelParametersService.listResultaattypes(
          this.parameters.zaaktype.uuid ?? "",
        ),
      ]).subscribe(
        ([
          caseDefinitions,
          formulierDefinities,
          referentieTabellen,
          domeinen,
          afzenders,
          replyTos,
          groepen,
          zaakbeeindigRedenen,
          mailtemplates,
          resultaattypes,
        ]) => {
          this.caseDefinitions = caseDefinitions;
          this.formulierDefinities = formulierDefinities;
          this.referentieTabellen = referentieTabellen;
          this.domeinen = domeinen;
          this.groepen = groepen;
          this.zaakbeeindigRedenen = zaakbeeindigRedenen;
          this.mailtemplates = mailtemplates;
          this.zaakAfzenders = afzenders;
          this.replyTos = replyTos;
          this.resultaattypes = resultaattypes;
          this.createForm();
        },
      );
    });
  }

  ngOnInit(): void {
    this.mailOpties = this.utilService.getEnumAsSelectList(
      "statusmail.optie",
      ZaakStatusmailOptie,
    );
    this.setupMenu("title.parameters.wijzigen");
  }

  ngAfterViewInit(): void {
    this.cdr.detectChanges();
    if (this.smartDocsFormGroup) {
      this.smartDocsFormGroup.saveSmartDocumentsMapping();
    }
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    for (const subscription of this.subscriptions$) {
      subscription.unsubscribe();
    }
  }

  caseDefinitionChanged(event: MatSelectChange): void {
    this.readHumanTaskParameters(event.value);
    this.readUserEventListenerParameters(event.value);
  }

  private readHumanTaskParameters(
    caseDefinition: Api<"RESTCaseDefinition">,
  ): void {
    this.humanTaskParameters = [];
    this.caseDefinitions
      .find(({ key }) => key === caseDefinition?.key)
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

  private readUserEventListenerParameters(
    caseDefinition: Api<"RESTCaseDefinition">,
  ): void {
    this.userEventListenerParameters = [];
    this.caseDefinitions
      .find(({ key }) => key === caseDefinition?.key)
      ?.userEventListenerDefinitions?.forEach(({ id, naam }) => {
        this.userEventListenerParameters.push({ id, naam });
      });
    this.createUserEventListenerForm();
  }

  getHumanTaskControl(
    parameter: Api<"RESTHumanTaskParameters">,
    field: string,
  ): FormControl {
    const formGroup = this.humanTasksFormGroup.get(
      parameter.planItemDefinition?.id ?? "",
    ) as FormGroup;
    return formGroup.get(field) as FormControl;
  }

  getMailtemplateKoppelingControl(
    koppeling: MailtemplateKoppelingMail,
    field: string,
  ) {
    const formGroup = this.mailFormGroup.get(koppeling);
    return formGroup?.get(field);
  }

  createForm() {
    this.algemeenFormGroup = this.formBuilder.group({
      caseDefinition: [this.parameters.caseDefinition, [Validators.required]],
      domein: [this.parameters.domein],
      defaultGroepId: [this.parameters.defaultGroepId, [Validators.required]],
      defaultBehandelaarId: [this.parameters.defaultBehandelaarId],
      einddatumGeplandWaarschuwing: [
        this.parameters.einddatumGeplandWaarschuwing,
      ],
      uiterlijkeEinddatumAfdoeningWaarschuwing: [
        this.parameters.uiterlijkeEinddatumAfdoeningWaarschuwing,
      ],
      productaanvraagtype: [this.parameters.productaanvraagtype],
    });
    this.createHumanTasksForm();
    this.createUserEventListenerForm();
    this.createMailForm();
    this.createZaakbeeindigForm();
    this.createSmartDocumentsEnabledForm();
    this.createBetrokkeneKoppelingenForm();
    this.setMedewerkersForGroup(this.parameters.defaultGroepId);

    this.subscriptions$.push(
      this.algemeenFormGroup.controls.defaultGroepId.valueChanges.subscribe(
        this.setMedewerkersForGroup.bind(this),
      ),
    );

    this.subscriptions$.push(
      this.algemeenFormGroup.controls.einddatumGeplandWaarschuwing.valueChanges.subscribe(
        (value) => {
          this.algemeenFormGroup.controls.einddatumGeplandWaarschuwing.setValue(
            this.sanitizeNumericInput(value),
            { emitEvent: false },
          );
        },
      ),
    );

    this.subscriptions$.push(
      this.algemeenFormGroup.controls.uiterlijkeEinddatumAfdoeningWaarschuwing.valueChanges.subscribe(
        (value) => {
          this.algemeenFormGroup.controls.uiterlijkeEinddatumAfdoeningWaarschuwing.setValue(
            this.sanitizeNumericInput(value),
            { emitEvent: false },
          );
        },
      ),
    );
  }

  private setMedewerkersForGroup(groepId?: string | null) {
    if (!groepId) return;

    return this.identityService
      .listUsersInGroup(groepId)
      .subscribe((medewerkers) => {
        this.medewerkers = medewerkers;
      });
  }

  isHumanTaskParameterValid(
    humanTaskParameter: Api<"RESTHumanTaskParameters">,
  ): boolean {
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
    humanTaskParameters: Api<"RESTHumanTaskParameters">,
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
        doorlooptijdControl.valueChanges.subscribe((value) => {
          doorlooptijdControl.setValue(this.sanitizeNumericInput(value), {
            emitEvent: false,
          });
        }),
      );
    }

    return humanTaskFormGroup;
  }

  private getReferentieTabel(
    humanTaskParameters: Api<"RESTHumanTaskParameters">,
    veld: FormulierVeldDefinitie,
  ) {
    const humanTaskReferentieTabel =
      humanTaskParameters.referentieTabellen?.find((r) => (r.veld = veld.naam));
    return humanTaskReferentieTabel != null
      ? humanTaskReferentieTabel.tabel
      : this.referentieTabellen.find((r) => (r.code = veld.naam));
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

  createZaakbeeindigForm() {
    this.zaakbeeindigFormGroup = this.formBuilder.group({});
    this.addZaakbeeindigParameter(
      this.getZaaknietontvankelijkParameter(this.parameters),
    );
    for (const reden of this.zaakbeeindigRedenen) {
      this.addZaakbeeindigParameter(this.getZaakbeeindigParameter(reden));
    }
  }

  createBetrokkeneKoppelingenForm() {
    console.log(this.parameters);
    this.betrokkeneKoppelingen = this.formBuilder.group({
      kvkKoppelen: [
        this.parameters.betrokkeneKoppelingen?.kvkKoppelen ?? false,
      ],
      brpKoppelen: [
        this.parameters.betrokkeneKoppelingen?.brpKoppelen ?? false,
      ],
    });
  }

  createSmartDocumentsEnabledForm() {
    this.smartDocumentsEnabledForm = this.formBuilder.group({
      enabledForZaaktype: this.parameters.smartDocuments.enabledForZaaktype,
    });
  }

  isZaaknietontvankelijkParameter(parameter: Api<"RESTZaakbeeindigParameter">) {
    return parameter.zaakbeeindigReden === undefined;
  }

  private addZaakbeeindigParameter(
    parameter: Api<"RESTZaakbeeindigParameter">,
  ): void {
    this.zaakbeeindigParameters.push(parameter);
    this.zaakbeeindigFormGroup.addControl(
      parameter.zaakbeeindigReden?.id + "__beeindigResultaat",
      new FormControl(parameter.resultaattype),
    );
    this.updateZaakbeeindigForm(parameter);
  }

  private getZaaknietontvankelijkParameter(
    zaakafhandelParameters: Api<"RestZaakafhandelParameters">,
  ) {
    const parameter: Api<"RESTZaakbeeindigParameter"> = {
      resultaattype:
        zaakafhandelParameters.zaakNietOntvankelijkResultaattype ?? undefined,
    };
    this.selection.select(parameter);
    return parameter;
  }

  private getZaakbeeindigParameter(reden: Api<"RESTZaakbeeindigReden">) {
    let parameter: Api<"RESTZaakbeeindigParameter"> | null = null;
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

  updateZaakbeeindigForm(parameter: Api<"RESTZaakbeeindigParameter">) {
    const control = this.getZaakbeeindigControl(parameter, "beeindigResultaat");
    if (this.selection.isSelected(parameter)) {
      control?.addValidators([Validators.required]);
    } else {
      control?.clearValidators();
    }
    control?.updateValueAndValidity({ emitEvent: false });
  }

  changeSelection(
    $event: MatCheckboxChange,
    parameter: Api<"RESTZaakbeeindigParameter">,
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

  addZaakAfzender(afzender: string): void {
    const zaakAfzender: Api<"RESTZaakAfzender"> & { index: number } = {
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

  updateZaakAfzenders(afzender: string): void {
    for (const zaakAfzender of this.parameters.zaakAfzenders) {
      zaakAfzender.defaultMail = zaakAfzender.mail === afzender;
    }
  }

  removeZaakAfzender(afzender: string): void {
    for (let i = 0; i < this.parameters.zaakAfzenders.length; i++) {
      const zaakAfzender = this.parameters.zaakAfzenders[i];
      if (zaakAfzender.mail === afzender) {
        this.parameters.zaakAfzenders.splice(i, 1);
      }
    }
    this.loadZaakAfzenders();
    this.addAfzender(afzender);
  }

  private addZaakAfzenderControl(zaakAfzender: Api<"RESTZaakAfzender">) {
    // @ts-expect-error TODO: add proper type to `mailFormGroup`
    this.mailFormGroup.addControl(
      "afzender" + (zaakAfzender as { index: number }).index + "__replyTo",
      new FormControl(zaakAfzender.replyTo),
    );
  }

  getZaakAfzenderControl(
    zaakAfzender: Api<"RESTZaakAfzender"> & { index?: number },
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

  getZaakbeeindigControl(
    parameter: Api<"RESTZaakbeeindigParameter">,
    field: string,
  ) {
    return this.zaakbeeindigFormGroup.get(
      `${parameter.zaakbeeindigReden?.id}__${field}`,
    );
  }

  isValid(): boolean {
    return (
      this.algemeenFormGroup.valid &&
      this.humanTasksFormGroup.valid &&
      this.zaakbeeindigFormGroup.valid &&
      this.isSmartDocumentsStepValid
    );
  }

  opslaan(): void {
    this.loading = true;
    Object.assign(this.parameters, this.algemeenFormGroup.value);
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
            bestaandeHumanTaskReferentieTabel != null
              ? bestaandeHumanTaskReferentieTabel
              : new HumanTaskReferentieTabel();
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

    const parameterMailtemplateKoppelingen: Api<"RESTMailtemplateKoppeling">[] =
      [];
    this.mailtemplateKoppelingen.forEach((koppeling) => {
      const mailtemplateKoppeling: Api<"RESTMailtemplateKoppeling"> = {
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

    this.zaakafhandelParametersService
      .updateZaakafhandelparameters(this.parameters)
      .subscribe(
        (data) => {
          this.loading = false;
          this.utilService.openSnackbar(
            "msg.zaakafhandelparameters.opgeslagen",
          );
          this.parameters = data;
          for (const afzender of this.parameters.zaakAfzenders) {
            for (let i = 0; i < index.length; i++) {
              if (index[i] === afzender.mail) {
                (
                  afzender as Api<"RESTZaakAfzender"> & {
                    index: number;
                  }
                ).index = i;
                break;
              }
            }
          }
        },
        () => {
          this.loading = false;
        },
      );

    if (
      this.parameters.smartDocuments.enabledGlobally &&
      this.parameters.smartDocuments.enabledForZaaktype
    ) {
      this.smartDocsFormGroup?.saveSmartDocumentsMapping().subscribe();
    }
  }

  compareObject = (a: unknown, b: unknown) => this.utilService.compare(a, b);

  formulierDefinitieChanged(
    $event: MatSelectChange,
    humanTaskParameter: Api<"RESTHumanTaskParameters">,
  ): void {
    humanTaskParameter.formulierDefinitieId = $event.value;
    this.humanTasksFormGroup.setControl(
      humanTaskParameter.planItemDefinition?.id ?? "",
      this.getHumanTaskFormGroup(humanTaskParameter),
    );
  }

  getVeldDefinities(formulierDefinitieId: string) {
    if (formulierDefinitieId) {
      return this.formulierDefinities.find((f) => f.id === formulierDefinitieId)
        ?.veldDefinities;
    } else {
      return [];
    }
  }

  getBeschikbareMailtemplates(mailtemplate: MailtemplateKoppelingMail) {
    return this.mailtemplates.filter(
      (template) => template.mail === mailtemplate,
    );
  }

  sanitizeNumericInput(value: number) {
    return parseInt(value?.toString(), 10);
  }
}
