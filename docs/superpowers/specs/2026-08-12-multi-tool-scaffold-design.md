# Multi-tool Scaffold Design

## Goal

Extend the Tool Scaffold so one UseCase can expose multiple MCP Tools, and each
Tool can call its own typed integration Client using the same flow as
`CustomerGuidanceToolUseCaseImpl`.

## Generated structure

For a Scaffold group named `Customer`, the generator creates one UseCase and
implementation, with one method per Tool:

```java
public interface CustomerUseCase {
    @McpTool(name = "cmm_customer_guidance", ...)
    CustomerGuidanceResponse searchGuidance(CustomerGuidanceRequest request);

    @McpTool(name = "cmm_customer_contract", ...)
    CustomerContractResponse searchContract(CustomerContractRequest request);
}
```

Each Tool has independent metadata:

- MCP Tool name, title, description, category and registration flag
- Java method name
- integration interface ID
- Client class and Client method name
- request and response fields
- generated V17 Tool definition YAML

The implementation follows the CustomerGuidance pattern per method:

```text
Tool Request -> MapStruct Converter -> Typed *Client -> MapStruct Converter -> Tool Response
```

For MCI, the generator creates a typed Client method such as
`callOnild0320(ONILD0320_I)` rather than having the UseCase call generic
`callTo(...)` directly. The Client remains the only layer that calls
`AxhubMciComponent`.

## Field model

Existing scalar types remain supported: `String`, `Integer`, `Long`, `Double`,
`Boolean`, and `BigDecimal`.

Two structured choices are added:

1. **Enum**: the user supplies allowed values. The generator creates a named
   enum class beside the DTO, uses it as the field type, and emits the same
   values in the MCP parameter schema.
2. **List**: the user selects an item kind.
   - Primitive lists generate e.g. `List<String>`.
   - Object lists contain user-entered item fields and generate a separate
     `...Item` DTO plus `List<...Item>`.

The Scaffold UI presents enum values and list item fields in dedicated dialogs,
instead of asking the user to hand-author Java or JSON type expressions.

## Compatibility

- The existing one-Tool request payload continues to work and generates the
  current single-Tool shape.
- The new multi-Tool payload is additive and is used by the updated UI.
- Existing generated source is not rewritten.
- Each generated Tool continues to receive its own V17 YAML file, which keeps
  runtime tool discovery and validation unchanged.

## Validation and tests

- Validate unique Tool names and Java method names within a UseCase group.
- Validate a Client class/method and interface ID for each MCI Tool.
- Validate enum values and object-list item fields.
- Add generator tests for multiple methods, typed MCI Client calls, enum DTOs,
  primitive lists, object lists, and V17 schema output.
