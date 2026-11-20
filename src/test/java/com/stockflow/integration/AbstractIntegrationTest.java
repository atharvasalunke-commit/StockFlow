package com.stockflow.integration;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    static {
        mysql.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("stockflow.security.jwt.secret", () -> "integration-test-secret-0123456789-abcdef");
    }

    @Autowired
    MockMvc mockMvc;

    record TestUser(long id, String email, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    TestUser newUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";

        String registerBody = """
                {"email": "%s", "password": "Secret1234", "fullName": "Test User"}
                """.formatted(email);
        String registerResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(registerResponse, "$.id")).longValue();

        String loginBody = """
                {"email": "%s", "password": "Secret1234"}
                """.formatted(email);
        String loginResponse = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.accessToken");

        return new TestUser(id, email, token);
    }

    long createOrganization(TestUser owner, String name) throws Exception {
        String body = """
                {"name": "%s"}
                """.formatted(name);
        String response = post("/api/organizations", owner, body)
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    void addMember(TestUser owner, long organizationId, TestUser member, String role) throws Exception {
        String body = """
                {"email": "%s", "role": "%s"}
                """.formatted(member.email(), role);
        post("/api/organizations/" + organizationId + "/members", owner, body);
    }

    ResultActions get(String url, TestUser user) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                .header("Authorization", user.bearer()));
    }

    ResultActions delete(String url, TestUser user) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(url)
                .header("Authorization", user.bearer()));
    }

    ResultActions post(String url, TestUser user, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    ResultActions put(String url, TestUser user, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(url)
                .header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    ResultActions patch(String url, TestUser user, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.patch(url)
                .header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
