import React from 'react';
import { Sparkles, X, CheckCircle2, ArrowUpCircle, RefreshCw, Radio, Terminal, Send, ShieldCheck, Download, Smartphone } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const UpdateModal: React.FC = () => {
  const {
    openUpdateModal,
    setOpenUpdateModal,
    setOpenApkInstallerModal,
    versionInfo,
    isCheckingUpdates,
    checkForUpdates,
    applyUpdate,
    setReleaseChannel,
  } = useApp();

  if (!openUpdateModal) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="relative w-full max-w-md rounded-3xl bg-zinc-950/90 border border-white/12 shadow-[0_20px_60px_-15px_rgba(0,0,0,0.9)] p-5 space-y-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-2 border-b border-white/8">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center text-white">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white tracking-tight">Software Updates</h3>
              <p className="text-[11px] text-zinc-400 font-mono">OTA Release Engine</p>
            </div>
          </div>

          <button
            id="close-update-modal"
            onClick={() => setOpenUpdateModal(false)}
            className="p-1.5 rounded-xl text-zinc-400 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Current Installed Version Card */}
        <div className="rounded-2xl bg-white/[0.03] border border-white/10 p-3.5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] uppercase tracking-wider text-zinc-400 font-semibold">Installed Version</span>
            <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-[10px] font-bold bg-white/10 text-white border border-white/15">
              <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
              v{versionInfo.version} (Build {versionInfo.buildNumber})
            </span>
          </div>

          <p className="text-xs text-zinc-300">
            Current build: <span className="text-white font-medium">Revival Edition</span> &bull; Maintained by <span className="text-white font-semibold">Zehen</span>
          </p>

          {/* Release Channel Selector */}
          <div className="pt-2 border-t border-white/8">
            <p className="text-[10px] uppercase tracking-wider text-zinc-400 font-medium mb-1.5">Release Channel</p>
            <div className="grid grid-cols-3 gap-1.5">
              {(['Stable', 'Beta', 'Nightly'] as const).map(ch => (
                <button
                  key={ch}
                  onClick={() => setReleaseChannel(ch)}
                  className={`py-1.5 px-2 rounded-xl text-xs font-semibold border transition-all ${
                    versionInfo.channel === ch
                      ? 'bg-white text-zinc-950 border-white shadow-sm'
                      : 'bg-white/[0.04] text-zinc-400 border-white/8 hover:text-zinc-200 hover:bg-white/[0.08]'
                  }`}
                >
                  {ch}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Update Status / Available Version Card */}
        <div className="rounded-2xl bg-white/[0.04] border border-white/10 p-4 space-y-3">
          <div className="flex items-start justify-between gap-2">
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-xs font-bold text-white uppercase tracking-wider">
                  {versionInfo.isLatest ? 'System Up to Date' : 'New Release Available'}
                </h4>
                {versionInfo.isLatest && (
                  <CheckCircle2 className="w-3.5 h-3.5 text-white" />
                )}
              </div>
              <p className="text-xs text-zinc-300 mt-1">
                {versionInfo.isLatest
                  ? `InstaEclipse v${versionInfo.version} is currently running the newest features.`
                  : `Version ${versionInfo.latestVersion} is ready to install.`}
              </p>
            </div>

            <button
              id="check-updates-btn"
              onClick={checkForUpdates}
              disabled={isCheckingUpdates}
              className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/15 active:scale-95 border border-white/15 text-xs text-white font-semibold flex items-center gap-1.5 shrink-0 transition-all"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isCheckingUpdates ? 'animate-spin' : ''}`} />
              <span>{isCheckingUpdates ? 'Checking...' : 'Check'}</span>
            </button>
          </div>

          {/* Changelog Highlights */}
          <div className="rounded-xl bg-black/40 border border-white/8 p-3 space-y-2">
            <p className="text-[10px] uppercase font-bold tracking-wider text-zinc-400">
              Release Notes & Improvements
            </p>
            <ul className="space-y-1.5 text-xs text-zinc-300">
              {versionInfo.changelog.map((log, idx) => (
                <li key={idx} className="flex items-start gap-2 text-[11px] leading-relaxed">
                  <span className="text-white shrink-0 mt-0.5">&bull;</span>
                  <span>{log}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Action Button */}
          {!versionInfo.isLatest ? (
            <button
              id="apply-update-btn"
              onClick={applyUpdate}
              className="w-full py-2.5 rounded-xl bg-white text-zinc-950 hover:bg-zinc-200 active:scale-[0.99] font-bold text-xs shadow-lg shadow-white/10 transition-all flex items-center justify-center gap-2"
            >
              <ArrowUpCircle className="w-4 h-4" />
              <span>Install & Apply Update v{versionInfo.latestVersion}</span>
            </button>
          ) : (
            <div className="space-y-2">
              <div className="flex items-center justify-center gap-2 py-2 text-xs font-semibold text-zinc-400 bg-white/[0.02] rounded-xl border border-white/5">
                <ShieldCheck className="w-4 h-4 text-white" />
                <span>Synchronized & Stable on {versionInfo.channel}</span>
              </div>
              <button
                id="open-apk-downloader-btn"
                onClick={() => {
                  setOpenUpdateModal(false);
                  setOpenApkInstallerModal(true);
                }}
                className="w-full py-2.5 rounded-xl bg-white text-zinc-950 hover:bg-zinc-200 active:scale-[0.99] font-bold text-xs transition-all flex items-center justify-center gap-2 cursor-pointer shadow-lg shadow-white/10"
              >
                <Download className="w-4 h-4" />
                <span>Download & Install v2.0 APK ({versionInfo.apkFileSize || '18.4 MB'})</span>
              </button>
            </div>
          )}
        </div>

        {/* Telegram Support and Community Pill */}
        <div className="space-y-2">
          <div className="p-3 rounded-2xl bg-white/[0.03] border border-white/10 flex items-center justify-between gap-3 text-xs">
            <div className="min-w-0">
              <p className="font-semibold text-white truncate">Community Telegram Chat</p>
              <p className="text-[11px] text-zinc-400 truncate">Join @InstaEclipsechat for news & help</p>
            </div>
            <a
              href="https://t.me/InstaEclipsechat"
              target="_blank"
              rel="noreferrer"
              className="px-3 py-1.5 rounded-xl bg-white text-zinc-950 hover:bg-zinc-200 border border-white/15 text-xs font-bold flex items-center gap-1.5 transition-colors shrink-0 shadow-sm"
            >
              <Send className="w-3 h-3" />
              <span>Join Chat</span>
            </a>
          </div>

          <div className="p-3 rounded-2xl bg-white/[0.02] border border-white/8 flex items-center justify-between gap-3 text-xs">
            <div className="min-w-0">
              <p className="font-semibold text-white truncate">Maintainer Support</p>
              <p className="text-[11px] text-zinc-400 truncate">Direct questions to @Zehen0i</p>
            </div>
            <a
              href="https://t.me/Zehen0i"
              target="_blank"
              rel="noreferrer"
              className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 border border-white/15 text-white font-semibold text-[11px] flex items-center gap-1.5 transition-colors shrink-0"
            >
              <Send className="w-3 h-3" />
              <span>@Zehen0i</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
