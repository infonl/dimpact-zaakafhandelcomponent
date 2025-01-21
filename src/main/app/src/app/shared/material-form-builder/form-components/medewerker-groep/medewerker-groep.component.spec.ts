/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HttpClientTestingModule } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { FormControl } from "@angular/forms";
import { MatAutocomplete } from "@angular/material/autocomplete";
import { MatFormField, MatLabel } from "@angular/material/form-field";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { IdentityService } from "../../../../identity/identity.service";
import { GeneratedType } from "../../../utils/generated-types";
import { MedewerkerGroepFormField } from "./medewerker-groep-form-field";
import { MedewerkerGroepComponent } from "./medewerker-groep.component";

describe(MedewerkerGroepComponent.name, () => {
  let component: MedewerkerGroepComponent;

  let identityService: IdentityService;
  let medewerkerGroepFormField = new MedewerkerGroepFormField();
  let medewerkerFormControl: FormControl;
  const user: GeneratedType<"RestUser"> = {
    id: "test-user-id",
    naam: "test-medewerker",
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [MedewerkerGroepComponent],
      providers: [IdentityService],
      imports: [
        TranslateModule.forRoot(),
        HttpClientTestingModule,
        MatFormField,
        MatLabel,
        MatAutocomplete,
      ],
    });

    const fixture = TestBed.createComponent(MedewerkerGroepComponent);
    component = fixture.componentInstance;

    medewerkerFormControl = new FormControl(user, { nonNullable: true });
    const groepFormControl = new FormControl({ id: "1", naam: "test-groep" });
    jest
      .spyOn(medewerkerGroepFormField, "medewerker", "get")
      .mockReturnValue(medewerkerFormControl);
    jest
      .spyOn(medewerkerGroepFormField, "groep", "get")
      .mockReturnValue(groepFormControl);
    component.data = medewerkerGroepFormField;

    identityService = TestBed.inject(IdentityService);
    jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(of([user]));
  });

  describe("when the group value changes", () => {
    it("should update the employee", () => {
      const setValue = jest.spyOn(medewerkerFormControl, "setValue");

      component.ngOnInit();
      component.data.groep.setValue({ id: "2", naam: "new-test-groep" });

      expect(setValue).toHaveBeenCalledWith(user);
    });
  });
});
