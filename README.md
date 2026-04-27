# ACI_LigaACLabs2026

# Install app
Make sure you have maven installed and run the following command:
mvn clean install

# Run app
mvn spring-boot:run

# API documentation
Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`
Generate a local JSON file with:
```bash
curl http://localhost:8080/v3/api-docs -o openapi.json
```
# Actuator
Actuator endpoints are exposed on:
`http://localhost:8080/actuator`
