# Shared Scroll Paging Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make newly Scaffold-generated MCI scroll paging implementations delegate common page-state handling to `dat-lib-datmt`.

**Architecture:** `dat-lib-datmt` provides a typed `ScrollPagingSupport` algorithm plus a small Adapter contract. Gateway's `ToolScaffolder` generates the target-system MCI mapping inside that Adapter and delegates defaults, continuation values, `hasMore`, and next-page construction to the common support.

**Tech Stack:** Java 21, Spring Boot, Gradle 8.14.3, Lombok, JUnit 5, Glow `ScrPageInfo`, DAT common paging library.

**Spec:** `docs/superpowers/specs/2026-09-21-scroll-paging-support-design.md`

## Global Constraints

- Generate this structure only for new MCI tools whose `pagingMode` is `SCROLL`.
- Do not add `scaffold.html` inputs or change `ScaffoldingController` request contracts.
- Apply `scrImhdNm=""`, `scrSortValu=""`, `pageDataCc=20`, and first-call `nextDataExtYn="Y"` when absent.
- Do not modify existing generated Pod sources.
- Generated scroll code must not import `java.lang.reflect.Method` or silently ignore reflection failures.
- `dat-lib-datmt` must be published as an artifact containing the new support classes before a newly generated Pod is built elsewhere.

## Review Focus

- Null Tool request must fail before any Adapter invocation; Task 1 tests this.
- A first request with no page object must receive blank defaults, count 20, and `Y`; Task 1 tests this.
- A continuation request must preserve non-null scroll key, interval, sort value, count, and its supplied next-data flag; Task 1 tests this.
- A null MCI body must yield a null Tool response and `hasMore=false`; Task 1 tests this.
- A generated SCROLL implementation must compile against typed single-value `ScrPageInfo` accessors without reflection; Task 2 tests generated text and compiles the library module.

---

## File Structure

### `dat-lib-datmt`

- Create: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/paging/ScrollPagingAdapter.java`
  - Typed bridge between a generated Pod's request/response/MCI call and common paging flow.
- Create: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/paging/ScrollPagingResult.java`
  - Adapter result containing Tool response and response `ScrPageInfo`.
- Create: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/paging/ScrollPagingSupport.java`
  - Stateless common algorithm for defaulting, continuation state, `hasMore`, and next paging information.
- Create: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/paging/ScrollPagingSupportTest.java`
  - Unit tests for the common algorithm.

### `axhub-backend-main`

- Modify: `dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java:2719-2910`
  - Replace generated Reflection-based SCROLL implementation template with typed Adapter delegation.
- Modify: `dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java:20-65`
  - Verify generated SCROLL output imports and uses common support and contains no Reflection logic.

## Task 1: Add common typed scroll paging support

**Files:**
- Create: `C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt\dat-was-lib\src\main\java\io\shinhanlife\dat\lib\paging\ScrollPagingAdapter.java`
- Create: `C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt\dat-was-lib\src\main\java\io\shinhanlife\dat\lib\paging\ScrollPagingResult.java`
- Create: `C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt\dat-was-lib\src\main\java\io\shinhanlife\dat\lib\paging\ScrollPagingSupport.java`
- Test: `C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt\dat-was-lib\src\test\java\io\shinhanlife\dat\lib\paging\ScrollPagingSupportTest.java`

**Interfaces:**
- Produces `ScrollPagingAdapter<REQUEST, RESPONSE>` with `getRequestPageInfo`, `setRequestPageInfo`, `invoke`, and `setHasMore` methods.
- Produces `ScrollPagingResult<RESPONSE>(RESPONSE response, ScrPageInfo responsePageInfo)`.
- Produces `ScrollPagingSupport.execute(REQUEST request, ScrollPagingInfo pagingInfo, ScrollPagingAdapter<REQUEST, RESPONSE> adapter)` returning `MciPage<RESPONSE, ScrollPagingInfo>`.

- [ ] **Step 1: Write failing common-support tests**

```java
@Test
void appliesFirstPageDefaultsAndReturnsNoContinuationWhenMciHasNoNextPage() {
    SampleRequest request = new SampleRequest(null);
    SampleAdapter adapter = new SampleAdapter(new ScrPageInfo());

    MciPage<SampleResponse, ScrollPagingInfo> page =
            new ScrollPagingSupport().execute(request, null, adapter);

    assertEquals("", adapter.invokedPageInfo.getScrImhdNm());
    assertEquals("", adapter.invokedPageInfo.getScrSortValu());
    assertEquals(20, adapter.invokedPageInfo.getPageDataCc());
    assertEquals("Y", adapter.invokedPageInfo.getNextDataExtYn());
    assertFalse(page.hasMore());
}

@Test
void appliesContinuationValuesAndBuildsNextScrollPagingInfo() {
    ScrollPagingInfo continuation = new ScrollPagingInfo("fundCd", "before", "fundCd=0", true, 5);
    ScrPageInfo responsePage = new ScrPageInfo();
    responsePage.setNextDataExtYn("Y");
    responsePage.setScrItva("after");
    responsePage.setScrSortValu("fundCd=5");

    MciPage<SampleResponse, ScrollPagingInfo> page = new ScrollPagingSupport().execute(
            new SampleRequest(new ScrPageInfo()), continuation, new SampleAdapter(responsePage));

    assertEquals("fundCd", page.pagingInfo().getScrImhdNm());
    assertEquals("after", page.pagingInfo().getScrItva());
    assertEquals("fundCd=5", page.pagingInfo().getScrSortValu());
    assertTrue(page.hasMore());
}
```

- [ ] **Step 2: Run the tests and verify they fail because the support classes do not exist**

Run:

```powershell
cd C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt
.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.paging.ScrollPagingSupportTest" --no-daemon
```

Expected: compilation failure naming missing `ScrollPagingSupport`, `ScrollPagingAdapter`, or `ScrollPagingResult`.

- [ ] **Step 3: Create the Adapter and result contracts**

```java
public interface ScrollPagingAdapter<REQUEST, RESPONSE> {
    ScrPageInfo getRequestPageInfo(REQUEST request);
    void setRequestPageInfo(REQUEST request, ScrPageInfo pageInfo);
    ScrollPagingResult<RESPONSE> invoke(REQUEST request, ScrPageInfo pageInfo);
    void setHasMore(RESPONSE response, boolean hasMore);
}

public record ScrollPagingResult<RESPONSE>(RESPONSE response, ScrPageInfo responsePageInfo) {
}
```

- [ ] **Step 4: Implement the common support algorithm**

```java
public final class ScrollPagingSupport {
    public static final int DEFAULT_PAGE_DATA_COUNT = 20;
    public static final String DEFAULT_SCR_IMHD_NM = "";
    public static final String DEFAULT_SCR_SORT_VALU = "";

    public <REQUEST, RESPONSE> MciPage<RESPONSE, ScrollPagingInfo> execute(
            REQUEST request, ScrollPagingInfo pagingInfo,
            ScrollPagingAdapter<REQUEST, RESPONSE> adapter) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (adapter == null) {
            throw new IllegalArgumentException("adapter is required");
        }
        ScrPageInfo requestPage = adapter.getRequestPageInfo(request);
        if (requestPage == null) {
            requestPage = new ScrPageInfo();
        }
        if (requestPage.getPageDataCc() <= 0) {
            requestPage.setPageDataCc(DEFAULT_PAGE_DATA_COUNT);
        }
        if (requestPage.getScrImhdNm() == null || requestPage.getScrImhdNm().isBlank()) {
            requestPage.setScrImhdNm(DEFAULT_SCR_IMHD_NM);
        }
        if (requestPage.getScrSortValu() == null || requestPage.getScrSortValu().isBlank()) {
            requestPage.setScrSortValu(DEFAULT_SCR_SORT_VALU);
        }
        if (pagingInfo == null) {
            requestPage.setNextDataExtYn("Y");
        } else {
            if (pagingInfo.getScrImhdNm() != null) requestPage.setScrImhdNm(pagingInfo.getScrImhdNm());
            if (pagingInfo.getScrItva() != null) requestPage.setScrItva(pagingInfo.getScrItva());
            if (pagingInfo.getScrSortValu() != null) requestPage.setScrSortValu(pagingInfo.getScrSortValu());
            if (pagingInfo.getPageDataCc() > 0) requestPage.setPageDataCc(pagingInfo.getPageDataCc());
            requestPage.setNextDataExtYn(
                    pagingInfo.getNextDataExtYn() == null ? "Y" : pagingInfo.getNextDataExtYn());
        }
        adapter.setRequestPageInfo(request, requestPage);
        ScrollPagingResult<RESPONSE> result = adapter.invoke(request, requestPage);
        if (result == null) {
            throw new IllegalStateException("scroll paging adapter result is required");
        }
        ScrPageInfo responsePage = result.responsePageInfo();
        boolean hasMore = responsePage != null
                && "Y".equalsIgnoreCase(responsePage.getNextDataExtYn());
        if (result.response() != null) {
            adapter.setHasMore(result.response(), hasMore);
        }
        ScrollPagingInfo nextPaging = new ScrollPagingInfo(
                requestPage.getScrImhdNm(),
                responsePage == null ? null : responsePage.getScrItva(),
                responsePage == null ? null : responsePage.getScrSortValu(),
                hasMore,
                requestPage.getPageDataCc());
        return new MciPage<>(result.response(), nextPaging);
    }
}
```

The completed implementation must use the exact default and continuation rules
described in the spec, must call `adapter.setHasMore` only when response is not
null, and must treat missing response page data as `hasMore=false`.

- [ ] **Step 5: Run the focused common-support test and verify it passes**

Run:

```powershell
cd C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt
.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.paging.ScrollPagingSupportTest" --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the isolated common-library change**

```powershell
git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/paging/ScrollPagingAdapter.java dat-was-lib/src/main/java/io/shinhanlife/dat/lib/paging/ScrollPagingResult.java dat-was-lib/src/main/java/io/shinhanlife/dat/lib/paging/ScrollPagingSupport.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/paging/ScrollPagingSupportTest.java
git commit -m "feat(paging): add shared scroll paging support"
```

## Task 2: Generate typed common-support delegation for new SCROLL tools

**Files:**
- Modify: `C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main\dat-was-lib\src\main\java\io\shinhanlife\dat\lib\util\ToolScaffolder.java:2719-2910`
- Modify: `C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main\dat-was-lib\src\test\java\io\shinhanlife\dat\lib\util\ToolScaffolderTest.java:20-65`

**Interfaces:**
- Consumes Task 1's `ScrollPagingSupport`, `ScrollPagingAdapter`, and `ScrollPagingResult`.
- Produces generated `{BaseName}ScrollPagingImpl.fetch({BaseName}Request, ScrollPagingInfo)` code that returns `MciPage<{BaseName}Response, ScrollPagingInfo>`.

- [ ] **Step 1: Extend the existing SCROLL Scaffold test with failing assertions**

```java
assertTrue(pagingImplSource.contains("import io.shinhanlife.dat.lib.paging.ScrollPagingSupport;"));
assertTrue(pagingImplSource.contains("import io.shinhanlife.dat.lib.paging.ScrollPagingAdapter;"));
assertTrue(pagingImplSource.contains("import io.shinhanlife.dat.lib.paging.ScrollPagingResult;"));
assertTrue(pagingImplSource.contains("scrollPagingSupport.execute(request, pagingInfo"));
assertFalse(pagingImplSource.contains("java.lang.reflect.Method"));
assertFalse(pagingImplSource.contains("catch (Exception ignored)"));
```

- [ ] **Step 2: Run the single Scaffold test and verify it fails against the Reflection template**

Run:

```powershell
cd C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main
.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.util.ToolScaffolderTest.generatesScrollPagingAdapterOnlyWhenScrollPagingIsSelected" --no-daemon
```

Expected: assertion failure because generated implementation imports `Method` and does not use `ScrollPagingSupport`.

- [ ] **Step 3: Replace only the SCROLL implementation template**

Generate these typed members in `{BaseName}ScrollPagingImpl`:

```java
private final ScrollPagingSupport scrollPagingSupport = new ScrollPagingSupport();

@Override
public MciPage<ContractListResponse, ScrollPagingInfo> fetch(
        ContractListRequest request, ScrollPagingInfo pagingInfo) {
    return scrollPagingSupport.execute(request, pagingInfo,
            new ScrollPagingAdapter<>() {
                @Override
                public ScrPageInfo getRequestPageInfo(ContractListRequest source) {
                    return source.getScrPageInfo();
                }

                @Override
                public void setRequestPageInfo(ContractListRequest source, ScrPageInfo pageInfo) {
                    source.setScrPageInfo(pageInfo);
                }

                @Override
                public ScrollPagingResult<ContractListResponse> invoke(
                        ContractListRequest source, ScrPageInfo pageInfo) {
                    ONBTA2380_I mciRequest = converter.toRequest(source);
                    mciRequest.setScrPageInfo(pageInfo);
                    Transfer<ONBTA2380_O> transfer = mci.callTo(
                            "ONBTA2380", "ONBTA2380", mciRequest, ONBTA2380_O.class);
                    ONBTA2380_O mciResponse = transfer == null ? null : transfer.getBody();
                    return new ScrollPagingResult<>(converter.toResponse(mciResponse),
                            mciResponse == null ? null : mciResponse.getScrPageInfo());
                }

                @Override
                public void setHasMore(ContractListResponse response, boolean hasMore) {
                    response.setHasMore(hasMore);
                }
            });
}
```

Use the actual generated base name, MCI client, Converter, request/response
types, interface ID, receive service ID, and generated MCI request/response
classes. Do not alter the `PAGE_NUMBER` branch or the `NONE` branch.

- [ ] **Step 4: Run the focused Scaffold test and verify it passes**

Run:

```powershell
cd C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main
.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.util.ToolScaffolderTest.generatesScrollPagingAdapterOnlyWhenScrollPagingIsSelected" --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Compile Gateway and inspect the generated-source contract**

Run:

```powershell
.\gradlew.bat :dat-gateway:compileJava --no-daemon
git diff --check
```

Expected: Gateway compilation succeeds and no whitespace errors are reported.

- [ ] **Step 6: Commit only the Scaffold template and its test**

```powershell
git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java
git commit -m "feat(scaffold): delegate scroll paging to common support"
```

## Task 3: Package and verify the common artifact

**Files:**
- Modify: none.
- Verify: common library artifact and generated Scaffold source contract.

**Interfaces:**
- Consumes Task 1 compiled support classes and Task 2 generated source imports.
- Produces a local common-library JAR containing the support API for later Nexus publication.

- [ ] **Step 1: Build the common library JAR**

Run:

```powershell
cd C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt
.\gradlew.bat :dat-was-lib:jar --no-daemon
Get-ChildItem .\dat-was-lib\build\libs\*.jar
```

Expected: a `dat-lib-datmt` library JAR is present under `dat-was-lib/build/libs`.

- [ ] **Step 2: Run the full common-library suite and record any existing failures separately**

Run:

```powershell
.\gradlew.bat :dat-was-lib:test --no-daemon
```

Expected: report the exact failing tests if the suite is not green; do not attribute existing failures to this change without evidence.

- [ ] **Step 3: Publish only after explicit user authorization**

Run after the user provides the Nexus target and credentials mechanism:

```powershell
.\gradlew.bat :dat-was-lib:publish --no-daemon
```

Expected: the artifact version consumed by generated Pods contains `ScrollPagingSupport`, `ScrollPagingAdapter`, and `ScrollPagingResult`.

## Self-Review

- Spec coverage: Task 1 owns the shared algorithm and errors; Task 2 owns typed future Scaffold output; Task 3 owns JAR delivery and explicit publication authorization.
- Placeholder scan: no implementation step relies on an unspecified class or method name.
- Type consistency: Task 2 imports all three types defined in Task 1; all generated `fetch` methods return `MciPage<RESPONSE, ScrollPagingInfo>`.
- Review focus coverage: Task 1 contains the four request/continuation/null-response cases; Task 2 contains the generated no-Reflection case.
