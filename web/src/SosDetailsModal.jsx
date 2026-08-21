import React from 'react';
import { X, Languages, Mic, MapPin, AlertCircle, FileText } from 'lucide-react';

export default function SosDetailsModal({ sos, onClose }) {
  if (!sos) return null;

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, 
      backgroundColor: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(4px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999
    }}>
      <div style={{
        background: 'var(--panel-bg)', width: '600px', borderRadius: '16px',
        border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column',
        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 10px 10px -5px rgba(0, 0, 0, 0.2)'
      }}>
        <div style={{ padding: '24px', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0, fontSize: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileText color="var(--primary-color)" /> SOS Audit Trail: {sos.id}
          </h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}>
            <X size={24} />
          </button>
        </div>

        <div style={{ padding: '24px', flex: 1 }}>
          <div style={{ display: 'flex', gap: '24px', marginBottom: '24px' }}>
            <div style={{ flex: 1, background: 'rgba(255,255,255,0.03)', padding: '16px', borderRadius: '12px' }}>
              <div style={{ color: 'var(--text-secondary)', fontSize: '12px', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '4px' }}><Mic size={14}/> Raw Original Input (Odia)</div>
              <p style={{ margin: 0, fontSize: '16px', lineHeight: '1.5' }}>"{sos.rawMessage || 'ଆମେ ବନ୍ୟାରେ ଫସି ଯାଇଛୁ, ଦୟାକରି ସାହାଯ୍ୟ କରନ୍ତୁ।'}"</p>
            </div>
            
            <div style={{ flex: 1, background: 'rgba(16, 185, 129, 0.05)', padding: '16px', borderRadius: '12px', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
              <div style={{ color: '#10b981', fontSize: '12px', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '4px' }}><Languages size={14}/> AI Translated (English)</div>
              <p style={{ margin: 0, fontSize: '16px', lineHeight: '1.5' }}>"{sos.translatedMessage || 'We are trapped in the flood, please help.'}"</p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '16px', marginBottom: '16px' }}>
             <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <MapPin size={16} color="var(--text-secondary)" /> 
                <span style={{ color: 'var(--text-secondary)' }}>Location: </span>
                <span>{sos.location}</span>
             </div>
             <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <AlertCircle size={16} color={sos.priority === 'high' ? 'var(--primary-color)' : 'var(--warning-color)'} /> 
                <span style={{ color: 'var(--text-secondary)' }}>Priority: </span>
                <span style={{ color: sos.priority === 'high' ? 'var(--primary-color)' : 'var(--warning-color)', textTransform: 'capitalize' }}>{sos.priority}</span>
             </div>
          </div>

          <div style={{ display: 'flex', gap: '16px', marginBottom: '16px', background: 'rgba(255,255,255,0.02)', padding: '12px', borderRadius: '8px' }}>
            <div style={{ flex: 1 }}>
              <span style={{ color: 'var(--text-secondary)' }}>From: </span>
              <strong>{sos.userName || 'Unknown Citizen'}</strong>
            </div>
            <div style={{ flex: 1 }}>
              <span style={{ color: 'var(--text-secondary)' }}>Phone: </span>
              <strong>{sos.userPhone || 'N/A'}</strong>
            </div>
            <div style={{ flex: 1 }}>
              <span style={{ color: 'var(--text-secondary)' }}>Email: </span>
              <strong>{sos.userEmail || 'N/A'}</strong>
            </div>
          </div>
          
          <div style={{ fontSize: '12px', color: 'var(--text-secondary)', textAlign: 'center', marginTop: '24px', borderTop: '1px solid var(--border-color)', paddingTop: '16px' }}>
            PRANSETU Legal & Government Auditing Requirement: AI translation never overwrites canonical data.
          </div>
        </div>
      </div>
    </div>
  );
}
