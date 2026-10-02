package io.shinhanlife.dat.mcg.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScaffoldingControllerOrganizationPreQueryTest {

    @TempDir
    Path root;

    @Test
    void createsOrganizationPreQueryAdapterInSelectedPod() throws Exception {
        createMciContract();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String workspacePath = root.toString().replace("\\", "\\\\");

        mockMvc.perform(post("/api/v1/scaffold/pre-query/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"workspacePath":"%s","moduleName":"dat-was-pro",
                                 "adapterName":"OrganizationLookup","interfaceId":"CLHTTP00005",
                                 "mciIoPrefix":"ONCSD1340","clientSystemCode":"ONCSD1340","requestFieldName":"employeeNo",
                                 "responseOrganizationNoFieldName":"organizationNo",
                                 "responseOrganizationNameFieldName":"organizationName"}
                                """.formatted(workspacePath)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Generated organization pre-query adapter")));

        Path adapter = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/common/impl/OrganizationLookupAdapterImpl.java");
        assertTrue(Files.exists(adapter));
    }

    private void createMciContract() throws Exception {
        Path mciDirectory = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/infra/itrf/mci/ncs/d");
        Files.createDirectories(mciDirectory.resolve("io"));
        Files.writeString(mciDirectory.resolve("MciNcsdClient.java"), "class MciNcsdClient {}");
        Files.writeString(mciDirectory.resolve("io/ONCSD1340_I.java"), "class ONCSD1340_I {}");
        Files.writeString(mciDirectory.resolve("io/ONCSD1340_O.java"), "class ONCSD1340_O {}");
    }

    @Test
    void automaticallyCreatesMissingMciContractFilesWhenScaffoldingPreQuery() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String workspacePath = root.toString().replace("\\", "\\\\");

        mockMvc.perform(post("/api/v1/scaffold/pre-query/organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                 {"workspacePath":"%s","moduleName":"dat-was-pro",
                                 "adapterName":"EmployeeLookup","interfaceId":"CTMNILO00007",
                                 "clientSystemCode":"ONILD0007","requestFieldName":"employeeName",
                                 "responseOrganizationNoFieldName":"employeeNo",
                                 "responseOrganizationNameFieldName":"departmentName"}
                                """.formatted(workspacePath)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Generated organization pre-query adapter")));

        Path reqIo = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/infra/itrf/mci/nil/d/io/ONILD0007_I.java");
        Path resIo = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/infra/itrf/mci/nil/d/io/ONILD0007_O.java");
        Path client = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/infra/itrf/mci/nil/d/MciNildClient.java");
        Path adapter = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/common/impl/EmployeeLookupAdapterImpl.java");
        Path adapterInterface = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/common/EmployeeLookupAdapter.java");

        assertTrue(Files.exists(reqIo));
        assertTrue(Files.exists(resIo));
        assertTrue(Files.exists(client));
        assertTrue(Files.exists(adapter));
        assertTrue(Files.exists(adapterInterface));

        String adapterContent = Files.readString(adapterInterface);
        assertTrue(adapterContent.contains("ONILD0007_O find(ONILD0007_I request);"));

        String reqContent = Files.readString(reqIo);
        assertTrue(reqContent.contains("private String employeeName;"));

        String resContent = Files.readString(resIo);
        assertTrue(resContent.contains("private String employeeNo;"));
        assertTrue(resContent.contains("private String departmentName;"));
    }

}
