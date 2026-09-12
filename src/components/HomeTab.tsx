import React from 'react';
import {
  Download,
  Play,
  Sparkles,
  ShieldCheck,
  Search,
  Eye,
  Shield,
  Film,
  Sliders,
  Send,
  Info,
  ChevronRight,
} from 'lucide-react';
import { DETECTED_PACKAGES } from '../data/contributors';
import { useApp } from '../context/AppContext';

export const HomeTab: React.FC<{ onNavigateToSettings?: () => void }> = ({ onNavigateToSettings }) => {
  const {
    activePackage,
    showToast,
    addLog,
    versionInfo,
    setOpenAboutDialog,
    setOpenApkInstallerModal,
  } = useApp();

  const currentPkg =
    DETECTED_PACKAGES.find(p => p.pkg === activePackage) || DETECTED_PACKAGES[0];

  const handleLaunchInstagram = () => {
    addLog('INFO', `Attempting launch of ${currentPkg.pkg}...`);
    showToast(`Launching ${currentPkg.label}...`);
    window.open('https://www.instagram.com/', '_blank', 'noopener,noreferrer');
  };

  const handleDownloadApk = () => {
    addLog('INFO', 'Opening InstaEclipse APK installer...');
    setOpenApkInstallerModal(true);
  };

  return (
    <div className="space-y-4 pb-24 max-w-xl mx-auto">
      {/* 1. Clean Status & Quick Action Hero */}
      <div className="rounded-3xl p-5 bg-white/[0.04] border border-white/10 shadow-xl space-y-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-white/10 border border-white/15 flex items-center justify-center shrink-0">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-white tracking-tight">InstaEclipse</h2>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-white/10 text-zinc-300 border border-white/10">
                  v{versionInfo.version}
                </span>
              </div>
              <p className="text-xs text-zinc-400 mt-0.5 flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                <span>Hook Ready &bull; {currentPkg.label}</span>
              </p>
            </div>
          </div>

          <button
            onClick={() => setOpenAboutDialog(true)}
            className="p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-white/10 transition-colors"
            title="About & Credits"
          >
            <Info className="w-4 h-4" />
          </button>
        </div>

        {/* Primary Actions */}
        <div className="grid grid-cols-2 gap-2.5 pt-1">
          <button
            id="home-download-apk-btn"
            onClick={handleDownloadApk}
            className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-white text-zinc-950 hover:bg-zinc-200 active:scale-[0.98] text-xs font-bold transition-all cursor-pointer shadow-md"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download APK</span>
          </button>

          <button
            id="home-launch-ig-btn"
            onClick={handleLaunchInstagram}
            className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 border border-white/15 text-white text-xs font-semibold active:scale-[0.98] transition-all cursor-pointer"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>Launch Instagram</span>
          </button>
        </div>
      </div>

      {/* 2. Clear 3-Step Setup Guide */}
      <div className="rounded-3xl p-5 bg-white/[0.03] border border-white/8 space-y-3">
        <div className="flex items-center gap-2">
          <Search className="w-4 h-4 text-zinc-400" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-300">
            How to Use
          </h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-1 text-xs">
          <div className="p-3 rounded-2xl bg-white/[0.02] border border-white/6 space-y-1">
            <span className="inline-flex w-5 h-5 rounded-full bg-white/10 text-white font-bold text-[10px] items-center justify-center">
              1
            </span>
            <p className="font-semibold text-white">Install APK</p>
            <p className="text-[11px] text-zinc-400 leading-relaxed">
              Install InstaEclipse and enable it in LSPosed or LSPatch.
            </p>
          </div>

          <div className="p-3 rounded-2xl bg-white/[0.02] border border-white/6 space-y-1">
            <span className="inline-flex w-5 h-5 rounded-full bg-white/10 text-white font-bold text-[10px] items-center justify-center">
              2
            </span>
            <p className="font-semibold text-white">Open Instagram</p>
            <p className="text-[11px] text-zinc-400 leading-relaxed">
              Launch Instagram ({currentPkg.versionName}).
            </p>
          </div>

          <div className="p-3 rounded-2xl bg-white/[0.02] border border-white/6 space-y-1">
            <span className="inline-flex w-5 h-5 rounded-full bg-white text-zinc-950 font-black text-[10px] items-center justify-center">
              3
            </span>
            <p className="font-semibold text-white">Hold Search</p>
            <p className="text-[11px] text-zinc-400 leading-relaxed">
              <strong className="text-white">Long-press Search icon</strong> to open in-app mod menu.
            </p>
          </div>
        </div>
      </div>

      {/* 3. Key Capabilities (Clean 4-Card Grid) */}
      <div className="rounded-3xl p-5 bg-white/[0.03] border border-white/8 space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-300">
            Core Features
          </h3>
          {onNavigateToSettings && (
            <button
              onClick={onNavigateToSettings}
              className="text-[11px] text-zinc-400 hover:text-white flex items-center gap-1 font-medium transition-colors"
            >
              Configure <ChevronRight className="w-3 h-3" />
            </button>
          )}
        </div>

        <div className="grid grid-cols-2 gap-2 text-xs">
          <div className="p-3 rounded-2xl bg-white/[0.02] border border-white/6 space-y-1">
            <div className="flex items-center gap-2 text-white font-semibold">
              <Eye className="w-3.5 h-3.5" />
              <span>Ghost Mode</span>
            </div>
            <p className="text-[11px] text-zinc-400">Stealth story views, hidden read receipts & typing.</p>
          </div>

          <div className="p-3 rounded-2xl bg-white/[0.02] border border-white/6 space-y-1">
            <div className="flex items-center gap-2 text-white font-semibold">
              <Shield className="w-3.5 h-3.5" />
              <span>Ad Blocker</span>
            </div>
            <p className="text-[11px] text-zinc-400">Block sponsored posts, story ads & tracking links.</p>
          </div>

          <div className="p-3 rounded-2xl bg-white/[0.02] border border-white/6 space-y-1">
            <div className="flex items-center gap-2 text-white font-semibold">
              <Film className="w-3.5 h-3.5" />
              <span>Downloader</span>
            </div>
            <p className="text-[11px] text-zinc-400">Save reels, stories, audio & posts in original quality.</p>
          </div>

          <div className="p-3 rounded-2xl bg-white/[0.02] border border-white/6 space-y-1">
            <div className="flex items-center gap-2 text-white font-semibold">
              <Sliders className="w-3.5 h-3.5" />
              <span>Developer Flags</span>
            </div>
            <p className="text-[11px] text-zinc-400">Unlock MetaConfig internal flags & prototype UI.</p>
          </div>
        </div>
      </div>

      {/* 4. Minimal, Respectful Credits & Community */}
      <div className="rounded-2xl p-3.5 bg-white/[0.02] border border-white/6 flex items-center justify-between gap-3 text-xs">
        <div className="min-w-0">
          <p className="text-zinc-300 font-medium truncate">
            Maintained by <strong className="text-white">Zehen</strong> &bull; Founded by <strong className="text-white">Somil Khan</strong>
          </p>
          <p className="text-[11px] text-zinc-500">InstaEclipse Open Source Project</p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <a
            href="https://t.me/InstaEclipsechat"
            target="_blank"
            rel="noreferrer"
            className="px-2.5 py-1.5 rounded-xl bg-white/10 hover:bg-white/15 text-white font-medium text-[11px] flex items-center gap-1.5 transition-colors"
          >
            <Send className="w-3 h-3" />
            <span>Telegram</span>
          </a>

          <button
            onClick={() => setOpenAboutDialog(true)}
            className="px-2.5 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-zinc-300 hover:text-white font-medium text-[11px] transition-colors"
          >
            Credits
          </button>
        </div>
      </div>
    </div>
  );
};
