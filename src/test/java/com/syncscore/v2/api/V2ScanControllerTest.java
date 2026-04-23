package com.syncscore.v2.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncscore.auth.domain.Role;
import com.syncscore.security.JwtService;
import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v2.service.V2ScanAsyncBridge;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class V2ScanControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired AgencyProfileRepository agencyRepo;
    @Autowired ObjectMapper objectMapper;

    @MockBean V2ScanAsyncBridge asyncBridge;

    String accessToken;
    AgencyProfile agency;

    @BeforeEach
    void setUp() {
        when(asyncBridge.runAsync(any(), any(), anyInt(), anyInt()))
                .thenReturn(CompletableFuture.completedFuture(null));

        UUID userId = UUID.randomUUID();
        agency = new AgencyProfile(userId, "Acme AI");
        agency.updateProfile("Acme AI", null, null, null, null, "acme-github", false);
        agencyRepo.save(agency);

        accessToken = jwtService.issueAccessToken(userId, "acmeuser", Set.of(Role.USER), Duration.ofHours(1));
    }

    @Test
    void triggerScan_unauthenticated_returns403() throws Exception {
        mvc.perform(post("/api/v2/scans/trigger").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggerScan_noBody_createsPublicScan() throws Exception {
        MvcResult result = mvc.perform(post("/api/v2/scans/trigger")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.evidenceSource").value("PUBLIC_EVIDENCE"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("agencyId");
    }

    @Test
    void triggerScan_withConfidentialGithubSource_createsConfidentialScan() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("source", "github", "exclusions", new String[]{"secrets", "pii"}, "customExclusions", ""));

        mvc.perform(post("/api/v2/scans/trigger")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.evidenceSource").value("CONFIDENTIAL_GITHUB"));
    }

    @Test
    void triggerScan_withInterviewSource_createsConfidentialScan() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("source", "interview", "exclusions", new String[]{}, "customExclusions", ""));

        mvc.perform(post("/api/v2/scans/trigger")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceSource").value("CONFIDENTIAL_INTERVIEW"));
    }

    @Test
    void getScan_returnsOwnScan() throws Exception {
        // Create scan first
        MvcResult triggerResult = mvc.perform(post("/api/v2/scans/trigger")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String scanId = objectMapper.readTree(triggerResult.getResponse().getContentAsString())
                .get("id").asText();

        mvc.perform(get("/api/v2/scans/" + scanId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scanId));
    }

    @Test
    void getScan_ofOtherAgency_returns403() throws Exception {
        // Create scan with primary user
        MvcResult triggerResult = mvc.perform(post("/api/v2/scans/trigger")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String scanId = objectMapper.readTree(triggerResult.getResponse().getContentAsString())
                .get("id").asText();

        // Different user tries to access it
        UUID otherId = UUID.randomUUID();
        AgencyProfile other = new AgencyProfile(otherId, "Other Agency");
        agencyRepo.save(other);
        String otherToken = jwtService.issueAccessToken(otherId, "otheruser", Set.of(Role.USER), Duration.ofHours(1));

        mvc.perform(get("/api/v2/scans/" + scanId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}
