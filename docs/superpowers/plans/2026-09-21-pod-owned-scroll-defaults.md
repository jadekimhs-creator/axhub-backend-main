# Pod-Owned Scroll Paging Defaults Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Move target-system scroll item and sort defaults from the common library into each generated Pod, then refresh the local common-library artifact so CusSearchScrollPagingImpl compiles.

**Architecture:** ScrollPagingSupport accepts two defaults supplied by its caller. The SCROLL Scaffold and manually created PRO implementation own DEFAULT_SCR_IMHD_NM and DEFAULT_SCR_SORT_VALU; the common lib retains only the reusable state algorithm. Maven Local is refreshed before rebuilding PRO.

**Tech Stack:** Java 21, Gradle 8.14.3, JUnit 5, Maven Local, Spring Boot.

**Spec:** docs/superpowers/specs/2026-09-21-scroll-paging-support-design.md

## Global Constraints

- ScrollPagingSupport must not declare DEFAULT_SCR_IMHD_NM or DEFAULT_SCR_SORT_VALU.
- New SCROLL Scaffold output owns both constants with empty-string values.
- Do not add a Scaffold HTML input or alter controller contracts.
- Preserve common pageDataCc=20 and initial nextDataExtYn="Y".
- Do not change PAGE_NUMBER or NONE generation.
- Publish only to Maven Local; do not publish to Nexus.

## Review Focus

- Null/blank caller defaults normalize to empty strings; Task 1 tests this.
- Continuation values override Pod defaults; Task 1 tests this.
- SCROLL output declares and passes local constants without reflection; Task 2 tests this.
- CusSearchScrollPagingImpl compiles against refreshed Maven Local; Task 3 tests this.
- Maven Local JAR contains ScrollPagingSupport.class; Task 3 checks this.

## File Structure

- dat-lib-datmt/dat-was-lib/.../paging/ScrollPagingSupport.java: generic state algorithm and caller-default parameters.
- dat-lib-datmt/dat-was-lib/.../paging/ScrollPagingSupportTest.java: default and continuation behavior.
- axhub-backend-main/dat-was-lib/.../util/ToolScaffolder.java: SCROLL-only local constants and invocation.
- axhub-backend-main/dat-was-lib/.../util/ToolScaffolderTest.java: generated output contract.
- dat-was-datps/dat-was-pro/.../CusSearchScrollPagingImpl.java: manually created PRO contract.

### Task 1: Make common support accept Pod defaults

**Files:**
- Modify: C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt\dat-was-lib\src\main\java\io\shinhanlife\dat\lib\paging\ScrollPagingSupport.java
- Modify: C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt\dat-was-lib\src\test\java\io\shinhanlife\dat\lib\paging\ScrollPagingSupportTest.java

**Interfaces:**
- Produces execute(REQUEST request, ScrollPagingInfo pagingInfo, String defaultScrImhdNm, String defaultScrSortValu, ScrollPagingAdapter<REQUEST, RESPONSE> adapter).
- Removes only the two target-system default constants from the common library.

- [ ] **Step 1: Write failing default and continuation tests**

~~~java
MciPage<SampleResponse, ScrollPagingInfo> page = new ScrollPagingSupport().execute(
        request, null, "fundCd", "fundCd=0", adapter);
assertEquals("fundCd", adapter.invokedPageInfo.getScrImhdNm());
assertEquals("fundCd=0", adapter.invokedPageInfo.getScrSortValu());

new ScrollPagingSupport().execute(new SampleRequest(new ScrPageInfo()),
        new ScrollPagingInfo("contractNo", "before", "contractNo=5", true, 5),
        "fundCd", "fundCd=0", adapter);
assertEquals("contractNo", adapter.invokedPageInfo.getScrImhdNm());
assertEquals("contractNo=5", adapter.invokedPageInfo.getScrSortValu());
~~~

- [ ] **Step 2: Run the focused test to verify RED**

Run: cd C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt; .\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.paging.ScrollPagingSupportTest" --no-daemon

Expected: current execute signature rejects the two added string arguments. If unrelated test-source compilation errors occur first, record them and use production compilation after GREEN.

- [ ] **Step 3: Implement caller-owned defaults**

~~~java
public <REQUEST, RESPONSE> MciPage<RESPONSE, ScrollPagingInfo> execute(
        REQUEST request, ScrollPagingInfo pagingInfo,
        String defaultScrImhdNm, String defaultScrSortValu,
        ScrollPagingAdapter<REQUEST, RESPONSE> adapter) {
    applyDefaults(requestPageInfo,
            defaultScrImhdNm == null ? "" : defaultScrImhdNm,
            defaultScrSortValu == null ? "" : defaultScrSortValu);
}

private void applyDefaults(ScrPageInfo pageInfo, String defaultScrImhdNm,
        String defaultScrSortValu) {
    if (pageInfo.getPageDataCc() <= 0) pageInfo.setPageDataCc(DEFAULT_PAGE_DATA_COUNT);
    if (pageInfo.getScrImhdNm() == null || pageInfo.getScrImhdNm().isBlank()) pageInfo.setScrImhdNm(defaultScrImhdNm);
    if (pageInfo.getScrSortValu() == null || pageInfo.getScrSortValu().isBlank()) pageInfo.setScrSortValu(defaultScrSortValu);
}
~~~

- [ ] **Step 4: Verify GREEN and production compilation**

Run:
~~~powershell
.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.paging.ScrollPagingSupportTest" --no-daemon
.\gradlew.bat :dat-was-lib:compileJava --no-daemon
~~~

Expected: test passes unless documented unrelated test-source errors block it; production compilation succeeds.

- [ ] **Step 5: Commit**

~~~powershell
git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/paging/ScrollPagingSupport.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/paging/ScrollPagingSupportTest.java
git commit -m "feat(paging): accept pod scroll defaults"
~~~

### Task 2: Emit defaults in new SCROLL Pods

**Files:**
- Modify: C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main\dat-was-lib\src\main\java\io\shinhanlife\dat\lib\util\ToolScaffolder.java
- Modify: C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main\dat-was-lib\src\test\java\io\shinhanlife\dat\lib\util\ToolScaffolderTest.java

**Interfaces:** Consumes Task 1 five-argument execute; produces constants inside SCROLL implementations only.

- [ ] **Step 1: Add failing generated-source assertions**

~~~java
assertTrue(pagingImplSource.contains("private static final String DEFAULT_SCR_IMHD_NM = \\"\\";"));
assertTrue(pagingImplSource.contains("private static final String DEFAULT_SCR_SORT_VALU = \\"\\";"));
assertTrue(pagingImplSource.contains(
        "scrollPagingSupport.execute(request, pagingInfo, DEFAULT_SCR_IMHD_NM, DEFAULT_SCR_SORT_VALU"));
~~~

- [ ] **Step 2: Run RED**

Run: cd C:\eGovFrameDev-4.3.1-64bit\workspace-egov\axhub-backend-main; .\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.util.ToolScaffolderTest.generatesScrollPagingAdapterOnlyWhenScrollPagingIsSelected" --no-daemon

Expected: constants and expanded invocation are absent.

- [ ] **Step 3: Change only the SCROLL template**

~~~java
private static final String DEFAULT_SCR_IMHD_NM = "";
private static final String DEFAULT_SCR_SORT_VALU = "";

return scrollPagingSupport.execute(request, pagingInfo,
        DEFAULT_SCR_IMHD_NM, DEFAULT_SCR_SORT_VALU,
        new ScrollPagingAdapter<>() { /* existing typed MCI adapter */ });
~~~

- [ ] **Step 4: Verify GREEN**

Run:
~~~powershell
.\gradlew.bat :dat-was-lib:test --tests "io.shinhanlife.dat.lib.util.ToolScaffolderTest.generatesScrollPagingAdapterOnlyWhenScrollPagingIsSelected" --no-daemon
.\gradlew.bat :dat-gateway:compileJava --no-daemon
git diff --check
~~~

Expected: all commands succeed.

- [ ] **Step 5: Commit**

~~~powershell
git add dat-was-lib/src/main/java/io/shinhanlife/dat/lib/util/ToolScaffolder.java dat-was-lib/src/test/java/io/shinhanlife/dat/lib/util/ToolScaffolderTest.java
git commit -m "feat(scaffold): emit pod scroll defaults"
~~~

### Task 3: Refresh Maven Local and repair PRO

**Files:**
- Modify: C:\eGovFrameDev-4.3.1-64bit\workspace\dat-was-datps\dat-was-pro\src\main\java\io\shinhanlife\dat\mcc\biz\pro\paging\impl\CusSearchScrollPagingImpl.java

**Interfaces:** Consumes Task 1 execute signature and Maven Local io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT; produces a compiling manually created PRO implementation.

- [ ] **Step 1: Adopt the generated contract in Cus**

~~~java
private static final String DEFAULT_SCR_IMHD_NM = "";
private static final String DEFAULT_SCR_SORT_VALU = "";

return scrollPagingSupport.execute(request, pagingInfo,
        DEFAULT_SCR_IMHD_NM, DEFAULT_SCR_SORT_VALU,
        new ScrollPagingAdapter<>() { /* existing adapter body */ });
~~~

- [ ] **Step 2: Publish only to Maven Local and inspect the artifact**

Run:
~~~powershell
cd C:\eGovFrameDev-4.3.1-64bit\workspace\dat-lib-datmt
.\gradlew.bat :dat-was-lib:publishToMavenLocal --no-daemon
jar tf "$env:USERPROFILE\.m2\repository\io\shinhanlife\dat-lib-datmt\0.0.1-SNAPSHOT\dat-lib-datmt-0.0.1-SNAPSHOT.jar" | Select-String "ScrollPagingSupport.class"
~~~

Expected: Maven Local JAR lists io/shinhanlife/dat/lib/paging/ScrollPagingSupport.class.

- [ ] **Step 3: Compile PRO using the refreshed artifact**

Run:
~~~powershell
cd C:\eGovFrameDev-4.3.1-64bit\workspace\dat-was-datps
.\gradlew.bat :dat-was-pro:compileJava --refresh-dependencies --no-daemon
~~~

Expected: all three paging imports in CusSearchScrollPagingImpl resolve and PRO compiles.

- [ ] **Step 4: Commit only the PRO source**

~~~powershell
git add dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/biz/pro/paging/impl/CusSearchScrollPagingImpl.java
git commit -m "fix(pro): configure Cus scroll paging defaults"
~~~

## Self-Review

- Spec coverage: Task 1 removes common target defaults; Task 2 adds them to future Pods; Task 3 updates the manual Cus implementation and repairs the stale Maven Local artifact.
- Placeholder scan: every code task has concrete paths, signatures, commands, and expected output.
- Type consistency: Task 1 inserts the two String defaults immediately before ScrollPagingAdapter; Tasks 2 and 3 call that exact order.
- Review focus: Task 1 covers defaults and continuation; Task 2 covers generated code; Task 3 covers the runtime artifact and PRO compilation.

