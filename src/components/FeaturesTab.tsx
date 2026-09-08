import React, { useState, useRef } from 'react';
import {
  Sliders,
  Eye,
  EyeOff,
  Shield,
  Sparkles,
  Ban,
  Settings,
  Download,
  MapPin,
  Film,
  Palette,
  Save,
  Folder,
  Info,
  RotateCcw,
  ChevronRight,
  Check,
  AlertTriangle,
  Upload,
  Clock,
  Camera,
  MessageSquare,
  Radio,
  Copy,
  Heart,
  Search,
  AtSign,
  Share2,
  Trash2,
  CheckCircle2,
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { FeatureSubmenu, SettingsState } from '../types';
import { DEFAULT_MC_OVERRIDES } from '../data/initialSettings';

interface FeaturesTabProps {
  currentSubmenu: FeatureSubmenu;
  onSubmenuChange: (menu: FeatureSubmenu) => void;
}

export const FeaturesTab: React.FC<FeaturesTabProps> = ({ currentSubmenu, onSubmenuChange }) => {
  const {
    settings,
    versionInfo,
    stagedChanges,
    hasStagedChanges,
    stageChange,
    commitStagedChanges,
    discardStagedChanges,
    updateSettingImmediately,
    restartInstagram,
    backupSettings,
    restoreSettings,
    setOpenAboutDialog,
    setOpenThemeCustomizer,
    setOpenLocationPicker,
    showToast,
    addLog,
  } = useApp();

  const fileInputRef = useRef<HTMLInputElement>(null);
  const restoreFileRef = useRef<HTMLInputElement>(null);

  // Modals for specific features
  const [showExtremeDialog, setShowExtremeDialog] = useState(false);
  const [showQualityDialog, setShowQualityDialog] = useState(false);
  const [showDevImportDialog, setShowDevImportDialog] = useState(false);
  const [importedJsonText, setImportedJsonText] = useState('');
  const [showFolderModal, setShowFolderModal] = useState(false);
  const [customFolderPath, setCustomFolderPath] = useState(settings.downloaderCustomPath);

  // Helper to read current or staged value
  const getValue = <K extends keyof SettingsState>(key: K): SettingsState[K] => {
    if (key in stagedChanges) {
      return stagedChanges[key] as SettingsState[K];
    }
    return settings[key];
  };

  const handleToggle = <K extends keyof SettingsState>(key: K, currentVal: boolean) => {
    const nextVal = !currentVal;

    // Distraction Extreme mode warning
    if (key === 'isExtremeMode' && nextVal) {
      setShowExtremeDialog(true);
      return;
    }

    stageChange(key, nextVal as any);

    // Dependencies
    if (key === 'disableReelsExceptDM' && nextVal) {
      stageChange('disableReels', true);
    }
    if (key === 'disableReels' && !nextVal) {
      stageChange('disableReelsExceptDM', false);
    }
  };

  const handleMasterToggle = (keys: (keyof SettingsState)[]) => {
    const allOn = keys.every(k => getValue(k) === true);
    const targetState = !allOn;
    keys.forEach(k => stageChange(k, targetState as any));
  };

  // Dev Config Actions
  const handleExportDevConfig = () => {
    const data = JSON.stringify(DEFAULT_MC_OVERRIDES, null, 2);
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'mc_overrides.json';
    a.click();
    URL.revokeObjectURL(url);
    addLog('INFO', 'Exported MetaConfig overrides to mc_overrides.json');
    showToast('Exported MetaConfig JSON');
  };

  const handleRestoreDefaultDevConfig = () => {
    addLog('INFO', 'Restored bundled default_mc_overrides.json');
    showToast('Default MetaConfig overrides restored');
  };

  const handleImportDevConfig = () => {
    try {
      JSON.parse(importedJsonText);
      addLog('INFO', 'Imported custom JSON into mc_overrides.json');
      showToast('Config imported successfully');
      setShowDevImportDialog(false);
      setImportedJsonText('');
    } catch {
      showToast('Invalid JSON syntax');
    }
  };

  const handleRestoreFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = evt => {
      const content = evt.target?.result as string;
      if (content) {
        restoreSettings(content);
      }
    };
    reader.readAsText(file);
  };

  // Quality labels
  const getQualityLabel = (val: number) => {
    if (val === 360) return '360p';
    if (val === 480) return '480p';
    if (val === 720) return '720p (HD)';
    if (val === 1080) return '1080p (FHD)';
    if (val >= 9999) return 'Max Available';
    return 'Auto (Adaptive)';
  };

  // Render Row for Switch
  const SwitchRow: React.FC<{
    title: string;
    icon: any;
    accentColor: string;
    prefKey: keyof SettingsState;
    subtitle?: string;
    disabled?: boolean;
    isExtreme?: boolean;
  }> = ({ title, icon: Icon, accentColor, prefKey, subtitle, disabled = false }) => {
    const isChecked = Boolean(getValue(prefKey));

    return (
      <div
        onClick={() => !disabled && handleToggle(prefKey, isChecked)}
        className={`flex items-center justify-between p-3.5 rounded-2xl border transition-all select-none cursor-pointer ${
          disabled
            ? 'opacity-40 cursor-not-allowed bg-zinc-950/40 border-zinc-900'
            : 'bg-white/[0.03] hover:bg-white/[0.06] border-white/8 hover:border-white/16'
        }`}
      >
        <div className="flex items-center gap-3 min-w-0 pr-2">
          <div
            className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 bg-white/10 text-white border border-white/10 shadow-sm"
          >
            <Icon className="w-4 h-4 text-white" />
          </div>
          <div className="min-w-0">
            <p className="text-xs font-semibold text-white truncate">{title}</p>
            {subtitle && <p className="text-[11px] text-zinc-400 mt-0.5">{subtitle}</p>}
          </div>
        </div>

        {/* Custom Monochrome Toggle Switch */}
        <div
          className={`w-11 h-6 rounded-full transition-all duration-200 relative flex items-center px-0.5 shrink-0 ${
            isChecked ? 'bg-white shadow-[0_0_12px_rgba(255,255,255,0.35)]' : 'bg-zinc-800/90 border border-white/10'
          }`}
        >
          <div
            className={`w-5 h-5 rounded-full transition-transform duration-200 transform shadow-sm ${
              isChecked ? 'translate-x-5 bg-zinc-950' : 'translate-x-0 bg-zinc-400'
            }`}
          />
        </div>
      </div>
    );
  };

  // Submenu Switch Views
  return (
    <div className="space-y-4 pb-28 max-w-3xl mx-auto">
      {/* Hidden file input for restore */}
      <input
        type="file"
        ref={restoreFileRef}
        onChange={handleRestoreFileSelect}
        accept=".json"
        className="hidden"
      />

      {/* Floating Save/Apply Changes Bar - Monochrome Glass */}
      {hasStagedChanges && (
        <div className="fixed bottom-22 left-4 right-4 z-40 max-w-md mx-auto animate-in fade-in slide-in-from-bottom-4 duration-200">
          <div className="p-3.5 rounded-2xl glass-pill bg-zinc-950/90 border border-white/20 shadow-2xl backdrop-blur-2xl flex items-center justify-between gap-3">
            <div className="flex items-center gap-2.5">
              <span className="w-2 h-2 rounded-full bg-white shadow-[0_0_8px_rgba(255,255,255,0.8)] animate-pulse" />
              <div>
                <p className="text-xs font-bold text-white">
                  {Object.keys(stagedChanges).length} Unsaved Changes
                </p>
                <p className="text-[10px] text-zinc-400">Click Apply to save & sync</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                id="discard-staged-btn"
                onClick={discardStagedChanges}
                className="px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/15 text-zinc-300 hover:text-white text-xs font-semibold transition-colors cursor-pointer"
              >
                Discard
              </button>
              <button
                id="apply-staged-btn"
                onClick={commitStagedChanges}
                className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-white text-zinc-950 text-xs font-bold shadow-lg shadow-white/10 hover:bg-zinc-200 active:scale-95 transition-all cursor-pointer"
              >
                <Check className="w-3.5 h-3.5 stroke-[3]" />
                Apply Changes
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Main Categories Menu */}
      {currentSubmenu === 'main' && (
        <div className="space-y-5">
          {/* Categories Section */}
          <div className="space-y-2">
            <h3 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1">
              Categories:
            </h3>

            <div className="grid gap-2">
              <button
                onClick={() => onSubmenuChange('dev')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Sliders className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Developer Options</p>
                    <p className="text-[11px] text-zinc-400">Unlock MetaConfig internal panel</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('ghost')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Eye className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Ghost Mode Settings</p>
                    <p className="text-[11px] text-zinc-400">Hide seen, typing, story views & screenshots</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('ads')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Shield className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Ad & Analytics Block</p>
                    <p className="text-[11px] text-zinc-400">Sponsored posts, telemetry, link tracking</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('cleanfeed')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Sparkles className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Clean Feed</p>
                    <p className="text-[11px] text-zinc-400">Remove suggested posts & Threads units</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('distract')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Ban className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Distraction-Free Instagram</p>
                    <p className="text-[11px] text-zinc-400">Disable Stories, Feed, Reels, or Comments</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('misc')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Settings className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Misc Features</p>
                    <p className="text-[11px] text-zinc-400">Double-tap like, photo zoom, copy comments</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('downloader')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Download className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Downloader</p>
                    <p className="text-[11px] text-zinc-400">Save posts, reels, stories, profile pictures</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('location')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <MapPin className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Location</p>
                    <p className="text-[11px] text-zinc-400">GPS spoofing & interactive map pin</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('quality')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Film className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Video Quality</p>
                    <p className="text-[11px] text-zinc-400">
                      Force Reels quality: {getQualityLabel(settings.forceReelQuality)}
                    </p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>

              <button
                onClick={() => onSubmenuChange('theme')}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 transition-all text-left group cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                    <Palette className="w-4 h-4" />
                  </div>
                  <div>
                    <p className="text-xs font-semibold text-white">Theme Customizer</p>
                    <p className="text-[11px] text-zinc-400">Monochrome presets & custom styling slots</p>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-zinc-500 group-hover:text-white transition-colors" />
              </button>
            </div>
          </div>

          {/* Tools Section */}
          <div className="space-y-2">
            <h3 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1">
              Tools:
            </h3>

            <div className="grid gap-2">
              <button
                onClick={backupSettings}
                className="flex items-center gap-3 p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 text-left transition-all cursor-pointer"
              >
                <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                  <Save className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-white">Backup Settings</p>
                  <p className="text-[11px] text-zinc-400">Export current preferences to JSON</p>
                </div>
              </button>

              <button
                onClick={() => restoreFileRef.current?.click()}
                className="flex items-center gap-3 p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 text-left transition-all cursor-pointer"
              >
                <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                  <Folder className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-white">Restore Settings</p>
                  <p className="text-[11px] text-zinc-400">Load preferences from backup file</p>
                </div>
              </button>

              <button
                onClick={() => setOpenAboutDialog(true)}
                className="flex items-center gap-3 p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 text-left transition-all cursor-pointer"
              >
                <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                  <Info className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-white">About InstaEclipse & Credits</p>
                  <p className="text-[11px] text-zinc-400">v{versionInfo.version} &bull; Maintained by Zehen & Somil Khan</p>
                </div>
              </button>

              <button
                onClick={restartInstagram}
                className="flex items-center gap-3 p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/16 text-left transition-all cursor-pointer"
              >
                <div className="w-9 h-9 rounded-xl bg-white/10 text-white border border-white/10 flex items-center justify-center shrink-0">
                  <RotateCcw className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-white">Restart Instagram</p>
                  <p className="text-[11px] text-zinc-400">Kill process and reload hooked modules</p>
                </div>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Developer Options Submenu */}
      {currentSubmenu === 'dev' && (
        <div className="space-y-4">
          <div className="space-y-2">
            <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Features:</h4>
            <SwitchRow
              title="Enable Developer Options"
              subtitle="Unlocks MetaConfig developer settings in Instagram"
              icon={Sliders}
              accentColor="#0A84FF"
              prefKey="isDevEnabled"
            />
          </div>

          <div className="space-y-2">
            <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Config:</h4>
            <div className="grid gap-2">
              <button
                onClick={() => setShowDevImportDialog(true)}
                className="flex items-center gap-3 p-3.5 rounded-xl bg-zinc-900/80 hover:bg-zinc-900 border border-zinc-800/80 text-left"
              >
                <div className="w-9 h-9 rounded-xl bg-emerald-500/15 text-emerald-400 flex items-center justify-center shrink-0">
                  <Download className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-zinc-100">Import Config</p>
                  <p className="text-[11px] text-zinc-400">Load JSON into mc_overrides.json</p>
                </div>
              </button>

              <button
                onClick={handleExportDevConfig}
                className="flex items-center gap-3 p-3.5 rounded-xl bg-zinc-900/80 hover:bg-zinc-900 border border-zinc-800/80 text-left"
              >
                <div className="w-9 h-9 rounded-xl bg-blue-500/15 text-blue-400 flex items-center justify-center shrink-0">
                  <Upload className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-zinc-100">Export Config</p>
                  <p className="text-[11px] text-zinc-400">Download current MetaConfig overrides</p>
                </div>
              </button>

              <button
                onClick={handleRestoreDefaultDevConfig}
                className="flex items-center gap-3 p-3.5 rounded-xl bg-zinc-900/80 hover:bg-zinc-900 border border-zinc-800/80 text-left"
              >
                <div className="w-9 h-9 rounded-xl bg-amber-500/15 text-amber-400 flex items-center justify-center shrink-0">
                  <RotateCcw className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-xs font-semibold text-zinc-100">Restore Default Config</p>
                  <p className="text-[11px] text-zinc-400">Reset to bundled stable overrides</p>
                </div>
              </button>
            </div>
          </div>

          <div className="space-y-2">
            <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Options:</h4>
            <SwitchRow
              title="Remove Build Expired Popup"
              subtitle="Suppress expiration warnings on older Instagram builds"
              icon={Ban}
              accentColor="#FF453A"
              prefKey="removeBuildExpiredPopup"
            />
          </div>
        </div>
      )}

      {/* Ghost Mode Submenu */}
      {currentSubmenu === 'ghost' && (
        <div className="space-y-4">
          <div className="p-3.5 rounded-xl bg-indigo-950/30 border border-indigo-500/30 flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-indigo-200">Quick Toggle Settings</p>
              <p className="text-[11px] text-indigo-300/80">Configure internal one-tap switcher</p>
            </div>
            <button
              onClick={() => onSubmenuChange('qt')}
              className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition-colors"
            >
              Customize
            </button>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between px-1">
              <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Features:</h4>
              <button
                onClick={() =>
                  handleMasterToggle([
                    'isGhostSeen',
                    'isGhostTyping',
                    'isGhostStory',
                    'isGhostLive',
                    'allowScreenshots',
                    'isGhostScreenshot',
                    'isGhostViewOnce',
                    'enableUnlimitedReplays',
                    'permanentViewMode',
                    'keepEphemeralMessages',
                  ])
                }
                className="text-xs font-semibold text-indigo-400 hover:text-indigo-300"
              >
                Enable / Disable All
              </button>
            </div>

            <div className="grid gap-2">
              <SwitchRow
                title="Hide DM Seen"
                subtitle="Read messages without sending read receipts"
                icon={EyeOff}
                accentColor="#5E5CE6"
                prefKey="isGhostSeen"
              />
              <SwitchRow
                title="Hide Typing Indicator"
                subtitle="Type freely without revealing activity"
                icon={MessageSquare}
                accentColor="#5E5CE6"
                prefKey="isGhostTyping"
              />
              <SwitchRow
                title="Hide Story Views"
                subtitle="View stories anonymously"
                icon={Radio}
                accentColor="#5E5CE6"
                prefKey="isGhostStory"
              />
              <SwitchRow
                title="Hide Live Presence"
                subtitle="Join Instagram Lives without joining notification"
                icon={Radio}
                accentColor="#5E5CE6"
                prefKey="isGhostLive"
              />
              <SwitchRow
                title="Allow Screenshots in DMs"
                subtitle="Enable screenshot capture in protected chats"
                icon={Camera}
                accentColor="#5E5CE6"
                prefKey="allowScreenshots"
              />
              <SwitchRow
                title="Bypass Screenshot Detection"
                subtitle="Take screenshots without triggering alert messages"
                icon={Camera}
                accentColor="#5E5CE6"
                prefKey="isGhostScreenshot"
              />
              <SwitchRow
                title="Hide View Once Opened"
                subtitle="Open disappearing media without marking as seen"
                icon={EyeOff}
                accentColor="#5E5CE6"
                prefKey="isGhostViewOnce"
              />
              <SwitchRow
                title="Unlimited View-Once Replays"
                subtitle="Replay view-once media indefinitely"
                icon={RotateCcw}
                accentColor="#5E5CE6"
                prefKey="enableUnlimitedReplays"
              />
              <SwitchRow
                title="Permanent View Once Media"
                subtitle="Prevent media from vanishing (Unstable)"
                icon={Eye}
                accentColor="#5E5CE6"
                prefKey="permanentViewMode"
              />
              <SwitchRow
                title="Keep Disappearing Messages"
                subtitle="Stop ephemeral messages from automatically deleting"
                icon={Clock}
                accentColor="#5E5CE6"
                prefKey="keepEphemeralMessages"
              />
            </div>
          </div>
        </div>
      )}

      {/* Quick Toggles Submenu */}
      {currentSubmenu === 'qt' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Quick Toggles:</h4>
            <button
              onClick={() =>
                handleMasterToggle([
                  'quickToggleSeen',
                  'quickToggleTyping',
                  'quickToggleScreenshot',
                  'quickToggleViewOnce',
                  'quickToggleStory',
                  'quickToggleLive',
                  'quickToggleEphemeral',
                  'quickToggleReplays',
                  'quickTogglePermanentView',
                  'quickToggleAllowScreenshots',
                ])
              }
              className="text-xs font-semibold text-indigo-400 hover:text-indigo-300"
            >
              Toggle All
            </button>
          </div>

          <div className="grid gap-2">
            <SwitchRow title="Hide DM Seen" icon={EyeOff} accentColor="#5E5CE6" prefKey="quickToggleSeen" />
            <SwitchRow title="Hide Typing" icon={MessageSquare} accentColor="#5E5CE6" prefKey="quickToggleTyping" />
            <SwitchRow title="Bypass Screenshot" icon={Camera} accentColor="#5E5CE6" prefKey="quickToggleScreenshot" />
            <SwitchRow title="Hide View Once" icon={EyeOff} accentColor="#5E5CE6" prefKey="quickToggleViewOnce" />
            <SwitchRow title="Hide Story Seen" icon={Radio} accentColor="#5E5CE6" prefKey="quickToggleStory" />
            <SwitchRow title="Hide Live Seen" icon={Radio} accentColor="#5E5CE6" prefKey="quickToggleLive" />
            <SwitchRow title="Keep Disappearing" icon={Clock} accentColor="#5E5CE6" prefKey="quickToggleEphemeral" />
            <SwitchRow title="Unlimited Replays" icon={RotateCcw} accentColor="#5E5CE6" prefKey="quickToggleReplays" />
            <SwitchRow title="Permanent View" icon={Eye} accentColor="#5E5CE6" prefKey="quickTogglePermanentView" />
            <SwitchRow title="Allow Screenshots" icon={Camera} accentColor="#5E5CE6" prefKey="quickToggleAllowScreenshots" />
          </div>
        </div>
      )}

      {/* Ad & Analytics Submenu */}
      {currentSubmenu === 'ads' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Features:</h4>
            <button
              onClick={() =>
                handleMasterToggle(['isAdBlockEnabled', 'isAnalyticsBlocked', 'disableTrackingLinks'])
              }
              className="text-xs font-semibold text-red-400 hover:text-red-300"
            >
              Enable / Disable All
            </button>
          </div>

          <div className="grid gap-2">
            <SwitchRow
              title="Block Sponsored Ads"
              subtitle="Filter out sponsored posts and video promotions"
              icon={Shield}
              accentColor="#FF453A"
              prefKey="isAdBlockEnabled"
            />
            <SwitchRow
              title="Block Telemetry & Analytics"
              subtitle="Prevent Instagram analytics logging and background trackers"
              icon={Shield}
              accentColor="#FF453A"
              prefKey="isAnalyticsBlocked"
            />
            <SwitchRow
              title="Disable Tracking Links"
              subtitle="Strip tracking parameters from URLs in direct messages and bios"
              icon={Share2}
              accentColor="#FF453A"
              prefKey="disableTrackingLinks"
            />
          </div>
        </div>
      )}

      {/* Clean Feed Submenu */}
      {currentSubmenu === 'cleanfeed' && (
        <div className="space-y-4">
          <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1">Features:</h4>
          <div className="grid gap-2">
            <SwitchRow
              title="Hide Suggestions in Feed"
              subtitle="Remove suggested posts, reels widgets, and non-followed accounts"
              icon={Sparkles}
              accentColor="#64D2FF"
              prefKey="hideSuggestionsInFeed"
            />
            <SwitchRow
              title="Hide Threads Suggestions"
              subtitle="Remove Threads promotion banners and cross-app post units"
              icon={Sparkles}
              accentColor="#64D2FF"
              prefKey="hideThreadsSuggestions"
            />
          </div>
        </div>
      )}

      {/* Distraction Free Submenu */}
      {currentSubmenu === 'distract' && (
        <div className="space-y-4">
          {/* Danger Zone */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold text-red-400 uppercase tracking-wider px-1">
              Danger Zone:
            </h4>
            <SwitchRow
              title="Extreme Mode"
              subtitle="Permanently locks selected distraction blocks until app reinstall"
              icon={AlertTriangle}
              accentColor="#FF453A"
              prefKey="isExtremeMode"
            />
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between px-1">
              <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Features:</h4>
              <button
                disabled={getValue('isExtremeMode')}
                onClick={() =>
                  handleMasterToggle([
                    'disableStories',
                    'disableFeed',
                    'disableReels',
                    'disableReelsExceptDM',
                    'disableExplore',
                    'disableComments',
                  ])
                }
                className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 disabled:opacity-40"
              >
                Toggle All
              </button>
            </div>

            <div className="grid gap-2">
              <SwitchRow
                title="Disable Stories"
                subtitle="Completely hides story tray at top of the feed"
                icon={Radio}
                accentColor="#30D158"
                prefKey="disableStories"
                disabled={getValue('isExtremeMode')}
              />
              <SwitchRow
                title="Disable Feed"
                subtitle="Removes the home feed timeline"
                icon={Ban}
                accentColor="#30D158"
                prefKey="disableFeed"
                disabled={getValue('isExtremeMode')}
              />
              <SwitchRow
                title="Disable Reels"
                subtitle="Completely removes Reels video player and tab"
                icon={Film}
                accentColor="#30D158"
                prefKey="disableReels"
                disabled={getValue('isExtremeMode')}
              />
              <SwitchRow
                title="Disable Reels (Except DMs)"
                subtitle="Allows viewing reels sent directly to your inbox"
                icon={Film}
                accentColor="#30D158"
                prefKey="disableReelsExceptDM"
                disabled={getValue('isExtremeMode')}
              />
              <SwitchRow
                title="Disable Explore Tab"
                subtitle="Hides the search grid discovery feed"
                icon={Search}
                accentColor="#30D158"
                prefKey="disableExplore"
                disabled={getValue('isExtremeMode')}
              />
              <SwitchRow
                title="Disable Comments"
                subtitle="Removes comment sections and prevents reading comments"
                icon={MessageSquare}
                accentColor="#30D158"
                prefKey="disableComments"
                disabled={getValue('isExtremeMode')}
              />
            </div>
          </div>
        </div>
      )}

      {/* Misc Features Submenu */}
      {currentSubmenu === 'misc' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Features:</h4>
            <button
              onClick={() =>
                handleMasterToggle([
                  'disableStoryFlipping',
                  'disableVideoAutoPlay',
                  'spoofLastSeen',
                  'disableRepost',
                  'showFollowerToast',
                  'showFeatureToasts',
                  'enableStoryMentions',
                  'disableDiscoverPeople',
                  'enableCopyComment',
                  'disableDoubleTapLike',
                  'enableCaptionCopy',
                  'enablePhotoZoom',
                ])
              }
              className="text-xs font-semibold text-purple-400 hover:text-purple-300"
            >
              Toggle All
            </button>
          </div>

          <div className="grid gap-2">
            <SwitchRow
              title="Disable Story Autoswipe"
              subtitle="Stories won't advance automatically"
              icon={Radio}
              accentColor="#BF5AF2"
              prefKey="disableStoryFlipping"
            />
            <SwitchRow
              title="Disable Video Autoplay"
              subtitle="Videos will only play when tapped"
              icon={Film}
              accentColor="#BF5AF2"
              prefKey="disableVideoAutoPlay"
            />
            <SwitchRow
              title="Freeze Last Seen"
              subtitle="Freeze active status timestamp instead of updating live"
              icon={Clock}
              accentColor="#BF5AF2"
              prefKey="spoofLastSeen"
            />
            <SwitchRow
              title="Disable Repost"
              subtitle="Hide repost suggestions and buttons"
              icon={Share2}
              accentColor="#BF5AF2"
              prefKey="disableRepost"
            />
            <SwitchRow
              title="Show Follower Toast"
              subtitle="Pop-up indicator when visiting a user who follows you back"
              icon={CheckCircle2}
              accentColor="#BF5AF2"
              prefKey="showFollowerToast"
            />
            <SwitchRow
              title="Show Feature Status Toasts"
              subtitle="Display toast notifications when hook triggers fire"
              icon={Info}
              accentColor="#BF5AF2"
              prefKey="showFeatureToasts"
            />
            <SwitchRow
              title="View Story Mentions"
              subtitle="List all hidden @mentions in any story"
              icon={AtSign}
              accentColor="#BF5AF2"
              prefKey="enableStoryMentions"
            />
            <SwitchRow
              title="Disable Discover People"
              subtitle="Remove the 'People you may know' suggestions carousel"
              icon={Ban}
              accentColor="#BF5AF2"
              prefKey="disableDiscoverPeople"
            />
            <SwitchRow
              title="Copy Comment with One Tap"
              subtitle="Easily copy any comment text to clipboard"
              icon={Copy}
              accentColor="#BF5AF2"
              prefKey="enableCopyComment"
            />
            <SwitchRow
              title="Disable Double Tap to Like"
              subtitle="Prevent accidental likes when scrolling"
              icon={Heart}
              accentColor="#BF5AF2"
              prefKey="disableDoubleTapLike"
            />
            <SwitchRow
              title="Copy Caption"
              subtitle="Adds 'Copy Caption' option to overflow post menu"
              icon={Copy}
              accentColor="#BF5AF2"
              prefKey="enableCaptionCopy"
            />
            <SwitchRow
              title="Photo Zoom (Long-Press)"
              subtitle="Long-press any feed photo for full-screen pinch-zoom"
              icon={Search}
              accentColor="#BF5AF2"
              prefKey="enablePhotoZoom"
            />
          </div>
        </div>
      )}

      {/* Downloader Submenu */}
      {currentSubmenu === 'downloader' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between px-1">
            <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider">Features:</h4>
            <button
              onClick={() =>
                handleMasterToggle([
                  'enablePostDownload',
                  'enableStoryDownload',
                  'enableReelDownload',
                  'enableProfileDownload',
                ])
              }
              className="text-xs font-semibold text-orange-400 hover:text-orange-300"
            >
              Toggle All
            </button>
          </div>

          <div className="grid gap-2">
            <SwitchRow
              title="Download Posts"
              subtitle="Save images and carousel posts"
              icon={Download}
              accentColor="#FF9F0A"
              prefKey="enablePostDownload"
            />
            <SwitchRow
              title="Download Stories"
              subtitle="Save expiring stories directly"
              icon={Download}
              accentColor="#FF9F0A"
              prefKey="enableStoryDownload"
            />
            <SwitchRow
              title="Download Reels"
              subtitle="Save high-definition reels"
              icon={Download}
              accentColor="#FF9F0A"
              prefKey="enableReelDownload"
            />
            <SwitchRow
              title="Download Profile Pictures"
              subtitle="Long-press avatar to save full-resolution photo"
              icon={Download}
              accentColor="#FF9F0A"
              prefKey="enableProfileDownload"
            />
          </div>

          <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1 pt-2">Options:</h4>
          <div className="grid gap-2">
            <SwitchRow
              title="Username Subfolder"
              subtitle="Organize downloads into folders by creator username"
              icon={Folder}
              accentColor="#FF9F0A"
              prefKey="downloaderUsernameFolder"
            />
            <SwitchRow
              title="Add Timestamp to Filename"
              subtitle="Append exact capture date and time to filename"
              icon={Clock}
              accentColor="#FF9F0A"
              prefKey="downloaderAddTimestamp"
            />
          </div>

          <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1 pt-2">
            Download Folder:
          </h4>
          <div className="grid gap-2">
            <button
              onClick={() => setShowFolderModal(true)}
              className="flex items-center gap-3 p-3.5 rounded-xl bg-zinc-900/80 hover:bg-zinc-900 border border-zinc-800/80 text-left transition-all"
            >
              <div className="w-9 h-9 rounded-xl bg-orange-500/15 text-orange-400 flex items-center justify-center shrink-0">
                <Folder className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <p className="text-xs font-semibold text-zinc-100">Storage Location</p>
                <p className="text-[11px] font-mono text-zinc-400 truncate">
                  {settings.downloaderCustomPath}
                </p>
              </div>
            </button>

            <button
              onClick={() => {
                updateSettingImmediately(
                  'downloaderCustomPath',
                  '/storage/emulated/0/Download/InstaEclipse'
                );
                showToast('Reset download folder to default');
              }}
              className="flex items-center gap-3 p-3.5 rounded-xl bg-zinc-900/80 hover:bg-zinc-900 border border-zinc-800/80 text-left transition-all"
            >
              <div className="w-9 h-9 rounded-xl bg-red-500/15 text-red-400 flex items-center justify-center shrink-0">
                <Trash2 className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs font-semibold text-zinc-100">Reset Download Folder</p>
                <p className="text-[11px] text-zinc-400">Restore default Downloads directory</p>
              </div>
            </button>
          </div>
        </div>
      )}

      {/* Location Submenu */}
      {currentSubmenu === 'location' && (
        <div className="space-y-4">
          <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1">Features:</h4>
          <SwitchRow
            title="Enable Location Spoofing"
            subtitle="Overrides GPS coordinates reported to Instagram"
            icon={MapPin}
            accentColor="#FFD60A"
            prefKey="spoofLocation"
          />

          <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1 pt-2">Options:</h4>
          <button
            onClick={() => setOpenLocationPicker(true)}
            className="w-full flex items-center justify-between p-4 rounded-xl bg-zinc-900/80 hover:bg-zinc-900 border border-zinc-800/80 text-left transition-all"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-yellow-500/15 text-yellow-400 flex items-center justify-center shrink-0">
                <MapPin className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-semibold text-zinc-100">Interactive Map Location</p>
                <p className="text-xs font-mono text-yellow-400 mt-0.5">
                  Lat: {Number(settings.spoofLat).toFixed(4)}, Lng: {Number(settings.spoofLng).toFixed(4)}
                </p>
                <p className="text-[11px] text-zinc-400 mt-0.5">Tap to choose pin or search city</p>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-zinc-500" />
          </button>
        </div>
      )}

      {/* Video Quality Submenu */}
      {currentSubmenu === 'quality' && (
        <div className="space-y-4">
          <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1">Features:</h4>
          <button
            onClick={() => setShowQualityDialog(true)}
            className="w-full flex items-center justify-between p-4 rounded-xl bg-zinc-900/80 hover:bg-zinc-900 border border-zinc-800/80 text-left transition-all"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-500/15 text-emerald-400 flex items-center justify-center shrink-0">
                <Film className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-semibold text-zinc-100">Force Reels Quality</p>
                <p className="text-xs text-emerald-400 font-semibold mt-0.5">
                  {getQualityLabel(settings.forceReelQuality)}
                </p>
                <p className="text-[11px] text-zinc-400 mt-0.5">
                  Locks video playback resolution instead of adaptive bitrate
                </p>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-zinc-500" />
          </button>
        </div>
      )}

      {/* Theme Submenu */}
      {currentSubmenu === 'theme' && (
        <div className="space-y-4">
          <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1">Features:</h4>
          <SwitchRow
            title="Enable Custom Theme"
            subtitle="Recolor Instagram UI with selected theme palette"
            icon={Palette}
            accentColor="#FF2D55"
            prefKey="customThemeEnabled"
          />

          <h4 className="text-xs font-bold text-zinc-400 uppercase tracking-wider px-1 pt-2">Options:</h4>
          <button
            onClick={() => setOpenThemeCustomizer(true)}
            className="w-full flex items-center justify-between p-4 rounded-xl bg-zinc-900/80 hover:bg-zinc-900 border border-zinc-800/80 text-left transition-all"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-pink-500/15 text-pink-400 flex items-center justify-center shrink-0">
                <Palette className="w-5 h-5" />
              </div>
              <div>
                <p className="text-xs font-semibold text-zinc-100">Open Theme Customizer</p>
                <p className="text-[11px] text-zinc-400 mt-0.5">
                  Explore 30 presets, customize 15 color slots & view live mockup
                </p>
              </div>
            </div>
            <ChevronRight className="w-4 h-4 text-zinc-500" />
          </button>
        </div>
      )}

      {/* Extreme Mode Warning Dialog */}
      {showExtremeDialog && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-zinc-900 border border-red-500/40 rounded-2xl max-w-sm w-full p-5 shadow-2xl space-y-4">
            <div className="w-10 h-10 rounded-xl bg-red-500/20 text-red-400 flex items-center justify-center">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-zinc-100">Warning: Extreme Distraction-Free</h3>
              <p className="text-xs text-zinc-300 mt-2 leading-relaxed">
                Extreme mode permanently locks all distraction blocks. You will not be able to re-enable disabled sections through the companion app without clearing Instagram cache or reinstalling.
              </p>
            </div>
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setShowExtremeDialog(false)}
                className="px-4 py-2 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-xs font-semibold text-zinc-300"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  stageChange('isExtremeMode', true);
                  setShowExtremeDialog(false);
                  showToast('Extreme mode enabled');
                }}
                className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-500 text-xs font-bold text-white shadow-lg shadow-red-600/30"
              >
                Yes, Enable
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Video Quality Picker Dialog */}
      {showQualityDialog && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl max-w-xs w-full p-4 shadow-2xl space-y-3">
            <h3 className="font-bold text-sm text-zinc-100">Select Reels Playback Quality</h3>
            <div className="space-y-1.5">
              {[
                { label: 'Auto (Instagram Adaptive)', val: 0 },
                { label: '360p', val: 360 },
                { label: '480p', val: 480 },
                { label: '720p (HD)', val: 720 },
                { label: '1080p (FHD)', val: 1080 },
                { label: 'Max Available Quality', val: 9999 },
              ].map(opt => (
                <button
                  key={opt.val}
                  onClick={() => {
                    updateSettingImmediately('forceReelQuality', opt.val);
                    setShowQualityDialog(false);
                    showToast(`Quality set to ${opt.label}`);
                  }}
                  className={`w-full text-left px-3 py-2.5 rounded-xl text-xs font-medium transition-all flex items-center justify-between ${
                    settings.forceReelQuality === opt.val
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                      : 'hover:bg-zinc-800 text-zinc-300'
                  }`}
                >
                  <span>{opt.label}</span>
                  {settings.forceReelQuality === opt.val && (
                    <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                  )}
                </button>
              ))}
            </div>
            <button
              onClick={() => setShowQualityDialog(false)}
              className="w-full py-2 rounded-xl bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-semibold"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Dev Config Import Dialog */}
      {showDevImportDialog && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl max-w-md w-full p-4 shadow-2xl space-y-3">
            <h3 className="font-bold text-sm text-zinc-100">Import MetaConfig JSON</h3>
            <p className="text-xs text-zinc-400">
              Paste valid JSON overrides to inject into mobileconfig/mc_overrides.json
            </p>
            <textarea
              rows={6}
              value={importedJsonText}
              onChange={e => setImportedJsonText(e.target.value)}
              placeholder='{"69718:": ["4: : true"]}'
              className="w-full p-3 rounded-xl bg-zinc-950 border border-zinc-800 text-xs font-mono text-zinc-200 focus:outline-none focus:border-indigo-500"
            />
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setShowDevImportDialog(false)}
                className="px-3 py-1.5 rounded-xl bg-zinc-800 text-xs font-semibold text-zinc-300"
              >
                Cancel
              </button>
              <button
                onClick={handleImportDevConfig}
                className="px-4 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white shadow-lg shadow-indigo-600/30"
              >
                Import
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Custom Folder Path Dialog */}
      {showFolderModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl max-w-sm w-full p-4 shadow-2xl space-y-3">
            <h3 className="font-bold text-sm text-zinc-100">Set Download Directory</h3>
            <p className="text-xs text-zinc-400">
              Specify folder where media from Instagram will be saved.
            </p>
            <input
              type="text"
              value={customFolderPath}
              onChange={e => setCustomFolderPath(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl bg-zinc-950 border border-zinc-800 text-xs font-mono text-zinc-200 focus:outline-none focus:border-orange-500"
            />
            <div className="flex gap-2 justify-end">
              <button
                onClick={() => setShowFolderModal(false)}
                className="px-3 py-1.5 rounded-xl bg-zinc-800 text-xs font-semibold text-zinc-300"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  updateSettingImmediately('downloaderCustomPath', customFolderPath);
                  setShowFolderModal(false);
                  showToast('Download folder updated');
                }}
                className="px-4 py-1.5 rounded-xl bg-orange-600 hover:bg-orange-500 text-xs font-bold text-white"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
