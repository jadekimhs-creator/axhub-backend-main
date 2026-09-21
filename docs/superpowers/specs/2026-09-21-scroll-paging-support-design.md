# Scroll Paging Support and Scaffold Design

## Goal

New MCI tools generated with the `SCROLL` paging mode must use a shared paging
flow. Generated Pod code must contain only the target-system request/response
mapping and MCI invocation details. Existing generated Pods are out of scope.

## Scope

- Add generic scroll-paging support types to the `dat-lib-datmt` common library.
- Update the AX Hub `ToolScaffolder` template used by Gateway to generate a
  small typed adapter and a `ScrollPagingImpl` that delegates to the common
  support.
- Do not add Scaffold UI inputs. Each generated Pod owns empty-string defaults
  for scroll item name and sort value; page size remains 20, and the first
  request uses `Y`.
- Do not modify any existing generated Pod source.

## Component Boundary

`dat-lib-datmt` owns the reusable paging algorithm, but not target-system
scroll defaults.

- `ScrollPagingSupport`: creates and completes `ScrPageInfo`, applies the
  caller-provided initial defaults and continuation values, computes `hasMore`,
  and returns `MciPage`. It must not define `DEFAULT_SCR_IMHD_NM` or
  `DEFAULT_SCR_SORT_VALU`.
- `ScrollPagingAdapter<REQUEST, RESPONSE>`: target-system boundary supplied by
  generated Pod code. It reads/writes a Tool request page object, invokes MCI,
  extracts the response page object, and sets the Tool response `hasMore`.
- `ScrollPagingResult<RESPONSE>`: returns the Tool response and MCI response
  page object from the Adapter to the support.

The generated Pod owns typed MCI DTO calls and two defaults:
`DEFAULT_SCR_IMHD_NM` and `DEFAULT_SCR_SORT_VALU`. It passes those values to
`ScrollPagingSupport` for each `fetch` call. It directly calls the generated
`setScrPageInfo(...)` and `getScrPageInfo()` methods. Reflection and silently
ignored exceptions are not generated.

## Request and Response Flow

1. `ScrollPagingSupport` obtains or creates `ScrPageInfo` from the Tool
   request through the Adapter.
2. It applies `pageDataCc=20` and the generated Pod's `scrImhdNm` and
   `scrSortValu` defaults only when the corresponding value is absent.
3. For the first call it sends `nextDataExtYn="Y"`. For a continuation call it
   uses the incoming `ScrollPagingInfo.nextDataExtYn`, with `Y` as fallback.
4. The Adapter converts the Tool request to the MCI request, attaches the page
   information using the declared DTO shape, calls MCI, converts the response,
   and returns the MCI response page information.
5. The support interprets response `nextDataExtYn`, writes `hasMore` to the
   Tool response, and produces the next `ScrollPagingInfo`.

## Error Handling

- Null Tool requests and null Adapter results fail fast with descriptive
  `IllegalArgumentException` or `IllegalStateException` messages.
- Generated adapters do not swallow failures caused by incompatible generated
  DTOs. Compile-time type errors are preferred over runtime reflection.
- A null MCI body results in a null Tool response and a `hasMore=false` next
  page state unless the Adapter explicitly supplies a response page object.

## Compatibility and Delivery

- Existing `ScrollPagingInfo`, `MciPage`, and `AutoPagingExecutor` remain
  compatible.
- New generated Pods require a `dat-lib-datmt` artifact that contains the new
  support types. The common-library artifact must be published to Maven Local
  or Nexus before those Pods are built. A cached `0.0.1-SNAPSHOT` artifact does
  not automatically contain newly compiled classes: publish the updated JAR,
  refresh Gradle dependencies, then rebuild the Pod.
- Existing Pods retain their current implementations and are not migrated.

## Verification

- Common-library unit tests cover caller-provided defaults, initial `Y`,
  continuation values, `hasMore`, and next-page values.
- `ToolScaffolderTest` verifies that a SCROLL scaffold emits an implementation
  using `ScrollPagingSupport` and no reflection imports or ignored reflection
  exceptions.
- Compile the Gateway and run the focused common-library and Scaffold tests.
- The full existing `ToolScaffolderTest` suite has known unrelated generated
  path expectation failures; report them separately if they remain.
