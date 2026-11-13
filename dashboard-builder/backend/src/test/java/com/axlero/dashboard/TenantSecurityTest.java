package com.axlero.dashboard;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TenantSecurityTest {
    @Autowired MockMvc mvc;

    private String register(String user, String workspace) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"password123\",\"workspaceName\":\"" + workspace + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String login(String user) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void unauthenticatedRequestsGet401() throws Exception {
        mvc.perform(get("/api/dashboards")).andExpect(status().isUnauthorized());
    }

    @Test
    void tenantsCannotSeeEachOthersDashboards() throws Exception {
        String a = register("alice", "Alice Co");
        String b = register("bob", "Bob Co");
        String created = mvc.perform(post("/api/dashboards").header("Authorization", "Bearer " + a)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sales\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");

        mvc.perform(get("/api/dashboards/" + id).header("Authorization", "Bearer " + a)).andExpect(status().isOk());
        mvc.perform(get("/api/dashboards/" + id).header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/dashboards/" + id).header("Authorization", "Bearer " + b)).andExpect(status().isNotFound());
    }

    @Test
    void viewersCannotEdit() throws Exception {
        String admin = register("carol", "Carol Co");
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"viewer1\",\"password\":\"password123\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isCreated());
        String viewer = login("viewer1");
        mvc.perform(post("/api/dashboards").header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nope\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboards").header("Authorization", "Bearer " + viewer)).andExpect(status().isOk());
    }

    @Test
    void invalidQueryReturnsCleanError() throws Exception {
        String t = register("dave", "Dave Co");
        mvc.perform(post("/api/metrics/preview").header("Authorization", "Bearer " + t)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dimension\":\"region; DROP TABLE x\",\"aggregation\":\"SUM\",\"measure\":\"amount\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown dimension: region; DROP TABLE x"));
    }
}
