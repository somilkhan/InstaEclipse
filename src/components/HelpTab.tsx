import React, { useState } from 'react';
import {
  ExternalLink,
  Send,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Terminal,
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
      'Inside the Instagram app, press and hold (long-press) the Search / Explore icon on the bottom navigation bar. The mod menu will appear with all toggleable features.',
  },
  {
    question: 'What is the difference between LSPosed and LSPatch?',
    answer:
      'LSPosed requires a rooted Android device (with Magisk, KernelSU, or APatch). LSPatch allows non-rooted devices to embed InstaEclipse directly into a patched Instagram APK.',
  },
  {
    question: 'Which Instagram versions are supported?',
    answer:
      'InstaEclipse supports official stable Instagram builds (443.x and above) using dynamic DexKit signature resolution.',
  },
  {
    question: 'How does Ghost Mode work?',
    answer:
      'Ghost Mode blocks outgoing read receipts, suppresses typing indicators, and prevents the story viewer list from recording your account.',
  },
  {
    question: 'Where are downloaded reels and posts saved?',
    answer:
      'Downloaded media is saved to your device Pictures/InstaEclipse folder and is immediately visible in your phone gallery.',
  },
];

export const HelpTab: React.FC<{ onOpenLogs?: () => void }> = ({ onOpenLogs }) => {
  const [openFaq, setOpenFaq] = useState<number | null>(0);

  const toggleFaq = (idx: number) => {
    setOpenFaq(prev => (prev === idx ? null : idx));
  };

  return (
    <div className="space-y-4 pb-24 max-w-xl mx-auto">
      {/* 1. Quick Troubleshooting */}
      <div className="rounded-3xl p-5 bg-white/[0.03] border border-white/8 space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-300">
          Setup & Checklist
        </h3>

        <div className="space-y-2 text-xs">
          <div className="flex items-start gap-2.5 p-3 rounded-2xl bg-white/[0.02] border border-white/6">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-white">Enable in LSPosed Scope</p>
              <p className="text-zinc-400 text-[11px] mt-0.5">
                Ensure <code className="text-white font-mono bg-white/10 px-1 py-0.5 rounded">com.instagram.android</code> is checked in your LSPosed manager.
              </p>
            </div>
          </div>

          <div className="flex items-start gap-2.5 p-3 rounded-2xl bg-white/[0.02] border border-white/6">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold text-white">Restart Instagram After Toggling</p>
              <p className="text-zinc-400 text-[11px] mt-0.5">
                Certain hooks initialize on cold startup. Force close and reopen Instagram to apply new changes.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 2. FAQ Accordion */}
      <div className="rounded-3xl p-5 bg-white/[0.03] border border-white/8 space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-300">
          Frequently Asked Questions
        </h3>

        <div className="space-y-1.5">
          {FAQ_ITEMS.map((item, idx) => {
            const isOpen = openFaq === idx;

            return (
              <div
                key={item.question}
                className="rounded-2xl bg-white/[0.02] border border-white/6 overflow-hidden transition-colors"
              >
                <button
                  onClick={() => toggleFaq(idx)}
                  className="w-full p-3.5 flex items-center justify-between text-left hover:bg-white/[0.03] transition-colors cursor-pointer"
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
                  <div className="p-3.5 pt-0 text-xs text-zinc-400 leading-relaxed border-t border-white/6 mt-1">
                    {item.answer}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* 3. Community Support & Diagnostic Logs */}
      <div className="rounded-3xl p-5 bg-white/[0.03] border border-white/8 space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-300">
          Community & Diagnostics
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
          <a
            href="https://t.me/InstaEclipsechat"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-2.5 p-3 rounded-2xl bg-white/[0.02] border border-white/6 hover:border-white/15 text-white font-medium hover:bg-white/[0.04] transition-all cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-white/10 flex items-center justify-center shrink-0">
              <Send className="w-4 h-4 text-white" />
            </div>
            <div className="min-w-0">
              <p className="font-semibold text-white">Telegram Support</p>
              <p className="text-[11px] text-zinc-400 truncate">@InstaEclipsechat</p>
            </div>
            <ExternalLink className="w-3.5 h-3.5 text-zinc-500 ml-auto shrink-0" />
          </a>

          {onOpenLogs && (
            <button
              onClick={onOpenLogs}
              className="flex items-center gap-2.5 p-3 rounded-2xl bg-white/[0.02] border border-white/6 hover:border-white/15 text-white font-medium hover:bg-white/[0.04] transition-all cursor-pointer text-left"
            >
              <div className="w-8 h-8 rounded-xl bg-white/10 flex items-center justify-center shrink-0">
                <Terminal className="w-4 h-4 text-white" />
              </div>
              <div className="min-w-0">
                <p className="font-semibold text-white">System Logs</p>
                <p className="text-[11px] text-zinc-400">View Hook Traces</p>
              </div>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
