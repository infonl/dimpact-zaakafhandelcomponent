/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { MatHint, MatLabel } from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatSidenavModule } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { ReferentieTabelService } from "../../admin/referentie-tabel.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { KlantenService } from "../../klanten/klanten.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { NavigationService } from "../../shared/navigation/navigation.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakCreateComponent } from "./zaak-create.component";

describe(ZaakCreateComponent.name, () => {
  let identityService: IdentityService;
  let zakenService: ZakenService;

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
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-zaaktype-1",
          omschrijving: "test-description-1",
        }),
        fromPartial<GeneratedType<"RestZaaktype">>({
          uuid: "test-zaaktype-2",
          omschrijving: "test-description-2",
        }),
      ]),
    );
  });

  describe(ZaakCreateComponent.prototype.caseTypeSelected.name, () => {
    // TODO validate that we can use the `MatAutocompleteHarness` to write this test
    it.todo(`should set the default group`);

    it.todo(`should set the default case worker`);

    it.todo(`should set the confidentiality notice`);
  });
});
