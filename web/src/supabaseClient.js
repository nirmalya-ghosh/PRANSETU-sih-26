import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || 'https://jdgypmmixkzamzcqdewk.supabase.co';
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImpkZ3lwbW1peGt6YW16Y3FkZXdrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODczMjc5NTQsImV4cCI6MjEwMjkwMzk1NH0.M_BS1bOQZ_PxblmX7zY5RJeyU6FB8kmISymHvfMityI';

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
  realtime: {
    params: {
      eventsPerSecond: 10,
    },
  },
});
