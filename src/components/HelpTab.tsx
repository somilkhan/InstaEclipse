import React, { useState } from 'react';
import {
  HelpCircle,
  ExternalLink,
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
  Smartphone,
} from 'lucide-react';
import { useApp } from '../context/AppContext';

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
  const { setOpenApkInstallerModal } = useApp();
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  const toggleFaq = (idx: number) => {
    setOpenFaq(prev => (prev === idx ? null : idx));
  };

  return (
    <div className="space-y-4 pb-28 max-w-3xl mx-auto">
      {/* Latest Release APK Banner */}
      <div className="rounded-2xl bg-white/[0.04] border border-white/12 p-4 flex items-center justify-between gap-3">
        <div className="space-y-0.5 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <h3 className="text-xs font-bold uppercase tracking-wider text-white">
              InstaEclipse v2.0 Release APK
            </h3>
            <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-full bg-white text-zinc-950">
              Build 20
            </span>
          </div>
          <p className="text-[11px] text-zinc-400 truncate">
            All feature branches & patches merged & verified for Instagram 443.x+
          </p>
        </div>

        <button
          id="help-download-apk-btn"
          onClick={() => setOpenApkInstallerModal(true)}
          className="px-3.5 py-2 rounded-xl bg-white text-zinc-950 hover:bg-zinc-200 active:scale-[0.98] text-xs font-bold shadow-lg shadow-white/10 transition-all flex items-center gap-1.5 shrink-0 cursor-pointer"
        >
          <Download className="w-3.5 h-3.5" />
          <span>Get APK</span>
        </button>
      </div>
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

          {/* Telegram Chat */}
          <a
            href="https://t.me/InstaEclipsechat"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-2.5 p-3 rounded-2xl bg-white/[0.04] border border-white/15 hover:border-white/30 text-white text-xs font-semibold transition-all hover:bg-white/[0.08] group cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/15 flex items-center justify-center shrink-0">
              <Send className="w-4 h-4 text-white" />
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <p className="truncate text-white font-bold">Telegram Chat</p>
                <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-full bg-white text-zinc-950">
                  Chat
                </span>
              </div>
              <p className="text-[10px] text-zinc-400 font-normal">@InstaEclipsechat &bull; Discussion & Help</p>
            </div>
            <ExternalLink className="w-3.5 h-3.5 text-zinc-400 group-hover:text-white ml-auto shrink-0 transition-colors" />
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

          {/* Community Q&A */}
          <a
            href="https://t.me/InstaEclipsechat"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-2.5 p-3 rounded-2xl bg-white/[0.02] border border-white/8 hover:border-white/20 text-white text-xs font-semibold transition-all hover:bg-white/[0.05] group cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center shrink-0">
              <Send className="w-4 h-4 text-zinc-300" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-white">Community Support</p>
              <p className="text-[10px] text-zinc-400 font-normal">Ask in @InstaEclipsechat</p>
            </div>
            <ExternalLink className="w-3.5 h-3.5 text-zinc-500 group-hover:text-white ml-auto shrink-0 transition-colors" />
          </a>
        </div>
      </div>

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
