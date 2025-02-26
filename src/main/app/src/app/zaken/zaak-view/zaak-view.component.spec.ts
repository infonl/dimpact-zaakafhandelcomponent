import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { ButtonMenuItem } from "../../shared/side-nav/menu-item/button-menu-item";
import { ZaakViewComponent } from "./zaak-view.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { HttpClient, provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Vertrouwelijkheidaanduiding } from "src/app/informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { VertrouwelijkaanduidingToTranslationKeyPipe } from "src/app/shared/pipes/vertrouwelijkaanduiding-to-translation-key.pipe";
import zaakMock from "./zaak-mock";
import { queryByText } from "src/test-helpers";

describe(ZaakViewComponent.name, () => {
  let component: ZaakViewComponent;
  let fixture: ComponentFixture<ZaakViewComponent>;

  const zaak: GeneratedType<"RestZaak"> = {
    uuid: "zaak-001",
    identificatie: "test",
    indicaties: [],
    omschrijving: "test omschrijving",
    vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding.openbaar,
    isEerderOpgeschort: false, // Toggle this for the test cases
    toelichting: "Some toelichting",
    rechten: {},
    zaaktype: {
      uuid: "zaaktype-001",
    },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZaakViewComponent],
      imports: [
        MatSortModule,
        MatTableModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        PipesModule,
        MaterialModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({ zaak }),
          },
        },
        VertrouwelijkaanduidingToTranslationKeyPipe, // Ensure the pipe is provided
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakViewComponent);
    component = fixture.componentInstance;

    jest.spyOn(component as any, "loadHistorie").mockReturnValueOnce(undefined);

    jest
      .spyOn(component as any, "loadBetrokkenen")
      .mockReturnValueOnce(undefined);
    jest
      .spyOn(component as any, "loadBagObjecten")
      .mockReturnValueOnce(undefined);
    jest
      .spyOn(component as any, "setEditableFormFields")
      .mockReturnValueOnce(undefined);
    jest
      .spyOn(component as any, "loadOpschorting")
      .mockReturnValueOnce(undefined);

    component.init(zaakMock);
  });

  describe("actie.zaak.opschorten", () => {
    it("should not show the opschorten button when isEerderOpgeschort is true", async () => {
      component.zaak.isEerderOpgeschort = true;

      const button = queryByText(fixture, "button", "actie.zaak.opschorten");

      expect(button).toBeUndefined();
    });
  });
});
