/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatAutocompleteHarness } from "@angular/material/autocomplete/testing";
import { MatHint, MatLabel } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatSidenavModule } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { CaseDefinition } from "../../admin/model/case-definition";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { ZaakStatusmailOptie } from "../model/zaak-statusmail-optie";
import { Zaaktype } from "../model/zaaktype";
import { ZakenService } from "../zaken.service";
import { ZaakCreateComponent } from "./zaak-create.component";

describe(ZaakCreateComponent.name, () => {
  let loader: HarnessLoader;

  let identityService: IdentityService;
  let zakenService: ZakenService;

  const zaakType = new Zaaktype();
  zaakType.zaakafhandelparameters = {
    defaultBehandelaarId: "default-behandelaar",
    defaultGroepId: "default-group",
    einddatumGeplandWaarschuwing: 10,
    zaaktype: zaakType,
    afrondenMail: ZaakStatusmailOptie.BESCHIKBAAR_AAN,
    caseDefinition: new CaseDefinition(),
    creatiedatum: new Date().toJSON(),
    domein: "test",
    humanTaskParameters: [],
    intakeMail: ZaakStatusmailOptie.BESCHIKBAAR_AAN,
    mailtemplateKoppelingen: [],
    productaanvraagtype: "",
    uiterlijkeEinddatumAfdoeningWaarschuwing: 10,
    valide: true,
    userEventListenerParameters: [],
    zaakAfzenders: [],
    zaakbeeindigParameters: [],
    smartDocuments: {},
    zaakNietOntvankelijkResultaattype: {
      id: "1",
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZaakCreateComponent],
      providers: [
        ZakenService,
        NavigationService,
        KlantenService,
        ReferentieTabelService,
        UtilService,
        IdentityService,
        provideHttpClient(),
      ],
      imports: [
        RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        MatSidenavModule,
        MaterialFormBuilderModule,
        MatHint,
        MatIcon,
        MatLabel,
        FormsModule,
        ReactiveFormsModule,
      ],
    }).compileComponents();

    identityService = TestBed.inject(IdentityService);
    jest
      .spyOn(identityService, "listGroups")
      .mockReturnValue(of([{ id: "test-group-id", naam: "test group" }]));
    jest
      .spyOn(identityService, "listUsersInGroup")
      .mockReturnValue(of([{ id: "test-user-id", naam: "test user" }]));

    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "listZaaktypes").mockReturnValue(
      of([
        fromPartial<Zaaktype>({
          uuid: "test-zaaktype-1",
          omschrijving: "test-description-1",
        }),
        fromPartial<Zaaktype>({
          uuid: "test-zaaktype-2",
          omschrijving: "test-description-2",
        }),
      ]),
    );

    const fixture = TestBed.createComponent(ZaakCreateComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe(ZaakCreateComponent.prototype.caseTypeSelected.name, () => {
    it(`should set the default group`, async () => {
      const caseType = await loader.getHarness(MatAutocompleteHarness);

      // It is impossible to test an auto-complete field with the test harness
      // https://stackoverflow.com/a/57569268
      await caseType.enterText("test");

      expect(true).toBeTruthy(); // TODO validate that we can use the `MatAutocompleteHarness` to write this test
    });

    it.todo(`should set the default case worker`);

    it.todo(`should set the confidentiality notice`);
  });
});
