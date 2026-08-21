-- PRANSETU Full Schema + Integration Migration
-- Run this ONCE in Supabase SQL Editor (Dashboard → SQL Editor → New Query)
-- Creates the sos_events table (if not exists) and applies all integration fixes.

-- 0. Enable PostGIS for geospatial queries
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Create the sos_events table
CREATE TABLE IF NOT EXISTS sos_events (
    "sosId" UUID PRIMARY KEY,
    "protocolVersion" VARCHAR(10) NOT NULL DEFAULT '1.0',
    "createdAt" BIGINT NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'android_app',
    "deviceIdentifier" VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    "locationTimestamp" BIGINT,
    "locationAccuracy" REAL,
    "severityCode" INT NOT NULL DEFAULT 1,
    "peopleCount" INT NOT NULL DEFAULT 1,
    "medicalRequired" BOOLEAN NOT NULL DEFAULT FALSE,
    "hopCount" INT NOT NULL DEFAULT 0,
    ttl INT NOT NULL DEFAULT 64,
    "deliveryState" VARCHAR(50) NOT NULL DEFAULT 'SERVER_RECEIVED',
    message TEXT,
    "serverReceivedAt" TIMESTAMPTZ DEFAULT NOW(),

    -- Geospatial Point (auto-populated by trigger)
    location geometry(Point, 4326)
);

-- 2. Function to automatically populate the geometry column from lat/lon
CREATE OR REPLACE FUNCTION update_sos_location()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Trigger to run the function on insert/update
DROP TRIGGER IF EXISTS trg_update_sos_location ON sos_events;
CREATE TRIGGER trg_update_sos_location
BEFORE INSERT OR UPDATE OF latitude, longitude ON sos_events
FOR EACH ROW
EXECUTE FUNCTION update_sos_location();

-- 4. Enable Row Level Security
ALTER TABLE sos_events ENABLE ROW LEVEL SECURITY;

-- 5. RLS Policies (idempotent — skip if already exists)

-- Allow the Android app to insert SOS events (anon role via PostgREST)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'sos_events' AND policyname = 'Allow public insert'
    ) THEN
        CREATE POLICY "Allow public insert" ON sos_events FOR INSERT WITH CHECK (true);
    END IF;
END $$;

-- Allow the web EOC dashboard to read SOS events (anon role)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'sos_events' AND policyname = 'Allow anon read'
    ) THEN
        CREATE POLICY "Allow anon read" ON sos_events FOR SELECT USING (true);
    END IF;
END $$;

-- Allow authenticated operators to update SOS state (acknowledge, close)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'sos_events' AND policyname = 'Allow authenticated update'
    ) THEN
        CREATE POLICY "Allow authenticated update" ON sos_events FOR UPDATE USING (auth.role() = 'authenticated');
    END IF;
END $$;

-- 6. Enable Supabase Realtime so the web dashboard gets instant push updates
ALTER PUBLICATION supabase_realtime ADD TABLE sos_events;
