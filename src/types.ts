export interface ContributorItem {
  name: string;
  role?: string;
  badge?: string;
  bio?: string;
  avatarInitials?: string;
  githubUrl: string | null;
  linkedinUrl: string | null;
  telegramUrl: string | null;
  isLead?: boolean;
}

export interface AppVersionInfo {
  version: string;
  buildNumber: number;
  releaseDate: string;
  channel: 'Stable' | 'Beta' | 'Nightly';
  isLatest: boolean;
  latestVersion: string;
  changelog: string[];
}

export interface InstagramPackage {
  pkg: string;
  label: string;
  versionName: string;
  versionCode: number;
  isInstalled: boolean;
}

export interface ThemePalette {
  background: string;
  surface: string;
  primaryText: string;
  secondaryText: string;
  accent: string;
  button: string;
  icon: string;
  glyph: string;
  divider: string;
  border: string;
  statusBar: string;
  navigation: string;
  link: string;
  error: string;
  destructive: string;
}

export interface ThemePresetData {
  id: number;
  name: string;
  palette: ThemePalette;
}

export interface LogEntry {
  id: string;
  timestamp: string;
  tag: 'HOOK' | 'DEXKIT' | 'SYNC' | 'SETTINGS' | 'INFO' | 'ERROR';
  message: string;
  source: 'InstaEclipse' | 'Instagram' | 'DexKit';
}

export interface SettingsState {
  // Dev
  isDevEnabled: boolean;
  removeBuildExpiredPopup: boolean;

  // Ghost
  isGhostSeen: boolean;
  isGhostTyping: boolean;
  isGhostStory: boolean;
  isGhostLive: boolean;
  allowScreenshots: boolean;
  isGhostScreenshot: boolean;
  isGhostViewOnce: boolean;
  enableUnlimitedReplays: boolean;
  permanentViewMode: boolean;
  keepEphemeralMessages: boolean;

  // Quick Toggles
  quickToggleSeen: boolean;
  quickToggleTyping: boolean;
  quickToggleScreenshot: boolean;
  quickToggleViewOnce: boolean;
  quickToggleStory: boolean;
  quickToggleLive: boolean;
  quickToggleEphemeral: boolean;
  quickToggleReplays: boolean;
  quickTogglePermanentView: boolean;
  quickToggleAllowScreenshots: boolean;

  // Ads
  isAdBlockEnabled: boolean;
  isAnalyticsBlocked: boolean;
  disableTrackingLinks: boolean;

  // Clean Feed
  hideSuggestionsInFeed: boolean;
  hideThreadsSuggestions: boolean;

  // Distraction Free
  isExtremeMode: boolean;
  disableStories: boolean;
  disableFeed: boolean;
  disableReels: boolean;
  disableReelsExceptDM: boolean;
  disableExplore: boolean;
  disableComments: boolean;

  // Misc
  disableStoryFlipping: boolean;
  disableVideoAutoPlay: boolean;
  spoofLastSeen: boolean;
  disableRepost: boolean;
  showFollowerToast: boolean;
  showFeatureToasts: boolean;
  enableStoryMentions: boolean;
  disableDiscoverPeople: boolean;
  enableCopyComment: boolean;
  disableDoubleTapLike: boolean;
  enableCaptionCopy: boolean;
  enablePhotoZoom: boolean;

  // Location
  spoofLocation: boolean;
  spoofLat: string;
  spoofLng: string;

  // Downloader
  enablePostDownload: boolean;
  enableStoryDownload: boolean;
  enableReelDownload: boolean;
  enableProfileDownload: boolean;
  downloaderUsernameFolder: boolean;
  downloaderAddTimestamp: boolean;
  downloaderCustomPath: string;

  // Video Quality
  forceReelQuality: number; // 0=Auto, 360, 480, 720, 1080, 9999=Max

  // Theme
  customThemeEnabled: boolean;
  themePresetId: number;
  customPalette: ThemePalette;
}

export type ActiveTab = 'home' | 'features' | 'logs' | 'help';
export type FeatureSubmenu =
  | 'main'
  | 'dev'
  | 'ghost'
  | 'qt'
  | 'ads'
  | 'cleanfeed'
  | 'distract'
  | 'misc'
  | 'downloader'
  | 'location'
  | 'quality'
  | 'theme';
