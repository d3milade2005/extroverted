-- Create user database
CREATE DATABASE user_db;
CREATE DATABASE event_db;


-- Enable PostGIS extension in user_db
\c user_db
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

\c event_db
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- notification_db doesn't need PostGIS, so we skip it