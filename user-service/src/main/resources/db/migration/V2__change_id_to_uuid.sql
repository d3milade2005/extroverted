ALTER TABLE users
ALTER COLUMN keycloak_id TYPE UUID
USING keycloak_id::uuid;