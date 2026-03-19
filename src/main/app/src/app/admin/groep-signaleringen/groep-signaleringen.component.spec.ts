/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AsyncPipe, NgClass, NgFor, NgIf } from "@angular/common";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatCardModule } from "@angular/material/card";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatSelectModule } from "@angular/material/select";
import { MatSidenavModule } from "@angular/material/sidenav";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of, throwError } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { SignaleringenSettingsBeheerService } from "../signaleringen-settings-beheer.service";
import { GroepSignaleringenComponent } from "./groep-signaleringen.component";

@Component({
  templateUrl: "./groep-signaleringen.component.html",
  standalone: true,
  imports: [
    AsyncPipe,
    NgClass,
    NgFor,
    NgIf,
    MatSidenavModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatTableModule,
    MatCheckboxModule,
    SideNavComponent,
    TranslateModule,
  ],
})
class TestGroepSignaleringenComponent extends GroepSignaleringenComponent {
  public override laadSignaleringSettings(
    groep: GeneratedType<"RestGroup">,
  ): void {
    super.laadSignaleringSettings(groep);
  }

  public override changed(
    row: GeneratedType<"RestSignaleringInstellingen">,
    column: string,
    checked: boolean,
  ): void {
    super.changed(row, column, checked);
  }

  get testIsLoadingResults(): boolean {
    return this.isLoadingResults;
  }

  get testGroepId(): string | undefined {
    return this.groepId;
  }

  get testDataSource(): MatTableDataSource<
    GeneratedType<"RestSignaleringInstellingen">
  > {
    return this.dataSource;
  }
}

describe(GroepSignaleringenComponent.name, () => {
  let fixture: ComponentFixture<TestGroepSignaleringenComponent>;
  let component: TestGroepSignaleringenComponent;
  let utilServiceMock: Pick<UtilService, "setTitle" | "setLoading">;
  let identityServiceMock: Pick<IdentityService, "listGroups">;
  let signaleringenServiceMock: Pick<
    SignaleringenSettingsBeheerService,
    "list" | "put"
  >;

  beforeEach(async () => {
    utilServiceMock = { setTitle: jest.fn(), setLoading: jest.fn() };
    identityServiceMock = { listGroups: jest.fn().mockReturnValue(of([])) };
    signaleringenServiceMock = {
      list: jest.fn().mockReturnValue(of([])),
      put: jest.fn().mockReturnValue(of(null)),
    };

    await TestBed.configureTestingModule({
      imports: [
        TestGroepSignaleringenComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        RouterModule.forRoot([]),
      ],
      providers: [
        { provide: UtilService, useValue: utilServiceMock },
        {
          provide: ConfiguratieService,
          useValue: {} satisfies Partial<ConfiguratieService>,
        },
        { provide: IdentityService, useValue: identityServiceMock },
        {
          provide: SignaleringenSettingsBeheerService,
          useValue: signaleringenServiceMock,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestGroepSignaleringenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should call setTitle and listGroups on init", () => {
    expect(utilServiceMock.setTitle).toHaveBeenCalledWith(
      "title.signaleringen.settings.groep",
      undefined,
    );
    expect(identityServiceMock.listGroups).toHaveBeenCalled();
  });

  it("should load signalering settings for a group", () => {
    const groep = {
      id: "groep-1",
      naam: "Groep 1",
    } as GeneratedType<"RestGroup">;
    component.laadSignaleringSettings(groep);
    expect(signaleringenServiceMock.list).toHaveBeenCalledWith("groep-1");
  });

  it("should populate dataSource after loading settings", () => {
    const settings = [
      {
        type: "ZAAK_OP_NAAM",
        subjecttype: "ZAAK",
        dashboard: true,
        mail: false,
      },
    ] as GeneratedType<"RestSignaleringInstellingen">[];
    (signaleringenServiceMock.list as jest.Mock).mockReturnValue(of(settings));

    const groep = {
      id: "groep-1",
      naam: "Groep 1",
    } as GeneratedType<"RestGroup">;
    component.laadSignaleringSettings(groep);

    expect(component.testDataSource.data).toEqual(settings);
  });

  it("should set isLoadingResults false after loading settings", () => {
    (signaleringenServiceMock.list as jest.Mock).mockReturnValue(of([]));
    const groep = {
      id: "groep-1",
      naam: "Groep 1",
    } as GeneratedType<"RestGroup">;

    component.laadSignaleringSettings(groep);

    expect(component.testIsLoadingResults).toBe(false);
  });

  it("should set groepId after loading settings", () => {
    const groep = {
      id: "groep-1",
      naam: "Groep 1",
    } as GeneratedType<"RestGroup">;
    component.laadSignaleringSettings(groep);
    expect(component.testGroepId).toBe("groep-1");
  });

  it("should call put with updated row on changed", () => {
    const groep = {
      id: "groep-1",
      naam: "Groep 1",
    } as GeneratedType<"RestGroup">;
    component.laadSignaleringSettings(groep);

    const row = {
      type: "ZAAK_OP_NAAM",
      subjecttype: "ZAAK",
      dashboard: false,
      mail: false,
    } as GeneratedType<"RestSignaleringInstellingen">;

    component.changed(row, "dashboard", true);

    expect(utilServiceMock.setLoading).toHaveBeenCalledWith(true);
    expect(signaleringenServiceMock.put).toHaveBeenCalledWith("groep-1", row);
  });

  it("should call setLoading false after put completes", () => {
    const groep = {
      id: "groep-1",
      naam: "Groep 1",
    } as GeneratedType<"RestGroup">;
    component.laadSignaleringSettings(groep);

    const row = {
      type: "ZAAK_OP_NAAM",
      subjecttype: "ZAAK",
      dashboard: false,
      mail: false,
    } as GeneratedType<"RestSignaleringInstellingen">;

    component.changed(row, "dashboard", true);

    expect(utilServiceMock.setLoading).toHaveBeenCalledWith(false);
  });

  it("should call setLoading false even when put fails", () => {
    const groep = {
      id: "groep-1",
      naam: "Groep 1",
    } as GeneratedType<"RestGroup">;
    component.laadSignaleringSettings(groep);
    (signaleringenServiceMock.put as jest.Mock).mockReturnValue(
      throwError(() => new Error("put failed")),
    );

    const row = {
      type: "ZAAK_OP_NAAM",
      subjecttype: "ZAAK",
      dashboard: false,
      mail: false,
    } as GeneratedType<"RestSignaleringInstellingen">;

    component.changed(row, "dashboard", true);

    expect(utilServiceMock.setLoading).toHaveBeenCalledWith(false);
  });

  it("should mutate row column value on changed", () => {
    const groep = {
      id: "groep-1",
      naam: "Groep 1",
    } as GeneratedType<"RestGroup">;
    component.laadSignaleringSettings(groep);

    const row = {
      type: "ZAAK_OP_NAAM",
      subjecttype: "ZAAK",
      dashboard: false,
      mail: false,
    } as GeneratedType<"RestSignaleringInstellingen">;

    component.changed(row, "dashboard", true);

    expect((row as Record<string, unknown>)["dashboard"]).toBe(true);
  });
});
