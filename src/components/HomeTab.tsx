import React, { useState } from 'react';
import {
  CheckCircle2,
  ExternalLink,
  Download,
  Play,
  Code2,
  Globe,
  Send,
  Sparkles,
  ShieldCheck,
  Search,
  ChevronRight,
  Info,
  Smartphone,
  Crown,
  GitFork,
  ArrowUpCircle,
  Copy,
  Check,
} from 'lucide-react';
import { CONTRIBUTORS, SPECIAL_THANKS, DETECTED_PACKAGES, LEAD_MAINTAINER, ORIGINAL_FOUNDER } from '../data/contributors';
import { useApp } from '../context/AppContext';

export const HomeTab: React.FC = () => {
  const {
    activePackage,
    setActivePackage,
    restartInstagram,
    isRestarting,
    showToast,
    addLog,
    versionInfo,
    setOpenUpdateModal,
    setOpenAboutDialog,
    setOpenApkInstallerModal,
  } = useApp();

  const [showPackageModal, setShowPackageModal] = useState(false);
  const [showInfoModal, setShowInfoModal] = useState(false);
  const [contributorFilter, setContributorFilter] = useState<'all' | 'leads' | 'special'>('all');
  const [copiedTelegram, setCopiedTelegram] = useState(false);

  const currentPkg =
    DETECTED_PACKAGES.find(p => p.pkg === activePackage) || DETECTED_PACKAGES[0];

  const handleLaunchInstagram = () => {
    addLog('INFO', `Attempting launch of ${currentPkg.pkg}...`);
    showToast(`Launching ${currentPkg.label}...`);
    window.open('https://www.instagram.com/', '_blank', 'noopener,noreferrer');
  };

  const handleDownloadApk = () => {
    addLog('INFO', 'Opening InstaEclipse v2.0.0 in-app APK installer...');
    setOpenApkInstallerModal(true);
  };

  const handleCopyTelegram = () => {
    navigator.clipboard.writeText('https://t.me/Zehen0i');
    setCopiedTelegram(true);
    showToast('Copied Telegram link to clipboard!');
    setTimeout(() => setCopiedTelegram(false), 2000);
  };

  return (
    <div className="space-y-4 pb-28 max-w-3xl mx-auto">
      {/* v2.0.0 Consolidated Release Announcement Banner */}
      <div className="relative overflow-hidden rounded-3xl glass-card p-5 border border-white/15 bg-gradient-to-r from-white/[0.08] via-white/[0.04] to-transparent shadow-2xl space-y-3">
        <div className="flex items-start justify-between gap-3">
          <div className="space-y-1">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="px-2 py-0.5 rounded-full bg-white text-zinc-950 font-black text-[10px] tracking-wider uppercase shadow-sm">
                v2.0 Released
              </span>
              <h3 className="text-sm sm:text-base font-bold text-white flex items-center gap-1.5">
                <span>InstaEclipse v2.0.0 (Build 20)</span>
                <Sparkles className="w-4 h-4 text-white" />
              </h3>
            </div>
            <p className="text-xs text-zinc-300 leading-relaxed max-w-xl">
              All feature branches & patches have been consolidated into main: Ghost Mode v2, Media Downloader, DexKit 2.0.4 resolvers for Instagram 443.x, and GPS spoofing.
            </p>
          </div>
          <span className="hidden sm:inline-flex text-[10px] font-mono px-2 py-0.5 rounded-md bg-white/10 text-zinc-300 border border-white/10 shrink-0">
            Stable
          </span>
        </div>

        <div className="flex items-center gap-2 flex-wrap pt-1">
          <button
            id="banner-download-v2-apk-btn"
            onClick={() => setOpenApkInstallerModal(true)}
            className="px-3.5 py-2 rounded-xl bg-white text-zinc-950 hover:bg-zinc-200 active:scale-[0.98] text-xs font-bold shadow-lg shadow-white/10 transition-all flex items-center gap-1.5 cursor-pointer"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Download & Install APK</span>
          </button>
          <a
            id="banner-telegram-btn"
            href="https://t.me/InstaEclipsechat"
            target="_blank"
            rel="noreferrer"
            className="px-3.5 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-white font-medium text-xs flex items-center gap-1.5 border border-white/15 transition-all cursor-pointer"
          >
            <Send className="w-3.5 h-3.5" />
            <span>Telegram @InstaEclipsechat</span>
          </a>
        </div>
      </div>

      {/* Target Instagram Status Card - Monochrome Glass */}
      <div className="relative overflow-hidden rounded-3xl glass-card p-5 border border-white/12 shadow-2xl">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-start gap-3.5">
            {/* Monochrome Emblem */}
            <div className="w-12 h-12 rounded-2xl bg-white/10 border border-white/20 p-0.5 shadow-lg flex-shrink-0 flex items-center justify-center">
              <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z" />
              </svg>
            </div>

            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="font-bold text-white text-base leading-tight">
                  {currentPkg.label}
                </h2>
                <span className="inline-flex items-center gap-1.5 text-[10px] font-bold bg-white/10 text-white border border-white/15 px-2 py-0.5 rounded-full">
                  <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
                  Hook Active
                </span>
              </div>
              <p className="text-xs text-zinc-400 mt-1">
                Version: <span className="font-mono text-white font-medium">{currentPkg.versionName}</span>{' '}
                <span className="text-zinc-500 font-mono text-[11px]">({currentPkg.pkg})</span>
              </p>
            </div>
          </div>

          <button
            id="open-pkg-info"
            onClick={() => setShowInfoModal(true)}
            className="p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-white/10 transition-colors"
            title="Package Info"
          >
            <Info className="w-4 h-4" />
          </button>
        </div>

        {/* Action Buttons - Monochrome Style */}
        <div className="grid grid-cols-2 gap-2.5 mt-4">
          <button
            id="launch-instagram-btn"
            onClick={handleLaunchInstagram}
            className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-white text-zinc-950 hover:bg-zinc-200 active:scale-[0.98] text-xs font-bold shadow-lg shadow-white/10 transition-all cursor-pointer"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            Launch Instagram
          </button>

          <button
            id="download-apk-btn"
            onClick={handleDownloadApk}
            className="flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 border border-white/15 text-white text-xs font-semibold cursor-pointer transition-all active:scale-[0.98]"
          >
            <Download className="w-3.5 h-3.5" />
            Download v2.0 APK
          </button>
        </div>

        {/* Variant Picker Trigger */}
        <div className="mt-3.5 pt-3 border-t border-white/8 flex items-center justify-between text-xs text-zinc-400">
          <span>Target variant: <strong className="text-zinc-200 font-medium">{currentPkg.label}</strong></span>
          <button
            id="switch-target-variant-btn"
            onClick={() => setShowPackageModal(true)}
            className="text-white hover:text-zinc-300 font-semibold flex items-center gap-1 hover:underline cursor-pointer"
          >
            Switch Target <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Continuation & Lead Maintainer Showcase (Zehen & Somil Khan) */}
      <div className="rounded-3xl glass-card p-5 border border-white/12 space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-white/8">
          <div className="flex items-center gap-2">
            <Crown className="w-4 h-4 text-white" />
            <h3 className="text-xs font-bold uppercase tracking-wider text-white">
              Project Leadership & Continuation
            </h3>
          </div>
          <button
            onClick={() => setOpenAboutDialog(true)}
            className="text-[11px] text-zinc-400 hover:text-white flex items-center gap-1 font-medium transition-colors"
          >
            Full Credits <ChevronRight className="w-3 h-3" />
          </button>
        </div>

        {/* Zehen - Active Project Continuation Lead */}
        <div className="rounded-2xl bg-white/[0.04] border border-white/15 p-4 relative overflow-hidden group hover:border-white/25 transition-all">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-3">
              <div className="w-11 h-11 rounded-2xl bg-white/15 border border-white/20 flex items-center justify-center font-black text-sm text-white shadow-[0_0_15px_rgba(255,255,255,0.15)] shrink-0">
                {LEAD_MAINTAINER.avatarInitials || 'ZH'}
              </div>
              <div className="space-y-0.5">
                <div className="flex items-center gap-2 flex-wrap">
                  <h4 className="font-bold text-white text-sm">{LEAD_MAINTAINER.name}</h4>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-white text-zinc-950 shadow-sm">
                    {LEAD_MAINTAINER.badge}
                  </span>
                </div>
                <p className="text-xs font-semibold text-zinc-300">{LEAD_MAINTAINER.role}</p>
                <p className="text-xs text-zinc-400 leading-relaxed pt-1">
                  {LEAD_MAINTAINER.bio}
                </p>
              </div>
            </div>
          </div>

          <div className="mt-3.5 pt-3 border-t border-white/8 flex items-center justify-between gap-2 flex-wrap">
            <span className="text-[11px] text-zinc-400 font-mono">Telegram: @Zehen0i</span>
            <div className="flex items-center gap-2">
              <button
                onClick={handleCopyTelegram}
                className="px-2.5 py-1 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-[11px] text-zinc-300 hover:text-white flex items-center gap-1 transition-colors cursor-pointer"
                title="Copy Telegram Link"
              >
                {copiedTelegram ? <Check className="w-3 h-3 text-white" /> : <Copy className="w-3 h-3" />}
                <span>{copiedTelegram ? 'Copied' : 'Copy'}</span>
              </button>

              <a
                href={LEAD_MAINTAINER.telegramUrl || '#'}
                target="_blank"
                rel="noreferrer"
                className="px-3 py-1 rounded-xl bg-white text-zinc-950 font-bold text-[11px] hover:bg-zinc-200 transition-colors flex items-center gap-1.5 shadow-sm cursor-pointer"
              >
                <Send className="w-3 h-3" />
                <span>Contact Zehen</span>
              </a>
            </div>
          </div>
        </div>

        {/* Somil Khan - Original Founder */}
        <div className="rounded-2xl bg-white/[0.02] border border-white/8 p-3.5 flex items-center justify-between gap-3">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center font-bold text-xs text-white shrink-0">
              {ORIGINAL_FOUNDER.avatarInitials || 'SK'}
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <p className="font-semibold text-white text-xs truncate">{ORIGINAL_FOUNDER.name}</p>
                <span className="text-[9px] font-semibold px-1.5 py-0.5 rounded-full bg-white/10 text-zinc-300 border border-white/10">
                  {ORIGINAL_FOUNDER.badge}
                </span>
              </div>
              <p className="text-[11px] text-zinc-400 truncate">{ORIGINAL_FOUNDER.role}</p>
            </div>
          </div>

          <a
            href={ORIGINAL_FOUNDER.telegramUrl || 'https://t.me/InstaEclipsechat'}
            target="_blank"
            rel="noreferrer"
            className="px-2.5 py-1 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-[11px] text-zinc-300 hover:text-white flex items-center gap-1.5 transition-colors shrink-0 cursor-pointer"
            title="Telegram Chat"
          >
            <Send className="w-3 h-3 text-white" />
            <span>@InstaEclipsechat</span>
          </a>
        </div>

        {/* Official Community Telegram Chat */}
        <div className="rounded-2xl bg-white/[0.03] border border-white/10 p-3.5 flex items-center justify-between gap-3 hover:border-white/20 transition-all">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-9 h-9 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center text-white shrink-0">
              <Send className="w-4 h-4 text-white" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <p className="font-semibold text-white text-xs truncate">Telegram Chat</p>
                <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-full bg-white text-zinc-950">
                  Community
                </span>
              </div>
              <p className="text-[11px] text-zinc-400 truncate">Join @InstaEclipsechat for discussions & support</p>
            </div>
          </div>

          <a
            href="https://t.me/InstaEclipsechat"
            target="_blank"
            rel="noreferrer"
            className="px-3 py-1.5 rounded-xl bg-white text-zinc-950 font-bold text-[11px] hover:bg-zinc-200 transition-colors flex items-center gap-1.5 shrink-0 shadow-sm cursor-pointer"
          >
            <Send className="w-3 h-3" />
            <span>Join Chat</span>
          </a>
        </div>
      </div>

      {/* How to use banner - Monochrome Glass */}
      <div className="rounded-3xl glass-card p-4.5 border border-white/10 relative overflow-hidden">
        <div className="flex items-start gap-3">
          <div className="w-9 h-9 rounded-2xl bg-white/10 border border-white/15 text-white flex items-center justify-center shrink-0">
            <Search className="w-4 h-4" />
          </div>
          <div className="space-y-1">
            <h3 className="text-xs font-bold uppercase tracking-wider text-white flex items-center gap-1.5">
              <span>How to use InstaEclipse</span>
              <Sparkles className="w-3 h-3 text-white" />
            </h3>
            <p className="text-xs text-zinc-300 leading-relaxed">
              Inside Instagram, <strong className="text-white font-semibold underline underline-offset-2">long-press the search icon</strong> to summon the InstaEclipse overlay menu. Toggle ghost mode, ad blocking, and quality locks on-the-fly.
            </p>
          </div>
        </div>
      </div>

      {/* Module Status Card */}
      <div className="rounded-3xl glass-card p-5 border border-white/10 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-white" />
            <h3 className="text-xs font-bold uppercase tracking-wider text-white">
              LSPosed Runtime & DexKit
            </h3>
          </div>
          <span className="text-[10px] font-bold text-white bg-white/10 px-2.5 py-0.5 rounded-full border border-white/15">
            Hooks Active
          </span>
        </div>

        <div className="grid grid-cols-2 gap-2 text-xs">
          <div className="p-3 rounded-2xl bg-white/[0.03] border border-white/8">
            <p className="text-zinc-400 text-[10px] uppercase font-semibold">Framework</p>
            <p className="font-semibold text-white mt-0.5">LSPosed / LSPatch</p>
          </div>
          <div className="p-3 rounded-2xl bg-white/[0.03] border border-white/8">
            <p className="text-zinc-400 text-[10px] uppercase font-semibold">DexKit Resolver</p>
            <p className="font-semibold text-white mt-0.5">Cached & Adaptive v2.0.4</p>
          </div>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-white/8 text-[11px] text-zinc-400">
          <span>Target SDK 36 &bull; Min SDK 28</span>
          <button
            id="force-sync-hooks-btn"
            onClick={restartInstagram}
            disabled={isRestarting}
            className="text-white hover:text-zinc-300 font-semibold cursor-pointer disabled:opacity-50"
          >
            {isRestarting ? 'Syncing...' : 'Force Sync Hooks'}
          </button>
        </div>
      </div>

      {/* Organized Contributors Grid & Filter */}
      <div className="rounded-3xl glass-card p-5 border border-white/10 space-y-4">
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <div className="flex items-center gap-2">
            <Code2 className="w-4 h-4 text-white" />
            <h3 className="text-xs font-bold uppercase tracking-wider text-white">
              Credits & Contributors ({CONTRIBUTORS.length + SPECIAL_THANKS.length})
            </h3>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-1 bg-white/[0.04] p-1 rounded-xl border border-white/8">
            <button
              onClick={() => setContributorFilter('all')}
              className={`px-2 py-0.5 rounded-lg text-[10px] font-semibold transition-all ${
                contributorFilter === 'all'
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              All
            </button>
            <button
              onClick={() => setContributorFilter('leads')}
              className={`px-2 py-0.5 rounded-lg text-[10px] font-semibold transition-all ${
                contributorFilter === 'leads'
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              Core
            </button>
            <button
              onClick={() => setContributorFilter('special')}
              className={`px-2 py-0.5 rounded-lg text-[10px] font-semibold transition-all ${
                contributorFilter === 'special'
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              Special Thanks
            </button>
          </div>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          {contributorFilter !== 'special' &&
            CONTRIBUTORS.filter(c => (contributorFilter === 'leads' ? c.isLead : true)).map((c) => (
              <div
                key={c.name}
                className="p-3 rounded-2xl bg-white/[0.03] border border-white/8 hover:border-white/20 transition-all flex items-start justify-between gap-2"
              >
                <div className="flex items-start gap-2.5 min-w-0">
                  <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center font-bold text-xs text-white shrink-0">
                    {c.avatarInitials || c.name.slice(0, 2).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <p className="text-xs font-bold text-white truncate">{c.name}</p>
                      {c.badge && (
                        <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-full bg-white/10 text-zinc-300 border border-white/10">
                          {c.badge}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-zinc-400 truncate">{c.role}</p>
                  </div>
                </div>

                <div className="flex items-center gap-1 shrink-0 text-zinc-400">
                  {c.telegramUrl && (
                    <a
                      href={c.telegramUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="p-1 hover:text-white transition-colors"
                      title="Telegram"
                    >
                      <Send className="w-3.5 h-3.5" />
                    </a>
                  )}
                  {c.linkedinUrl && (
                    <a
                      href={c.linkedinUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="p-1 hover:text-white transition-colors"
                      title="LinkedIn"
                    >
                      <Globe className="w-3.5 h-3.5" />
                    </a>
                  )}
                </div>
              </div>
            ))}

          {contributorFilter !== 'leads' &&
            SPECIAL_THANKS.map(st => (
              <div
                key={st.name}
                className="p-3 rounded-2xl bg-white/[0.02] border border-white/6 hover:border-white/15 transition-all flex items-center justify-between gap-2"
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className="w-7 h-7 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center font-bold text-[10px] text-zinc-300 shrink-0">
                    {st.name.charAt(0)}
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-semibold text-zinc-200 truncate">{st.name}</p>
                    <p className="text-[10px] text-zinc-500 truncate">Honorable Contributor</p>
                  </div>
                </div>

                {st.telegramUrl && (
                  <a
                    href={st.telegramUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-white/10 transition-colors"
                    title="Telegram"
                  >
                    <Send className="w-3.5 h-3.5" />
                  </a>
                )}
              </div>
            ))}
        </div>

        {/* Continuous Loop Marquee at Bottom */}
        <div className="pt-3 border-t border-white/8">
          <p className="text-[10px] uppercase font-bold tracking-wider text-zinc-500 mb-2">
            Continuous Contributor Stream
          </p>
          <div className="relative w-full overflow-hidden mask-fade-edges">
            <div className="animate-marquee flex gap-2.5 py-1">
              {[...CONTRIBUTORS, ...CONTRIBUTORS].map((c, i) => (
                <div
                  key={`${c.name}-${i}`}
                  className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/[0.04] border border-white/10 text-xs text-zinc-200 shrink-0 hover:border-white/25 transition-colors shadow-sm"
                >
                  <div className="w-5 h-5 rounded-full bg-white/15 flex items-center justify-center font-bold text-[9px] text-white">
                    {c.avatarInitials || c.name.slice(0, 2).toUpperCase()}
                  </div>
                  <span className="font-semibold text-white text-[11px]">{c.name}</span>
                  <span className="text-zinc-500 text-[10px]">&bull;</span>
                  <span className="text-zinc-400 text-[10px]">{c.role}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Package Switcher Modal */}
      {showPackageModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in duration-150">
          <div className="bg-zinc-950 border border-white/12 rounded-3xl max-w-sm w-full p-5 shadow-2xl space-y-3.5">
            <h3 className="font-bold text-sm text-white">Select Target Instagram Package</h3>
            <p className="text-xs text-zinc-400">
              InstaEclipse will broadcast hook configurations and XML preferences to this package.
            </p>

            <div className="space-y-2 max-h-64 overflow-y-auto">
              {DETECTED_PACKAGES.map(pkg => (
                <button
                  key={pkg.pkg}
                  onClick={() => {
                    setActivePackage(pkg.pkg);
                    setShowPackageModal(false);
                    showToast(`Active target switched to ${pkg.label}`);
                    addLog('SETTINGS', `Active target switched to: ${pkg.pkg}`);
                  }}
                  className={`w-full text-left p-3 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
                    activePackage === pkg.pkg
                      ? 'bg-white/15 border-white text-white shadow-sm'
                      : 'bg-white/[0.03] border-white/8 text-zinc-300 hover:border-white/20'
                  }`}
                >
                  <div>
                    <p className="text-xs font-bold text-white">{pkg.label}</p>
                    <p className="text-[11px] font-mono text-zinc-400">v{pkg.versionName}</p>
                    <p className="text-[10px] text-zinc-500 font-mono">{pkg.pkg}</p>
                  </div>
                  {activePackage === pkg.pkg && (
                    <CheckCircle2 className="w-4 h-4 text-white shrink-0" />
                  )}
                </button>
              ))}
            </div>

            <button
              onClick={() => setShowPackageModal(false)}
              className="w-full py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-bold transition-colors cursor-pointer"
            >
              Close
            </button>
          </div>
        </div>
      )}

      {/* Package Info Modal */}
      {showInfoModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in duration-150">
          <div className="bg-zinc-950 border border-white/12 rounded-3xl max-w-sm w-full p-5 shadow-2xl space-y-3.5">
            <h3 className="font-bold text-sm text-white flex items-center gap-2">
              <Smartphone className="w-4 h-4 text-white" />
              Package Metadata
            </h3>
            <div className="space-y-2.5 text-xs bg-white/[0.03] p-3.5 rounded-2xl border border-white/8">
              <div className="flex justify-between">
                <span className="text-zinc-500">Package Name:</span>
                <span className="font-mono text-white font-medium">{currentPkg.pkg}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Version:</span>
                <span className="font-mono text-white">{currentPkg.versionName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Version Code:</span>
                <span className="font-mono text-white">{currentPkg.versionCode}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-zinc-500">Hook Engine:</span>
                <span className="text-white font-semibold">DexKit 2.0.4 Dynamic</span>
              </div>
            </div>
            <button
              onClick={() => setShowInfoModal(false)}
              className="w-full py-2.5 rounded-xl bg-white text-zinc-950 text-xs font-bold hover:bg-zinc-200 transition-colors cursor-pointer"
            >
              Done
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
