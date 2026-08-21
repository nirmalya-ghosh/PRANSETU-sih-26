import React, { useState, useEffect } from 'react';
import { Brain, AlertCircle, ShieldAlert, Activity, ArrowRight } from 'lucide-react';

export default function AIIntelligence() {
  const [clusters, setClusters] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setTimeout(() => {
      setClusters([
        {
          id: "cluster_001",
          title: "Major Incident: Flash Flood in Sector 4",
          risk_level: "CRITICAL",
          sos_count: 50,
          domino_prediction: [
            "Water levels rising at 2ft/hr",
            "Substation 12 is at elevation +3ft",
            "Power grid failure imminent in < 90 mins"
          ],
          recommendation: "Preemptively dispatch rescue boats and alert Sector 5 to evacuate."
        },
        {
          id: "cluster_002",
          title: "Structural Collapse: Highway 6 Bridge",
          risk_level: "HIGH",
          sos_count: 12,
          domino_prediction: [
            "Main artery to District Hospital severed",
            "Ambulance routing compromised for 3 sectors"
          ],
          recommendation: "Deploy air-medics to bypass highway. Reroute ground traffic via Old Road."
        }
      ]);
      setLoading(false);
    }, 1500);
  }, []);

  return (
    <div className="ai-panel" style={{ padding: '24px', background: 'var(--panel-bg)', borderRadius: '20px', border: '1px solid var(--border-color)', height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px', fontSize: '18px', fontWeight: '600' }}>
        <Brain color="var(--accent-color)" />
        AI "Domino Effect" Predictor
      </div>
      
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {loading ? (
          <div style={{ color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Activity className="spinner" /> Analyzing incoming mesh signals...
          </div>
        ) : (
          clusters.map(cluster => (
            <div key={cluster.id} style={{ background: 'rgba(255,255,255,0.03)', padding: '16px', borderRadius: '12px', marginBottom: '16px', borderLeft: `4px solid ${cluster.risk_level === 'CRITICAL' ? 'var(--primary-color)' : '#eab308'}` }}>
              <h4 style={{ color: cluster.risk_level === 'CRITICAL' ? 'var(--primary-color)' : '#eab308', margin: '0 0 12px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <ShieldAlert size={18} /> {cluster.title}
              </h4>
              <div style={{ fontSize: '14px', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ background: cluster.risk_level === 'CRITICAL' ? 'rgba(244, 63, 94, 0.2)' : 'rgba(234, 179, 8, 0.2)', padding: '4px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: 'bold' }}>{cluster.risk_level}</span>
                <span style={{ color: 'var(--text-secondary)' }}>{cluster.sos_count} Offline Signals Clustered</span>
              </div>
              
              <div style={{ fontSize: '14px', background: 'rgba(0,0,0,0.2)', padding: '12px', borderRadius: '8px', marginBottom: '12px' }}>
                <strong style={{ color: '#fff', display: 'block', marginBottom: '8px' }}>Predicted Domino Effect:</strong>
                {cluster.domino_prediction.map((step, idx) => (
                  <div key={idx} style={{ color: 'var(--warning-color)', display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
                    {idx > 0 && <ArrowRight size={12} />}
                    <span>{step}</span>
                  </div>
                ))}
              </div>

              <div style={{ fontSize: '14px', background: 'rgba(16, 185, 129, 0.1)', padding: '12px', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
                <strong style={{ color: '#10b981', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                  <Brain size={14} /> AI Recommendation:
                </strong>
                <span style={{ color: '#e2e8f0' }}>{cluster.recommendation}</span>
              </div>
            </div>
          ))
        )}
      </div>
      
      <style>{`
        @keyframes spin { 100% { transform: rotate(360deg); } }
        .spinner { animation: spin 2s linear infinite; }
      `}</style>
    </div>
  );
}
