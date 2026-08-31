/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { convertToParamMap } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../shared/utils/generated-types";
import { MailtemplateBeheerService } from "./mailtemplate-beheer.service";
import { MailtemplateResolver } from "./mailtemplate-resolver.service";

describe(MailtemplateResolver.name, () => {
  let mailtemplateResolver: MailtemplateResolver;
  let queryClient: QueryClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MailtemplateResolver,
        MailtemplateBeheerService,
        QueryClient,
        provideHttpClient(withInterceptorsFromDi()),
      ],
      imports: [TranslateModule.forRoot()],
    });

    mailtemplateResolver = TestBed.inject(MailtemplateResolver);
    queryClient = TestBed.inject(QueryClient);
    jest
      .spyOn(queryClient, "fetchQuery")
      .mockResolvedValue(
        fromPartial<GeneratedType<"RestMailtemplate">>({ id: 42 }),
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

    expect(queryClient.fetchQuery).toHaveBeenCalledWith(
      expect.objectContaining({
        queryKey: expect.arrayContaining(["/rest/beheer/mailtemplates/{id}"]),
      }),
    );
    expect(result).toEqual(expect.objectContaining({ id: 42 }));
  });
});
