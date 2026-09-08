import React, { useState } from 'react';
import {
  HelpCircle,
  ExternalLink,
  Code2,
  Send,
  ChevronDown,
  ChevronUp,
  ShieldCheck,
  AlertCircle,
  Terminal,
  Search,
  Sliders,
  CheckCircle2,
  Download,
  Copy,
  Check,
  Sparkles,
  Maximize2,
  X,
  Share2,
} from 'lucide-react';

interface FaqItem {
  question: string;
  answer: string;
}

const FAQ_ITEMS: FaqItem[] = [
  {
    question: 'How do I open the InstaEclipse in-app menu?',
    answer:
      'Inside the Instagram app, press and hold (long-press) the Search / Explore icon on the bottom navigation bar. A native dialog will pop up giving you access to all features and toggles on the fly.',
  },
  {
    question: 'What is the difference between LSPosed and LSPatch?',
    answer:
      'LSPosed requires a rooted Android device (with Magisk, KernelSU, or APatch). LSPatch allows non-rooted devices to embed the InstaEclipse module directly into an Instagram APK without needing root privileges.',
  },
  {
    question: 'Which Instagram versions are supported?',
    answer:
      'InstaEclipse utilizes DexKit dynamic bytecode scanning to hook classes across different version releases. For optimal stability, use official stable builds from APKMirror rather than daily alpha/beta releases.',
  },
  {
    question: 'How does Ghost Mode protect my privacy?',
    answer:
      'Ghost Mode blocks outgoing network payloads and hooks into internal Instagram dispatchers: DirectSeenHelper prevents read receipts from reaching the server, TypingController suppresses typing status, and StoryViewer blocks view registration.',
  },
  {
    question: 'What are MetaConfig Developer Options?',
    answer:
      'MetaConfig Developer Options unlock Meta employee internal debugging flags and UI prototypes. You can experiment with test navigations, new reel interfaces, or import curated community overrides.',
  },
];

export const HelpTab: React.FC = () => {
  const [openFaq, setOpenFaq] = useState<number | null>(0);
  const [copiedLink, setCopiedLink] = useState(false);
  const [showLogoModal, setShowLogoModal] = useState(false);

  const toggleFaq = (idx: number) => {
    setOpenFaq(prev => (prev === idx ? null : idx));
  };

  const handleCopyLogoUrl = () => {
    const fullUrl = window.location.origin + '/telegram_logo.jpg';
    navigator.clipboard.writeText(fullUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  return (
    <div className="space-y-4 pb-28 max-w-3xl mx-auto">
      {/* Community Links */}
      <div className="rounded-2xl bg-white/[0.03] border border-white/10 p-4 space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-400">
            Official Links & Maintainer Support
          </h3>
          <span className="text-[10px] font-mono text-zinc-500">Revival Edition</span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          {/* Zehen Maintainer Telegram */}
          <a
            href="https://t.me/Zehen0i"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-2.5 p-3 rounded-2xl bg-white/[0.04] border border-white/15 hover:border-white/30 text-white text-xs font-semibold transition-all hover:bg-white/[0.08] group cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center shrink-0">
              <Send className="w-4 h-4 text-white" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <p className="truncate text-white font-bold">Zehen (@Zehen0i)</p>
                <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-full bg-white text-zinc-950">
                  Lead
                </span>
              </div>
              <p className="text-[10px] text-zinc-400 font-normal">Continuation & OTA Updates</p>
            </div>
            <ExternalLink className="w-3.5 h-3.5 text-zinc-400 group-hover:text-white ml-auto shrink-0 transition-colors" />
          </a>

          {/* GitHub Repository */}
          <a
            href="https://github.com/somilkhan/InstaEclipse"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-2.5 p-3 rounded-2xl bg-white/[0.02] border border-white/8 hover:border-white/20 text-white text-xs font-semibold transition-all hover:bg-white/[0.05] group cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center shrink-0">
              <Code2 className="w-4 h-4 text-white" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-white">GitHub Repository</p>
              <p className="text-[10px] text-zinc-400 font-normal">Original Core & Releases</p>
            </div>
            <ExternalLink className="w-3.5 h-3.5 text-zinc-500 group-hover:text-white ml-auto shrink-0 transition-colors" />
          </a>

          {/* Channel */}
          <a
            href="https://t.me/instaeclipse_channel"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-2.5 p-3 rounded-2xl bg-white/[0.02] border border-white/8 hover:border-white/20 text-white text-xs font-semibold transition-all hover:bg-white/[0.05] group cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center shrink-0">
              <Send className="w-4 h-4 text-zinc-300" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-white">Telegram Channel</p>
              <p className="text-[10px] text-zinc-400 font-normal">Announcements & Releases</p>
            </div>
            <ExternalLink className="w-3.5 h-3.5 text-zinc-500 group-hover:text-white ml-auto shrink-0 transition-colors" />
          </a>

          {/* Discussion Group */}
          <a
            href="https://t.me/instaeclipse_group"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-2.5 p-3 rounded-2xl bg-white/[0.02] border border-white/8 hover:border-white/20 text-white text-xs font-semibold transition-all hover:bg-white/[0.05] group cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center shrink-0">
              <Send className="w-4 h-4 text-zinc-300" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-white">Discussion Group</p>
              <p className="text-[10px] text-zinc-400 font-normal">Community Q&A</p>
            </div>
            <ExternalLink className="w-3.5 h-3.5 text-zinc-500 group-hover:text-white ml-auto shrink-0 transition-colors" />
          </a>
        </div>
      </div>

      {/* Official Telegram Channel & Chat Logo Kit */}
      <div className="rounded-2xl bg-white/[0.03] border border-white/10 p-4 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-white/10 border border-white/15 flex items-center justify-center">
              <Send className="w-3.5 h-3.5 text-white" />
            </div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-300">
              Telegram Channel & Chat Logo Kit
            </h3>
          </div>
          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-white text-zinc-950">
            1:1 Avatar Safe
          </span>
        </div>

        <p className="text-xs text-zinc-400 leading-relaxed">
          Official high-resolution emblem tailored for Telegram channels, group avatars, and announcement posts. Centered circular-safe composition with the iconic lunar eclipse crescent and frosted glass aperture.
        </p>

        {/* Visual Preview Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-12 gap-3.5 items-center">
          {/* Logo Showcase (Square & Circular Preview) */}
          <div className="sm:col-span-5 flex flex-col items-center justify-center p-4 rounded-2xl bg-black/60 border border-white/10 relative group">
            {/* Circular Telegram Crop Preview */}
            <div className="relative">
              <div
                onClick={() => setShowLogoModal(true)}
                className="w-28 h-28 sm:w-32 sm:h-32 rounded-full overflow-hidden border-2 border-white/30 shadow-[0_0_25px_rgba(255,255,255,0.2)] cursor-pointer relative group-hover:scale-105 transition-transform"
                title="Click to view full resolution"
              >
                <img
                  src="/telegram_logo.jpg"
                  alt="InstaEclipse Telegram Channel Logo"
                  referrerPolicy="no-referrer"
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                  <Maximize2 className="w-6 h-6 text-white drop-shadow-md" />
                </div>
              </div>

              {/* Online Telegram Status Dot */}
              <span className="absolute bottom-1 right-2 w-4 h-4 rounded-full bg-white ring-2 ring-zinc-950 shadow-[0_0_8px_rgba(255,255,255,0.9)]" />
            </div>

            <p className="text-[11px] font-mono text-zinc-400 mt-2.5">
              Circular Telegram Avatar Crop
            </p>
          </div>

          {/* Realistic Telegram Channel Post Simulation */}
          <div className="sm:col-span-7 flex flex-col gap-2.5">
            {/* Telegram Channel Header Mockup */}
            <div className="p-3 rounded-xl bg-white/[0.04] border border-white/10 flex items-center gap-3">
              <div className="w-10 h-10 rounded-full overflow-hidden border border-white/20 shrink-0">
                <img
                  src="/telegram_logo.jpg"
                  alt="Avatar"
                  referrerPolicy="no-referrer"
                  className="w-full h-full object-cover"
                />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-bold text-white truncate">InstaEclipse | Official</span>
                  <span className="w-3.5 h-3.5 rounded-full bg-white text-zinc-950 flex items-center justify-center text-[9px] font-black shrink-0">
                    ✓
                  </span>
                </div>
                <p className="text-[10px] text-zinc-400 truncate">14.8K subscribers &bull; @instaeclipse_channel</p>
              </div>
            </div>

            {/* Telegram Message Simulation */}
            <div className="p-3 rounded-xl bg-white/[0.02] border border-white/8 space-y-1.5 text-xs">
              <div className="flex items-center justify-between text-[10px] text-zinc-400">
                <span className="font-semibold text-white">Channel Broadcast</span>
                <span>Today 12:45</span>
              </div>
              <p className="text-zinc-300 text-[11px] leading-relaxed">
                🚀 <strong className="text-white">InstaEclipse Revival Edition</strong> is now live! Featuring Monochrome GlassUI, DexKit dynamic hooks, and full Ghost Mode.
              </p>
            </div>

            {/* Actions Bar */}
            <div className="flex items-center gap-2 pt-1">
              <a
                href="/telegram_logo.jpg"
                download="InstaEclipse_Telegram_Logo.jpg"
                className="flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-xl bg-white hover:bg-zinc-200 text-zinc-950 text-xs font-bold shadow-lg shadow-white/10 transition-all cursor-pointer"
              >
                <Download className="w-3.5 h-3.5 stroke-[2.5]" />
                <span>Download Logo</span>
              </a>

              <button
                onClick={handleCopyLogoUrl}
                className="flex items-center gap-1.5 py-2 px-3 rounded-xl bg-white/10 hover:bg-white/15 border border-white/10 text-white text-xs font-semibold transition-colors cursor-pointer"
                title="Copy Direct Link to Logo"
              >
                {copiedLink ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-white" />
                    <span>Copied</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    <span>Copy Link</span>
                  </>
                )}
              </button>

              <button
                onClick={() => setShowLogoModal(true)}
                className="p-2 rounded-xl bg-white/10 hover:bg-white/15 border border-white/10 text-white text-xs transition-colors cursor-pointer"
                title="Expand Full Resolution"
              >
                <Maximize2 className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {/* Telegram Profile Setup Tips */}
        <div className="p-3 rounded-xl bg-white/[0.02] border border-white/6 text-[11px] text-zinc-400 space-y-1">
          <p className="font-semibold text-zinc-300">💡 Telegram Setup Tip:</p>
          <p>
            In Telegram, tap <strong className="text-white">Edit Channel</strong> &rarr; <strong className="text-white">Set Photo or Video</strong>, select this downloaded file, and keep the circular frame centered.
          </p>
        </div>
      </div>

      {/* Full Resolution Logo Modal */}
      {showLogoModal && (
        <div className="fixed inset-0 z-50 bg-black/90 backdrop-blur-2xl flex items-center justify-center p-4 animate-in fade-in duration-200">
          <div className="relative max-w-md w-full bg-zinc-950 border border-white/20 rounded-3xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-white" />
                <h4 className="text-sm font-bold text-white">Telegram Channel & Chat Logo</h4>
              </div>
              <button
                onClick={() => setShowLogoModal(false)}
                className="p-1.5 rounded-xl hover:bg-white/10 text-zinc-400 hover:text-white transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* High-res Image Preview */}
            <div className="w-full aspect-square rounded-2xl overflow-hidden border border-white/15 bg-black flex items-center justify-center relative shadow-2xl">
              <img
                src="/telegram_logo.jpg"
                alt="InstaEclipse High-Res Logo"
                referrerPolicy="no-referrer"
                className="w-full h-full object-cover"
              />
            </div>

            <div className="flex items-center justify-between text-xs text-zinc-400">
              <span>Aspect Ratio: 1:1 (Square)</span>
              <span className="font-mono text-[11px]">1024 × 1024 px</span>
            </div>

            <div className="flex items-center gap-2 pt-1">
              <a
                href="/telegram_logo.jpg"
                download="InstaEclipse_Telegram_Logo.jpg"
                className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl bg-white hover:bg-zinc-200 text-zinc-950 text-xs font-bold shadow-lg shadow-white/10 transition-all cursor-pointer"
              >
                <Download className="w-4 h-4 stroke-[2.5]" />
                <span>Download High-Res (JPG)</span>
              </a>

              <button
                onClick={handleCopyLogoUrl}
                className="px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 border border-white/10 text-white text-xs font-semibold transition-colors cursor-pointer"
              >
                {copiedLink ? 'Link Copied!' : 'Copy Link'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Troubleshooting Checklist */}
      <div className="rounded-2xl bg-white/[0.03] border border-white/10 p-4 space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-400">
          Troubleshooting Checklist
        </h3>

        <div className="space-y-2 text-xs">
          <div className="flex items-start gap-2.5 p-3 rounded-xl bg-white/[0.02] border border-white/8">
            <CheckCircle2 className="w-4 h-4 text-white shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-white">Restart Instagram after changing settings</p>
              <p className="text-zinc-400 mt-0.5">
                Certain hooks load classes on Instagram startup. Tap "Restart Instagram" in Features or Home to reload.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2.5 p-3 rounded-xl bg-white/[0.02] border border-white/8">
            <CheckCircle2 className="w-4 h-4 text-white shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-white">Verify LSPosed Scope</p>
              <p className="text-zinc-400 mt-0.5">
                Ensure <code className="font-mono text-white bg-white/10 px-1 rounded">com.instagram.android</code> is checked in the LSPosed manager module scope.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2.5 p-3 rounded-xl bg-white/[0.02] border border-white/8">
            <CheckCircle2 className="w-4 h-4 text-white shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-white">Clear Instagram Cache if Hook Crashes</p>
              <p className="text-zinc-400 mt-0.5">
                If Instagram experiences a crash loop after an Instagram update, clear the app cache to allow DexKit to rebuild signature mappings.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Frequently Asked Questions */}
      <div className="rounded-2xl bg-white/[0.03] border border-white/10 p-4 space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-400">
          Frequently Asked Questions
        </h3>

        <div className="space-y-2">
          {FAQ_ITEMS.map((item, idx) => {
            const isOpen = openFaq === idx;

            return (
              <div
                key={item.question}
                className="rounded-2xl bg-white/[0.02] border border-white/8 overflow-hidden transition-colors hover:border-white/15"
              >
                <button
                  onClick={() => toggleFaq(idx)}
                  className="w-full p-3.5 flex items-center justify-between text-left hover:bg-white/[0.02] transition-colors cursor-pointer"
                >
                  <span className="text-xs font-semibold text-white pr-2">
                    {item.question}
                  </span>
                  {isOpen ? (
                    <ChevronUp className="w-4 h-4 text-zinc-400 shrink-0" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-zinc-400 shrink-0" />
                  )}
                </button>

                {isOpen && (
                  <div className="p-3.5 pt-0 text-xs text-zinc-300 leading-relaxed border-t border-white/6 mt-1">
                    {item.answer}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
