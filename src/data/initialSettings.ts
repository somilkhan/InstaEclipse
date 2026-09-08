import { SettingsState } from '../types';
import { THEME_PRESETS } from './presets';

export const DEFAULT_SETTINGS: SettingsState = {
  // Dev
  isDevEnabled: false,
  removeBuildExpiredPopup: true,

  // Ghost
  isGhostSeen: false,
  isGhostTyping: false,
  isGhostStory: false,
  isGhostLive: false,
  allowScreenshots: true,
  isGhostScreenshot: false,
  isGhostViewOnce: false,
  enableUnlimitedReplays: false,
  permanentViewMode: false,
  keepEphemeralMessages: false,

  // Quick Toggles
  quickToggleSeen: true,
  quickToggleTyping: true,
  quickToggleScreenshot: true,
  quickToggleViewOnce: true,
  quickToggleStory: true,
  quickToggleLive: true,
  quickToggleEphemeral: true,
  quickToggleReplays: true,
  quickTogglePermanentView: true,
  quickToggleAllowScreenshots: true,

  // Ads
  isAdBlockEnabled: true,
  isAnalyticsBlocked: true,
  disableTrackingLinks: true,

  // Clean Feed
  hideSuggestionsInFeed: true,
  hideThreadsSuggestions: true,

  // Distraction Free
  isExtremeMode: false,
  disableStories: false,
  disableFeed: false,
  disableReels: false,
  disableReelsExceptDM: false,
  disableExplore: false,
  disableComments: false,

  // Misc
  disableStoryFlipping: false,
  disableVideoAutoPlay: false,
  spoofLastSeen: false,
  disableRepost: false,
  showFollowerToast: true,
  showFeatureToasts: true,
  enableStoryMentions: true,
  disableDiscoverPeople: true,
  enableCopyComment: true,
  disableDoubleTapLike: false,
  enableCaptionCopy: true,
  enablePhotoZoom: true,

  // Location
  spoofLocation: false,
  spoofLat: '35.6895',
  spoofLng: '139.6917',

  // Downloader
  enablePostDownload: true,
  enableStoryDownload: true,
  enableReelDownload: true,
  enableProfileDownload: true,
  downloaderUsernameFolder: true,
  downloaderAddTimestamp: false,
  downloaderCustomPath: '/storage/emulated/0/Download/InstaEclipse',

  // Video Quality
  forceReelQuality: 0, // Auto

  // Theme
  customThemeEnabled: true,
  themePresetId: 0,
  customPalette: { ...THEME_PRESETS[0].palette },
};

export const DEFAULT_MC_OVERRIDES = {
  "69718:": ["4: : true", "2: : true"],
  "52089:": ["1: : 2"],
  "99051:": ["0: : 2"],
  "24298:": ["0: : 8000000"],
  "26104:": ["1: : 50"],
  "96230:": ["30: : 100"],
  "31447:": ["4: : 20", "3: : 3000"],
  "34514:": ["38: : 5000000"],
  "55416:": ["2: : 0", "1: : 100"],
  "83992:": ["1: : 2"],
  "76154:": ["15: : 8000", "14: : 100", "12: : 100", "11: : 1440", "10: : 2160", "16: : 10000"],
  "77821:": ["52: : 60000"],
  "80122:": ["1: : 100"],
  "74153:": ["22: : true"],
  "47131:": ["14: : true", "12: : true", "10: : news", "9: : share", "4: : direct", "3: : clips", "2: : profile", "1: : explore"],
  "66222:": ["5: : true"],
  "85030:": ["7: : true"],
  "72311:": ["10: : true", "2: : true", "1: : true"],
  "76670:": ["6: : true", "5: : 60"],
  "117422:": ["0: : true"],
  "67697:": ["10: : true"],
  "23744:": ["0: : true"],
  "61596:": ["51: : 1440", "26: : 1440"],
  "83371:": ["0: : true"],
  "107820:": ["0: : true"],
  "76418:": ["3: : true"],
  "71155:": ["0: : false"],
  "106294:": ["0: : true"],
  "72984:": ["1: : true"],
  "44565:": ["0: : true", "1: : true"],
  "38195:": ["0: : true", "2: : 100"],
  "110034:": ["3: : true", "1: : true", "0: : true", "2: : true"],
  "107757:": ["2: : true", "0: : true"],
  "83689:": ["4: : true", "0: : true", "3: : true"],
  "75711:": ["1: : true", "5: : true", "4: : true", "2: : true", "0: : true"],
  "56859:": ["51: : true"],
  "87480:": ["6: : true", "5: : true", "4: : true", "2: : true", "1: : false", "0: : true", "3: : true"],
  "93842:": ["0: : true"],
  "82625:": ["7: : true", "3: : true", "4: : true", "15: : true", "13: : true", "0: : true"],
  "91489:": ["6: : true", "0: : true", "2: : true"],
  "106149:": ["0: : true"],
  "84236:": ["4: : true"],
  "_qe_overrides_": []
};
