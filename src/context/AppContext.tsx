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
const VERSION_STORAGE_KEY = 'instaeclipse_version_cache';

const DEFAULT_VERSION: AppVersionInfo = {
  version: '0.7.0',
  buildNumber: 18,
  releaseDate: '2026-09-08',
  channel: 'Stable',
  isLatest: false,
  latestVersion: '0.7.2',
  changelog: [
    '✨ Monochrome x GlassUI: Aesthetic redesign with obsidian glassmorphism & silver accents',
    '👑 Project Continuation: Zehen (@Zehen0i) credited as active maintainer and continuation lead',
    '🧭 Floating Navpill: Fluid animated glass navigation pill with layout transitions',
    '🔄 Sync & Multi-tab Engine: Synchronized preferences across tabs with instant localStorage sync',
    '⚡ Enhanced Hook Dispatcher: DexKit 2.0.4 bytecode caching & fast module restart simulation',
    '🛡️ Ghost & Privacy: Enhanced screenshot allowance & view-once permanence stability',
  ],
};

const INITIAL_LOGS: LogEntry[] = [
  {
    id: '1',
    timestamp: new Date(Date.now() - 1000 * 60 * 12).toLocaleTimeString(),
    tag: 'INFO',
    message: 'InstaEclipse Companion initialized (v0.7.0 - Revival by Zehen)',
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
  const [isCheckingUpdates, setIsCheckingUpdates] = useState<boolean>(false);
  const [isRestarting, setIsRestarting] = useState<boolean>(false);

  // Auto-save settings changes to localStorage
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    } catch (e) {
      console.error('Failed to save settings to localStorage', e);
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
    
    // If user is already on latest, let them know or offer beta
    if (versionInfo.version === '0.7.2') {
      setVersionInfo(prev => ({ ...prev, isLatest: true }));
      showToast('You are on the latest version!');
      addLog('INFO', `Release check: InstaEclipse v${versionInfo.version} is up to date.`);
    } else {
      setVersionInfo(prev => ({ ...prev, isLatest: false, latestVersion: '0.7.2' }));
      showToast('New update available: v0.7.2 (Monochrome Glass)');
      addLog('INFO', 'New update found: InstaEclipse v0.7.2 with enhanced Monochrome Glass UI');
    }
  };

  const applyUpdate = () => {
    const nextVer = versionInfo.latestVersion || '0.7.2';
    setVersionInfo(prev => ({
      ...prev,
      version: nextVer,
      buildNumber: prev.buildNumber + 1,
      releaseDate: new Date().toISOString().split('T')[0],
      isLatest: true,
    }));
    addLog('INFO', `InstaEclipse updated to version v${nextVer} (Build ${versionInfo.buildNumber + 1})`);
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
