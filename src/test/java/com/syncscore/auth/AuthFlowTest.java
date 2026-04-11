package com.syncscore.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.syncscore.auth.service.dto.SignupCompleteRequest;
import com.syncscore.auth.service.dto.SignupStartEmailRequest;
import com.syncscore.auth.service.dto.SignupVerifyOtpRequest;
import com.syncscore.auth.service.dto.TokenPairResponse;
import com.syncscore.auth.service.dto.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {
    @Autowired
    MockMvc mvc;

    @Test
    void emailSignupThenCompleteThenMe() throws Exception {
        ObjectMapper om = new ObjectMapper();
        String email = "u1@example.com";
        String startBody = om.writeValueAsString(new SignupStartEmailRequest(email));
        mvc.perform(
                        post("/api/v1/auth/signup/email/start")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(startBody)
                )
                .andExpect(status().isOk())
                .andReturn();

        mvc.perform(
                        post("/api/v1/auth/signup/email/resend-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(startBody)
                )
                .andExpect(status().isTooManyRequests());

        String otp = otpSender.lastOtpFor(email);
        assertThat(otp).isNotBlank();

        String verifyBody = om.writeValueAsString(new SignupVerifyOtpRequest(email, otp));
        String verifyResp = mvc.perform(
                        post("/api/v1/auth/signup/email/verify-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(verifyBody)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TokenResponse start = om.readValue(verifyResp, TokenResponse.class);

        String completeBody = om.writeValueAsString(new SignupCompleteRequest("user1", "password123"));
        String completeResp = mvc.perform(
                        post("/api/v1/auth/signup/complete")
                                .header("Authorization", "Bearer " + start.signupToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(completeBody)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TokenPairResponse tokens = om.readValue(completeResp, TokenPairResponse.class);
        assertThat(tokens).isNotNull();
        assertThat(tokens.accessToken()).isNotBlank();

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"));
    }

    @Autowired
    TestOtpSender otpSender;

    @TestConfiguration
    static class OtpTestConfig {
        @Bean
        public TestOtpSender testOtpSender() {
            return new TestOtpSender();
        }

        @Bean
        public com.syncscore.auth.service.OtpSender otpSender(TestOtpSender testOtpSender) {
            return testOtpSender;
        }
    }
}
