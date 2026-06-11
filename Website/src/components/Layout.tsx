import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';

export function Layout() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <div className="flex min-h-screen relative w-full max-w-full overflow-x-hidden">
      {/* Mobile Backdrop */}
      {mobileMenuOpen && (
        <div 
          className="fixed inset-0 z-30 bg-slate-900/50 backdrop-blur-sm md:hidden"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}
      
      <Sidebar 
        collapsed={collapsed} 
        onToggle={() => setCollapsed(!collapsed)} 
        mobileMenuOpen={mobileMenuOpen}
        setMobileMenuOpen={setMobileMenuOpen}
      />
      <div className={`flex h-screen overflow-y-auto flex-1 flex-col min-w-0 transition-all duration-300 ml-0 ${collapsed ? 'md:ml-[68px]' : 'md:ml-60'}`}>
        <Topbar onMenuClick={() => setMobileMenuOpen(true)} />
        <main className="flex-1 p-4 md:p-6 w-full max-w-full overflow-x-hidden min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
