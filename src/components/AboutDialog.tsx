import React, { useState } from 'react';
import {
  Sparkles,
  X,
  Code2,
  ExternalLink,
  ShieldAlert,
  Send,
  Globe,
  Crown,
  Heart,
  ArrowUpCircle,
  GitBranch,
  Copy,
  Check,
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { LEAD_MAINTAINER, ORIGINAL_FOUNDER, CONTRIBUTORS, SPECIAL_THANKS } from '../data/contributors';

export const AboutDialog: React.FC = () => {
  const { openAboutDialog, setOpenAboutDialog, versionInfo, setOpenUpdateModal, showToast } = useApp();
  const [activeCreditTab, setActiveCreditTab] = useState<'leadership' | 'team' | 'thanks'>('leadership');
  const [copiedTg, setCopiedTg] = useState(false);

  if (!openAboutDialog) return null;

  const handleCopyTelegram = () => {
    navigator.clipboard.writeText('https://t.me/Zehen0i');
    setCopiedTg(true);
    showToast('Copied Telegram link to clipboard');
    setTimeout(() => setCopiedTg(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/85 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="relative bg-zinc-950 border border-white/12 rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-5 max-h-[90vh] overflow-y-auto">
        {/* Close Button */}
        <div className="flex items-center justify-between pb-2 border-b border-white/8">
          <div className="flex items-center gap-2">
            <span className="text-[11px] font-mono uppercase tracking-wider text-zinc-400">InstaEclipse Companion</span>
          </div>
          <button
            onClick={() => setOpenAboutDialog(false)}
            className="p-1.5 rounded-xl text-zinc-400 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Hero Branding */}
        <div className="flex flex-col items-center text-center pt-1">
          <div className="w-16 h-16 rounded-2xl overflow-hidden border border-white/20 shadow-[0_0_25px_rgba(255,255,255,0.18)] mb-3 bg-zinc-950 flex items-center justify-center">
            <img
              src="/telegram_logo.jpg"
              alt="InstaEclipse Emblem"
              referrerPolicy="no-referrer"
              className="w-full h-full object-cover"
            />
          </div>

          <h2 className="text-xl font-black text-white tracking-tight">InstaEclipse</h2>
          
          <div className="flex items-center gap-2 mt-1 flex-wrap justify-center">
            <span className="text-xs font-bold text-white bg-white/10 px-2.5 py-0.5 rounded-full border border-white/15">
              v{versionInfo.version} (Build {versionInfo.buildNumber})
            </span>
            <span className="text-[10px] uppercase font-bold tracking-wider text-zinc-400">
              Revival Edition
            </span>
          </div>
          <p className="text-[11px] text-zinc-500 font-mono mt-0.5">ps.reso.instaeclipse &bull; LSPosed Active</p>
        </div>

        {/* Tab Navigation for Credits */}
        <div className="flex items-center p-1 rounded-2xl bg-white/[0.04] border border-white/8">
          <button
            onClick={() => setActiveCreditTab('leadership')}
            className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition-all ${
              activeCreditTab === 'leadership'
                ? 'bg-white text-zinc-950 shadow-sm'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            Maintainers
          </button>
          <button
            onClick={() => setActiveCreditTab('team')}
            className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition-all ${
              activeCreditTab === 'team'
                ? 'bg-white text-zinc-950 shadow-sm'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            Contributors ({CONTRIBUTORS.length})
          </button>
          <button
            onClick={() => setActiveCreditTab('thanks')}
            className={`flex-1 py-1.5 rounded-xl text-xs font-bold transition-all ${
              activeCreditTab === 'thanks'
                ? 'bg-white text-zinc-950 shadow-sm'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            Special Thanks
          </button>
        </div>

        {/* TAB 1: LEADERSHIP & CONTINUATION (ZEHEN & SOMIL) */}
        {activeCreditTab === 'leadership' && (
          <div className="space-y-3 animate-in fade-in duration-150">
            {/* Lead Maintainer: Zehen */}
            <div className="rounded-2xl bg-white/[0.05] border border-white/15 p-4 space-y-3 relative overflow-hidden">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                  <div className="w-11 h-11 rounded-2xl bg-white/15 border border-white/20 flex items-center justify-center font-black text-sm text-white shadow-sm shrink-0">
                    {LEAD_MAINTAINER.avatarInitials || 'ZH'}
                  </div>
                  <div>
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="font-bold text-white text-sm">{LEAD_MAINTAINER.name}</h3>
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-white text-zinc-950">
                        {LEAD_MAINTAINER.badge}
                      </span>
                    </div>
                    <p className="text-xs font-semibold text-zinc-300 mt-0.5">{LEAD_MAINTAINER.role}</p>
                    <p className="text-xs text-zinc-400 leading-relaxed mt-1">
                      {LEAD_MAINTAINER.bio}
                    </p>
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-white/8 flex items-center justify-between flex-wrap gap-2 text-xs">
                <span className="text-[11px] font-mono text-zinc-400">Telegram: @Zehen0i</span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleCopyTelegram}
                    className="px-2.5 py-1 rounded-xl bg-white/10 hover:bg-white/15 border border-white/10 text-white text-[11px] font-semibold flex items-center gap-1 transition-colors cursor-pointer"
                  >
                    {copiedTg ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                    <span>{copiedTg ? 'Copied' : 'Copy'}</span>
                  </button>
                  <a
                    href={LEAD_MAINTAINER.telegramUrl || '#'}
                    target="_blank"
                    rel="noreferrer"
                    className="px-3 py-1 rounded-xl bg-white text-zinc-950 text-[11px] font-bold hover:bg-zinc-200 transition-colors flex items-center gap-1.5 cursor-pointer"
                  >
                    <Send className="w-3 h-3" />
                    <span>Open Channel</span>
                  </a>
                </div>
              </div>
            </div>

            {/* Original Creator: Somil Khan */}
            <div className="rounded-2xl bg-white/[0.02] border border-white/8 p-3.5 space-y-2">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center font-bold text-xs text-white shrink-0">
                    {ORIGINAL_FOUNDER.avatarInitials || 'SK'}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="font-bold text-white text-xs">{ORIGINAL_FOUNDER.name}</h4>
                      <span className="text-[9px] font-semibold px-2 py-0.5 rounded-full bg-white/10 text-zinc-300 border border-white/10">
                        {ORIGINAL_FOUNDER.badge}
                      </span>
                    </div>
                    <p className="text-[11px] text-zinc-400 mt-0.5">{ORIGINAL_FOUNDER.role}</p>
                    <p className="text-[11px] text-zinc-500 leading-relaxed mt-1">
                      {ORIGINAL_FOUNDER.bio}
                    </p>
                  </div>
                </div>

                <a
                  href={ORIGINAL_FOUNDER.githubUrl || '#'}
                  target="_blank"
                  rel="noreferrer"
                  className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white transition-colors shrink-0 cursor-pointer"
                  title="GitHub Profile"
                >
                  <Code2 className="w-4 h-4" />
                </a>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: CONTRIBUTORS */}
        {activeCreditTab === 'team' && (
          <div className="space-y-2 max-h-64 overflow-y-auto pr-1 animate-in fade-in duration-150">
            {CONTRIBUTORS.map((c) => (
              <div
                key={c.name}
                className="p-3 rounded-2xl bg-white/[0.03] border border-white/8 flex items-center justify-between gap-2.5 hover:border-white/20 transition-all"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center font-bold text-xs text-white shrink-0">
                    {c.avatarInitials || c.name.slice(0, 2).toUpperCase()}
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      <p className="text-xs font-bold text-white truncate">{c.name}</p>
                      {c.badge && (
                        <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-full bg-white/10 text-zinc-300">
                          {c.badge}
                        </span>
                      )}
                    </div>
                    <p className="text-[11px] text-zinc-400 truncate">{c.role}</p>
                  </div>
                </div>

                <div className="flex items-center gap-1 text-zinc-400 shrink-0">
                  {c.telegramUrl && (
                    <a
                      href={c.telegramUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="p-1.5 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                      title="Telegram"
                    >
                      <Send className="w-3.5 h-3.5" />
                    </a>
                  )}
                  {c.githubUrl && (
                    <a
                      href={c.githubUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="p-1.5 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                      title="GitHub"
                    >
                      <Code2 className="w-3.5 h-3.5" />
                    </a>
                  )}
                  {c.linkedinUrl && (
                    <a
                      href={c.linkedinUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="p-1.5 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                      title="LinkedIn"
                    >
                      <Globe className="w-3.5 h-3.5" />
                    </a>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* TAB 3: SPECIAL THANKS */}
        {activeCreditTab === 'thanks' && (
          <div className="space-y-2 animate-in fade-in duration-150">
            <div className="grid grid-cols-2 gap-2">
              {SPECIAL_THANKS.map((st) => (
                <div
                  key={st.name}
                  className="p-3 rounded-2xl bg-white/[0.03] border border-white/8 flex items-center justify-between gap-2"
                >
                  <div className="min-w-0">
                    <p className="text-xs font-bold text-white truncate">{st.name}</p>
                    <p className="text-[10px] text-zinc-400">Community supporter</p>
                  </div>

                  {st.telegramUrl && (
                    <a
                      href={st.telegramUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="p-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-zinc-300 hover:text-white transition-colors shrink-0"
                    >
                      <Send className="w-3.5 h-3.5" />
                    </a>
                  )}
                </div>
              ))}
            </div>
            <p className="text-[11px] text-zinc-500 text-center pt-2">
              Heartfelt thanks to the entire Xposed, LSPosed, and DexKit reverse-engineering communities.
            </p>
          </div>
        )}

        {/* Disclaimer note */}
        <div className="text-[11px] text-zinc-400 leading-relaxed bg-white/[0.02] p-3 rounded-2xl border border-white/6 flex items-start gap-2">
          <ShieldAlert className="w-3.5 h-3.5 text-zinc-300 shrink-0 mt-0.5" />
          <span>
            InstaEclipse is an independent open-source module and companion application. Not affiliated with or endorsed by Instagram or Meta Platforms, Inc.
          </span>
        </div>

        {/* Footer Actions */}
        <div className="flex gap-2 pt-2 border-t border-white/8">
          <button
            onClick={() => {
              setOpenAboutDialog(false);
              setOpenUpdateModal(true);
            }}
            className="flex-1 py-2.5 px-3 rounded-xl bg-white/10 hover:bg-white/15 border border-white/15 text-white text-xs font-bold flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
          >
            <ArrowUpCircle className="w-4 h-4" />
            <span>Check Updates</span>
          </button>

          <button
            onClick={() => setOpenAboutDialog(false)}
            className="flex-1 py-2.5 px-3 rounded-xl bg-white hover:bg-zinc-200 text-zinc-950 text-xs font-bold transition-colors cursor-pointer"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
