import React, { useState } from 'react';
import { ShieldAlert, LogIn, Loader2 } from 'lucide-react';
import { supabase } from './supabaseClient';

export default function LoginScreen() {
  const [isLoading, setIsLoading] = useState(false);

  const handleGoogleLogin = async () => {
    try {
      setIsLoading(true);
      const { error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          // Important: In production this will redirect to whatever the Vercel Site URL is
          redirectTo: window.location.origin
        }
      });
      if (error) throw error;
    } catch (error) {
      console.error('Error logging in:', error.message);
      setIsLoading(false);
      alert('Failed to log in with Google. Ensure Client ID/Secret are configured in Supabase.');
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'var(--bg-color)',
      color: 'var(--text-primary)',
      padding: '24px'
    }}>
      <div style={{
        background: 'var(--panel-bg)',
        padding: '48px',
        borderRadius: '24px',
        border: '1px solid var(--border-color)',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
        maxWidth: '480px',
        width: '100%',
        textAlign: 'center'
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '24px' }}>
          <div style={{ 
            background: 'rgba(239, 68, 68, 0.1)', 
            padding: '16px', 
            borderRadius: '50%',
            color: 'var(--primary-color)'
          }}>
            <ShieldAlert size={48} />
          </div>
        </div>
        
        <h1 style={{ fontSize: '28px', marginBottom: '12px', fontWeight: 'bold' }}>PRANSETU</h1>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '40px', fontSize: '16px', lineHeight: '1.5' }}>
          Emergency Operations Centre.<br/>
          Authorized Government Personnel Only.
        </p>

        <button 
          onClick={handleGoogleLogin}
          disabled={isLoading}
          style={{
            width: '100%',
            padding: '16px',
            background: 'white',
            color: 'black',
            border: 'none',
            borderRadius: '12px',
            fontSize: '16px',
            fontWeight: 'bold',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '12px',
            cursor: isLoading ? 'not-allowed' : 'pointer',
            transition: 'all 0.2s',
            opacity: isLoading ? 0.7 : 1
          }}
        >
          {isLoading ? (
            <Loader2 size={24} className="spin" />
          ) : (
            <>
              {/* Simple Google G icon SVG */}
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
              Sign in with Google
            </>
          )}
        </button>

        <div style={{ marginTop: '24px', fontSize: '12px', color: 'var(--text-secondary)' }}>
          By accessing this system, you agree to strict confidentiality<br/>and data logging policies.
        </div>
      </div>
      
      <style dangerouslySetInnerHTML={{__html: `
        .spin {
          animation: spin 1s linear infinite;
        }
        @keyframes spin {
          from { transform: rotate(0deg); }
          to { transform: rotate(360deg); }
        }
      `}} />
    </div>
  );
}
