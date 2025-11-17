import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../setupJest";
import { AppComponent } from "./app.component";
import { IdentityService } from "./identity/identity.service";

describe(AppComponent.name, () => {
  let fixture: ComponentFixture<AppComponent>;
  let identityService: IdentityService;
  const removeQueriesSpy = jest.spyOn(testQueryClient, "removeQueries");

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AppComponent],
      providers: [
        IdentityService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
      imports: [TranslateModule.forRoot()],
    });

    identityService = TestBed.inject(IdentityService);

    fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
  });

  it("should delete the logged in user session on init", () => {

    expect(removeQueriesSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        queryKey: identityService.readLoggedInUser().queryKey,
      }),
    );
  });
});
