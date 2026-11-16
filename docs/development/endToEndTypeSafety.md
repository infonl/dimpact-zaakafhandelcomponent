# End-to-end type safety

The ZAC Angular frontend uses [openapi-typescript](https://github.com/drwpow/openapi-typescript) to make requests to the backend and process the responses. This ensures that the frontend and backend align well (think of typed responses) and that the frontend cannot make invalid requests (think of wrong parameters or non-existing endpoints).

With this, we hope to catch bugs faster before they go into production and reduce development time because the frontend and backend align better.

This feature consists of 2 packages:

- [openapi-typescript](https://github.com/drwpow/openapi-typescript/tree/main/packages/openapi-typescript)
  - This is a package that can read an OpenAPI specification file and generate typescript code from it.
- [openapi-typescript-fetch](https://github.com/openapi-ts/openapi-typescript/tree/main/packages/openapi-fetch)
  - This code is a wrapper around axios fetch that has made the axios API completely type-safe. We don't use this package directly in ZAC; we only use the types in this package to write our wrapper around Angular's httpclient.

## The HttpClient and the ZacQueryClient

The package openapi-typescript-fetch has an API that is not compatible with Angular's recommended httpclient. That's why we wrote a wrapper around Angular's httpclient that uses the types from openapi-typescript-fetch: the `HttpClient` in `src/main/app/src/app/shared/http/http-client.ts`. It works like Angular's httpclient, but with type-safe arguments for URL, body, query, and params. Also, the response is type-safe.

Services do not call it directly. They use the `ZacQueryClient`, which turns every endpoint into TanStack Query options with the same type safety: `GET` and `PUT_QUERY` give query options, the other methods give mutation options.

```typescript
@Injectable({ providedIn: "root" })
export class BagService {
  private readonly zacQueryClient = inject(ZacQueryClient);

  list(zaakUuid: string) {
    return this.zacQueryClient.GET("/rest/bag/zaak/{zaakUuid}", {
      path: { zaakUuid },
    });
  }
}
```

## How does this work?

On the Java side, an openapi document is generated based on the java code. Want to know more about how this works? Check out the [openapi-generator](https://github.com/OpenAPITools/openapi-generator) GitHub page.

The open-api document is built when the script `./gradlew generateOpenApiSpec` or `./gradlew build` is executed. This document is then placed in the folder `build/generated/openapi/META-INF/openapi`, and the frontend can then run `npm run generate:types:zac-openapi` to generate the typescript code.

This code is then placed in the folder `src/main/app/src/generated/types/zac-openapi-types.d.ts`, and you can import the typings from there. However, it's noted that it's not recommended to import these types directly everywhere, but to think of a smart solution to use the types, for example, with the ZacQueryClient. Good TypeScript code is written with as little TypeScript code as possible.
