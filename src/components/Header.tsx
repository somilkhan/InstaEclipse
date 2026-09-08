import React from 'react';
import { ArrowLeft, Info, RefreshCw, Sparkles, ShieldCheck, ArrowUpCircle, Download } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { FeatureSubmenu } from '../types';

interface HeaderProps {
  title: string;
  showBack: boolean;
  onBack: () => void;
  currentSubmenu?: FeatureSubmenu;
}

export const Header: React.FC<HeaderProps> = ({ title, showBack, onBack }) => {
  const {
    setOpenAboutDialog,
    setOpenUpdateModal,
    setOpenApkInstallerModal,
    restartInstagram,
    isRestarting,
    activePackage,
    versionInfo,
  } = useApp();

  return (
    <header className="sticky top-0 z-30 bg-zinc-950/75 backdrop-blur-2xl border-b border-white/[0.08] px-4 py-3">
      <div className="max-w-3xl mx-auto flex items-center justify-between gap-3">
        <div className="flex items-center gap-2.5 min-w-0">
          {showBack ? (
            <button
              onClick={onBack}
              className="p-1.5 -ml-1.5 rounded-xl hover:bg-white/10 active:bg-white/15 text-zinc-300 hover:text-white transition-colors"
              title="Back"
              aria-label="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
          ) : (
            <div className="relative flex-shrink-0">
              <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/20 flex items-center justify-center shadow-[0_0_15px_rgba(255,255,255,0.1)]">
                <Sparkles className="w-4 h-4 text-white" />
              </div>
              <div className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-white rounded-full ring-2 ring-zinc-950 shadow-[0_0_6px_rgba(255,255,255,0.8)]" />
            </div>
          )}

          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-bold text-white truncate tracking-tight">{title}</h1>
              {!showBack && (
                <button
                  id="header-version-badge"
                  onClick={() => setOpenUpdateModal(true)}
                  className="group inline-flex items-center gap-1 text-[10px] uppercase font-bold tracking-wider bg-white/10 hover:bg-white/20 text-white px-2 py-0.5 rounded-full border border-white/15 transition-all cursor-pointer"
                  title="Check for updates"
                >
                  <span>v{versionInfo.version}</span>
                  {!versionInfo.isLatest && <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />}
                </button>
              )}
            </div>
            {!showBack && (
              <p className="text-xs text-zinc-400 flex items-center gap-1.5 truncate">
                <ShieldCheck className="w-3 h-3 text-zinc-300 shrink-0" />
                <span className="truncate text-[11px] text-zinc-400">LSPosed Active &bull; {activePackage}</span>
              </p>
            )}
          </div>
        </div>

        <div className="flex items-center gap-1">
          {!versionInfo.isLatest && (
            <button
              id="header-update-btn"
              onClick={() => setOpenUpdateModal(true)}
              title="Update Available"
              aria-label="Update available"
              className="p-2 rounded-xl text-white bg-white/10 hover:bg-white/20 border border-white/15 transition-all animate-pulse"
            >
              <ArrowUpCircle className="w-4 h-4" />
            </button>
          )}

          <button
            id="header-download-apk-btn"
            onClick={() => setOpenApkInstallerModal(true)}
            title={`Download & Install InstaEclipse v${versionInfo.version}`}
            aria-label="Download and install APK"
            className="p-2 rounded-xl text-zinc-300 hover:text-white hover:bg-white/10 active:bg-white/15 transition-colors"
          >
            <Download className="w-4 h-4" />
          </button>

          <button
            id="header-restart-btn"
            onClick={restartInstagram}
            disabled={isRestarting}
            title="Restart Instagram Hook"
            aria-label="Restart Instagram"
            className="p-2 rounded-xl text-zinc-300 hover:text-white hover:bg-white/10 active:bg-white/15 transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isRestarting ? 'animate-spin text-white' : ''}`} />
          </button>

          <button
            id="header-about-btn"
            onClick={() => setOpenAboutDialog(true)}
            title="About & Credits"
            aria-label="About and credits"
            className="p-2 rounded-xl text-zinc-300 hover:text-white hover:bg-white/10 active:bg-white/15 transition-colors"
          >
            <Info className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
