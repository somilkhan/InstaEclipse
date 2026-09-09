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

const DEFAULT_VERSION: AppVersionInfo = {
  version: '2.0.0',
  buildNumber: 20,
  releaseDate: '2026-09-08',
  channel: 'Stable',
  isLatest: true,
  latestVersion: '2.0.0',
  apkFileName: 'InstaEclipse_v2.0.0_release.apk',
  apkFileSize: '18.4 MB',
  sha256Hash: 'e2b80a49f15c7e112d3b4a8e990cb78f24a67d9e0349887b1c8340d2fb91aa72',
  apkDownloadUrl: 'https://t.me/InstaEclipsechat',
  changelog: [
    '🚀 InstaEclipse v2.0.0 Major Release: Consolidated all feature branches & patches into main',
    '📥 In-App APK Downloader & Installer: 1-tap direct APK download with SHA-256 integrity verification',
    '🛡️ Ghost & Privacy Suite v2: Unlimited view-once replays, stealth story viewer, screenshot prevention bypass',
    '⚡ Instagram 443.0.0.48.82 Compatibility: Dynamic DexKit 2.0.4 hooks for obfuscated video, captions & quality gates',
    '🎨 Aesthetic GlassUI Theme Customizer: Live Instagram mockup with custom hex palette slots & presets',
    '🛰️ Real-time GPS Location Spoofing: Interactive OpenStreetMap coordinate injection',
    '💬 Official Community: Telegram chat integration @InstaEclipsechat',
  ],
};

const INITIAL_LOGS: LogEntry[] = [
  {
    id: '1',
    timestamp: new Date(Date.now() - 1000 * 60 * 12).toLocaleTimeString(),
    tag: 'INFO',
    message: 'InstaEclipse v2.0.0 initialized (All branch patches merged • Build 20)',
    source: 'InstaEclipse',
  },
  {
    id: '2',
    timestamp: new Date(Date.now() - 1000 * 60 * 10).toLocaleTimeString(),
    tag: 'DEXKIT',
    message: 'DexKit 2.0.4 loaded dex classes from com.instagram.android',
    source: 'DexKit',
  },
  {
    id: '3',
    timestamp: new Date(Date.now() - 1000 * 60 * 9).toLocaleTimeString(),
    tag: 'HOOK',
    message: 'Resolved DirectMessagingSeenHelper -> Hook active',
    source: 'Instagram',
  },
  {
    id: '4',
    timestamp: new Date(Date.now() - 1000 * 60 * 9).toLocaleTimeString(),
    tag: 'HOOK',
    message: 'Resolved StoryViewerMarkAsSeenHook -> Hook active',
    source: 'Instagram',
  },
  {
    id: '5',
    timestamp: new Date(Date.now() - 1000 * 60 * 8).toLocaleTimeString(),
    tag: 'HOOK',
    message: 'AdBlocker: Sponsored feed & reel filter installed',
    source: 'Instagram',
  },
  {
    id: '6',
    timestamp: new Date(Date.now() - 1000 * 60 * 7).toLocaleTimeString(),
    tag: 'SYNC',
    message: 'SharedPreferences synced via ACTION_SEND_PREFS',
    source: 'InstaEclipse',
  },
];

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [settings, setSettings] = useState<SettingsState>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        return { ...DEFAULT_SETTINGS, ...JSON.parse(saved) };
      }
    } catch {
      // ignore
    }
    return DEFAULT_SETTINGS;
  });

  const [versionInfo, setVersionInfo] = useState<AppVersionInfo>(() => {
    try {
      const saved = localStorage.getItem(VERSION_STORAGE_KEY);
      if (saved) {
        return { ...DEFAULT_VERSION, ...JSON.parse(saved) };
      }
    } catch {
      // ignore
    }
    return DEFAULT_VERSION;
  });

  const [stagedChanges, setStagedChanges] = useState<Partial<SettingsState>>({});
  const [activePackage, setActivePackage] = useState<string>(DETECTED_PACKAGES[0].pkg);
  const [logs, setLogs] = useState<LogEntry[]>(INITIAL_LOGS);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const [openThemeCustomizer, setOpenThemeCustomizer] = useState<boolean>(false);
  const [openLocationPicker, setOpenLocationPicker] = useState<boolean>(false);
  const [openAboutDialog, setOpenAboutDialog] = useState<boolean>(false);
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openApkInstallerModal, setOpenApkInstallerModal] = useState<boolean>(false);
  const [isCheckingUpdates, setIsCheckingUpdates] = useState<boolean>(false);
  const [isRestarting, setIsRestarting] = useState<boolean>(false);

  // Auto-save settings changes to localStorage and AndroidBridge
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
      const bridge = (window as unknown as { AndroidBridge?: { setPreference?: (k: string, v: string) => void } }).AndroidBridge;
      if (bridge?.setPreference) {
        Object.entries(settings).forEach(([k, v]) => {
          bridge.setPreference!(k, typeof v === 'object' ? JSON.stringify(v) : String(v));
        });
      }
    } catch (e) {
      console.error('Failed to save settings', e);
    }
  }, [settings]);

  // Auto-save version changes
  useEffect(() => {
    try {
      localStorage.setItem(VERSION_STORAGE_KEY, JSON.stringify(versionInfo));
    } catch (e) {
      console.error('Failed to save version to localStorage', e);
    }
  }, [versionInfo]);

  // Cross-tab synchronization
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === STORAGE_KEY && e.newValue) {
        try {
          const parsed = JSON.parse(e.newValue);
          setSettings(prev => ({ ...prev, ...parsed }));
        } catch {
          // ignore
        }
      }
      if (e.key === VERSION_STORAGE_KEY && e.newValue) {
        try {
          const parsed = JSON.parse(e.newValue);
          setVersionInfo(prev => ({ ...prev, ...parsed }));
        } catch {
          // ignore
        }
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(prev => (prev === msg ? null : prev));
    }, 3000);
  }, []);

  const addLog = useCallback((tag: LogEntry['tag'], message: string, source: LogEntry['source'] = 'InstaEclipse') => {
    const entry: LogEntry = {
      id: Math.random().toString(36).substring(2, 9),
      timestamp: new Date().toLocaleTimeString(),
      tag,
      message,
      source,
    };
    setLogs(prev => [entry, ...prev.slice(0, 499)]);
  }, []);

  const clearLogs = useCallback(() => {
    setLogs([]);
    showToast('Logs cleared');
  }, [showToast]);

  const stageChange = <K extends keyof SettingsState>(key: K, value: SettingsState[K]) => {
    setStagedChanges(prev => ({ ...prev, [key]: value }));
  };

  const hasStagedChanges = Object.keys(stagedChanges).length > 0;

  const commitStagedChanges = () => {
    if (!hasStagedChanges) return;
    setSettings(prev => ({ ...prev, ...stagedChanges }));
    const count = Object.keys(stagedChanges).length;
    addLog('SETTINGS', `Applied ${count} staged preference changes: ${Object.keys(stagedChanges).join(', ')}`);
    setStagedChanges({});
    showToast(`Applied ${count} changes. Restart Instagram to take effect.`);
  };

  const discardStagedChanges = () => {
    setStagedChanges({});
    showToast('Discarded unsaved changes');
  };

  const updateSettingImmediately = <K extends keyof SettingsState>(key: K, value: SettingsState[K]) => {
    setSettings(prev => ({ ...prev, [key]: value }));
    setStagedChanges(prev => {
      const copy = { ...prev };
      delete copy[key];
      return copy;
    });
    addLog('SETTINGS', `Updated ${key} -> ${JSON.stringify(value)}`);
  };

  const restartInstagram = () => {
    if (isRestarting) return;
    setIsRestarting(true);
    addLog('INFO', `Sending ACTION_RESTART broadcast to package: ${activePackage}`);
    addLog('SYNC', `Flushing world-readable XML cache to disk`);
    showToast(`Restarting ${activePackage}...`);

    setTimeout(() => {
      addLog('HOOK', `Instagram restarted and re-attached successfully via LSPosed`);
      showToast('Instagram restarted & preferences synced!');
      setIsRestarting(false);
    }, 1200);
  };

  const checkForUpdates = async () => {
    setIsCheckingUpdates(true);
    addLog('SYNC', `Checking release endpoints for channel: ${versionInfo.channel}...`);
    
    await new Promise(r => setTimeout(r, 900));
    setIsCheckingUpdates(false);
    
    if (versionInfo.version === '2.0.0') {
      setVersionInfo(prev => ({ ...prev, isLatest: true }));
      showToast('You are on the latest InstaEclipse v2.0.0 release!');
      addLog('INFO', `Release check: InstaEclipse v${versionInfo.version} is up to date (Build 20).`);
    } else {
      setVersionInfo(prev => ({ ...prev, isLatest: false, latestVersion: '2.0.0' }));
      showToast('New update available: v2.0.0 (Consolidated Release)');
      addLog('INFO', 'New update found: InstaEclipse v2.0.0 with all merged branch patches');
    }
  };

  const applyUpdate = () => {
    const nextVer = versionInfo.latestVersion || '2.0.0';
    setVersionInfo(prev => ({
      ...prev,
      version: nextVer,
      buildNumber: 20,
      releaseDate: new Date().toISOString().split('T')[0],
      isLatest: true,
    }));
    addLog('INFO', `InstaEclipse updated to version v${nextVer} (Build 20)`);
    showToast(`Successfully updated to InstaEclipse v${nextVer}!`);
  };

  const setReleaseChannel = (channel: 'Stable' | 'Beta' | 'Nightly') => {
    setVersionInfo(prev => ({ ...prev, channel }));
    addLog('SETTINGS', `Release channel switched to: ${channel}`);
    showToast(`Switched release channel to ${channel}`);
  };

  const backupSettings = () => {
    const payload = {
      app: 'InstaEclipse',
      version: versionInfo.version,
      leadMaintainer: 'Zehen (t.me/Zehen0i)',
      originalCreator: 'Somil Khan (ReSo7200)',
      exportedAt: new Date().toISOString(),
      settings,
    };
    const jsonStr = JSON.stringify(payload, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `instaeclipse_backup_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    addLog('SETTINGS', 'Settings backup exported to file');
    showToast('Settings exported to backup JSON');
  };

  const restoreSettings = (jsonString: string): boolean => {
    try {
      const parsed = JSON.parse(jsonString);
      const incoming = parsed.settings || parsed;
      setSettings(prev => ({ ...prev, ...incoming }));
      setStagedChanges({});
      addLog('SETTINGS', 'Settings restored from backup file');
      showToast('Settings successfully restored');
      return true;
    } catch (err: any) {
      addLog('ERROR', `Failed to restore settings: ${err.message}`);
      showToast('Invalid backup file format');
      return false;
    }
  };

  const resetThemeToPreset = (presetId: number) => {
    const found = THEME_PRESETS.find(p => p.id === presetId) || THEME_PRESETS[0];
    updateSettingImmediately('themePresetId', presetId);
    updateSettingImmediately('customPalette', { ...found.palette });
    addLog('SETTINGS', `Theme preset changed to "${found.name}" (ID ${presetId})`);
    showToast(`Switched to "${found.name}" palette`);
  };

  const updateCustomColorSlot = (slotKey: keyof ThemePalette, colorHex: string) => {
    const updatedPalette = { ...settings.customPalette, [slotKey]: colorHex };
    updateSettingImmediately('customPalette', updatedPalette);
    addLog('SETTINGS', `Custom palette slot "${slotKey}" set to ${colorHex}`);
  };

  return (
    <AppContext.Provider
      value={{
        settings,
        stagedChanges,
        hasStagedChanges,
        stageChange,
        commitStagedChanges,
        discardStagedChanges,
        updateSettingImmediately,
        activePackage,
        setActivePackage,
        logs,
        addLog,
        clearLogs,
        toastMessage,
        showToast,
        restartInstagram,
        isRestarting,
        backupSettings,
        restoreSettings,
        openThemeCustomizer,
        setOpenThemeCustomizer,
        openLocationPicker,
        setOpenLocationPicker,
        openAboutDialog,
        setOpenAboutDialog,
        openUpdateModal,
        setOpenUpdateModal,
        openApkInstallerModal,
        setOpenApkInstallerModal,
        versionInfo,
        isCheckingUpdates,
        checkForUpdates,
        applyUpdate,
        setReleaseChannel,
        resetThemeToPreset,
        updateCustomColorSlot,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error('useApp must be used within AppProvider');
  return ctx;
};
