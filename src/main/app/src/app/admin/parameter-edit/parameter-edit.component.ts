/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SelectionModel } from "@angular/cdk/collections";
import {
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
import { switchMap, tap } from "rxjs/operators";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakStatusmailOptie } from "../../zaken/model/zaak-statusmail-optie";
import { AdminComponent } from "../admin/admin.component";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { CaseDefinition } from "../model/case-definition";
import { FormulierDefinitie } from "../model/formulier-definitie";
import { FormulierVeldDefinitie } from "../model/formulier-veld-definitie";
import { HumanTaskParameter } from "../model/human-task-parameter";
import { HumanTaskReferentieTabel } from "../model/human-task-referentie-tabel";
import { Mailtemplate } from "../model/mailtemplate";
import { MailtemplateKoppeling } from "../model/mailtemplate-koppeling";
import {
  MailtemplateKoppelingMail,
  MailtemplateKoppelingMailUtil,
} from "../model/mailtemplate-koppeling-mail";
import { ReferentieTabel } from "../model/referentie-tabel";
import { ReplyTo } from "../model/replyto";
import { UserEventListenerParameter } from "../model/user-event-listener-parameter";
import { ZaakafhandelParameters } from "../model/zaakafhandel-parameters";
import { ZaakAfzender } from "../model/zaakafzender";
import { ZaakbeeindigParameter } from "../model/zaakbeeindig-parameter";
import { ZaakbeeindigReden } from "../model/zaakbeeindig-reden";
import { ZaaknietontvankelijkParameter } from "../model/zaaknietontvankelijk-parameter";
import { ZaaknietontvankelijkReden } from "../model/zaaknietontvankelijk-reden";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { SmartDocumentsFormComponent } from "./smart-documents-form/smart-documents-form.component";

@Component({
  templateUrl: "./parameter-edit.component.html",
  styleUrls: ["./parameter-edit.component.less"],
})
export class ParameterEditComponent
  extends AdminComponent
  implements OnInit, OnDestroy
{
  @ViewChild("sideNavContainer") sideNavContainer: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav: MatSidenav;

  @ViewChild("smartDocumentsFormRef")
  smartDocsFormGroup!: SmartDocumentsFormComponent;

  isSmartDocumentsStepValid: boolean = true;

  parameters: ZaakafhandelParameters;
  humanTaskParameters: HumanTaskParameter[] = [];
  userEventListenerParameters: UserEventListenerParameter[] = [];
  zaakbeeindigParameters: ZaakbeeindigParameter[] = [];
  selection = new SelectionModel<ZaakbeeindigParameter>(true);
  zaakAfzenders: string[] = [];
  zaakAfzendersDataSource = new MatTableDataSource<ZaakAfzender>();
  mailtemplateKoppelingen: MailtemplateKoppelingMail[] =
    MailtemplateKoppelingMailUtil.getBeschikbareMailtemplateKoppelingen();

  algemeenFormGroup: FormGroup;
  humanTasksFormGroup: FormGroup;
  userEventListenersFormGroup: FormGroup;
  mailFormGroup: FormGroup;
  zaakbeeindigFormGroup: FormGroup;
  smartDocumentsEnabledForm: FormGroup;

  mailOpties: { label: string; value: string }[];

  caseDefinitions: CaseDefinition[];
  domeinen: string[];
  groepen: GeneratedType<"RestGroup">[];
  medewerkers: GeneratedType<"RestLoggedInUser">[];
  resultaattypes: GeneratedType<"RestResultaattype">[];
  referentieTabellen: ReferentieTabel[];
  formulierDefinities: FormulierDefinitie[];
  zaakbeeindigRedenen: ZaakbeeindigReden[];
  mailtemplates: Mailtemplate[];
  replyTos: ReplyTo[];
  loading: boolean;
  defaultGroepSubscription$: Subscription;

  constructor(
    public utilService: UtilService,
    public adminService: ZaakafhandelParametersService,
    public configuratieService: ConfiguratieService,
    private identityService: IdentityService,
    private route: ActivatedRoute,
    private referentieTabelService: ReferentieTabelService,
    private mailtemplateBeheerService: MailtemplateBeheerService,
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
      adminService
        .listResultaattypes(this.parameters.zaaktype.uuid)
        .subscribe((resultaattypes) => (this.resultaattypes = resultaattypes));
      forkJoin([
        adminService.listCaseDefinitions(),
        adminService.listFormulierDefinities(),
        referentieTabelService.listReferentieTabellen(),
        referentieTabelService.listDomeinen(),
        referentieTabelService.listAfzenders(),
        adminService.listReplyTos(),
        identityService.listGroups(),
        identityService.listUsers(),
        adminService.listZaakbeeindigRedenen(),
        mailtemplateBeheerService.listKoppelbareMailtemplates(),
      ]).subscribe(
        ([
          caseDefinitions,
          formulierDefinities,
          referentieTabellen,
          domeinen,
          afzenders,
          replyTos,
          groepen,
          medewerkers,
          zaakbeeindigRedenen,
          mailtemplates,
        ]) => {
          this.caseDefinitions = caseDefinitions;
          this.formulierDefinities = formulierDefinities;
          this.referentieTabellen = referentieTabellen;
          this.domeinen = domeinen;
          this.groepen = groepen;
          this.medewerkers = medewerkers;
          this.zaakbeeindigRedenen = zaakbeeindigRedenen;
          this.mailtemplates = mailtemplates;
          this.zaakAfzenders = afzenders;
          this.replyTos = replyTos;
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
    this.defaultGroepSubscription$.unsubscribe();
  }

  caseDefinitionChanged(event: MatSelectChange): void {
    this.readHumanTaskParameters(event.value);
    this.readUserEventListenerParameters(event.value);
  }

  private readHumanTaskParameters(caseDefinition: CaseDefinition): void {
    this.humanTaskParameters = [];
    this.caseDefinitions
      .find((cd) => cd.key === caseDefinition.key)
      .humanTaskDefinitions.forEach((humanTaskDefinition) => {
        const humanTaskParameter: HumanTaskParameter = new HumanTaskParameter();
        humanTaskParameter.planItemDefinition = humanTaskDefinition;
        humanTaskParameter.defaultGroepId = this.parameters.defaultGroepId;
        humanTaskParameter.formulierDefinitieId =
          humanTaskDefinition.defaultFormulierDefinitie;
        humanTaskParameter.referentieTabellen = [];
        humanTaskParameter.actief = true;
        this.humanTaskParameters.push(humanTaskParameter);
      });
    this.createHumanTasksForm();
  }

  private readUserEventListenerParameters(
    caseDefinition: CaseDefinition,
  ): void {
    this.userEventListenerParameters = [];
    this.caseDefinitions
      .find((cd) => cd.key === caseDefinition.key)
      .userEventListenerDefinitions.forEach((userEventListenerDefinition) => {
        const userEventListenerParameter: UserEventListenerParameter =
          new UserEventListenerParameter();
        userEventListenerParameter.id = userEventListenerDefinition.id;
        userEventListenerParameter.naam = userEventListenerDefinition.naam;
        this.userEventListenerParameters.push(userEventListenerParameter);
      });
    this.createUserEventListenerForm();
  }

  getHumanTaskControl(
    parameter: HumanTaskParameter,
    field: string,
  ): FormControl {
    const formGroup = this.humanTasksFormGroup.get(
      parameter.planItemDefinition.id,
    ) as FormGroup;
    return formGroup.get(field) as FormControl;
  }

  getMailtemplateKoppelingControl(
    koppeling: MailtemplateKoppelingMail,
    field: string,
  ): FormControl {
    const formGroup = this.mailFormGroup.get(koppeling) as FormGroup;
    return formGroup.get(field) as FormControl;
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
    this.defaultGroepSubscription$ =
      this.algemeenFormGroup.controls.defaultGroepId.valueChanges
        .pipe(
          switchMap((groepId) =>
            this.identityService
              .listUsersInGroup(groepId)
              .pipe(tap((medewerkers) => (this.medewerkers = medewerkers))),
          ),
        )
        .subscribe();
    this.createHumanTasksForm();
    this.createUserEventListenerForm();
    this.createMailForm();
    this.createZaakbeeindigForm();
    this.createSmartDocumentsEnabledForm();
  }

  isHumanTaskParameterValid(humanTaskParameter: HumanTaskParameter): boolean {
    return (
      this.humanTasksFormGroup.get(humanTaskParameter.planItemDefinition.id)
        .status === "VALID"
    );
  }

  private createHumanTasksForm() {
    this.humanTasksFormGroup = this.formBuilder.group({});
    this.humanTaskParameters.forEach((parameter) => {
      this.humanTasksFormGroup.addControl(
        parameter.planItemDefinition.id,
        this.getHumanTaskFormGroup(parameter),
      );
    });
  }

  private getHumanTaskFormGroup(
    humanTaskParameters: HumanTaskParameter,
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
      )) {
        humanTaskFormGroup.addControl(
          "referentieTabel" + veld.naam,
          this.formBuilder.control(
            this.getReferentieTabel(humanTaskParameters, veld),
            Validators.required,
          ),
        );
      }
    }
    return humanTaskFormGroup;
  }

  private getReferentieTabel(
    humanTaskParameters: HumanTaskParameter,
    veld: FormulierVeldDefinitie,
  ): ReferentieTabel {
    const humanTaskReferentieTabel: HumanTaskReferentieTabel =
      humanTaskParameters.referentieTabellen.find((r) => (r.veld = veld.naam));
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
      this.userEventListenersFormGroup.addControl(parameter.id, formGroup);
    });
  }

  private createMailForm() {
    this.mailFormGroup = this.formBuilder.group({
      intakeMail: [this.parameters.intakeMail, [Validators.required]],
      afrondenMail: [this.parameters.afrondenMail, [Validators.required]],
    });
    this.mailtemplateKoppelingen.forEach((beschikbareKoppeling) => {
      const mailtemplate: Mailtemplate =
        this.parameters.mailtemplateKoppelingen.find(
          (mailtemplateKoppeling) =>
            mailtemplateKoppeling.mailtemplate.mail === beschikbareKoppeling,
        )?.mailtemplate;
      const formGroup = this.formBuilder.group({
        mailtemplate: mailtemplate?.id,
      });
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

  createSmartDocumentsEnabledForm() {
    this.smartDocumentsEnabledForm = this.formBuilder.group({
      enabledForZaaktype: this.parameters.smartDocuments.enabledForZaaktype,
    });
  }

  isZaaknietontvankelijkParameter(parameter): boolean {
    return ZaaknietontvankelijkReden.is(parameter.zaakbeeindigReden);
  }

  private addZaakbeeindigParameter(parameter: ZaakbeeindigParameter): void {
    this.zaakbeeindigParameters.push(parameter);
    this.zaakbeeindigFormGroup.addControl(
      parameter.zaakbeeindigReden.id + "__beeindigResultaat",
      new FormControl(parameter.resultaattype),
    );
    this.updateZaakbeeindigForm(parameter);
  }

  private getZaaknietontvankelijkParameter(
    zaakafhandelParameters: ZaakafhandelParameters,
  ): ZaaknietontvankelijkParameter {
    const parameter: ZaaknietontvankelijkParameter =
      new ZaaknietontvankelijkParameter();
    parameter.resultaattype =
      zaakafhandelParameters.zaakNietOntvankelijkResultaattype;
    this.selection.select(parameter);
    return parameter;
  }

  private getZaakbeeindigParameter(
    reden: ZaakbeeindigReden,
  ): ZaakbeeindigParameter {
    let parameter: ZaakbeeindigParameter = null;
    for (const item of this.parameters.zaakbeeindigParameters) {
      if (this.compareObject(item.zaakbeeindigReden, reden)) {
        parameter = item;
        this.selection.select(parameter);
        break;
      }
    }
    if (parameter === null) {
      parameter = new ZaakbeeindigParameter();
      parameter.zaakbeeindigReden = reden;
    }
    return parameter;
  }

  updateZaakbeeindigForm(parameter: ZaakbeeindigParameter) {
    const control: FormControl = this.getZaakbeeindigControl(
      parameter,
      "beeindigResultaat",
    );
    if (this.selection.isSelected(parameter)) {
      control.addValidators([Validators.required]);
    } else {
      control.clearValidators();
    }
    control.updateValueAndValidity({ emitEvent: false });
  }

  changeSelection(
    $event: MatCheckboxChange,
    parameter: ZaakbeeindigParameter,
  ): void {
    if ($event) {
      this.selection.toggle(parameter);
      this.updateZaakbeeindigForm(parameter);
    }
  }

  private initZaakAfzenders() {
    let i = 0;
    for (const zaakAfzender of this.parameters.zaakAfzenders) {
      zaakAfzender.index = i++;
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
          : a.mail.localeCompare(b.mail);
      });
  }

  addZaakAfzender(afzender: string): void {
    const zaakAfzender: ZaakAfzender = new ZaakAfzender();
    zaakAfzender.speciaal = false;
    zaakAfzender.defaultMail = false;
    zaakAfzender.mail = afzender;
    zaakAfzender.replyTo = null;
    zaakAfzender.index = 0;
    for (const bestaand of this.parameters.zaakAfzenders) {
      if (zaakAfzender.index <= bestaand.index) {
        zaakAfzender.index = bestaand.index + 1;
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

  private addZaakAfzenderControl(zaakAfzender: ZaakAfzender) {
    this.mailFormGroup.addControl(
      "afzender" + zaakAfzender.index + "__replyTo",
      new FormControl(zaakAfzender.replyTo),
    );
  }

  getZaakAfzenderControl(
    zaakAfzender: ZaakAfzender,
    field: string,
  ): FormControl {
    return this.mailFormGroup.get(
      `afzender${zaakAfzender.index}__${field}`,
    ) as FormControl;
  }

  private initAfzenders() {
    for (const zaakAfzender of this.parameters.zaakAfzenders) {
      this.removeAfzender(zaakAfzender.mail);
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
    parameter: ZaakbeeindigParameter,
    field: string,
  ): FormControl {
    return this.zaakbeeindigFormGroup.get(
      `${parameter.zaakbeeindigReden.id}__${field}`,
    ) as FormControl;
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
      const bestaandeHumanTaskParameter: HumanTaskParameter =
        this.parameters.humanTaskParameters.find(
          (htp) => htp.planItemDefinition.id === param.planItemDefinition.id,
        );
      const bestaandeReferentietabellen = bestaandeHumanTaskParameter
        ? bestaandeHumanTaskParameter.referentieTabellen
        : [];
      param.referentieTabellen = [];
      this.getVeldDefinities(param.formulierDefinitieId).forEach((value) => {
        const bestaandeHumanTaskReferentieTabel: HumanTaskReferentieTabel =
          bestaandeReferentietabellen.find((o) => o.veld === value.naam);
        const tabel =
          bestaandeHumanTaskReferentieTabel != null
            ? bestaandeHumanTaskReferentieTabel
            : new HumanTaskReferentieTabel();
        tabel.veld = value.naam;
        tabel.tabel = this.getHumanTaskControl(
          param,
          "referentieTabel" + tabel.veld,
        ).value;
        param.referentieTabellen.push(tabel);
      });
    });
    this.parameters.humanTaskParameters = this.humanTaskParameters;
    this.userEventListenerParameters.forEach((param) => {
      param.toelichting = this.userEventListenersFormGroup
        .get(param.id)
        .get("toelichting").value;
    });
    this.parameters.userEventListenerParameters =
      this.userEventListenerParameters;

    this.parameters.intakeMail = this.mailFormGroup.get("intakeMail").value;
    this.parameters.afrondenMail = this.mailFormGroup.get("afrondenMail").value;

    const parameterMailtemplateKoppelingen: MailtemplateKoppeling[] = [];
    this.mailtemplateKoppelingen.forEach((koppeling) => {
      const mailtemplateKoppeling: MailtemplateKoppeling =
        new MailtemplateKoppeling();
      mailtemplateKoppeling.mailtemplate = this.mailtemplates.find(
        (mailtemplate) =>
          mailtemplate.id ===
          this.mailFormGroup.get(koppeling).get("mailtemplate").value,
      );

      if (mailtemplateKoppeling.mailtemplate) {
        parameterMailtemplateKoppelingen.push(mailtemplateKoppeling);
      }
    });
    this.parameters.mailtemplateKoppelingen = parameterMailtemplateKoppelingen;

    this.parameters.zaakbeeindigParameters = [];
    this.selection.selected.forEach((param) => {
      if (this.isZaaknietontvankelijkParameter(param)) {
        this.parameters.zaakNietOntvankelijkResultaattype =
          this.getZaakbeeindigControl(param, "beeindigResultaat").value;
      } else {
        param.resultaattype = this.getZaakbeeindigControl(
          param,
          "beeindigResultaat",
        ).value;
        this.parameters.zaakbeeindigParameters.push(param);
      }
    });

    const index: string[] = [];
    for (const afzender of this.parameters.zaakAfzenders) {
      index[afzender.index] = afzender.mail;
      afzender.replyTo = this.getZaakAfzenderControl(afzender, "replyTo").value;
    }

    this.parameters.smartDocuments.enabledForZaaktype =
      this.smartDocumentsEnabledForm.value.enabledForZaaktype;

    this.adminService.updateZaakafhandelparameters(this.parameters).subscribe(
      (data) => {
        this.loading = false;
        this.utilService.openSnackbar("msg.zaakafhandelparameters.opgeslagen");
        this.parameters = data;
        for (const afzender of this.parameters.zaakAfzenders) {
          for (let i = 0; i < index.length; i++) {
            if (index[i] === afzender.mail) {
              afzender.index = i;
              break;
            }
          }
        }
      },
      (error) => {
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

  compareObject(object1: any, object2: any): boolean {
    if (typeof object1 === "string") {
      return object1 === object2;
    }
    if (object1 && object2) {
      if (object1.hasOwnProperty("key")) {
        return object1.key === object2.key;
      } else if (object1.hasOwnProperty("id")) {
        return object1.id === object2.id;
      } else if (object1.hasOwnProperty("naam")) {
        return object1.naam === object2.naam;
      } else if (object1.hasOwnProperty("name")) {
        return object1.name === object2.name;
      }
      return object1 === object2;
    }
    return false;
  }

  formulierDefinitieChanged(
    $event: MatSelectChange,
    humanTaskParameter: HumanTaskParameter,
  ): void {
    humanTaskParameter.formulierDefinitieId = $event.value;
    this.humanTasksFormGroup.setControl(
      humanTaskParameter.planItemDefinition.id,
      this.getHumanTaskFormGroup(humanTaskParameter),
    );
  }

  getVeldDefinities(formulierDefinitieId: string): FormulierVeldDefinitie[] {
    if (formulierDefinitieId) {
      return this.formulierDefinities.find((f) => f.id === formulierDefinitieId)
        .veldDefinities;
    } else {
      return [];
    }
  }

  getBeschikbareMailtemplates(mailtemplate: MailtemplateKoppelingMail): any {
    return this.mailtemplates.filter(
      (template) => template.mail === mailtemplate,
    );
  }
}
