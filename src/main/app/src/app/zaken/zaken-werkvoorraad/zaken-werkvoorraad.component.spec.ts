/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { MaterialModule } from "../../shared/material/material.module";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenWerkvoorraadComponent } from "./zaken-werkvoorraad.component";

describe(ZakenWerkvoorraadComponent.name, () => {
  let component: ZakenWerkvoorraadComponent;
  let fixture: ComponentFixture<ZakenWerkvoorraadComponent>;
  let identityService: IdentityService;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      declarations: [ZakenWerkvoorraadComponent],
      imports: [
        MaterialModule,
        RouterModule,
        PipesModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        EmptyPipe,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            data: of({
              tabelGegevens: {
                aantalPerPagina: 10,
                pageSizeOptions: [10, 25, 50],
                werklijstRechten: fromPartial<
                  GeneratedType<"RestWerklijstRechten">
                >({}),
              },
            }),
          },
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = TestBed.createComponent(ZakenWerkvoorraadComponent);
    component = fixture.componentInstance;

    identityService = TestBed.inject(IdentityService);

    testQueryClient.setQueryData(identityService.readLoggedInUser().queryKey, {
      id: "user1",
      naam: "testuser-1",
      groupIds: ["groupA", "groupB"],
    });
    fixture.detectChanges();
  });

  describe(ZakenWerkvoorraadComponent.prototype.showAssignToMe.name, () => {
    it.each([
      ["user2", true],
      ["user1", false],
    ])("for user %s it should return %o", (user, expectation) => {
      const zaakZoekObject = fromPartial<ZaakZoekObject>({
        id: "zaak1",
        rechten: { toekennen: true },
        groepId: "groupA",
        behandelaarGebruikersnaam: user,
      });

      expect(component.showAssignToMe(zaakZoekObject)).toBe(expectation);
    });
  });
});
