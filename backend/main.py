import os
from fastapi import FastAPI, HTTPException, Depends, Header
from pydantic import BaseModel
from typing import Optional
from supabase import create_client, Client
from dotenv import load_dotenv

load_dotenv()

app = FastAPI(title="PRANSETU Backend")

# Initialize Supabase client
SUPABASE_URL = os.environ.get("SUPABASE_URL", "")
SUPABASE_KEY = os.environ.get("SUPABASE_KEY", "")

# Dependency to get Supabase client
def get_supabase() -> Client:
    if not SUPABASE_URL or not SUPABASE_KEY:
        # In a real setup, this would be a hard error. 
        # Here we allow startup for mock purposes if keys are missing.
        pass
    return create_client(SUPABASE_URL, SUPABASE_KEY)

class SosCanonicalModel(BaseModel):
    sosId: str
    protocolVersion: str = "1.0"
    createdAt: int
    source: str = "android_app"
    deviceIdentifier: str = ""
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    locationTimestamp: Optional[int] = None
    locationAccuracy: Optional[float] = None
    severityCode: int = 1
    peopleCount: int = 1
    medicalRequired: bool = False
    hopCount: int = 0
    ttl: int = 64
    deliveryState: str = "SERVER_RECEIVED"

@app.get("/")
def read_root():
    return {"status": "ok", "service": "PRANSETU Backend API"}

@app.post("/api/sos")
def submit_sos(sos: SosCanonicalModel, supabase: Client = Depends(get_supabase)):
    """
    Ingests an SOS event from a client or offline gateway.
    """
    try:
        # Deduplication check handled by Supabase unique constraints (sosId)
        # or an explicit check.
        
        # We can just attempt to insert it. If it fails with a duplicate key, 
        # it means it was already received.
        data, count = supabase.table('sos_events').insert(sos.model_dump()).execute()
        return {"status": "success", "message": "SOS received and acknowledged"}
    except Exception as e:
        # If it's a duplicate, we should still return success to the client 
        # so it stops retrying.
        if "duplicate key value" in str(e).lower():
            return {"status": "success", "message": "SOS already received"}
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/sos/active")
def get_active_sos(supabase: Client = Depends(get_supabase)):
    """
    Retrieves active SOS events for the EOC dashboard.
    """
    try:
        data, count = supabase.table('sos_events').select("*").neq("deliveryState", "CLOSED").execute()
        return {"status": "success", "data": data[1]}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/sos/geojson")
def get_sos_geojson(supabase: Client = Depends(get_supabase)):
    """
    Returns SOS events formatted as GeoJSON for the Web GIS Map.
    """
    try:
        # In a real environment with PostGIS, we'd use ST_AsGeoJSON
        # For this prototype, we'll construct the GeoJSON in Python
        data, count = supabase.table('sos_events').select("*").neq("deliveryState", "CLOSED").execute()
        
        features = []
        for sos in data[1]:
            if sos.get('latitude') and sos.get('longitude'):
                features.append({
                    "type": "Feature",
                    "geometry": {
                        "type": "Point",
                        "coordinates": [sos['longitude'], sos['latitude']]
                    },
                    "properties": {
                        "id": sos.get('sosId'),
                        "severity": sos.get('severityCode'),
                        "peopleCount": sos.get('peopleCount'),
                        "medical": sos.get('medicalRequired'),
                        "time": sos.get('createdAt')
                    }
                })
                
        geojson = {
            "type": "FeatureCollection",
            "features": features
        }
        return geojson
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/ai/cluster")
def trigger_ai_clustering(supabase: Client = Depends(get_supabase)):
    """
    Triggers Gemini AI to analyze all active SOS and cluster them into major incidents.
    (Stubbed until Gemini API Key is provided)
    """
    return {
        "status": "success", 
        "message": "AI Clustering executed", 
        "clusters": [
            {
                "id": "cluster_001",
                "title": "Major Incident: Flash Flood in Sector 4",
                "risk_level": "CRITICAL",
                "sos_count": 50,
                "center_lat": 20.296,
                "center_lon": 85.824,
                "cascading_risks": ["Hospital accessibility blocked", "Power grid failure imminent"]
            }
        ]
    }

@app.post("/api/broadcast")
def trigger_sms_broadcast(supabase: Client = Depends(get_supabase)):
    """
    (Feature 10) Triggers an emergency SMS broadcast to all registered citizens
    in a specific geographic area using Twilio API.
    Stubbed for demo without real credentials.
    """
    return {
        "status": "success",
        "message": "Twilio SMS broadcast dispatched to 14,592 devices in Danger Zone",
        "gateway": "Twilio Programmable Messaging API"
    }

@app.post("/api/ivr/webhook")
def twilio_voice_webhook():
    """
    (Feature 12) Webhook that Twilio calls when the system places an outbound
    AI Voice Bot call to a citizen who triggered an SOS. Returns TwiML.
    """
    # Generating mock TwiML response
    twiml_response = """<?xml version="1.0" encoding="UTF-8"?>
<Response>
    <Say voice="Polly.Aditi" language="hi-IN">
        Pransetu system se call. Aapne SOS trigger kiya hai. 
        Kripaya bataen aapko kya madad chahiye.
    </Say>
    <Record maxLength="10" action="/api/ivr/process_recording" />
</Response>"""
    from fastapi.responses import Response
    return Response(content=twiml_response, media_type="application/xml")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
