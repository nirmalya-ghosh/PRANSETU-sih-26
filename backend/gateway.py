import time
import requests
import json
import random
import os

# Simulated Gateway Node for SIH 2026 Demo
# In reality, this script would run on a Raspberry Pi or an internet-connected laptop,
# listening for Bluetooth LE / WiFi-Direct packets from offline phones, and pushing them to the backend.

API_URL = "http://localhost:8000/api/sos"
GATEWAY_ID = "gateway_node_01"

print(f"[{time.strftime('%X')}] PRANSETU Hardware Gateway Initialized (ID: {GATEWAY_ID})")
print(f"[{time.strftime('%X')}] Listening for offline Mesh packets (BLE/Wi-Fi Direct)...")

def simulate_mesh_reception():
    """Simulates receiving an SOS packet from an offline phone via the Mesh."""
    mock_packet = {
        "sosId": f"mesh_sos_{random.randint(1000,9999)}",
        "createdAt": int(time.time() * 1000),
        "source": "offline_mesh",
        "deviceIdentifier": "offline_peer_A",
        "latitude": 20.301 + random.uniform(-0.05, 0.05),
        "longitude": 85.820 + random.uniform(-0.05, 0.05),
        "severityCode": 3,
        "peopleCount": random.randint(1, 5),
        "medicalRequired": random.choice([True, False]),
        "hopCount": random.randint(1, 5),  # Proves it hopped through phones
        "ttl": 60,
        "deliveryState": "GATEWAY_REACHED"
    }
    
    print(f"\n[!] INCOMING MESH PACKET RECEIVED!")
    print(f"    Origin: {mock_packet['deviceIdentifier']} | Hops: {mock_packet['hopCount']}")
    print(f"    Forwarding to Cloud Backend ({API_URL})...")
    
    try:
        response = requests.post(API_URL, json=mock_packet)
        if response.status_code == 200:
            print("    -> [SUCCESS] Packet synced to EOC Cloud!")
        else:
            print(f"    -> [ERROR] Backend rejected packet: {response.text}")
    except Exception as e:
        print(f"    -> [NETWORK ERROR] Cloud unreachable. Caching packet locally... {e}")

if __name__ == "__main__":
    try:
        while True:
            # Simulate randomly receiving a packet every 15-30 seconds
            time.sleep(random.randint(15, 30))
            simulate_mesh_reception()
    except KeyboardInterrupt:
        print("\nGateway shutting down.")
