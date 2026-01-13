/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormBuilder } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { MaterialFormBuilderModule } from "src/app/shared/material-form-builder/material-form-builder.module";
import { sleep, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { MaterialModule } from "../../shared/material/material.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenService } from "../taken.service";
import { TakenVerdelenDialogComponent } from "./taken-verdelen-dialog.component";
import { provideExperimentalZonelessChangeDetection } from "@angular/core";

describe(TakenVerdelenDialogComponent.name, () => {
  let fixture: ComponentFixture<TakenVerdelenDialogComponent>;
  let component: TakenVerdelenDialogComponent;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;
  let dialogRef: MatDialogRef<TakenVerdelenDialogComponent>;

  const dialogData: {
    taken: TaakZoekObject[];
    screenEventResourceId: string;
  } = {
    taken: [
      {
        id: "taak-1",
        zaakUuid: "zaak-1",
      } as TaakZoekObject,
      {
        id: "taak-2",
        zaakUuid: "zaak-2",
      } as TaakZoekObject,
    ],
    screenEventResourceId: "screen-event-1",
  };

  const mockGroup: GeneratedType<"RestGroup"> = {
    id: "group-1",
    naam: "Test Group",
  };

  const mockUser: GeneratedType<"RestUser"> = {
    id: "user-1",
    naam: "Test User",
  };

  beforeEach(async () => {
    dialogRef = {
      close: jest.fn(),
    } as unknown as MatDialogRef<TakenVerdelenDialogComponent>;

    await TestBed.configureTestingModule({
      declarations: [TakenVerdelenDialogComponent],
      imports: [
        TranslateModule.forRoot(),
        MaterialModule,
        MaterialFormBuilderModule,
      ],
      providers: [
        IdentityService,
        FormBuilder,
        TakenService,
        provideExperimentalZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
        {
          provide: MAT_DIALOG_DATA,
          useValue: dialogData,
        },
        {
          provide: MatDialogRef,
          useValue: dialogRef,
        },
      ],
    }).compileComponents();

    const identiyService = TestBed.inject(IdentityService);
    jest.spyOn(identiyService, "listGroups").mockReturnValue(of([mockGroup]));
    jest
      .spyOn(identiyService, "listUsersInGroup")
      .mockReturnValue(of([mockUser]));

    fixture = TestBed.createComponent(TakenVerdelenDialogComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
  });

  it("should send the form data on request", async () => {
    component["form"].patchValue({
      groep: mockGroup,
      medewerker: mockUser,
      reden: "test-reden",
    });
    component["form"].markAsDirty();

    const button = await loader.getHarness(
      MatButtonHarness.with({ text: /actie.verdelen/i }),
    );
    await button.click();
    await new Promise(requestAnimationFrame);

    const req = httpTestingController.expectOne("/rest/taken/lijst/verdelen");

    expect(req.request.method).toBe("PUT");
    expect(req.request.body).toEqual({
      taken: dialogData.taken.map((taak) => ({
        zaakUuid: taak.zaakUuid,
        taakId: taak.id!,
      })),
      groepId: mockGroup.id,
      behandelaarGebruikersnaam: mockUser.id,
      reden: "test-reden",
      screenEventResourceId: dialogData.screenEventResourceId,
    });
  });

  it("should close the dialog with form data after successful mutation", async () => {
    const formData = {
      groep: mockGroup,
      medewerker: mockUser,
      reden: "test-reden",
    };

    component["form"].patchValue(formData);
    component["form"].markAsDirty();

    const button = await loader.getHarness(
      MatButtonHarness.with({ text: /actie.verdelen/i }),
    );
    await button.click();
    await new Promise(requestAnimationFrame);

    const req = httpTestingController.expectOne("/rest/taken/lijst/verdelen");
    req.flush({});

    await sleep();

    expect(dialogRef.close).toHaveBeenCalledWith(formData);
  });
});
