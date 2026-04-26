## ADDED Requirements

### Requirement: BRP protocollering header names are configured via Helm values with no application-level defaults
The system SHALL read BRP protocollering header names from environment variables with no hardcoded `defaultValue` in application code. The Helm chart SHALL provide non-empty default values for all header names. An operator disables a header by setting its Helm value to an empty string.

| Helm value | Env var | Default |
|---|---|---|
| `brpApi.protocollering.headers.doelbinding` | `BRP_HEADER_DOELBINDING` | `x-doelbinding` |
| `brpApi.protocollering.headers.verwerking` | `BRP_HEADER_VERWERKING` | `x-verwerking` |
| `brpApi.protocollering.headers.originOin` | `BRP_HEADER_ORIGIN_OIN` | `x-origin-oin` |
| `brpApi.protocollering.headers.gebruiker` | `BRP_HEADER_GEBRUIKER` | `x-gebruiker` |
| `brpApi.protocollering.headers.toepassing` | `BRP_HEADER_TOEPASSING` | `x-toepassing` |
| `brpApi.protocollering.toepassing` | `BRP_TOEPASSING` | `ZAC` |

#### Scenario: Default header names used when Helm values are not overridden
- **WHEN** none of the `brpApi.protocollering.headers.*` Helm values are overridden
- **THEN** the system SHALL send BRP requests using the default header names (`x-doelbinding`, `x-verwerking`, `x-origin-oin`, `x-gebruiker`, `x-toepassing`)

#### Scenario: Custom header name used when Helm value is overridden
- **WHEN** `brpApi.protocollering.headers.doelbinding` is set to a non-empty string (e.g. `X-Doel-Binding`)
- **THEN** BRP requests SHALL include a header named `X-Doel-Binding` instead of `x-doelbinding`

#### Scenario: All five header names can be overridden simultaneously
- **WHEN** all five `brpApi.protocollering.headers.*` Helm values are set to custom non-empty strings
- **THEN** BRP requests SHALL use all five custom header names

### Requirement: A BRP header is disabled and its value env var becomes optional when its name is set to blank
When a `brpApi.protocollering.headers.*` Helm value is set to an empty string, the corresponding `BRP_HEADER_*` env var is emitted as empty. The system SHALL not send that header. The corresponding value environment variable (`BRP_DOELBINDING_ZOEKMET` etc.) is NOT required by the Helm template and NOT validated by the application for that header.

| Header name env var | Corresponding value env vars |
|---|---|
| `BRP_HEADER_DOELBINDING` | `BRP_DOELBINDING_ZOEKMET`, `BRP_DOELBINDING_RAADPLEEGMET` |
| `BRP_HEADER_VERWERKING` | `BRP_VERWERKINGSREGISTER` |
| `BRP_HEADER_ORIGIN_OIN` | `BRP_ORIGIN_OIN` |
| `BRP_HEADER_GEBRUIKER` | *(none)* |
| `BRP_HEADER_TOEPASSING` | `BRP_TOEPASSING` |

#### Scenario: Header omitted when name Helm value is blank
- **WHEN** `BRP_ORIGIN_OIN` is set (protocollering enabled)
- **WHEN** `brpApi.protocollering.headers.doelbinding` is set to `""`
- **THEN** BRP requests SHALL NOT include any `x-doelbinding` header or any other doelbinding header
- **THEN** `BRP_DOELBINDING_ZOEKMET` and `BRP_DOELBINDING_RAADPLEEGMET` SHALL NOT be required by the Helm template

#### Scenario: Remaining headers sent when only one is disabled
- **WHEN** `BRP_ORIGIN_OIN` is set (protocollering enabled)
- **WHEN** `brpApi.protocollering.headers.verwerking` is set to `""`
- **WHEN** all other `brpApi.protocollering.headers.*` values are non-blank
- **THEN** BRP requests SHALL include all other protocollering headers (`x-doelbinding`, `x-origin-oin`, `x-gebruiker`, `x-toepassing`) but NOT `x-verwerking`

#### Scenario: Gebruiker header disabled suppresses 40-char truncation logic
- **WHEN** `brpApi.protocollering.headers.gebruiker` is set to `""`
- **THEN** the gebruiker header SHALL NOT be sent
- **THEN** no 40-character truncation logic SHALL be applied for the gebruiker header

### Requirement: Corresponding value env vars are mandatory when the header name is non-empty
When a `BRP_HEADER_*` env var is non-empty (protocollering enabled), the Helm template SHALL emit the corresponding value env var using `required`. The application SHALL also validate at startup that the corresponding value env var is present.

#### Scenario: Helm template fails when header enabled but value missing
- **WHEN** `brpApi.protocollering.headers.doelbinding` is non-empty
- **WHEN** `brpApi.protocollering.doelbinding.zoekmet` is not set
- **THEN** the Helm template SHALL fail to render with an error message

#### Scenario: Application startup fails when header enabled but value env var absent
- **WHEN** `BRP_HEADER_DOELBINDING` is non-empty
- **WHEN** `BRP_DOELBINDING_ZOEKMET` is absent from the environment
- **THEN** the application SHALL throw a `BrpProtocolleringConfigurationException` at startup

### Requirement: ZAC SHALL NOT inject the api key header
ZAC SHALL NOT inject an `x-api-key` header (or any authentication credential header) into BRP requests. Api key injection is the responsibility of the nginx proxy layer.

#### Scenario: No api key header in ZAC-originated BRP requests
- **WHEN** `BRP_ORIGIN_OIN` is set (protocollering enabled)
- **THEN** ZAC's BRP client SHALL NOT add any `x-api-key` header to the outgoing request

### Requirement: x-toepassing header is sent with a configurable value on protocollering-enabled BRP requests
The system SHALL inject an `x-toepassing` header (name configurable via `BRP_HEADER_TOEPASSING`) on every BRP request when protocollering is enabled. The value is set via `BRP_TOEPASSING`, which is required when `BRP_HEADER_TOEPASSING` is non-empty. The Helm chart provides a default value of `ZAC`.

#### Scenario: Default toepassing header sent with value ZAC
- **WHEN** `BRP_ORIGIN_OIN` is set (protocollering enabled)
- **WHEN** `BRP_HEADER_TOEPASSING` is non-empty (default: `x-toepassing`)
- **WHEN** `BRP_TOEPASSING` is `ZAC` (Helm default)
- **THEN** BRP requests SHALL include a header `x-toepassing: ZAC`

#### Scenario: Toepassing header value overridden via Helm
- **WHEN** `brpApi.protocollering.toepassing` is set to `MijnApp`
- **THEN** BRP requests SHALL include a header `x-toepassing: MijnApp`

#### Scenario: Toepassing header subject to 242-character truncation
- **WHEN** `BRP_TOEPASSING` is set to a string longer than 242 characters
- **THEN** the `x-toepassing` header value SHALL be truncated to 242 characters

### Requirement: Configurable header names are applied only when protocollering is enabled
The system SHALL only inject protocollering headers when `BRP_ORIGIN_OIN` is set. When protocollering is disabled, no protocollering headers SHALL be sent.

#### Scenario: No protocollering headers sent when protocollering is disabled
- **WHEN** `BRP_ORIGIN_OIN` is not set
- **THEN** BRP requests SHALL NOT include any of the protocollering headers (`x-doelbinding`, `x-verwerking`, `x-origin-oin`, `x-gebruiker`, `x-toepassing`)

### Requirement: Configurable header name applies to the gebruiker header size limit
The system SHALL apply the 40-character truncation limit to the gebruiker header identified by the configured name (defaulting to `x-gebruiker`), and the 242-character limit to all other protocollering headers.

#### Scenario: Gebruiker truncation uses configured header name
- **WHEN** `BRP_HEADER_GEBRUIKER` is set to `X-User`
- **THEN** the system SHALL apply the 40-character truncation limit to the `X-User` header value
- **THEN** all other protocollering headers SHALL use the 242-character truncation limit
