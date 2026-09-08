import React, { useState } from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { ActiveTab, FeatureSubmenu } from './types';
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

const SUBMENU_TITLES: Record<FeatureSubmenu, string> = {
  main: 'Features',
  dev: 'Developer Options',
  ghost: 'Ghost Mode',
  qt: 'Quick Toggles',
  ads: 'Ad & Analytics Block',
  cleanfeed: 'Clean Feed',
  distract: 'Distraction-Free',
  misc: 'Misc Features',
  downloader: 'Downloader',
  location: 'Location',
  quality: 'Video Quality',
  theme: 'Theme Customizer',
};

const MainContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ActiveTab>('home');
  const [featureSubmenu, setFeatureSubmenu] = useState<FeatureSubmenu>('main');

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
        return SUBMENU_TITLES[featureSubmenu];
      case 'logs':
        return 'Logs';
      case 'help':
        return 'Help & FAQ';
    }
  };

  const isSubmenuOpen = activeTab === 'features' && featureSubmenu !== 'main';

  const handleBack = () => {
    if (isSubmenuOpen) {
      setFeatureSubmenu('main');
    }
  };

  const handleTabChange = (tab: ActiveTab) => {
    setActiveTab(tab);
    if (tab === 'features' && featureSubmenu !== 'main') {
      // Keep submenu or let user navigate
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
        showBack={isSubmenuOpen}
        onBack={handleBack}
        currentSubmenu={featureSubmenu}
      />

      {/* Main Screen Container */}
      <main className="relative z-1 flex-1 max-w-3xl w-full mx-auto p-4 sm:p-6 overflow-x-hidden">
        {activeTab === 'home' && <HomeTab />}
        {activeTab === 'features' && (
          <FeaturesTab
            currentSubmenu={featureSubmenu}
            onSubmenuChange={menu => {
              if (menu === 'theme') {
                setOpenThemeCustomizer(true);
              } else if (menu === 'location') {
                setOpenLocationPicker(true);
              } else {
                setFeatureSubmenu(menu);
              }
            }}
          />
        )}
        {activeTab === 'logs' && <LogsTab />}
        {activeTab === 'help' && <HelpTab />}
      </main>

      {/* Bottom Navigation */}
      <BottomNav activeTab={activeTab} onTabChange={handleTabChange} />

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
          <div className="px-4 py-2 rounded-2xl glass-pill bg-zinc-950/90 border border-white/20 text-white text-xs font-semibold shadow-2xl backdrop-blur-2xl flex items-center gap-2.5">
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
