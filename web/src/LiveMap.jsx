import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, CircleMarker, Popup, useMap, Polyline, Polygon } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Helicopter, Anchor, Stethoscope, Navigation, SignalZero } from 'lucide-react';
import L from 'leaflet';
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});
L.Marker.prototype.options.icon = DefaultIcon;

const EOC_HQ = [20.296, 85.824]; // Bhubaneswar

// Simulated coordinates for a "Telecom Blackout Zone"
const blackoutZonePolygon = [
  [19.8, 85.1],
  [19.9, 86.0],
  [19.4, 86.2],
  [19.2, 85.2]
];

export default function LiveMap({ incidents }) {
  const defaultZoom = 7;
  const [dispatches, setDispatches] = useState([]);
  
  const handleDragStart = (e, type) => {
    e.dataTransfer.setData('resourceType', type);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    const type = e.dataTransfer.getData('resourceType');
    if (!type) return;

    // A hacky way to find where they dropped on the map in a demo
    // Usually we would hook into Leaflet's map event, but for simplicity we'll just attach it to the closest SOS or a random point.
    // For this demo, let's just pick the first HIGH priority incident or a random one.
    const target = incidents.find(i => i.priority === 'high' || i.severityCode === 3) || incidents[0];
    if (target && target.latitude) {
      setDispatches(prev => [...prev, {
        id: Date.now(),
        type,
        target: [target.latitude, target.longitude],
        eta: Math.floor(Math.random() * 20) + 10 // 10-30 mins
      }]);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  return (
    <div style={{ height: '100%', width: '100%', position: 'relative' }} onDrop={handleDrop} onDragOver={handleDragOver}>
      <MapContainer center={EOC_HQ} zoom={defaultZoom} style={{ height: '100%', width: '100%', zIndex: 1 }}>
        <TileLayer
          attribution='&copy; <a href="https://carto.com/">CartoDB</a>'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        />
        
        {/* Telecom Blackout Zone */}
        <Polygon 
          positions={blackoutZonePolygon}
          pathOptions={{ fillColor: '#ef4444', color: '#ef4444', fillOpacity: 0.1, weight: 1, dashArray: '4 4' }}
        >
          <Popup>
            <div style={{color: 'black', display: 'flex', alignItems: 'center', gap: '8px'}}>
              <SignalZero color="#ef4444" size={16}/> 
              <strong>Telecom Blackout Zone</strong>
            </div>
            <div style={{color: '#333', fontSize: '12px', marginTop: '4px'}}>
              Detected via offline mesh node clustering. 0% cellular connectivity.
            </div>
          </Popup>
        </Polygon>
        
        {incidents.map((incident) => {
          if (!incident.latitude || !incident.longitude) return null;
          let color = '#38bdf8'; // low
          if (incident.priority === 'high' || incident.severityCode === 3) color = '#f43f5e';
          else if (incident.priority === 'medium' || incident.severityCode === 2) color = '#eab308';
          
          return (
            <CircleMarker 
              key={incident.id || incident.sosId}
              center={[incident.latitude, incident.longitude]}
              radius={8}
              pathOptions={{ fillColor: color, color: color, fillOpacity: 0.7, weight: 2 }}
            >
              <Popup>
                <div style={{ color: '#0f172a' }}>
                  <strong>{incident.type || 'SOS Signal'}</strong><br/>
                  Status: {incident.status || incident.deliveryState}<br/>
                </div>
              </Popup>
            </CircleMarker>
          );
        })}

        {/* EOC HQ Marker */}
        <CircleMarker center={EOC_HQ} radius={10} pathOptions={{ color: '#10b981', fillColor: '#10b981', fillOpacity: 1 }}>
          <Popup><div style={{color: 'black'}}><strong>PRANSETU EOC HQ</strong></div></Popup>
        </CircleMarker>

        {/* Dispatch Lines */}
        {dispatches.map(dispatch => (
          <Polyline 
            key={dispatch.id}
            positions={[EOC_HQ, dispatch.target]} 
            pathOptions={{ color: '#10b981', dashArray: '10, 10', weight: 3, className: 'animated-line' }} 
          >
            <Popup>
              <div style={{color: 'black'}}>
                <strong>{dispatch.type} Dispatched</strong><br/>
                ETA: {dispatch.eta} mins<br/>
                <em>Relaying ETA back to citizen via Mesh...</em>
              </div>
            </Popup>
          </Polyline>
        ))}
      </MapContainer>

      {/* Floating Resources Panel */}
      <div style={{
        position: 'absolute', top: '20px', right: '20px', zIndex: 1000,
        background: 'rgba(15, 23, 42, 0.9)', padding: '16px', borderRadius: '12px',
        border: '1px solid var(--border-color)', backdropFilter: 'blur(10px)',
        color: 'white', width: '200px'
      }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Navigation size={18} /> Dispatch
        </h3>
        <p style={{ fontSize: '12px', color: '#94a3b8', marginBottom: '12px' }}>Drag onto map to dispatch</p>
        
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div draggable onDragStart={(e) => handleDragStart(e, 'Helicopter')} style={{ cursor: 'grab', display: 'flex', alignItems: 'center', gap: '12px', background: 'rgba(255,255,255,0.1)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.2)' }}>
            <Helicopter color="#38bdf8" /> <span>Helicopter</span>
          </div>
          <div draggable onDragStart={(e) => handleDragStart(e, 'Rescue Boat')} style={{ cursor: 'grab', display: 'flex', alignItems: 'center', gap: '12px', background: 'rgba(255,255,255,0.1)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.2)' }}>
            <Anchor color="#f43f5e" /> <span>Rescue Boat</span>
          </div>
          <div draggable onDragStart={(e) => handleDragStart(e, 'Medical Team')} style={{ cursor: 'grab', display: 'flex', alignItems: 'center', gap: '12px', background: 'rgba(255,255,255,0.1)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.2)' }}>
            <Stethoscope color="#10b981" /> <span>Medical Team</span>
          </div>
        </div>
      </div>
      
      <style>{`
        .animated-line { stroke-dashoffset: 1000; animation: dash 20s linear infinite; }
        @keyframes dash { to { stroke-dashoffset: 0; } }
      `}</style>
    </div>
  );
}
