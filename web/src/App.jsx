import { useState, useEffect, useCallback } from 'react';
import { 
  Activity, Map as MapIcon, Users, AlertTriangle, 
  Settings, Bell, Search, Menu, 
  ShieldAlert, Radio, UserCheck, Clock
} from 'lucide-react';
import { supabase } from './supabaseClient';
import LiveMap from './LiveMap';
import AIIntelligence from './AIIntelligence';
import MeshVisualizer from './MeshVisualizer';
import SosDetailsModal from './SosDetailsModal';
import LoginScreen from './LoginScreen';
import './index.css';

// --- Helpers to transform raw Supabase rows into UI format ---

/** Map severityCode (1–3) to a priority label used by CSS classes */
function severityToPriority(code) {
  if (code >= 3) return 'high';
  if (code === 2) return 'medium';
  return 'low';
}

/** Map deliveryState to a human-readable status */
function deliveryStateToStatus(state) {
  switch (state) {
    case 'ACKNOWLEDGED': return 'Acknowledged';
    case 'CLOSED': return 'Resolved';
    case 'SERVER_RECEIVED': return 'Active';
    default: return 'Active';
  }
}

/** Convert epoch millis to a relative time string */
function timeAgo(epochMs) {
  const diff = Date.now() - epochMs;
  if (diff < 60_000) return 'Just now';
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} min ago`;
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h ago`;
  return new Date(epochMs).toLocaleDateString();
}

/** Reverse-geocode lat/lon to a rough Odisha district label */
function approxLocation(lat, lon) {
  const districts = [
    { name: 'Cuttack, Odisha', lat: 20.46, lon: 85.88 },
    { name: 'Bhubaneswar, Odisha', lat: 20.30, lon: 85.82 },
    { name: 'Puri, Odisha', lat: 19.81, lon: 85.83 },
    { name: 'Balasore, Odisha', lat: 21.49, lon: 86.93 },
    { name: 'Bhadrak, Odisha', lat: 20.79, lon: 86.96 },
    { name: 'Jagatsinghpur, Odisha', lat: 20.32, lon: 86.61 },
    { name: 'Ganjam, Odisha', lat: 19.26, lon: 84.91 },
    { name: 'Sambalpur, Odisha', lat: 21.52, lon: 83.87 },
    { name: 'Sundargarh, Odisha', lat: 22.26, lon: 84.85 },
    { name: 'Koraput, Odisha', lat: 18.81, lon: 82.71 },
  ];
  if (!lat || !lon) return 'Unknown Location';
  let closest = districts[0];
  let minDist = Infinity;
  for (const d of districts) {
    const dist = Math.sqrt((d.lat - lat) ** 2 + (d.lon - lon) ** 2);
    if (dist < minDist) { minDist = dist; closest = d; }
  }
  return closest.name;
}

/** Map a raw Supabase sos_events row to the UI item format */
function mapSosToFeedItem(row) {
  const priority = severityToPriority(row.severityCode);
  return {
    id: row.sosId,
    sosId: row.sosId,
    priority,
    location: approxLocation(row.latitude, row.longitude),
    latitude: row.latitude,
    longitude: row.longitude,
    time: timeAgo(row.createdAt),
    createdAt: row.createdAt,
    type: row.medicalRequired ? 'Medical Emergency' : (row.severityCode >= 3 ? 'Critical SOS' : 'Distress Signal'),
    status: deliveryStateToStatus(row.deliveryState),
    rawMessage: row.message || '',
    translatedMessage: row.message || '',
    severityCode: row.severityCode,
    peopleCount: row.peopleCount,
    medicalRequired: row.medicalRequired,
    hopCount: row.hopCount,
    deliveryState: row.deliveryState,
    source: row.source,
    deviceIdentifier: row.deviceIdentifier,
    userName: row.userName,
    userPhone: row.userPhone,
    userEmail: row.userEmail,
  };
}

function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [currentTime, setCurrentTime] = useState(new Date());
  const [selectedSos, setSelectedSos] = useState(null);
  const [liveFeed, setLiveFeed] = useState([]);
  const [stats, setStats] = useState({ critical: 0, active: 0, acknowledged: 0, totalRelays: 0 });
  const [isLoading, setIsLoading] = useState(true);
  const [connectionStatus, setConnectionStatus] = useState('connecting'); // 'connecting' | 'live' | 'error'
  const [session, setSession] = useState(null);
  const [authInitialized, setAuthInitialized] = useState(false);

  // Compute stats from feed data
  const computeStats = useCallback((items) => {
    const critical = items.filter(i => i.priority === 'high').length;
    const active = items.filter(i => i.status === 'Active').length;
    const acknowledged = items.filter(i => i.status === 'Acknowledged' || i.status === 'Resolved').length;
    const totalRelays = items.reduce((sum, i) => sum + (i.hopCount || 0), 0);
    setStats({ critical, active, acknowledged, totalRelays });
  }, []);

  // Initialize Auth
  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      setAuthInitialized(true);
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session);
    });

    return () => subscription.unsubscribe();
  }, []);

  // Fetch initial data from Supabase
  useEffect(() => {
    if (!session) return; // Don't fetch if not logged in

    async function fetchSosEvents() {
      try {
        const { data, error } = await supabase
          .from('sos_events')
          .select('*')
          .neq('deliveryState', 'CLOSED')
          .order('createdAt', { ascending: false })
          .limit(200);

        if (error) {
          console.error('Supabase fetch error:', error);
          setConnectionStatus('error');
          return;
        }

        const mapped = (data || []).map(mapSosToFeedItem);
        setLiveFeed(mapped);
        computeStats(mapped);
        setConnectionStatus('live');
      } catch (err) {
        console.error('Network error fetching SOS events:', err);
        setConnectionStatus('error');
      } finally {
        setIsLoading(false);
      }
    }

    fetchSosEvents();
  }, [computeStats, session]);

  // Subscribe to Realtime changes for live push updates
  useEffect(() => {
    if (!session) return;
    const channel = supabase
      .channel('sos-realtime-feed')
      .on(
        'postgres_changes',
        { event: 'INSERT', schema: 'public', table: 'sos_events' },
        (payload) => {
          console.log('[REALTIME] New SOS received:', payload.new.sosId);
          const newItem = mapSosToFeedItem(payload.new);
          setLiveFeed((prev) => {
            // Dedup: skip if already present
            if (prev.some(i => i.id === newItem.id)) return prev;
            const updated = [newItem, ...prev];
            computeStats(updated);
            return updated;
          });
        }
      )
      .on(
        'postgres_changes',
        { event: 'UPDATE', schema: 'public', table: 'sos_events' },
        (payload) => {
          console.log('[REALTIME] SOS updated:', payload.new.sosId);
          const updatedItem = mapSosToFeedItem(payload.new);
          setLiveFeed((prev) => {
            const updated = prev.map(i => i.id === updatedItem.id ? updatedItem : i);
            computeStats(updated);
            return updated;
          });
        }
      )
      .subscribe((status) => {
        console.log('[REALTIME] Subscription status:', status);
        if (status === 'SUBSCRIBED') setConnectionStatus('live');
      });

    return () => {
      supabase.removeChannel(channel);
    };
  }, [computeStats, session]);

  // Clock tick
  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  // Connection status indicator
  const connectionDot = connectionStatus === 'live' ? '🟢' : connectionStatus === 'connecting' ? '🟡' : '🔴';
  const connectionLabel = connectionStatus === 'live' ? 'LIVE' : connectionStatus === 'connecting' ? 'CONNECTING' : 'OFFLINE';

  if (!authInitialized) {
    return <div style={{ minHeight: '100vh', background: 'var(--bg-color)' }} />; // Blank while checking session
  }

  if (!session) {
    return <LoginScreen />;
  }

  const handleSignOut = async () => {
    await supabase.auth.signOut();
  };

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="brand">
          <ShieldAlert className="brand-icon" size={32} />
          <span>PRANSETU</span>
        </div>
        
        <ul className="nav-links">
          <li>
            <a href="#" className={`nav-link ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => setActiveTab('dashboard')}>
              <Activity size={20} />
              <span>Command Center</span>
            </a>
          </li>
          <li>
            <a href="#" className={`nav-link ${activeTab === 'map' ? 'active' : ''}`} onClick={() => setActiveTab('map')}>
              <MapIcon size={20} />
              <span>Live Map</span>
            </a>
          </li>
          <li>
            <a href="#" className={`nav-link ${activeTab === 'incidents' ? 'active' : ''}`} onClick={() => setActiveTab('incidents')}>
              <AlertTriangle size={20} />
              <span>Incidents</span>
            </a>
          </li>
          <li>
            <a href="#" className={`nav-link ${activeTab === 'resources' ? 'active' : ''}`} onClick={() => setActiveTab('resources')}>
              <Users size={20} />
              <span>Resources</span>
            </a>
          </li>
          <li>
            <a href="#" className="nav-link">
              <Settings size={20} />
              <span>Settings</span>
            </a>
          </li>
        </ul>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        {/* Topbar */}
        <header className="topbar">
          <div className="topbar-title">
            Emergency Operations Centre
            <span style={{ fontSize: '12px', marginLeft: '12px', fontWeight: 400 }}>
              {connectionDot} {connectionLabel}
            </span>
          </div>
          <div className="topbar-actions">
            <div style={{ color: 'var(--text-secondary)', fontSize: '14px', marginRight: '16px' }}>
              {currentTime.toLocaleString()}
            </div>
            <button className="icon-btn">
              <Search size={20} />
            </button>
            <button className="icon-btn">
              <Bell size={20} />
            </button>
            <div 
              className="icon-btn" 
              style={{ background: 'var(--primary-color)', color: 'white', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', padding: '0 16px', width: 'auto', borderRadius: '20px' }}
              onClick={handleSignOut}
            >
              <img 
                src={session?.user?.user_metadata?.avatar_url || 'https://www.gravatar.com/avatar/00000000000000000000000000000000?d=mp&f=y'} 
                alt="Avatar" 
                style={{ width: '24px', height: '24px', borderRadius: '50%' }}
              />
              <span style={{ fontSize: '14px', fontWeight: 'bold' }}>Sign Out</span>
            </div>
          </div>
        </header>
        
        {/* Dashboard Content */}
        <div className="content-area">
          <div className="dashboard-grid">
            
            {/* Stats Cards — now driven by live data */}
            <div className="stat-card critical">
              <div className="stat-header">
                <span>Critical SOS</span>
                <AlertTriangle size={20} color="var(--primary-color)" />
              </div>
              <div className="stat-value" style={{ color: 'var(--primary-color)' }}>{stats.critical}</div>
              <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Severity ≥ 3</div>
            </div>
            
            <div className="stat-card active">
              <div className="stat-header">
                <span>Active Incidents</span>
                <Radio size={20} color="var(--warning-color)" className="pulse" />
              </div>
              <div className="stat-value">{stats.active}</div>
              <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Awaiting response</div>
            </div>
            
            <div className="stat-card resolved">
              <div className="stat-header">
                <span>Acknowledged</span>
                <UserCheck size={20} color="var(--success-color)" />
              </div>
              <div className="stat-value">{stats.acknowledged}</div>
              <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Operator confirmed</div>
            </div>
            
            <div className="stat-card total">
              <div className="stat-header">
                <span>Total Relays</span>
                <Activity size={20} color="var(--accent-color)" />
              </div>
              <div className="stat-value">{stats.totalRelays}</div>
              <div style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Mesh hops recorded</div>
            </div>

            {/* Map Section */}
            <div className="map-section" style={{ gridColumn: 'span 5' }}>
              <LiveMap incidents={liveFeed} />
            </div>

            {/* AI Intelligence Section */}
            <div style={{ gridColumn: 'span 3', height: '500px' }}>
              <AIIntelligence />
            </div>

            {/* Live Feed — now from Supabase Realtime */}
            <div className="feed-section" style={{ gridColumn: 'span 4' }}>
              <div className="section-header">
                Live SOS Feed
                {isLoading && <span style={{ fontSize: '12px', marginLeft: '8px', color: 'var(--text-secondary)' }}>Loading...</span>}
              </div>
              <div className="feed-list">
                {liveFeed.length === 0 && !isLoading && (
                  <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No active SOS events. Waiting for incoming signals...
                  </div>
                )}
                {liveFeed.map((feed) => (
                  <div key={feed.id} className={`feed-item priority-${feed.priority}`} style={{ cursor: 'pointer' }} onClick={() => setSelectedSos(feed)}>
                    <div className="feed-item-header">
                      <span className={`feed-badge badge-${feed.priority}`}>{feed.status}</span>
                      <span className="feed-time"><Clock size={12} style={{ display: 'inline', marginRight: '4px' }}/>{feed.time}</span>
                    </div>
                    <div className="feed-location">
                      <MapIcon size={14} />
                      {feed.location}
                    </div>
                    <div className="feed-meta">
                      <span><strong>Type:</strong> {feed.type}</span>
                      {feed.peopleCount > 1 && <span style={{ marginLeft: '12px' }}><strong>People:</strong> {feed.peopleCount}</span>}
                      {feed.userName && <span style={{ marginLeft: '12px', color: 'var(--text-primary)' }}><strong>From:</strong> {feed.userName}</span>}
                      {feed.userPhone && !feed.userName && <span style={{ marginLeft: '12px', color: 'var(--text-primary)' }}><strong>From:</strong> {feed.userPhone}</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            
            {/* Second Row */}
            <div style={{ gridColumn: 'span 6', height: '400px', marginTop: '24px' }}>
              <MeshVisualizer />
            </div>
          </div>
        </div>
      </main>
      <SosDetailsModal sos={selectedSos} onClose={() => setSelectedSos(null)} />
    </div>
  );
}

export default App;
