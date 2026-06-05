/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentRef } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { AbstractControl, FormControl, FormGroup } from "@angular/forms";
import { MatCheckboxHarness } from "@angular/material/checkbox/testing";
import { MatProgressSpinnerHarness } from "@angular/material/progress-spinner/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { Observable, of } from "rxjs";
import { InformatieObjectenService } from "src/app/informatie-objecten/informatie-objecten.service";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../utils/generated-types";
import { ZacDocuments } from "./documents";

type Document = GeneratedType<"RestEnkelvoudigInformatieobject">;

interface TestForm extends Record<string, AbstractControl> {
  documents: FormControl<Document[] | null>;
}

describe(ZacDocuments.name, () => {
  let component: ZacDocuments<TestForm, "documents", Document>;
  let componentRef: ComponentRef<typeof component>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;
  let informatieObjectenService: InformatieObjectenService;

  const makeDocument = (fields: Partial<Document> = {}): Document =>
    fromPartial<Document>({
      uuid: "doc-uuid-1",
      titel: "Test Document",
      bestandsnaam: "test.pdf",
      informatieobjectTypeOmschrijving: "Bijlage",
      status: "DEFINITIEF",
      versie: 1,
      auteur: "Auteur",
      creatiedatum: "2026-01-01",
      bestandsomvang: 1024,
      ...fields,
    });

  const createTestForm = () =>
    new FormGroup<TestForm>({
      documents: new FormControl<Document[] | null>(null),
    });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZacDocuments, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    jest
      .spyOn(informatieObjectenService, "getDownloadURL")
      .mockReturnValue("/download/doc-uuid-1");

    fixture = TestBed.createComponent(
      ZacDocuments<TestForm, "documents", Document>,
    );
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;
    loader = TestbedHarnessEnvironment.loader(fixture);

    componentRef.setInput("form", createTestForm());
    componentRef.setInput("key", "documents");
    componentRef.setInput("options", of([]));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  describe("Label display", () => {
    it("should display the key as label when no label input is given", () => {
      const labelElement = fixture.nativeElement.querySelector("div");
      expect(labelElement.textContent.trim()).toBe("documents");
    });

    it("should display custom label when provided", () => {
      componentRef.setInput("label", "Bijlagen");
      fixture.detectChanges();
      const labelElement = fixture.nativeElement.querySelector("div");
      expect(labelElement.textContent.trim()).toBe("Bijlagen");
    });
  });

  describe("Empty state", () => {
    it("should show geen gegevens message when no documents and not loading", () => {
      const emptyMessage = fixture.nativeElement.querySelector("em");
      expect(emptyMessage).toBeTruthy();
    });

    it("should show spinner when loading", async () => {
      componentRef.setInput("options", new Observable(() => {}));
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
      const spinner = await loader.getHarness(MatProgressSpinnerHarness);
      expect(spinner).toBeTruthy();
    });
  });

  describe("Document rows", () => {
    beforeEach(async () => {
      const document = makeDocument();
      componentRef.setInput("options", of([document]));
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it("should render a row for each document", () => {
      const rows = fixture.nativeElement.querySelectorAll("tr[mat-row]");
      expect(rows.length).toBe(1);
    });

    it("should show document title in the titel column", () => {
      const rows = fixture.nativeElement.querySelectorAll("td[mat-cell]");
      const titelCell = Array.from(rows).find((cell: Element) =>
        (cell as HTMLElement).textContent?.includes("Test Document"),
      );
      expect(titelCell).toBeTruthy();
    });

    it("should render a view link with correct href", () => {
      const viewLinks = fixture.nativeElement.querySelectorAll(
        "a[mat-icon-button]",
      );
      const viewLink = Array.from(viewLinks).find((link: Element) =>
        (link as HTMLAnchorElement).href?.includes(
          "/informatie-objecten/doc-uuid-1",
        ),
      );
      expect(viewLink).toBeTruthy();
    });

    it("should render a download link via service", () => {
      const downloadLinks = fixture.nativeElement.querySelectorAll(
        "a[mat-icon-button]",
      );
      const downloadLink = Array.from(downloadLinks).find((link: Element) =>
        (link as HTMLAnchorElement).href?.includes("/download/doc-uuid-1"),
      );
      expect(downloadLink).toBeTruthy();
    });
  });

  describe("Selection", () => {
    beforeEach(async () => {
      const document = makeDocument();
      componentRef.setInput("options", of([document]));
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();
    });

    it("should show checkbox when not readonly", async () => {
      const checkbox = await loader.getHarness(MatCheckboxHarness);
      expect(checkbox).toBeTruthy();
    });

    it("should hide checkbox when readonly", async () => {
      componentRef.setInput("readonly", true);
      fixture.detectChanges();
      const checkboxes = await loader.getAllHarnesses(MatCheckboxHarness);
      expect(checkboxes.length).toBe(0);
    });

    it("should update form control when checkbox is toggled", async () => {
      const checkbox = await loader.getHarness(MatCheckboxHarness);
      await checkbox.check();
      const value = component.form().controls.documents.value;
      expect(value).toHaveLength(1);
    });
  });
});
