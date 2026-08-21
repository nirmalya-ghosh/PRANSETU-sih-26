import React, { useState } from 'react';
import { Heart, Search, CheckCircle } from 'lucide-react';

export default function SurvivorRegistry() {
  const [search, setSearch] = useState('');
  
  // Mock data for SIH Demo
  const mockRegistry = [
    { id: 1, name: "Nimai Ghosh", phone: "+91 9876543210", status: "SAFE", location: "Relief Camp 4, Cuttack", time: "10 mins ago" },
    { id: 2, name: "Ramesh Kumar", phone: "+91 9988776655", status: "SAFE", location: "Bhubaneswar High School", time: "1 hour ago" }
  ];

  const filtered = mockRegistry.filter(p => p.name.toLowerCase().includes(search.toLowerCase()) || p.phone.includes(search));

  return (
    <div style={{ padding: '32px', background: 'var(--panel-bg)', minHeight: '100vh', color: 'var(--text-primary)' }}>
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <Heart size={48} color="#10B981" style={{ marginBottom: '16px' }} />
          <h1>PRANSETU Survivor Registry</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Search for loved ones who have marked themselves as safe via the offline mesh.</p>
        </div>

        <div style={{ display: 'flex', gap: '12px', marginBottom: '32px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search style={{ position: 'absolute', left: '16px', top: '12px', color: 'var(--text-secondary)' }} size={20} />
            <input 
              type="text" 
              placeholder="Search by Name or Phone Number..." 
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ width: '100%', padding: '12px 16px 12px 48px', borderRadius: '12px', border: '1px solid var(--border-color)', background: 'rgba(255,255,255,0.05)', color: 'white', fontSize: '16px' }}
            />
          </div>
        </div>

        <div>
          {filtered.map(person => (
            <div key={person.id} style={{ background: 'rgba(255,255,255,0.05)', borderRadius: '12px', padding: '24px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderLeft: '4px solid #10B981' }}>
              <div>
                <h3 style={{ fontSize: '20px', marginBottom: '4px' }}>{person.name}</h3>
                <div style={{ color: 'var(--text-secondary)', marginBottom: '8px' }}>{person.phone}</div>
                <div style={{ fontSize: '14px' }}><strong>Last Location:</strong> {person.location}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ background: 'rgba(16, 185, 129, 0.2)', color: '#10B981', padding: '6px 12px', borderRadius: '20px', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                  <CheckCircle size={16} /> {person.status}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Checked in {person.time}</div>
              </div>
            </div>
          ))}
          {filtered.length === 0 && (
            <div style={{ textAlign: 'center', padding: '48px', color: 'var(--text-secondary)' }}>
              No matching records found. Please check spelling or try another number.
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
