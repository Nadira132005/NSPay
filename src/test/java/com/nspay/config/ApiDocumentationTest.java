package com.nspay.config;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"app.jwt.filter-enabled=true","app.schedules.enabled=false"})
@AutoConfigureMockMvc
class ApiDocumentationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;
    @Test void everyApplicationEndpointHasDocumentationAndAuthenticationIsUsable() throws Exception {
        JsonNode spec=mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("NSPay API",spec.path("info").path("title").asText());
        assertEquals("bearer",spec.at("/components/securitySchemes/bearerAuth/scheme").asText());
        assertEquals("bearerAuth",spec.path("security").get(0).fieldNames().next());
        JsonNode paths=spec.path("paths");
        mappings.getHandlerMethods().forEach((mapping,handler)->{
            if(!handler.getBeanType().getPackageName().startsWith("com.nspay"))return;
            for(String path:mapping.getPatternValues())for(org.springframework.web.bind.annotation.RequestMethod method:mapping.getMethodsCondition().getMethods()){
                JsonNode operation=paths.path(path).path(method.name().toLowerCase());
                assertFalse(operation.isMissingNode(),"Missing operation: "+method+" "+path);
                assertFalse(operation.path("summary").asText().isBlank(),"Missing summary: "+path);
                assertFalse(operation.path("description").asText().isBlank(),"Missing behavior: "+path);
                assertTrue(operation.path("responses").has("401"),"Missing authentication response: "+path);
            }
        });
        assertTrue(paths.path("/api/login/").path("post").has("security"));
        assertTrue(paths.path("/api/login/").path("post").path("security").isEmpty());
        assertTrue(spec.at("/components/schemas/InvestmentOrder/properties/requestKey").has("description"));
        assertTrue(spec.at("/components/schemas/InvestmentPortfolio/properties/cash").has("description"));
        assertTrue(paths.path("/api/finance/documents").path("post").path("requestBody").path("content").has("multipart/form-data"));
        assertEquals("SignInCredentials",spec.at("/paths/~1api~1login~1/post/requestBody/content/application~1json/schema/$ref").asText().replace("#/components/schemas/",""));
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
