/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpErrorResponse,
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { convertToParamMap } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { MailtemplateBeheerService } from "./mailtemplate-beheer.service";
import { MailtemplateResolver } from "./mailtemplate-resolver.service";

describe(MailtemplateResolver.name, () => {
  let mailtemplateResolver: MailtemplateResolver;
  let queryClient: QueryClient;
  let foutAfhandelingService: FoutAfhandelingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MailtemplateResolver,
        MailtemplateBeheerService,
        FoutAfhandelingService,
        QueryClient,
        provideHttpClient(withInterceptorsFromDi()),
      ],
      imports: [TranslateModule.forRoot()],
    });

    mailtemplateResolver = TestBed.inject(MailtemplateResolver);
    queryClient = TestBed.inject(QueryClient);
    foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    jest
      .spyOn(foutAfhandelingService, "httpErrorAfhandelen")
      .mockReturnValue(EMPTY);
    jest
      .spyOn(queryClient, "ensureQueryData")
      .mockResolvedValue(
        fromPartial<GeneratedType<"RESTMailtemplate">>({ id: 42 }),
      );
  });

  it("should throw an error if no id is provided", () => {
    expect(() =>
      mailtemplateResolver.resolve(
        fromPartial({
          get paramMap() {
            return convertToParamMap({ id: null });
          },
        }),
      ),
    ).toThrowError("MailtemplateResolver: no 'id' parameter found in route");
  });

  it("should fetch the mailtemplate via the query client for the given id", async () => {
    const result = await mailtemplateResolver.resolve(
      fromPartial({
        get paramMap() {
          return convertToParamMap({ id: "42" });
        },
      }),
    );

    expect(queryClient.ensureQueryData).toHaveBeenCalledWith(
      expect.objectContaining({
        queryKey: expect.arrayContaining(["/rest/beheer/mailtemplates/{id}"]),
      }),
    );
    expect(result).toEqual(expect.objectContaining({ id: 42 }));
  });

  it("handles the error via foutAfhandelen and does not retry on failure", async () => {
    await mailtemplateResolver.resolve(
      fromPartial({
        get paramMap() {
          return convertToParamMap({ id: "42" });
        },
      }),
    );

    const { retry } = jest.mocked(queryClient.ensureQueryData).mock
      .calls[0][0] as {
      retry: (count: number, error: HttpErrorResponse) => boolean;
    };
    const error = new HttpErrorResponse({ status: 500 });

    const shouldRetry = retry(0, error);

    expect(foutAfhandelingService.httpErrorAfhandelen).toHaveBeenCalledWith(
      error,
    );
    expect(shouldRetry).toBe(false);
  });
});
