# ACI_LigaACLabs2026

# Install app
Use a local Java JDK, Maven, Node.js/npm, and MySQL server.
Install frontend dependencies from `angular-front-end` with `npm ci`.
Build the backend from the folder containing `pom.xml` with `mvn clean install`.

# Run app locally
Create a local MySQL database named `payments` and give the application user access to it.
The backend defaults to `127.0.0.1:3306`, username `user`, and password `userpass`.
Set `DB_USERNAME`, `DB_PASSWORD`, and optionally `DB_URL` in your shell to use your local credentials.
The local Maven command does not automatically load `.env`.

Start the backend from the folder containing `pom.xml`:
```bash
mvn spring-boot:run
```
Start the frontend in a separate terminal:
```bash
cd angular-front-end
npm start
```
Open `http://localhost:4200`; the frontend proxies API requests to port `8082`.
Docker files are available as an optional alternative; local startup does not require containers.

# API documentation
Swagger UI: `http://localhost:8082/swagger-ui.html`
OpenAPI JSON: `http://localhost:8082/v3/api-docs`
Generate a local JSON file with:
```bash
curl http://localhost:8082/v3/api-docs -o openapi.json
```
# Actuator
Actuator endpoints are exposed on:
`http://localhost:8082/actuator`
