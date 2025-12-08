-- Create user database
CREATE DATABASE user_db;
CREATE DATABASE event_db;
CREATE DATABASE recommendation_db;


-- Enable PostGIS extension in user_db and connect to database
\c user_db
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

\c event_db
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

\c recommendation_db
-- recommendation_service doesn't need PostGIS, so we skip it'


-- since this file runs at the very first start of the container
-- we have to add the db manually
-- docker exec extroverted-postgres-services psql -U extroverted -c "CREATE DATABASE recommendation_db;"
