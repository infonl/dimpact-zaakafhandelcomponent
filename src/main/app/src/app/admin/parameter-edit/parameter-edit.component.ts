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
import { HumanTaskReferentieTabel } from "../model/human-task-referentie-tabel";
import {
  MailtemplateKoppelingMail,
  MailtemplateKoppelingMailUtil,
} from "../model/mailtemplate-koppeling-mail";
import { ReferentieTabel } from "../model/referentie-tabel";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { SmartDocumentsFormComponent } from "./smart-documents-form/smart-documents-form.component";
import {MatSort} from "@angular/material/sort";

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

  isSmartDocumentsStepValid = true;

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

  zaakbeeindigParameters: Api<"RESTZaakbeeindigParameter">[] = [];
  selection = new SelectionModel<Api<"RESTZaakbeeindigParameter">>(true);
  zaakAfzendersDataSource = new MatTableDataSource<Api<"RESTZaakAfzender">>();
  mailtemplateKoppelingen =
    MailtemplateKoppelingMailUtil.getBeschikbareMailtemplateKoppelingen();

  zaakbeeindigFormGroup = new FormGroup({});
  smartDocumentsEnabledForm = new FormGroup({
    enabledForZaaktype: new FormControl<boolean | undefined>(false),
  });
  betrokkeneKoppelingen = new FormGroup({
    brpKoppelen: new FormControl(false),
    kvkKoppelen: new FormControl(false),
  });

  resultaattypes: Api<"RestResultaattype">[] = [];
  referentieTabellen: ReferentieTabel[] = [];
  zaakbeeindigRedenen: Api<"RESTZaakbeeindigReden">[] = [];
  mailtemplates: Api<"RESTMailtemplate">[] = [];
  loading = false;
  subscriptions$: Subscription[] = [];

  // Refactored
  protected caseDefinitions$ =
    this.zaakafhandelParametersService.listCaseDefinitions();
  protected domains$ = this.referentieTabelService.listDomeinen();
  protected groups$ = this.identityService.listGroups();
  protected formulierDefinities$ =
    this.zaakafhandelParametersService.listFormulierDefinities();
  protected replyTos$ = this.zaakafhandelParametersService.listReplyTos()

  protected users: Api<"RestUser">[] = [];
  protected humanTaskParameters: Api<"RESTHumanTaskParameters">[] = [];
  protected userEventListenerParameters: Api<"RESTUserEventListenerParameter">[] =
    [];
  private allZaakAfzenders: string[] = []
  protected zaakAfzenders: string[] = [];


  protected mailOpties = this.utilService.getEnumAsSelectList(
    "statusmail.optie",
    ZaakStatusmailOptie,
  );

  protected form = this.formBuilder.group({
    general: this.formBuilder.group({
      caseDefinition: new FormControl<Api<"RESTCaseDefinition"> | null>(null, [
        Validators.required,
      ]),
      domain: new FormControl<string | null>(null),
      defaultGroup: new FormControl<Api<"RestGroup"> | null>(null, [
        Validators.required,
      ]),
      defaultCaseWorker: new FormControl<Api<"RestUser"> | null>(null),
      einddatumGeplandWaarschuwing: new FormControl<number | null>(null, [
        Validators.min(0),
        Validators.max(31),
        Validators.pattern("^[0-9]*$"),
      ]),
      uiterlijkeEinddatumAfdoeningWaarschuwing: new FormControl<number | null>(
        null,
        [Validators.min(0), Validators.pattern("^[0-9]*$")],
      ),
      productaanvraagtype: new FormControl<string | null>(""),
    }),
    humanTaskParameters: this.formBuilder.group<
      Record<string, ReturnType<typeof this.createHumanTaskFormGroup>>
    >({}),
    userEventListenerParameters: this.formBuilder.group<
      Record<string, FormGroup>
    >({}),
    mailtemplateKoppelingen: this.formBuilder.group({
      intakeMail: new FormControl<(typeof this.mailOpties)[number] | null>(
        null,
        [Validators.required],
      ),
      afrondenMail: new FormControl<(typeof this.mailOpties)[number] | null>(
        null,
        [Validators.required],
      ),
    }),
  });

  constructor(
    public readonly utilService: UtilService,
    private readonly zaakafhandelParametersService: ZaakafhandelParametersService,
    public configuratieService: ConfiguratieService,
    private readonly identityService: IdentityService,
    private readonly route: ActivatedRoute,
    private readonly referentieTabelService: ReferentieTabelService,
    mailtemplateBeheerService: MailtemplateBeheerService,
    private readonly formBuilder: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
  ) {
    super(utilService, configuratieService);
    this.form.controls.general.controls.defaultCaseWorker.disable();

    this.form.controls.general.controls.defaultGroup.valueChanges.subscribe(
      (group) => {
        if (!group) {
          this.form.controls.general.controls.defaultCaseWorker.reset();
          this.form.controls.general.controls.defaultCaseWorker.disable();
          return;
        }

        this.form.controls.general.controls.defaultCaseWorker.enable();

        this.setUsersForGroup(group.id);
      },
    );

    this.form.controls.general.controls.caseDefinition.valueChanges.subscribe(
      (caseDefinition) => {
        if (!caseDefinition) return;

        this.readHumanTaskParameters(caseDefinition);
        this.readUserEventListenerParameters(caseDefinition);
      },
    );

    this.route.data.subscribe((data) => {
      const parameters = data.parameters as Api<"RestZaakafhandelParameters">;

      this.humanTaskParameters = parameters.humanTaskParameters;
      this.userEventListenerParameters = parameters.userEventListenerParameters;

      // TODO: refactor do remove `this.parameters` and use `this.form` instead
      console.log({parameters});
      this.parameters = data.parameters;
      this.parameters.zaakAfzenders = [{
        id: 1,
        speciaal: true,
        mail: "GEMEENTE",
      },
        {id: 2,
          speciaal: false,
          mail: "mail@sanderboer.nl"
        }]
      this.parameters.intakeMail = this.parameters.intakeMail
        ? this.parameters.intakeMail
        : ZaakStatusmailOptie.BESCHIKBAAR_UIT;
      this.parameters.afrondenMail = this.parameters.afrondenMail
        ? this.parameters.afrondenMail
        : ZaakStatusmailOptie.BESCHIKBAAR_UIT;

      forkJoin([
        referentieTabelService.listReferentieTabellen(),
        referentieTabelService.listAfzenders(),
        zaakafhandelParametersService.listZaakbeeindigRedenen(),
        mailtemplateBeheerService.listKoppelbareMailtemplates(),
        zaakafhandelParametersService.listResultaattypes(
          this.parameters.zaaktype.uuid ?? "",
        ),
      ]).subscribe(
        ([
          referentieTabellen,
          afzenders,
          zaakbeeindigRedenen,
          mailtemplates,
          resultaattypes,
        ]) => {
          this.referentieTabellen = referentieTabellen;
          this.zaakbeeindigRedenen = zaakbeeindigRedenen;
          this.mailtemplates = mailtemplates;
          this.allZaakAfzenders = this.zaakAfzenders = afzenders;
          this.resultaattypes = resultaattypes;
          this.createForm();
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

  ngOnDestroy(): void {
    super.ngOnDestroy();
    for (const subscription of this.subscriptions$) {
      subscription.unsubscribe();
    }
  }

  private readHumanTaskParameters(caseDefinition: Api<"RESTCaseDefinition">) {
    this.humanTaskParameters = [];
    this.caseDefinitions$.toPromise().then((caseDefinitions) => {
      caseDefinitions
        ?.find(({ key }) => key === caseDefinition?.key)
        ?.humanTaskDefinitions?.forEach((humanTaskDefinition) => {
          this.humanTaskParameters.push({
            planItemDefinition: humanTaskDefinition,
            defaultGroepId:
              this.form.controls.general.controls.defaultGroup.value?.id,
            formulierDefinitieId: humanTaskDefinition.defaultFormulierDefinitie,
            referentieTabellen: [],
            actief: true,
          });
        });
      this.createHumanTasksForm();
    });
  }

  private readUserEventListenerParameters(
    caseDefinition: Api<"RESTCaseDefinition">,
  ) {
    this.userEventListenerParameters = [];
    this.caseDefinitions$.toPromise().then((caseDefinitions) => {
      caseDefinitions
        ?.find(({ key }) => key === caseDefinition?.key)
        ?.userEventListenerDefinitions?.forEach(({ id, naam }) => {
          this.userEventListenerParameters.push({ id, naam });
        });
      this.createUserEventListenerForm();
    });
  }

  getHumanTaskControl(
    parameter: Api<"RESTHumanTaskParameters">,
    field: string,
  ) {
    return this.form.controls.humanTaskParameters
      .get(parameter.planItemDefinition?.id ?? "")
      ?.get(field) as FormControl;
  }

  getMailtemplateKoppelingControl(
    koppeling: MailtemplateKoppelingMail,
    field: string,
  ) {
    return this.form.controls.mailtemplateKoppelingen
      .get(koppeling)
      ?.get(field);
  }

  createForm() {
    console.log(this.parameters);
    // TODO set form fields from parameters
    // this.form.controls.general.setValue(this.parameters as any)
    // this.algemeenFormGroup = this.formBuilder.group({
    //   caseDefinition: [this.parameters.caseDefinition, [Validators.required]],
    //   domein: [this.parameters.domein],
    //   defaultGroepId: [this.parameters.defaultGroepId, [Validators.required]],
    //   defaultBehandelaarId: [this.parameters.defaultBehandelaarId],
    //   einddatumGeplandWaarschuwing: [
    //     this.parameters.einddatumGeplandWaarschuwing,
    //   ],
    //   uiterlijkeEinddatumAfdoeningWaarschuwing: [
    //     this.parameters.uiterlijkeEinddatumAfdoeningWaarschuwing,
    //   ],
    //   productaanvraagtype: [this.parameters.productaanvraagtype],
    // });
    this.createHumanTasksForm();
    this.createUserEventListenerForm();
    this.createMailForm();
    this.createZaakbeeindigForm();
    this.createSmartDocumentsEnabledForm();
    this.setUsersForGroup(this.parameters.defaultGroepId);
  }

  private setUsersForGroup(groupId?: string | null) {
    if (!groupId) return;

    return this.identityService.listUsersInGroup(groupId).subscribe((users) => {
      this.users = users;
    });
  }

  private createHumanTasksForm() {
    this.humanTaskParameters.forEach((parameter) => {
      this.form.controls.humanTaskParameters.addControl(
        parameter.planItemDefinition?.id ?? "",
        this.createHumanTaskFormGroup(parameter),
      );
    });
  }

  private createHumanTaskFormGroup(
    humanTaskParameters: Api<"RESTHumanTaskParameters">,
  ) {
    const formulierDefinitie =
      new FormControl<Api<"RESTTaakFormulierDefinitie"> | null>(null, [
        Validators.required,
      ]);

    const humanTaskFormGroup = this.formBuilder.group({
      formulierDefinitie,
      defaultGroep: [humanTaskParameters.defaultGroepId],
      doorlooptijd: [
        humanTaskParameters.doorlooptijd,
        [
          Validators.min(0),
          Validators.max(Number.MAX_SAFE_INTEGER),
          Validators.pattern(/^[0-9]*$/),
        ],
      ],
      actief: [humanTaskParameters.actief],
    });

    //
    // this.addVeldDefinities(
    //     humanTaskFormGroup,
    //     humanTaskParameters,
    //     await this.getVeldDefinities(humanTaskParameters.formulierDefinitieId),
    // )

    // TODO: check if value changes when value is set from the first time
    formulierDefinitie.valueChanges.subscribe((definitie) => {
      if (!definitie) return;

      console.log(definitie);

      // TODO: Remove fields again when the form is changed
      this.addVeldDefinities(
        humanTaskFormGroup,
        humanTaskParameters,
        definitie.veldDefinities,
      );
    });

    return humanTaskFormGroup;
  }

  private addVeldDefinities(
    humanTaskFormGroup: FormGroup,
    humanTaskParameters: Api<"RESTHumanTaskParameters">,
    veldDefinities?: Api<"RESTTaakFormulierVeldDefinitie">[],
  ) {
    veldDefinities?.forEach((veld) => {
      (humanTaskFormGroup as FormGroup).addControl(
        "referentieTabel" + veld.naam,
        this.formBuilder.control(
          this.getReferentieTabel(humanTaskParameters, veld),
          Validators.required,
        ),
      );
    });
  }

  private getReferentieTabel(
    humanTaskParameters: Api<"RESTHumanTaskParameters">,
    veld: Api<"RESTTaakFormulierVeldDefinitie">,
  ) {
    const humanTaskReferentieTabel =
      humanTaskParameters.referentieTabellen?.find(
        (tabel) => (tabel.veld = veld.naam),
      )?.tabel;
    return (
      humanTaskReferentieTabel ??
      this.referentieTabellen.find(({ code }) => code === veld.naam)
    );
  }

  private createUserEventListenerForm() {
    this.userEventListenerParameters.forEach((parameter) => {
      this.form.controls.userEventListenerParameters.addControl(
        parameter.id ?? "",
        this.formBuilder.group({
          toelichting: new FormControl(parameter.toelichting, [
            Validators.maxLength(1000),
          ]),
        }),
      );
    });
  }

  private createMailForm() {
    this.form.controls.mailtemplateKoppelingen.controls.intakeMail.setValue({
      label: `statusmail.optie.${this.parameters.intakeMail}`,
      value: this.parameters.intakeMail!,
    });
    this.form.controls.mailtemplateKoppelingen.controls.afrondenMail.setValue({
      label: `statusmail.optie.${this.parameters.afrondenMail}`,
      value: this.parameters.afrondenMail!,
    });

    this.mailtemplateKoppelingen.forEach((beschikbareKoppeling) => {
      const mailtemplate = this.parameters.mailtemplateKoppelingen.find(
        (mailtemplateKoppeling) =>
          mailtemplateKoppeling.mailtemplate?.mail === beschikbareKoppeling,
      )?.mailtemplate;
      const formGroup = this.formBuilder.group({
        mailtemplate: mailtemplate?.id,
      });
      // @ts-expect-error TODO: ts issue
      this.form.controls.mailtemplateKoppelingen.addControl(
        beschikbareKoppeling,
        formGroup,
      );
    });
    this.initZaakAfzenderFormControls();
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

  private initZaakAfzenderFormControls() {
    this.parameters.zaakAfzenders.forEach(({ mail, speciaal }, index) => {
      // @ts-expect-error TODO: ts issue
      this.form.controls.mailtemplateKoppelingen.addControl(
          index,
          new FormControl({ mail, speciaal, index }),
      )
      this.zaakAfzenders = this.zaakAfzenders.filter((afzender) => afzender !== mail)
    })

    this.setZaakAfzendersDataSource();
  }

  private setZaakAfzendersDataSource() {
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
    this.setZaakAfzendersDataSource();
    this.removeAfzender(afzender);
  }

  setDefaultMail(afzender: string): void {
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
    this.setZaakAfzendersDataSource();
    this.zaakAfzenders.push(afzender);
  }

  private addZaakAfzenderControl(zaakAfzender: Api<"RESTZaakAfzender">) {
    // @ts-expect-error TODO: ts issue
    this.form.controls.mailtemplateKoppelingen.addControl(
      "afzender" + (zaakAfzender as { index: number }).index + "__replyTo",
      new FormControl(zaakAfzender.replyTo),
    );
  }

  getZaakAfzenderControl(
    zaakAfzender: Api<"RESTZaakAfzender"> & { index?: number },
    field: string,
  ) {
    return this.form.controls.mailtemplateKoppelingen.get(
      `afzender${zaakAfzender.index}__${field}`,
    );
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
      this.form.valid &&
      this.zaakbeeindigFormGroup.valid &&
      this.isSmartDocumentsStepValid
    );
  }

  opslaan() {
    this.loading = true;
    // Object.assign(this.parameters, this.algemeenFormGroup.value);
    this.humanTaskParameters.forEach((param) => {
      param.formulierDefinitieId = this.getHumanTaskControl(
        param,
        "formulierDefinitie",
      )?.value;
      param.defaultGroepId = this.getHumanTaskControl(
        param,
        "defaultGroep",
      )?.value;
      param.actief = this.getHumanTaskControl(param, "actief")?.value;
      param.doorlooptijd = this.getHumanTaskControl(
        param,
        "doorlooptijd",
      )?.value;
      const bestaandeHumanTaskParameter =
        this.parameters.humanTaskParameters.find(
          ({ planItemDefinition }) =>
            planItemDefinition?.id === param.planItemDefinition?.id,
        );
      const bestaandeReferentietabellen =
        bestaandeHumanTaskParameter?.referentieTabellen ?? [];
      param.referentieTabellen = [];
      const veldDefinities = this.getVeldDefinities(param.formulierDefinitieId);
      veldDefinities.forEach((value) => {
        const bestaandeHumanTaskReferentieTabel =
          bestaandeReferentietabellen?.find(
            (o) => o.veld === (value as any).naam,
          );
        const tabel =
          bestaandeHumanTaskReferentieTabel != null
            ? bestaandeHumanTaskReferentieTabel
            : new HumanTaskReferentieTabel();
        tabel.veld = (value as any).naam;
        tabel.tabel = this.getHumanTaskControl(
          param,
          "referentieTabel" + tabel.veld,
        )?.value;
        param.referentieTabellen?.push(tabel);
      });
    });
    this.parameters.humanTaskParameters = this.humanTaskParameters;
    this.userEventListenerParameters.forEach((param) => {
      param.toelichting = this.form.controls.userEventListenerParameters
        ?.get(param.id ?? "")
        ?.get("toelichting")?.value;
    });
    this.parameters.userEventListenerParameters =
      this.userEventListenerParameters;

    this.parameters.intakeMail = this.form.controls.mailtemplateKoppelingen
      .controls.intakeMail?.value?.value as ZaakStatusmailOptie;
    this.parameters.afrondenMail = this.form.controls.mailtemplateKoppelingen
      .controls.afrondenMail?.value?.value as ZaakStatusmailOptie;

    const parameterMailtemplateKoppelingen: Api<"RESTMailtemplateKoppeling">[] =
      [];
    this.mailtemplateKoppelingen.forEach((koppeling) => {
      const mailtemplateKoppeling: Api<"RESTMailtemplateKoppeling"> = {
        mailtemplate: this.mailtemplates.find(
          (mailtemplate) =>
            mailtemplate.id ===
            this.form.controls.mailtemplateKoppelingen
              .get(koppeling)
              ?.get("mailtemplate")?.value,
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

  getVeldDefinities(formulierDefinitieId?: string) {
    console.log(formulierDefinitieId);
    return [];
    // const formulierDefinities = await this.formulierDefinities$.toPromise()
    //   return [].find(({id}) => id === formulierDefinitieId)
    //       ?.veldDefinities ?? []
  }

  getBeschikbareMailtemplates(mailtemplate: MailtemplateKoppelingMail) {
    return this.mailtemplates.filter(
      (template) => template.mail === mailtemplate,
    );
  }

  protected humanTaskFormGroup(
    humanTaskParameters: Api<"RESTHumanTaskParameters">,
  ) {
    return this.form.controls.humanTaskParameters.get(
      humanTaskParameters.planItemDefinition?.id ?? "",
    ) as FormGroup<{
      formulierDefinitie: FormControl<Api<"RESTTaakFormulierDefinitie"> | null>;
      defaultGroep: FormControl<Api<"RestGroup"> | null>;
      doorlooptijd: FormControl<number | null>;
      referentieTabellen: FormControl<HumanTaskReferentieTabel[] | null>;
      actief: FormControl<boolean | null>;
    }>;
  }

  protected userEventListenerFormGroup(
    userEventListenerParameters: Api<"RESTUserEventListenerParameter">,
  ) {
    return this.form.controls.userEventListenerParameters.get(
      userEventListenerParameters.id ?? "",
    ) as FormGroup<{
      toelichting: FormControl<string | null>;
    }>;
  }

  protected replyToDisplayValue(replyTo: Api<"RESTReplyTo"> | null) {
    if(!replyTo?.mail) return "---"

    if(replyTo.speciaal) {
      return "gegevens.mail.afzender." + replyTo.mail
    }

    return replyTo.mail
  }
}
