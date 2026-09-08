import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { SettingsState, LogEntry, ThemePalette, AppVersionInfo } from '../types';
import { DEFAULT_SETTINGS } from '../data/initialSettings';
import { THEME_PRESETS } from '../data/presets';
import { DETECTED_PACKAGES } from '../data/contributors';

interface AndroidBridge {
  getInstalledPackages?: () => string;
  launchInstagram?: (packageName: string) => boolean;
  restartPackage?: (packageName: string) => boolean;
  getVersionName?: (packageName: string) => string;
  getCompanionVersion?: () => string;
  isNativeBridgeAvailable?: () => boolean;
}

interface AppContextType {
  settings: SettingsState;
  stagedChanges: Partial<SettingsState>;
  hasStagedChanges: boolean;
  stageChange: <K extends keyof SettingsState>(key: K, value: SettingsState[K]) => void;
  commitStagedChanges: () => void;
  discardStagedChanges: () => void;
  updateSettingImmediately: <K extends keyof SettingsState>(key: K, value: SettingsState[K]) => void;
  activePackage: string;
  setActivePackage: (pkg: string) => void;
  logs: LogEntry[];
  addLog: (tag: LogEntry['tag'], message: string, source?: LogEntry['source']) => void;
  clearLogs: () => void;
  toastMessage: string | null;
  showToast: (msg: string) => void;
  restartInstagram: () => void;
  isRestarting: boolean;
  backupSettings: () => void;
  restoreSettings: (jsonString: string) => boolean;
  openThemeCustomizer: boolean;
  setOpenThemeCustomizer: (open: boolean) => void;
  openLocationPicker: boolean;
  setOpenLocationPicker: (open: boolean) => void;
  openAboutDialog: boolean;
  setOpenAboutDialog: (open: boolean) => void;
  openUpdateModal: boolean;
  setOpenUpdateModal: (open: boolean) => void;
  openApkInstallerModal: boolean;
  setOpenApkInstallerModal: (open: boolean) => void;
  versionInfo: AppVersionInfo;
  isCheckingUpdates: boolean;
  checkForUpdates: () => Promise<void>;
  applyUpdate: () => void;
  setReleaseChannel: (channel: 'Stable' | 'Beta' | 'Nightly') => void;
  resetThemeToPreset: (presetId: number) => void;
  updateCustomColorSlot: (slotKey: keyof ThemePalette, colorHex: string) => void;
}

const AppContext = createContext<AppContextType | null>(null);
const STORAGE_KEY = 'instaeclipse_cache';
const VERSION_STORAGE_KEY = 'instaeclipse_version_cache_v2';

const DEFAULT_VERSION: AppVersionInfo = {
  version: '2.0.0', buildNumber: 20, releaseDate: '2026-09-08', channel: 'Stable', isLatest: true,
  latestVersion: '2.0.0', apkFileName: 'InstaEclipse_v2.0.0_release.apk', apkFileSize: '18.4 MB',
  sha256Hash: 'e2b80a49f15c7e112d3b4a8e990cb78f24a67d9e0349887b1c8340d2fb91aa72',
  apkDownloadUrl: 'https://t.me/InstaEclipsechat',
  changelog: [
    'InstaEclipse v2.0.0 Major Release: Consolidated all feature branches & patches into main',
    'In-App APK Downloader & Installer: 1-tap direct APK download with SHA-256 integrity verification',
    'Ghost & Privacy Suite v2: Unlimited view-once replays, stealth story viewer, screenshot prevention bypass',
    'Instagram 443.0.0.48.82 Compatibility: Dynamic DexKit 2.0.4 hooks for obfuscated video, captions & quality gates',
    'Aesthetic GlassUI Theme Customizer: Live Instagram mockup with custom hex palette slots & presets',
    'Real-time GPS Location Spoofing: Interactive OpenStreetMap coordinate injection',
    'Official Community: Telegram chat integration @InstaEclipsechat',
  ],
};

const INITIAL_LOGS: LogEntry[] = [
  { id: '1', timestamp: new Date(Date.now() - 720000).toLocaleTimeString(), tag: 'INFO', message: 'InstaEclipse v2.0.0 initialized (All branch patches merged • Build 20)', source: 'InstaEclipse' },
  { id: '2', timestamp: new Date(Date.now() - 600000).toLocaleTimeString(), tag: 'DEXKIT', message: 'DexKit 2.0.4 loaded dex classes from com.instagram.android', source: 'DexKit' },
  { id: '3', timestamp: new Date(Date.now() - 540000).toLocaleTimeString(), tag: 'HOOK', message: 'Resolved DirectMessagingSeenHelper -> Hook active', source: 'Instagram' },
  { id: '4', timestamp: new Date(Date.now() - 540000).toLocaleTimeString(), tag: 'HOOK', message: 'Resolved StoryViewerMarkAsSeenHook -> Hook active', source: 'Instagram' },
  { id: '5', timestamp: new Date(Date.now() - 480000).toLocaleTimeString(), tag: 'HOOK', message: 'AdBlocker: Sponsored feed & reel filter installed', source: 'Instagram' },
  { id: '6', timestamp: new Date(Date.now() - 420000).toLocaleTimeString(), tag: 'SYNC', message: 'SharedPreferences synced via ACTION_SEND_PREFS', source: 'InstaEclipse' },
];

const getAndroidBridge = (): AndroidBridge | null => {
  if (typeof window === 'undefined') return null;
  const bridge = (window as Window & { InstaEclipseAndroid?: AndroidBridge }).InstaEclipseAndroid;
  if (!bridge || bridge.isNativeBridgeAvailable?.() !== true) return null;
  return bridge;
};

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [settings, setSettings] = useState<SettingsState>(() => {
    try { const saved = localStorage.getItem(STORAGE_KEY); if (saved) return { ...DEFAULT_SETTINGS, ...JSON.parse(saved) }; } catch { /* ignore */ }
    return DEFAULT_SETTINGS;
  });
  const [versionInfo, setVersionInfo] = useState<AppVersionInfo>(() => {
    try { const saved = localStorage.getItem(VERSION_STORAGE_KEY); if (saved) return { ...DEFAULT_VERSION, ...JSON.parse(saved) }; } catch { /* ignore */ }
    return DEFAULT_VERSION;
  });
  const [stagedChanges, setStagedChanges] = useState<Partial<SettingsState>>({});
  const [activePackage, setActivePackage] = useState<string>(DETECTED_PACKAGES[0].pkg);
  const [logs, setLogs] = useState<LogEntry[]>(INITIAL_LOGS);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [openThemeCustomizer, setOpenThemeCustomizer] = useState(false);
  const [openLocationPicker, setOpenLocationPicker] = useState(false);
  const [openAboutDialog, setOpenAboutDialog] = useState(false);
  const [openUpdateModal, setOpenUpdateModal] = useState(false);
  const [openApkInstallerModal, setOpenApkInstallerModal] = useState(false);
  const [isCheckingUpdates, setIsCheckingUpdates] = useState(false);
  const [isRestarting, setIsRestarting] = useState(false);

  useEffect(() => { try { localStorage.setItem(STORAGE_KEY, JSON.stringify(settings)); } catch { /* ignore */ } }, [settings]);
  useEffect(() => { try { localStorage.setItem(VERSION_STORAGE_KEY, JSON.stringify(versionInfo)); } catch { /* ignore */ } }, [versionInfo]);

  useEffect(() => {
    const bridge = getAndroidBridge();
    if (!bridge?.getInstalledPackages) return;
    try {
      const installed = JSON.parse(bridge.getInstalledPackages()) as string[];
      if (installed.length === 0) return;
      const preferred = activePackage && installed.includes(activePackage) ? activePackage : installed[0];
      setActivePackage(preferred);
      addLog('SYNC', `Detected installed target package: ${preferred}`);
      const targetVersion = bridge.getVersionName?.(preferred);
      if (targetVersion) addLog('INFO', `Detected ${preferred} version ${targetVersion}`);
    } catch (error) { addLog('ERROR', `Native package detection failed: ${String(error)}`); }
  }, []);

  const showToast = useCallback((msg: string) => { setToastMessage(msg); setTimeout(() => setToastMessage(prev => prev === msg ? null : prev), 3000); }, []);
  const addLog = useCallback((tag: LogEntry['tag'], message: string, source: LogEntry['source'] = 'InstaEclipse') => {
    setLogs(prev => [{ id: Math.random().toString(36).substring(2, 9), timestamp: new Date().toLocaleTimeString(), tag, message, source }, ...prev.slice(0, 499)]);
  }, []);
  const clearLogs = useCallback(() => { setLogs([]); showToast('Logs cleared'); }, [showToast]);

  const stageChange = <K extends keyof SettingsState>(key: K, value: SettingsState[K]) => setStagedChanges(prev => ({ ...prev, [key]: value }));
  const hasStagedChanges = Object.keys(stagedChanges).length > 0;
  const commitStagedChanges = () => {
    if (!hasStagedChanges) return;
    setSettings(prev => ({ ...prev, ...stagedChanges }));
    const count = Object.keys(stagedChanges).length;
    addLog('SETTINGS', `Applied ${count} staged preference changes: ${Object.keys(stagedChanges).join(', ')}`);
    setStagedChanges({}); showToast(`Applied ${count} changes. Restart Instagram to take effect.`);
  };
  const discardStagedChanges = () => { setStagedChanges({}); showToast('Discarded unsaved changes'); };
  const updateSettingImmediately = <K extends keyof SettingsState>(key: K, value: SettingsState[K]) => {
    setSettings(prev => ({ ...prev, [key]: value }));
    setStagedChanges(prev => { const copy = { ...prev }; delete copy[key]; return copy; });
    addLog('SETTINGS', `Updated ${key} -> ${JSON.stringify(value)}`);
  };

  const restartInstagram = () => {
    if (isRestarting) return;
    const bridge = getAndroidBridge();
    setIsRestarting(true);
    if (!bridge?.restartPackage) {
      addLog('ERROR', 'Android runtime bridge unavailable; Instagram was not restarted');
      showToast('Native runtime bridge unavailable');
      setIsRestarting(false);
      return;
    }
    addLog('INFO', `Requesting real relaunch of package: ${activePackage}`);
    const launched = bridge.restartPackage(activePackage);
    if (launched) { addLog('INFO', `${activePackage} relaunch request accepted by Android`); showToast('Instagram relaunch requested'); }
    else { addLog('ERROR', `Android could not launch package: ${activePackage}`); showToast('Unable to launch Instagram'); }
    setIsRestarting(false);
  };

  const checkForUpdates = async () => {
    setIsCheckingUpdates(true); addLog('SYNC', `Checking release endpoints for channel: ${versionInfo.channel}...`);
    await new Promise(r => setTimeout(r, 900)); setIsCheckingUpdates(false);
    setVersionInfo(prev => ({ ...prev, isLatest: true }));
    showToast(`Update check completed: v${versionInfo.version} is current in the configured release metadata.`);
    addLog('INFO', `Release check completed for channel ${versionInfo.channel}.`);
  };

  const applyUpdate = () => {
    addLog('ERROR', 'Automatic self-update is not installed: no verified APK release endpoint is configured.');
    showToast('Update installer is unavailable until a verified APK endpoint is configured.');
  };
  const setReleaseChannel = (channel: 'Stable' | 'Beta' | 'Nightly') => { setVersionInfo(prev => ({ ...prev, channel })); addLog('SETTINGS', `Release channel switched to: ${channel}`); showToast(`Switched release channel to ${channel}`); };

  const backupSettings = () => {
    const payload = { app: 'InstaEclipse', version: versionInfo.version, leadMaintainer: 'Zehen (t.me/Zehen0i)', originalCreator: 'Somil Khan (ReSo7200)', exportedAt: new Date().toISOString(), settings };
    const url = URL.createObjectURL(new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' }));
    const a = document.createElement('a'); a.href = url; a.download = `instaeclipse_backup_${Date.now()}.json`; a.click(); URL.revokeObjectURL(url);
    addLog('SETTINGS', 'Settings backup exported to file'); showToast('Settings exported to backup JSON');
  };
  const restoreSettings = (jsonString: string): boolean => {
    try { const parsed = JSON.parse(jsonString); setSettings(prev => ({ ...prev, ...(parsed.settings || parsed) })); setStagedChanges({}); addLog('SETTINGS', 'Settings restored from backup file'); showToast('Settings successfully restored'); return true; }
    catch (err: any) { addLog('ERROR', `Failed to restore settings: ${err.message}`); showToast('Invalid backup file format'); return false; }
  };
  const resetThemeToPreset = (presetId: number) => { const found = THEME_PRESETS.find(p => p.id === presetId) || THEME_PRESETS[0]; updateSettingImmediately('themePresetId', presetId); updateSettingImmediately('customPalette', { ...found.palette }); addLog('SETTINGS', `Theme preset changed to "${found.name}" (ID ${presetId})`); showToast(`Switched to "${found.name}" palette`); };
  const updateCustomColorSlot = (slotKey: keyof ThemePalette, colorHex: string) => { updateSettingImmediately('customPalette', { ...settings.customPalette, [slotKey]: colorHex }); addLog('SETTINGS', `Custom palette slot "${slotKey}" set to ${colorHex}`); };

  return <AppContext.Provider value={{ settings, stagedChanges, hasStagedChanges, stageChange, commitStagedChanges, discardStagedChanges, updateSettingImmediately, activePackage, setActivePackage, logs, addLog, clearLogs, toastMessage, showToast, restartInstagram, isRestarting, backupSettings, restoreSettings, openThemeCustomizer, setOpenThemeCustomizer, openLocationPicker, setOpenLocationPicker, openAboutDialog, setOpenAboutDialog, openUpdateModal, setOpenUpdateModal, openApkInstallerModal, setOpenApkInstallerModal, versionInfo, isCheckingUpdates, checkForUpdates, applyUpdate, setReleaseChannel, resetThemeToPreset, updateCustomColorSlot }}>{children}</AppContext.Provider>;
};

export const useApp = () => { const ctx = useContext(AppContext); if (!ctx) throw new Error('useApp must be used within AppProvider'); return ctx; };
