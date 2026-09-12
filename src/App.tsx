import React, { useState } from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { ActiveTab } from './types';
import { Header } from './components/Header';
import { BottomNav } from './components/BottomNav';
import { HomeTab } from './components/HomeTab';
import { FeaturesTab } from './components/FeaturesTab';
import { LogsTab } from './components/LogsTab';
import { HelpTab } from './components/HelpTab';
import { ThemeCustomizerModal } from './components/ThemeCustomizerModal';
import { LocationPickerModal } from './components/LocationPickerModal';
import { AboutDialog } from './components/AboutDialog';
import { UpdateModal } from './components/UpdateModal';
import { ApkInstallerModal } from './components/ApkInstallerModal';

const MainContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ActiveTab>('home');

  const {
    toastMessage,
    openThemeCustomizer,
    setOpenThemeCustomizer,
    openLocationPicker,
    setOpenLocationPicker,
    openApkInstallerModal,
    setOpenApkInstallerModal,
  } = useApp();

  const getHeaderTitle = () => {
    switch (activeTab) {
      case 'home':
        return 'InstaEclipse';
      case 'features':
        return 'Features & Toggles';
      case 'logs':
        return 'Diagnostics & Logs';
      case 'help':
        return 'Help & FAQ';
    }
  };

  return (
    <div className="min-h-screen bg-[#070709] text-zinc-100 flex flex-col selection:bg-white/20 selection:text-white relative overflow-x-hidden">
      {/* Subtle Monochrome Ambient Frosted Backdrop */}
      <div className="fixed inset-0 pointer-events-none z-0">
        <div className="absolute -top-32 left-1/2 -translate-x-1/2 w-[600px] h-[350px] bg-white/[0.03] rounded-full blur-3xl" />
        <div className="absolute top-1/3 -right-24 w-80 h-80 bg-white/[0.02] rounded-full blur-3xl" />
        <div className="absolute bottom-20 -left-20 w-80 h-80 bg-white/[0.015] rounded-full blur-3xl" />
      </div>

      {/* Top Header */}
      <Header
        title={getHeaderTitle()}
        showBack={false}
        onBack={() => setActiveTab('home')}
      />

      {/* Main Screen Container */}
      <main className="relative z-1 flex-1 max-w-2xl w-full mx-auto p-4 sm:p-5 overflow-x-hidden">
        {activeTab === 'home' && (
          <HomeTab onNavigateToSettings={() => setActiveTab('features')} />
        )}
        {activeTab === 'features' && <FeaturesTab />}
        {activeTab === 'logs' && <LogsTab />}
        {activeTab === 'help' && (
          <HelpTab onOpenLogs={() => setActiveTab('logs')} />
        )}
      </main>

      {/* Bottom Navigation */}
      <BottomNav activeTab={activeTab} onTabChange={setActiveTab} />

      {/* Overlays / Modals */}
      {openThemeCustomizer && (
        <ThemeCustomizerModal onClose={() => setOpenThemeCustomizer(false)} />
      )}
      {openLocationPicker && (
        <LocationPickerModal onClose={() => setOpenLocationPicker(false)} />
      )}
      <AboutDialog />
      <UpdateModal />
      <ApkInstallerModal
        isOpen={openApkInstallerModal}
        onClose={() => setOpenApkInstallerModal(false)}
      />

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed top-18 left-1/2 -translate-x-1/2 z-50 animate-in fade-in slide-in-from-top-2 duration-150 pointer-events-none">
          <div className="px-4 py-2 rounded-2xl bg-zinc-950/90 border border-white/20 text-white text-xs font-semibold shadow-2xl backdrop-blur-2xl flex items-center gap-2.5">
            <span className="w-2 h-2 rounded-full bg-white shadow-[0_0_8px_rgba(255,255,255,0.8)] shrink-0" />
            <span>{toastMessage}</span>
          </div>
        </div>
      )}
    </div>
  );
};

export default function App() {
  return (
    <AppProvider>
      <MainContent />
    </AppProvider>
  );
}
