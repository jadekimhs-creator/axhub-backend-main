package io.shinhanlife.dat.lib.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OrganizationPreQueryScaffolderTest {

    @TempDir
    Path root;

    @Test
    void generatesPodCommonOrganizationAdapterUsingExistingMciIo() throws Exception {
        System.setProperty("AXHUB_SOURCE_DIR", root.toString());
        createMciContract();

        OrganizationPreQueryScaffolder.scaffold(new OrganizationPreQueryScaffolder.Definition(
                "dat-was-pro", "OrganizationLookup", "CLHTTP00005", "ONCSD1340", "ONCSD1340",
                "employeeNo", "organizationNo", "organizationName"));

        Path adapterDir = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/common");
        assertTrue(Files.exists(adapterDir.resolve("OrganizationLookupAdapter.java")));

        String adapterInterface = Files.readString(adapterDir.resolve("OrganizationLookupAdapter.java"));
        assertTrue(adapterInterface.contains("ONCSD1340_O find(ONCSD1340_I request);"));

        Path implFile = adapterDir.resolve("impl/OrganizationLookupAdapterImpl.java");
        assertTrue(Files.exists(implFile));

        String implementation = Files.readString(implFile);
        assertTrue(implementation.contains("package io.shinhanlife.dat.mcc.common.impl;"));
        assertTrue(implementation.contains("import io.shinhanlife.dat.mcc.common.OrganizationLookupAdapter;"));
        assertTrue(implementation.contains("public class OrganizationLookupAdapterImpl implements OrganizationLookupAdapter"));
        assertTrue(implementation.contains("import io.shinhanlife.dat.mcc.infra.itrf.mci.ncs.d.MciNcsdClient;"));
        assertTrue(implementation.contains("import io.shinhanlife.dat.mcc.infra.itrf.mci.ncs.d.io.ONCSD1340_I;"));
        assertTrue(implementation.contains("import io.shinhanlife.dat.mcc.infra.itrf.mci.ncs.d.io.ONCSD1340_O;"));
        assertTrue(implementation.contains("private static final String INTERFACE_ID = \"CLHTTP00005\";"));
        assertTrue(implementation.contains("private static final String RECEIVE_SERVICE_ID = \"ONCSD1340\";"));
        assertTrue(implementation.contains("public ONCSD1340_O find(ONCSD1340_I request)"));
        assertTrue(implementation.contains("mci.callTo(INTERFACE_ID, RECEIVE_SERVICE_ID, request, ONCSD1340_O.class)"));
    }

    @Test
    void rejectsGenerationWhenSelectedMciContractDoesNotExist() {
        System.setProperty("AXHUB_SOURCE_DIR", root.toString());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                OrganizationPreQueryScaffolder.scaffold(new OrganizationPreQueryScaffolder.Definition(
                        "dat-was-pro", "OrganizationLookup", "CLHTTP00005", "ONCSD1340", "ONCSD1340",
                        "employeeNo", "organizationNo", "organizationName")));

        assertTrue(exception.getMessage().contains("ONCSD1340_I"));
    }

    private void createMciContract() throws Exception {
        Path mciDirectory = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/infra/itrf/mci/ncs/d");
        Files.createDirectories(mciDirectory.resolve("io"));
        Files.writeString(mciDirectory.resolve("MciNcsdClient.java"), "class MciNcsdClient {}");
        Files.writeString(mciDirectory.resolve("io/ONCSD1340_I.java"), "class ONCSD1340_I {}");
        Files.writeString(mciDirectory.resolve("io/ONCSD1340_O.java"), "class ONCSD1340_O {}");
    }
}
