-- PRANSETU Migration 002: Add citizen contact details to SOS events
-- Run this in Supabase SQL Editor (Dashboard → SQL Editor → New Query)

-- Add user identity columns so the EOC can see WHO sent each SOS
ALTER TABLE sos_events ADD COLUMN IF NOT EXISTS "userName" TEXT;
ALTER TABLE sos_events ADD COLUMN IF NOT EXISTS "userPhone" TEXT;
ALTER TABLE sos_events ADD COLUMN IF NOT EXISTS "userEmail" TEXT;
