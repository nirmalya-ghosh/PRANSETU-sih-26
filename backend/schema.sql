-- Supabase PostgreSQL Schema for PRANSETU

-- Enable PostGIS for geospatial queries
CREATE EXTENSION IF NOT EXISTS postgis;

-- SOS Events Table
CREATE TABLE IF NOT EXISTS sos_events (
    sosId UUID PRIMARY KEY,
    protocolVersion VARCHAR(10) NOT NULL DEFAULT '1.0',
    createdAt BIGINT NOT NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'android_app',
    deviceIdentifier VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    locationTimestamp BIGINT,
    locationAccuracy REAL,
    severityCode INT NOT NULL DEFAULT 1,
    peopleCount INT NOT NULL DEFAULT 1,
    medicalRequired BOOLEAN NOT NULL DEFAULT FALSE,
    hopCount INT NOT NULL DEFAULT 0,
    ttl INT NOT NULL DEFAULT 64,
    deliveryState VARCHAR(50) NOT NULL DEFAULT 'SERVER_RECEIVED',
    message TEXT,
    "userName" TEXT,
    "userPhone" TEXT,
    "userEmail" TEXT,
    serverReceivedAt TIMESTAMPTZ DEFAULT NOW(),
    
    -- Geospatial Point
    location geometry(Point, 4326)
);

-- Function to automatically update the geometry column when lat/lon is inserted/updated
CREATE OR REPLACE FUNCTION update_sos_location()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.latitude IS NOT NULL AND NEW.longitude IS NOT NULL THEN
        NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to run the function
DROP TRIGGER IF EXISTS trg_update_sos_location ON sos_events;
CREATE TRIGGER trg_update_sos_location
BEFORE INSERT OR UPDATE OF latitude, longitude ON sos_events
FOR EACH ROW
EXECUTE FUNCTION update_sos_location();

-- RLS (Row Level Security)
ALTER TABLE sos_events ENABLE ROW LEVEL SECURITY;

-- Allow anonymous inserts (for the Android app to ingest via PostgREST)
CREATE POLICY "Allow public insert" ON sos_events FOR INSERT WITH CHECK (true);

-- Allow anonymous reads (for the EOC web dashboard to display live SOS feed)
CREATE POLICY "Allow anon read" ON sos_events FOR SELECT USING (true);

-- Allow authenticated users to update SOS state (for operator acknowledgement)
CREATE POLICY "Allow authenticated update" ON sos_events FOR UPDATE USING (auth.role() = 'authenticated');

-- Enable Supabase Realtime push for live web dashboard updates
ALTER PUBLICATION supabase_realtime ADD TABLE sos_events;
