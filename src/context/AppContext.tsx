import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { SettingsState, LogEntry, ThemePalette, AppVersionInfo } from '../types';
import { DEFAULT_SETTINGS } from '../data/initialSettings';
import { THEME_PRESETS } from '../data/presets';
import { DETECTED_PACKAGES } from '../data/contributors';

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
const MANAGER_RELEASE_URL = 'https://github.com/somilkhan/InstaEclipse/releases/download/manager/v2.0.0/instaeclipse-2.0.0.apk';

const DEFAULT_VERSION: AppVersionInfo = {
  version: '2.0.0', buildNumber: 20, releaseDate: '2026-09-08', channel: 'Stable', isLatest: true,
  latestVersion: '2.0.0', apkFileName: 'instaeclipse-2.0.0.apk', apkFileSize: '18.5 MB',
  sha256Hash: '', apkDownloadUrl: MANAGER_RELEASE_URL,
  changelog: [
    'InstaEclipse v2.0.0: consolidated Web Manager and Android core.',
    'Embedded Web Manager UI with native Android runtime bridge.',
    'Instagram runtime stability and media-resolution improvements.',
    'Theme customizer, settings backup/restore and release management.',
  ],
};

const INITIAL_LOGS: LogEntry[] = [
  { id: '1', timestamp: new Date().toLocaleTimeString(), tag: 'INFO', message: 'InstaEclipse v2.0.0 initialized', source: 'InstaEclipse' },
];

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [settings, setSettings] = useState<SettingsState>(() => {
    try { const saved = localStorage.getItem(STORAGE_KEY); return saved ? { ...DEFAULT_SETTINGS, ...JSON.parse(saved) } : DEFAULT_SETTINGS; } catch { return DEFAULT_SETTINGS; }
  });
  const [versionInfo, setVersionInfo] = useState<AppVersionInfo>(() => {
    try { const saved = localStorage.getItem(VERSION_STORAGE_KEY); return saved ? { ...DEFAULT_VERSION, ...JSON.parse(saved) } : DEFAULT_VERSION; } catch { return DEFAULT_VERSION; }
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

  useEffect(() => { try { localStorage.setItem(STORAGE_KEY, JSON.stringify(settings)); } catch {} }, [settings]);
  useEffect(() => { try { localStorage.setItem(VERSION_STORAGE_KEY, JSON.stringify(versionInfo)); } catch {} }, [versionInfo]);

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    window.setTimeout(() => setToastMessage(prev => prev === msg ? null : prev), 3000);
  }, []);
  const addLog = useCallback((tag: LogEntry['tag'], message: string, source: LogEntry['source'] = 'InstaEclipse') => {
    setLogs(prev => [{ id: Math.random().toString(36).slice(2, 9), timestamp: new Date().toLocaleTimeString(), tag, message, source }, ...prev.slice(0, 499)]);
  }, []);
  const clearLogs = useCallback(() => { setLogs([]); showToast('Logs cleared'); }, [showToast]);
  const stageChange = <K extends keyof SettingsState>(key: K, value: SettingsState[K]) => setStagedChanges(prev => ({ ...prev, [key]: value }));
  const hasStagedChanges = Object.keys(stagedChanges).length > 0;
  const commitStagedChanges = () => {
    if (!hasStagedChanges) return;
    const keys = Object.keys(stagedChanges);
    setSettings(prev => ({ ...prev, ...stagedChanges })); setStagedChanges({});
    addLog('SETTINGS', `Applied ${keys.length} staged preference changes: ${keys.join(', ')}`); showToast(`Applied ${keys.length} changes. Restart Instagram to take effect.`);
  };
  const discardStagedChanges = () => { setStagedChanges({}); showToast('Discarded unsaved changes'); };
  const updateSettingImmediately = <K extends keyof SettingsState>(key: K, value: SettingsState[K]) => {
    setSettings(prev => ({ ...prev, [key]: value })); setStagedChanges(prev => { const next = { ...prev }; delete next[key]; return next; });
    addLog('SETTINGS', `Updated ${key} -> ${JSON.stringify(value)}`);
  };

  useEffect(() => {
    const bridge = (window as any).InstaEclipseAndroid;
    if (!bridge) return;
    try {
      const installed = JSON.parse(bridge.getInstalledPackages?.() || '[]') as string[];
      const detected = DETECTED_PACKAGES.find(p => installed.includes(p.pkg));
      if (detected) { setActivePackage(detected.pkg); addLog('SYNC', `Detected installed target: ${detected.pkg}`, 'InstaEclipse'); }
    } catch (e) { addLog('ERROR', 'Native package detection failed'); }
  }, [addLog]);

  const restartInstagram = () => {
    if (isRestarting) return;
    setIsRestarting(true); addLog('INFO', `Requesting Android restart for ${activePackage}`); showToast(`Restarting ${activePackage}...`);
    const bridge = (window as any).InstaEclipseAndroid;
    let ok = false;
    try { ok = !!bridge?.restartPackage?.(activePackage); } catch {}
    if (ok) { addLog('INFO', `${activePackage} launch request accepted by Android`); showToast('Instagram launch request sent'); }
    else { addLog('ERROR', `Android could not launch ${activePackage}`); showToast(`Unable to launch ${activePackage}`); }
    window.setTimeout(() => setIsRestarting(false), 700);
  };

  const checkForUpdates = async () => {
    setIsCheckingUpdates(true); addLog('SYNC', `Checking release channel: ${versionInfo.channel}...`);
    try {
      const response = await fetch('https://api.github.com/repos/somilkhan/InstaEclipse/releases/tags/manager/v2.0.0', { headers: { Accept: 'application/vnd.github+json' } });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const release = await response.json();
      const asset = Array.isArray(release.assets) ? release.assets.find((a: any) => a.name.endsWith('.apk')) : null;
      setVersionInfo(prev => ({ ...prev, latestVersion: '2.0.0', isLatest: prev.version === '2.0.0', apkDownloadUrl: asset?.browser_download_url || MANAGER_RELEASE_URL, apkFileName: asset?.name || prev.apkFileName, apkFileSize: asset?.size ? `${(asset.size / 1048576).toFixed(1)} MB` : prev.apkFileSize }));
      addLog('INFO', `Release endpoint verified: ${release.tag_name || 'manager/v2.0.0'}`); showToast('Release information verified');
    } catch (e) { addLog('ERROR', 'Release check failed; no update was claimed'); showToast('Unable to verify releases right now'); }
    finally { setIsCheckingUpdates(false); }
  };
  const applyUpdate = () => { showToast('Download the verified release APK to update InstaEclipse.'); window.open(versionInfo.apkDownloadUrl || MANAGER_RELEASE_URL, '_blank', 'noopener,noreferrer'); };
  const setReleaseChannel = (channel: 'Stable' | 'Beta' | 'Nightly') => { setVersionInfo(prev => ({ ...prev, channel })); addLog('SETTINGS', `Release channel switched to: ${channel}`); showToast(`Switched release channel to ${channel}`); };
  const backupSettings = () => {
    const blob = new Blob([JSON.stringify({ app: 'InstaEclipse', version: versionInfo.version, exportedAt: new Date().toISOString(), settings }, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `instaeclipse_backup_${Date.now()}.json`; a.click(); URL.revokeObjectURL(url); addLog('SETTINGS', 'Settings backup exported'); showToast('Settings exported');
  };
  const restoreSettings = (jsonString: string): boolean => {
    try { const parsed = JSON.parse(jsonString); const incoming = parsed.settings || parsed; setSettings(prev => ({ ...prev, ...incoming })); setStagedChanges({}); addLog('SETTINGS', 'Settings restored from backup'); showToast('Settings restored'); return true; }
    catch (err: any) { addLog('ERROR', `Failed to restore settings: ${err?.message || 'invalid JSON'}`); showToast('Invalid backup file format'); return false; }
  };
  const resetThemeToPreset = (presetId: number) => { const found = THEME_PRESETS.find(p => p.id === presetId) || THEME_PRESETS[0]; updateSettingImmediately('themePresetId', presetId); updateSettingImmediately('customPalette', { ...found.palette }); showToast(`Switched to "${found.name}" palette`); };
  const updateCustomColorSlot = (slotKey: keyof ThemePalette, colorHex: string) => updateSettingImmediately('customPalette', { ...settings.customPalette, [slotKey]: colorHex });

  return <AppContext.Provider value={{ settings, stagedChanges, hasStagedChanges, stageChange, commitStagedChanges, discardStagedChanges, updateSettingImmediately, activePackage, setActivePackage, logs, addLog, clearLogs, toastMessage, showToast, restartInstagram, isRestarting, backupSettings, restoreSettings, openThemeCustomizer, setOpenThemeCustomizer, openLocationPicker, setOpenLocationPicker, openAboutDialog, setOpenAboutDialog, openUpdateModal, setOpenUpdateModal, openApkInstallerModal, setOpenApkInstallerModal, versionInfo, isCheckingUpdates, checkForUpdates, applyUpdate, setReleaseChannel, resetThemeToPreset, updateCustomColorSlot }}>{children}</AppContext.Provider>;
};

export const useApp = () => { const ctx = useContext(AppContext); if (!ctx) throw new Error('useApp must be used within AppProvider'); return ctx; };
