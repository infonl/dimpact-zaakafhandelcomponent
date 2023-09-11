# Introductie

De ZAC angular frontend gebruikt (openapi-typescript)[https://github.com/drwpow/openapi-typescript] om requests te doen naar de backend en de responses te verwerken. Dit zorgt ervoor dat de frontend en backend goed op elkaar aansluiten (denk aan typed responses) en dat de frontend geen ongeldige requests kan doen (denk aan verkeerde parameters of niet bestaande endpoints).

Hiermee hopen wij sneller bugs op te vangen voordat ze naar productie gaan en de ontwikkeltijd te verkorten doordat de frontend en backend beter op elkaar aansluiten.

Deze feature bestaat uit 2 packages:

- (openapi-typescript)[https://github.com/drwpow/openapi-typescript/tree/main/packages/openapi-typescript]
  - Dit is een package die een openapi.yaml file kan uitlezen en hier typescript code van kan genereren.
- (openapi-typescript-fetch)[https://github.com/drwpow/openapi-typescript/tree/main/packages/openapi-typescript-fetch]
  - Deze code is een wrapper om axios fetch heen die de axios api geheel typesafe heeft gemaakt, wij gebruiken deze package niet direct in ZAC, wij gebruiken alleen de types in deze package om onze eigen wrapper te schrijven om de httpclient van angular heen.

## De ZacHttpClient

De package openapi-typescript-fetch heeft een api die niet compatible is met de aanbevolen httpclient van angular. Daarom hebben wij een eigen wrapper geschreven om de httpclient van angular heen die de types van openapi-typescript-fetch gebruikt. Deze wrapper is de ZacHttpClient. De ZacHttpClient zou helemaal hetzelfde moeten werken als de httpclient van angular, alleen dan met typesafe argumenten voor url, body, query en params, daarnaast is de response ook typesafe.

```typescript
class Test {
  constructor(private zacHttp: ZacHttpClient) {}

  public getSomething() {
    return this.zacHttp.GET("/rest/bag/zaak/{zaakUuid}", {
      params: { path: { zaakUuid: 1 } },
    });
  }
}
```

## Hoe gaat dit in zijn werking

Aan de Java kant word er een openapi document gegenereerd op basis van de java code, meer weten over hoe dit werkt? Kijk dan op de (openapi-generator)[https://github.com/OpenAPITools/openapi-generator] github pagina.

Het open-api document word gebouwd wanneer het script `./gradlew generateOpenApiSpec` of `./gradlew build` word uitgevoerd. Dit document wordt vervolgens in de folder `build/generated/openapi/META-INF/openapi/openapi.yaml` geplaatst en de frontend kan dan `npm run generate:types:zac-openapi` draaien om de typescript code te genereren.

Deze code word vervolgens in de folder `src/main/app/src/generated/types/zac-openapi-types.d.ts` geplaatst en hieruit kan je de typings importeren. Wel met de kantekening dat het niet wordt aangeraden om deze types direct overal te importeren, maar een slimme oplossing te bedenken om de types te gebruiken met als voorbeeld de ZacHttpClient. Goeie typescipt code schrijf je met zo min mogelijk typescript code.
