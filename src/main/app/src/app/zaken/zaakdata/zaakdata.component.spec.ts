/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentRef, provideExperimentalZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakdataComponent } from "./zaakdata.component";

describe(ZaakdataComponent.name, () => {
  let fixture: ComponentFixture<ZaakdataComponent>;
  let componentRef: ComponentRef<ZaakdataComponent>;
  let httpTestingController: HttpTestingController;

  const mockSideNav = fromPartial<MatDrawer>({ close: jest.fn() });

  const makeZaak = (
    zaakdata: Record<string, unknown> = {},
  ): GeneratedType<"RestZaak"> =>
    fromPartial<GeneratedType<"RestZaak">>({ uuid: "zaak-uuid", zaakdata });

  beforeEach(async () => {
    testQueryClient.clear();

    await TestBed.configureTestingModule({
      imports: [ZaakdataComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideExperimentalZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    testQueryClient.setQueryData(["/rest/zaken/procesvariabelen"], []);

    fixture = TestBed.createComponent(ZaakdataComponent);
    componentRef = fixture.componentRef;
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  async function createComponent(
    zaakdata: Record<string, unknown> = { veld: "waarde" },
    readonly = false,
  ) {
    componentRef.setInput("zaak", makeZaak(zaakdata));
    componentRef.setInput("sideNav", mockSideNav);
    componentRef.setInput("readonly", readonly);
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it("renders a form control for each zaakdata field", async () => {
    await createComponent({ veld1: "waarde1", veld2: "waarde2" });
    const formFields = fixture.nativeElement.querySelectorAll("zac-input");
    expect(formFields.length).toBe(2);
  });

  it("form is enabled in edit mode", async () => {
    await createComponent({ veld: "waarde" }, false);
    expect(fixture.componentInstance["form"].disabled).toBe(false);
  });

  it("form is disabled when readonly is true", async () => {
    await createComponent({ veld: "waarde" }, true);
    expect(fixture.componentInstance["form"].disabled).toBe(true);
  });

  it("hides the save button when readonly is true", async () => {
    await createComponent({}, true);
    const saveButton = fixture.nativeElement.querySelector(
      "button[type='submit']",
    );
    expect(saveButton).toBeNull();
  });

  it("sends zaakdata to the server on form submit", async () => {
    await createComponent({ veld: "waarde" });
    fixture.componentInstance["formSubmit"]();
    await new Promise((resolve) => setTimeout(resolve, 0));
    const req = httpTestingController.expectOne(
      (request) =>
        request.url.includes("/rest/zaken/zaakdata") &&
        request.method === "PUT",
    );
    expect(req.request.body).toMatchObject({ uuid: "zaak-uuid" });
    req.flush(null);
  });
});
