import React, { useState } from 'react';
import {
  ArrowLeft,
  RotateCcw,
  Palette,
  Check,
  ChevronDown,
  ChevronUp,
  Heart,
  MessageCircle,
  Send,
  Bookmark,
  Sparkles,
} from 'lucide-react';
import { THEME_PRESETS } from '../data/presets';
import { ThemePalette } from '../types';
import { useApp } from '../context/AppContext';

interface ThemeCustomizerModalProps {
  onClose: () => void;
}

const COLOR_SLOT_DESCRIPTIONS: { key: keyof ThemePalette; label: string; desc: string }[] = [
  { key: 'background', label: 'Background', desc: 'Main window and screen canvas background' },
  { key: 'surface', label: 'Surface', desc: 'Cards, bottom sheets, dialogs, and elevated items' },
  { key: 'primaryText', label: 'Primary Text', desc: 'Headings, usernames, captions, and body text' },
  { key: 'secondaryText', label: 'Secondary Text', desc: 'Timestamps, subheaders, and metadata labels' },
  { key: 'accent', label: 'Accent Color', desc: 'Story rings, active toggles, badges, and focus rings' },
  { key: 'button', label: 'Button Color', desc: 'Follow buttons, primary action triggers' },
  { key: 'icon', label: 'Icon Tint', desc: 'Header and feed action buttons (Heart, Comment, Share)' },
  { key: 'glyph', label: 'Glyphs & Badges', desc: 'Verified badges and special icon indicators' },
  { key: 'divider', label: 'Divider Lines', desc: 'Subtle section separators and post borders' },
  { key: 'border', label: 'Card Borders', desc: 'Container outlines and avatar borders' },
  { key: 'statusBar', label: 'Status Bar', desc: 'Top app bar and system status bar area' },
  { key: 'navigation', label: 'Navigation Bar', desc: 'Bottom tab navigation background' },
  { key: 'link', label: 'Links & Mentions', desc: 'URL links, #hashtags, and @user mentions' },
  { key: 'error', label: 'Error Messages', desc: 'Warning indicators and validation notices' },
  { key: 'destructive', label: 'Destructive Actions', desc: 'Delete, block, and report buttons' },
];

export const ThemeCustomizerModal: React.FC<ThemeCustomizerModalProps> = ({ onClose }) => {
  const {
    settings,
    updateSettingImmediately,
    resetThemeToPreset,
    updateCustomColorSlot,
    showToast,
  } = useApp();

  const [presetsExpanded, setPresetsExpanded] = useState(true);
  const [colorsExpanded, setColorsExpanded] = useState(true);
  const [previewTab, setPreviewTab] = useState<'feed' | 'dm'>('feed');

  const palette = settings.customPalette;

  return (
    <div className="fixed inset-0 z-50 bg-[#070709]/95 backdrop-blur-2xl flex flex-col overflow-hidden animate-in fade-in duration-200">
      {/* Header */}
      <header className="sticky top-0 z-20 bg-zinc-950/80 backdrop-blur-xl border-b border-white/10 px-4 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={onClose}
            className="p-1.5 -ml-1 rounded-xl hover:bg-white/10 text-zinc-300 hover:text-white transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <div className="w-6 h-6 rounded-lg bg-white/10 border border-white/15 flex items-center justify-center">
                <Palette className="w-3.5 h-3.5 text-white" />
              </div>
              Theme Customizer
            </h2>
            <p className="text-xs text-zinc-400">Personalize Instagram interface colors</p>
          </div>
        </div>

        <button
          onClick={() => resetThemeToPreset(settings.themePresetId)}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/15 border border-white/10 text-white text-xs font-semibold transition-colors cursor-pointer"
          title="Reset to default preset"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Reset</span>
        </button>
      </header>

      {/* Main Scrollable Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-5 max-w-3xl mx-auto w-full pb-24">
        {/* Enable Custom Theme Toggle */}
        <div
          onClick={() =>
            updateSettingImmediately('customThemeEnabled', !settings.customThemeEnabled)
          }
          className="flex items-center justify-between p-4 rounded-2xl bg-white/[0.04] border border-white/10 hover:border-white/20 cursor-pointer transition-all select-none shadow-sm"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/10 border border-white/15 text-white flex items-center justify-center shrink-0">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <p className="text-sm font-bold text-white">Enable Custom Theme</p>
              <p className="text-xs text-zinc-400">Activate custom palette injection</p>
            </div>
          </div>

          <div
            className={`w-11 h-6 rounded-full transition-all duration-200 relative flex items-center px-0.5 shrink-0 ${
              settings.customThemeEnabled ? 'bg-white shadow-[0_0_12px_rgba(255,255,255,0.4)]' : 'bg-zinc-800/90 border border-white/10'
            }`}
          >
            <div
              className={`w-5 h-5 rounded-full transition-transform duration-200 transform shadow-sm ${
                settings.customThemeEnabled ? 'translate-x-5 bg-zinc-950' : 'translate-x-0 bg-zinc-400'
              }`}
            />
          </div>
        </div>

        {/* Live Instagram UI Mockup Preview */}
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-bold uppercase tracking-wider text-zinc-400">
              Live Instagram UI Preview
            </h3>
            <div className="flex rounded-lg bg-zinc-950 p-0.5 border border-zinc-800 text-xs">
              <button
                onClick={() => setPreviewTab('feed')}
                className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
                  previewTab === 'feed'
                    ? 'bg-zinc-800 text-zinc-100'
                    : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                Feed
              </button>
              <button
                onClick={() => setPreviewTab('dm')}
                className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
                  previewTab === 'dm'
                    ? 'bg-zinc-800 text-zinc-100'
                    : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                Direct Message
              </button>
            </div>
          </div>

          {/* Render Mockup Container */}
          <div
            className="rounded-2xl border overflow-hidden shadow-2xl transition-colors duration-300"
            style={{
              backgroundColor: palette.background,
              borderColor: palette.border,
            }}
          >
            {/* Mock Header */}
            <div
              className="px-4 py-3 flex items-center justify-between border-b transition-colors"
              style={{
                backgroundColor: palette.statusBar,
                borderColor: palette.divider,
              }}
            >
              <span
                className="font-serif italic font-black text-lg tracking-tight"
                style={{ color: palette.primaryText }}
              >
                Instagram
              </span>
              <div className="flex items-center gap-3">
                <Heart className="w-4 h-4" style={{ color: palette.icon }} />
                <MessageCircle className="w-4 h-4" style={{ color: palette.icon }} />
              </div>
            </div>

            {previewTab === 'feed' ? (
              <div className="p-3 space-y-3">
                {/* Stories Row */}
                <div className="flex gap-2.5 overflow-x-auto pb-1">
                  {['Your Story', 'alex_design', 'creative_coder', 'eclipse_mod'].map((user, i) => (
                    <div key={user} className="flex flex-col items-center gap-1 shrink-0">
                      <div
                        className="w-11 h-11 rounded-full p-0.5"
                        style={{
                          background:
                            i === 0
                              ? palette.divider
                              : `linear-gradient(135deg, ${palette.accent}, ${palette.button})`,
                        }}
                      >
                        <div
                          className="w-full h-full rounded-full flex items-center justify-center font-bold text-xs"
                          style={{
                            backgroundColor: palette.surface,
                            color: palette.primaryText,
                          }}
                        >
                          {user.charAt(0).toUpperCase()}
                        </div>
                      </div>
                      <span
                        className="text-[9px] truncate max-w-[50px]"
                        style={{ color: palette.secondaryText }}
                      >
                        {user}
                      </span>
                    </div>
                  ))}
                </div>

                {/* Feed Card */}
                <div
                  className="rounded-xl border p-3 transition-colors shadow-sm"
                  style={{
                    backgroundColor: palette.surface,
                    borderColor: palette.border,
                  }}
                >
                  <div className="flex items-center justify-between mb-2.5">
                    <div className="flex items-center gap-2">
                      <div
                        className="w-7 h-7 rounded-full flex items-center justify-center font-bold text-[10px]"
                        style={{
                          backgroundColor: palette.accent,
                          color: palette.background,
                        }}
                      >
                        IE
                      </div>
                      <div>
                        <div className="flex items-center gap-1">
                          <span className="text-xs font-bold" style={{ color: palette.primaryText }}>
                            instaeclipse_dev
                          </span>
                          <span
                            className="w-1.5 h-1.5 rounded-full"
                            style={{ backgroundColor: palette.glyph }}
                          />
                        </div>
                        <span className="text-[10px]" style={{ color: palette.secondaryText }}>
                          Tokyo, Japan &bull; 2h
                        </span>
                      </div>
                    </div>

                    <button
                      className="px-2.5 py-1 rounded-lg text-[10px] font-bold transition-colors"
                      style={{
                        backgroundColor: palette.button,
                        color: palette.background,
                      }}
                    >
                      Follow
                    </button>
                  </div>

                  {/* Post Image Container */}
                  <div
                    className="w-full h-32 rounded-lg flex items-center justify-center border overflow-hidden relative"
                    style={{
                      background: `linear-gradient(145deg, ${palette.surface}, ${palette.divider})`,
                      borderColor: palette.divider,
                    }}
                  >
                    <div className="text-center p-2">
                      <p className="text-xs font-bold" style={{ color: palette.primaryText }}>
                        Active Palette: {THEME_PRESETS.find(p => p.id === settings.themePresetId)?.name || 'Custom'}
                      </p>
                      <p className="text-[10px] mt-0.5" style={{ color: palette.secondaryText }}>
                        Previewing Accent {palette.accent}
                      </p>
                    </div>
                  </div>

                  {/* Actions Bar */}
                  <div className="flex items-center justify-between mt-2.5 pt-1">
                    <div className="flex items-center gap-3">
                      <Heart className="w-4 h-4 fill-current" style={{ color: palette.destructive }} />
                      <MessageCircle className="w-4 h-4" style={{ color: palette.icon }} />
                      <Send className="w-4 h-4" style={{ color: palette.icon }} />
                    </div>
                    <Bookmark className="w-4 h-4" style={{ color: palette.icon }} />
                  </div>

                  {/* Caption & Comments */}
                  <div className="mt-2 text-xs space-y-0.5">
                    <p style={{ color: palette.primaryText }}>
                      <span className="font-bold mr-1">instaeclipse_dev</span>
                      Custom theme engine injected via LSPosed.{' '}
                      <span className="font-semibold" style={{ color: palette.link }}>
                        #xposed #instaeclipse
                      </span>
                    </p>
                    <p className="text-[10px]" style={{ color: palette.secondaryText }}>
                      View all 48 comments
                    </p>
                  </div>
                </div>
              </div>
            ) : (
              /* Direct Message Preview */
              <div className="p-3 space-y-2.5">
                <div className="flex items-start gap-2">
                  <div
                    className="w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0"
                    style={{ backgroundColor: palette.surface, color: palette.primaryText }}
                  >
                    JD
                  </div>
                  <div
                    className="p-2.5 rounded-2xl rounded-tl-sm text-xs max-w-[75%]"
                    style={{
                      backgroundColor: palette.surface,
                      color: palette.primaryText,
                    }}
                  >
                    Hey! Did you test Ghost Mode and Theme Customizer?
                  </div>
                </div>

                <div className="flex items-end justify-end gap-2">
                  <div
                    className="p-2.5 rounded-2xl rounded-tr-sm text-xs max-w-[75%]"
                    style={{
                      backgroundColor: palette.accent,
                      color: palette.background,
                    }}
                  >
                    Yes! Everything works flawlessly. No read receipts sent!
                  </div>
                </div>

                <div className="text-center pt-2">
                  <span
                    className="text-[10px] uppercase tracking-wider font-semibold"
                    style={{ color: palette.secondaryText }}
                  >
                    Seen at 10:42 PM (Ghost Spoofed)
                  </span>
                </div>
              </div>
            )}

            {/* Bottom Nav Mockup */}
            <div
              className="px-4 py-2.5 flex items-center justify-around border-t transition-colors"
              style={{
                backgroundColor: palette.navigation,
                borderColor: palette.divider,
              }}
            >
              <div className="w-5 h-5 rounded-md" style={{ backgroundColor: palette.primaryText }} />
              <div className="w-5 h-5 rounded-md opacity-50" style={{ backgroundColor: palette.secondaryText }} />
              <div className="w-5 h-5 rounded-md opacity-50" style={{ backgroundColor: palette.secondaryText }} />
              <div className="w-5 h-5 rounded-md opacity-50" style={{ backgroundColor: palette.secondaryText }} />
              <div
                className="w-5 h-5 rounded-full ring-2"
                style={{
                  backgroundColor: palette.accent,
                  color: palette.background,
                }}
              />
            </div>
          </div>
        </div>

        {/* 30 Built-in Presets Section */}
        <div className="rounded-2xl border border-white/10 bg-white/[0.03] overflow-hidden">
          <button
            onClick={() => setPresetsExpanded(prev => !prev)}
            className="w-full p-4 flex items-center justify-between text-left hover:bg-white/[0.04] transition-colors cursor-pointer"
          >
            <div>
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <span>Theme Presets</span>
                <span className="text-xs font-semibold bg-white/10 text-white px-2 py-0.5 rounded-full border border-white/10">
                  {THEME_PRESETS.length} available
                </span>
              </h3>
              <p className="text-xs text-zinc-400">Curated palettes ready for one-tap application</p>
            </div>
            {presetsExpanded ? (
              <ChevronUp className="w-4 h-4 text-zinc-400" />
            ) : (
              <ChevronDown className="w-4 h-4 text-zinc-400" />
            )}
          </button>

          {presetsExpanded && (
            <div className="p-4 pt-0 grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {THEME_PRESETS.map(preset => {
                const isSelected = settings.themePresetId === preset.id;

                return (
                  <button
                    key={preset.id}
                    onClick={() => resetThemeToPreset(preset.id)}
                    className={`p-3 rounded-xl border text-left transition-all flex items-center justify-between cursor-pointer ${
                      isSelected
                        ? 'bg-white/10 border-white/40 shadow-lg shadow-white/5'
                        : 'bg-white/[0.02] border-white/8 hover:border-white/20'
                    }`}
                  >
                    <div className="min-w-0 pr-2">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-white truncate">
                          {preset.name}
                        </span>
                        {isSelected && (
                          <Check className="w-3.5 h-3.5 text-white stroke-[3] shrink-0" />
                        )}
                      </div>
                      {/* Color Preview Swatches */}
                      <div className="flex items-center gap-1.5 mt-2">
                        <div
                          className="w-4 h-4 rounded-full border border-zinc-700 shadow-sm shrink-0"
                          style={{ backgroundColor: preset.palette.background }}
                          title="Background"
                        />
                        <div
                          className="w-4 h-4 rounded-full border border-zinc-700 shadow-sm shrink-0"
                          style={{ backgroundColor: preset.palette.surface }}
                          title="Surface"
                        />
                        <div
                          className="w-4 h-4 rounded-full border border-zinc-700 shadow-sm shrink-0"
                          style={{ backgroundColor: preset.palette.accent }}
                          title="Accent"
                        />
                        <div
                          className="w-4 h-4 rounded-full border border-zinc-700 shadow-sm shrink-0"
                          style={{ backgroundColor: preset.palette.primaryText }}
                          title="Primary Text"
                        />
                        <div
                          className="w-4 h-4 rounded-full border border-zinc-700 shadow-sm shrink-0"
                          style={{ backgroundColor: preset.palette.destructive }}
                          title="Destructive"
                        />
                      </div>
                    </div>

                    <span className="text-[10px] font-mono text-zinc-500 uppercase shrink-0">
                      ID {preset.id}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* 15 Custom Color Slots Section */}
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/60 overflow-hidden">
          <button
            onClick={() => setColorsExpanded(prev => !prev)}
            className="w-full p-4 flex items-center justify-between text-left hover:bg-zinc-800/40 transition-colors"
          >
            <div>
              <h3 className="text-sm font-bold text-zinc-100 flex items-center gap-2">
                <span>Custom Color Slots</span>
                <span className="text-xs font-semibold bg-indigo-500/20 text-indigo-300 px-2 py-0.5 rounded-full">
                  15 Slots
                </span>
              </h3>
              <p className="text-xs text-zinc-400">Granular control over each individual UI element</p>
            </div>
            {colorsExpanded ? (
              <ChevronUp className="w-4 h-4 text-zinc-400" />
            ) : (
              <ChevronDown className="w-4 h-4 text-zinc-400" />
            )}
          </button>

          {colorsExpanded && (
            <div className="p-4 pt-0 space-y-2.5">
              {COLOR_SLOT_DESCRIPTIONS.map(slot => {
                const currentColor = palette[slot.key];

                return (
                  <div
                    key={slot.key}
                    className="p-3 rounded-xl bg-zinc-950/80 border border-zinc-800 flex items-center justify-between gap-3"
                  >
                    <div className="min-w-0">
                      <p className="text-xs font-bold text-zinc-200">{slot.label}</p>
                      <p className="text-[11px] text-zinc-400 truncate">{slot.desc}</p>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      {/* Color Picker Input & Swatch */}
                      <div className="relative flex items-center">
                        <input
                          type="color"
                          value={currentColor}
                          onChange={e => updateCustomColorSlot(slot.key, e.target.value)}
                          className="w-8 h-8 rounded-lg cursor-pointer border-0 p-0 bg-transparent overflow-hidden opacity-0 absolute inset-0"
                          title={`Change ${slot.label} color`}
                        />
                        <div
                          className="w-8 h-8 rounded-lg border border-zinc-700 shadow-inner flex items-center justify-center cursor-pointer pointer-events-none"
                          style={{ backgroundColor: currentColor }}
                        />
                      </div>

                      {/* Manual Hex Input */}
                      <input
                        type="text"
                        value={currentColor}
                        onChange={e => {
                          const val = e.target.value;
                          if (val.startsWith('#') && val.length <= 7) {
                            updateCustomColorSlot(slot.key, val);
                          }
                        }}
                        className="w-20 px-2 py-1 rounded-lg bg-zinc-900 border border-zinc-800 text-xs font-mono text-zinc-300 text-center uppercase focus:outline-none focus:border-pink-500"
                        maxLength={7}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
