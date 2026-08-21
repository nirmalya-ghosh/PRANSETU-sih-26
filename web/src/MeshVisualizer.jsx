import React, { useRef, useEffect, useState, useCallback } from 'react';
import ForceGraph2D from 'react-force-graph-2d';
import { Wifi } from 'lucide-react';

export default function MeshVisualizer() {
  const fgRef = useRef();
  const containerRef = useRef();
  const [graphData, setGraphData] = useState({ nodes: [], links: [] });
  const [dimensions, setDimensions] = useState({ width: 400, height: 400 });

  useEffect(() => {
    const updateDimensions = () => {
      if (containerRef.current) {
        setDimensions({
          width: containerRef.current.clientWidth,
          height: containerRef.current.clientHeight
        });
      }
    };
    
    updateDimensions();
    window.addEventListener('resize', updateDimensions);
    return () => window.removeEventListener('resize', updateDimensions);
  }, []);

  useEffect(() => {
    // Simulated Mesh Network topology for the SIH Demo
    const mockNodes = [
      { id: 'gateway_1', name: 'Gateway Node (Internet)', val: 20, color: '#38bdf8', type: 'gateway' },
      { id: 'peer_1', name: 'Relay Phone A', val: 8, color: '#94a3b8', type: 'peer' },
      { id: 'peer_2', name: 'Relay Phone B', val: 8, color: '#94a3b8', type: 'peer' },
      { id: 'peer_3', name: 'SOS Origin (Offline)', val: 12, color: '#f43f5e', type: 'sos' },
      { id: 'peer_4', name: 'Relay Phone D', val: 8, color: '#94a3b8', type: 'peer' },
      { id: 'peer_5', name: 'Relay Phone E', val: 8, color: '#94a3b8', type: 'peer' },
    ];
    
    const mockLinks = [
      { source: 'peer_3', target: 'peer_2', color: 'rgba(244, 63, 94, 0.8)', active: true },
      { source: 'peer_2', target: 'peer_1', color: 'rgba(244, 63, 94, 0.8)', active: true },
      { source: 'peer_1', target: 'gateway_1', color: 'rgba(244, 63, 94, 0.8)', active: true },
      { source: 'peer_4', target: 'peer_1', color: 'rgba(148, 163, 184, 0.2)', active: false },
      { source: 'peer_5', target: 'peer_2', color: 'rgba(148, 163, 184, 0.2)', active: false },
    ];

    setGraphData({ nodes: mockNodes, links: mockLinks });

    if (fgRef.current) {
      fgRef.current.d3Force('charge').strength(-400);
      fgRef.current.d3Force('link').distance(100);
    }
  }, []);

  const paintNode = useCallback((node, ctx, globalScale) => {
    const { x, y, color, type, val } = node;
    const isSos = type === 'sos';
    const isGateway = type === 'gateway';
    
    ctx.beginPath();
    ctx.arc(x, y, val, 0, 2 * Math.PI, false);
    ctx.fillStyle = color;
    ctx.fill();

    if (isSos) {
      // Glow effect for SOS
      ctx.beginPath();
      ctx.arc(x, y, val * 2, 0, 2 * Math.PI, false);
      ctx.fillStyle = 'rgba(244, 63, 94, 0.2)';
      ctx.fill();
      ctx.beginPath();
      ctx.arc(x, y, val * 3, 0, 2 * Math.PI, false);
      ctx.fillStyle = 'rgba(244, 63, 94, 0.1)';
      ctx.fill();
    } else if (isGateway) {
      // Outer ring for Gateway
      ctx.beginPath();
      ctx.arc(x, y, val * 1.5, 0, 2 * Math.PI, false);
      ctx.strokeStyle = color;
      ctx.lineWidth = 1 / globalScale;
      ctx.stroke();
    }
    
    // Node Label
    const fontSize = 12 / globalScale;
    ctx.font = `${fontSize}px Sans-Serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
    ctx.fillText(node.name, x, y + val + fontSize);
  }, []);

  return (
    <div className="mesh-visualizer" style={{ background: 'var(--panel-bg)', borderRadius: '20px', border: '1px solid var(--border-color)', height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div style={{ padding: '24px', display: 'flex', alignItems: 'center', gap: '8px', borderBottom: '1px solid var(--border-color)', fontSize: '18px', fontWeight: '600' }}>
        <Wifi color="var(--primary-color)" />
        Live Mesh Topology
      </div>
      <div ref={containerRef} style={{ flex: 1, position: 'relative' }}>
        {graphData.nodes.length > 0 && (
          <ForceGraph2D
            ref={fgRef}
            width={dimensions.width}
            height={dimensions.height}
            graphData={graphData}
            nodeCanvasObject={paintNode}
            linkColor="color"
            linkWidth={link => link.active ? 3 : 1}
            linkDirectionalParticles={link => link.active ? 4 : 0}
            linkDirectionalParticleSpeed={0.01}
            linkDirectionalParticleWidth={4}
            linkDirectionalParticleColor={() => '#f43f5e'}
            backgroundColor="transparent"
            d3AlphaDecay={0.02}
            d3VelocityDecay={0.3}
          />
        )}
      </div>
    </div>
  );
}
