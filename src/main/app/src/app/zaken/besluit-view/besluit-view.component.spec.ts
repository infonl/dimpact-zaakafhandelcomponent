/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MatDialog } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { BesluitIntrekkenDialogComponent } from "./besluit-intrekken-dialog/besluit-intrekken-dialog.component";
import { BesluitViewComponent } from "./besluit-view.component";

const makeBesluit = (fields: Partial<GeneratedType<"RestBesluit">> = {}) =>
  fromPartial<GeneratedType<"RestBesluit">>({
    uuid: "besluit-uuid-1",
    identificatie: "BESLUIT-001",
    besluittype: fromPartial<GeneratedType<"RestBesluitType">>({
      naam: "Besluittype 1",
      publication: { enabled: false },
    }),
    ingangsdatum: "2026-01-01",
    vervaldatum: "2026-12-31",
    toelichting: "Een toelichting",
    isIngetrokken: false,
    informatieobjecten: [],
    ...fields,
  });

describe(BesluitViewComponent.name, () => {
  let fixture: ComponentFixture<BesluitViewComponent>;
  let component: BesluitViewComponent;
  let dialog: MatDialog;
  let zakenService: ZakenService;

  const setup = (
    besluiten: GeneratedType<"RestBesluit">[] = [makeBesluit()],
    readonly = false,
  ) => {
    fixture = TestBed.createComponent(BesluitViewComponent);
    component = fixture.componentInstance;
    component.besluiten = besluiten;
    component.readonly = readonly;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BesluitViewComponent,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideMomentDateAdapter(),
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);
    jest.spyOn(zakenService, "listBesluitHistorie").mockReturnValue(of([]));

    dialog = TestBed.inject(MatDialog);
    jest
      .spyOn(dialog, "open")
      .mockReturnValue(fromPartial({ afterClosed: () => of(undefined) }));
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  it("builds a documents form for the first besluit on init", () => {
    setup();

    expect(component["documentenForms"]["besluit-uuid-1"]).toBeDefined();
  });

  it("shows the besluit fields as read-only static text", () => {
    setup();

    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain("Besluittype 1");
    expect(text).toContain("Een toelichting");
  });

  it("renders the linked documents as a read-only list", () => {
    setup([
      makeBesluit({
        informatieobjecten: [
          fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
            uuid: "doc-1",
            titel: "Document 1",
            bestandsnaam: "document-1.pdf",
          }),
        ],
      }),
    ]);

    const documents = fixture.nativeElement.querySelector("zac-documents");
    expect(documents).not.toBeNull();
    expect(documents.querySelector("mat-checkbox")).toBeNull();
  });

  describe("isReadonly()", () => {
    it("is read-only when the component is read-only", () => {
      setup([makeBesluit()], true);

      expect(component.isReadonly(makeBesluit())).toBe(true);
    });

    it("is read-only when the besluit is already withdrawn", () => {
      setup();

      expect(component.isReadonly(makeBesluit({ isIngetrokken: true }))).toBe(
        true,
      );
    });

    it("is editable for an open besluit on an editable component", () => {
      setup();

      expect(component.isReadonly(makeBesluit())).toBe(false);
    });
  });

  describe("intrekken()", () => {
    it("opens the intrekken dialog with the besluit as data", () => {
      setup();
      const besluit = makeBesluit();

      component.intrekken(besluit);

      expect(dialog.open).toHaveBeenCalledWith(
        BesluitIntrekkenDialogComponent,
        {
          data: besluit,
        },
      );
    });
  });
});
