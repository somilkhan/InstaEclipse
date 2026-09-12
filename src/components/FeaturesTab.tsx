import React, { useState, useRef, useMemo } from 'react';
import {
  Search,
  Eye,
  Shield,
  Download,
  Sliders,
  Sparkles,
  MapPin,
  Palette,
  RotateCcw,
  Check,
  Save,
  Upload,
  ChevronRight,
  Filter,
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { SettingsState } from '../types';

type CategoryKey = 'all' | 'ghost' | 'ads' | 'downloader' | 'tweaks';

interface SettingItem {
  key: keyof SettingsState;
  title: string;
  subtitle: string;
  category: 'ghost' | 'ads' | 'downloader' | 'tweaks';
}

const SETTING_ITEMS: SettingItem[] = [
  // Ghost & Privacy
  {
    key: 'isGhostStory',
    title: 'Stealth Story Viewer',
    subtitle: 'View stories without appearing in the viewer list',
    category: 'ghost',
  },
  {
    key: 'isGhostSeen',
    title: 'Hide DM Read Receipts',
    subtitle: 'Do not mark direct messages as seen',
    category: 'ghost',
  },
  {
    key: 'isGhostTyping',
    title: 'Hide Typing Status',
    subtitle: 'Suppress typing indicator in direct messages',
    category: 'ghost',
  },
  {
    key: 'isGhostLive',
    title: 'Stealth Live Streams',
    subtitle: 'Join and watch live streams anonymously',
    category: 'ghost',
  },
  {
    key: 'isGhostViewOnce',
    title: 'Unlimited View-Once Media',
    subtitle: 'Prevent disappearing photos & videos from expiring',
    category: 'ghost',
  },
  {
    key: 'allowScreenshots',
    title: 'Bypass Screenshot Blocking',
    subtitle: 'Allow screenshots in view-once and secret chats',
    category: 'ghost',
  },
  {
    key: 'isGhostScreenshot',
    title: 'Hide Screenshot Alerts',
    subtitle: 'Do not notify the sender when you screenshot media',
    category: 'ghost',
  },

  // Ad Block & Feed
  {
    key: 'isAdBlockEnabled',
    title: 'Block Sponsored Posts & Ads',
    subtitle: 'Remove sponsored feed posts and interstitial story ads',
    category: 'ads',
  },
  {
    key: 'isAnalyticsBlocked',
    title: 'Block Telemetry & Tracking',
    subtitle: 'Stop background behavioral tracking pings to Meta',
    category: 'ads',
  },
  {
    key: 'disableTrackingLinks',
    title: 'Clean External URLs',
    subtitle: 'Strip tracking parameters (igshid, fbclid) from links',
    category: 'ads',
  },
  {
    key: 'hideSuggestionsInFeed',
    title: 'Hide Suggested Posts',
    subtitle: 'Remove recommended posts from accounts you do not follow',
    category: 'ads',
  },
  {
    key: 'hideThreadsSuggestions',
    title: 'Hide Threads Recommendations',
    subtitle: 'Remove Threads promotional units from your feed',
    category: 'ads',
  },

  // Downloader
  {
    key: 'enableReelDownload',
    title: 'Reels Downloader',
    subtitle: 'Download full reels in original audio and bitrate',
    category: 'downloader',
  },
  {
    key: 'enablePostDownload',
    title: 'Posts & Carousels Downloader',
    subtitle: 'Save high-resolution feed photos and album slides',
    category: 'downloader',
  },
  {
    key: 'enableStoryDownload',
    title: 'Story Downloader',
    subtitle: 'Save stories with one tap into your gallery',
    category: 'downloader',
  },
  {
    key: 'downloaderUsernameFolder',
    title: 'Organize by Creator',
    subtitle: 'Automatically save media into folders named by username',
    category: 'downloader',
  },
  {
    key: 'downloaderAddTimestamp',
    title: 'Add Timestamp to Filename',
    subtitle: 'Include creation date and time in downloaded filenames',
    category: 'downloader',
  },

  // Tweaks & Misc
  {
    key: 'removeBuildExpiredPopup',
    title: 'Disable Expiration Warnings',
    subtitle: 'Suppress popup notifications about Instagram build expiration',
    category: 'tweaks',
  },
  {
    key: 'disableDoubleTapLike',
    title: 'Disable Double-Tap to Like',
    subtitle: 'Prevent accidental likes while double-tapping to zoom',
    category: 'tweaks',
  },
  {
    key: 'enablePhotoZoom',
    title: 'Long-Press Photo Zoom',
    subtitle: 'Zoom in on profile pictures and feed images',
    category: 'tweaks',
  },
  {
    key: 'enableCaptionCopy',
    title: 'Copy Captions & Comments',
    subtitle: 'Tap to copy post captions or comment text',
    category: 'tweaks',
  },
  {
    key: 'spoofLocation',
    title: 'GPS Location Spoofing',
    subtitle: 'Inject custom coordinates into feed and posts',
    category: 'tweaks',
  },
];

interface FeaturesTabProps {
  currentSubmenu?: string;
  onSubmenuChange?: (menu: any) => void;
}

export const FeaturesTab: React.FC<FeaturesTabProps> = () => {
  const {
    settings,
    updateSettingImmediately,
    showToast,
    addLog,
    backupSettings,
    restoreSettings,
    setOpenThemeCustomizer,
    setOpenLocationPicker,
  } = useApp();

  const [activeCategory, setActiveCategory] = useState<CategoryKey>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const restoreFileRef = useRef<HTMLInputElement>(null);

  const categories = [
    { id: 'all' as CategoryKey, label: 'All' },
    { id: 'ghost' as CategoryKey, label: 'Ghost' },
    { id: 'ads' as CategoryKey, label: 'Ad Block' },
    { id: 'downloader' as CategoryKey, label: 'Downloader' },
    { id: 'tweaks' as CategoryKey, label: 'Tweaks' },
  ];

  const filteredItems = useMemo(() => {
    return SETTING_ITEMS.filter(item => {
      const matchCategory =
        activeCategory === 'all' || item.category === activeCategory;
      const q = searchQuery.toLowerCase().trim();
      const matchSearch =
        !q ||
        item.title.toLowerCase().includes(q) ||
        item.subtitle.toLowerCase().includes(q);
      return matchCategory && matchSearch;
    });
  }, [activeCategory, searchQuery]);

  const handleToggle = (key: keyof SettingsState) => {
    const currentVal = Boolean(settings[key]);
    const nextVal = !currentVal;
    updateSettingImmediately(key, nextVal as any);
    showToast(`${nextVal ? 'Enabled' : 'Disabled'}`);
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

  return (
    <div className="space-y-4 pb-24 max-w-xl mx-auto">
      {/* Hidden file input for restore */}
      <input
        type="file"
        ref={restoreFileRef}
        onChange={handleRestoreFileSelect}
        accept=".json"
        className="hidden"
      />

      {/* 1. Header & Quick Tools Bar */}
      <div className="flex items-center justify-between gap-2 flex-wrap pb-1">
        <div>
          <h2 className="text-base font-bold text-white tracking-tight">Features & Toggles</h2>
          <p className="text-xs text-zinc-400">Configure your Instagram module preferences</p>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setOpenLocationPicker(true)}
            className="px-2.5 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white text-xs font-medium flex items-center gap-1.5 transition-colors cursor-pointer"
            title="GPS Spoofing"
          >
            <MapPin className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">GPS</span>
          </button>

          <button
            onClick={() => setOpenThemeCustomizer(true)}
            className="px-2.5 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white text-xs font-medium flex items-center gap-1.5 transition-colors cursor-pointer"
            title="Theme Customizer"
          >
            <Palette className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Theme</span>
          </button>

          <button
            onClick={backupSettings}
            className="p-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white transition-colors cursor-pointer"
            title="Backup Settings"
          >
            <Save className="w-3.5 h-3.5" />
          </button>

          <button
            onClick={() => restoreFileRef.current?.click()}
            className="p-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-zinc-300 hover:text-white transition-colors cursor-pointer"
            title="Restore Settings"
          >
            <Upload className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* 2. Instant Search Bar */}
      <div className="relative">
        <Search className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
        <input
          type="text"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          placeholder="Search features (e.g. ghost, ads, download)..."
          className="w-full bg-white/[0.04] border border-white/10 rounded-2xl pl-10 pr-4 py-2.5 text-xs text-white placeholder:text-zinc-500 focus:outline-none focus:border-white/30 transition-colors"
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-xs text-zinc-400 hover:text-white"
          >
            Clear
          </button>
        )}
      </div>

      {/* 3. Category Filter Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 no-scrollbar">
        {categories.map(cat => {
          const isActive = activeCategory === cat.id;
          return (
            <button
              key={cat.id}
              onClick={() => setActiveCategory(cat.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                isActive
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'bg-white/[0.03] text-zinc-400 hover:text-white hover:bg-white/[0.06] border border-white/6'
              }`}
            >
              {cat.label}
            </button>
          );
        })}
      </div>

      {/* 4. Streamlined Toggle List */}
      <div className="space-y-2">
        {filteredItems.length === 0 ? (
          <div className="p-8 text-center rounded-3xl bg-white/[0.02] border border-white/6 space-y-2">
            <Filter className="w-6 h-6 text-zinc-500 mx-auto" />
            <p className="text-xs text-zinc-400">No matching features found</p>
            <button
              onClick={() => {
                setSearchQuery('');
                setActiveCategory('all');
              }}
              className="text-xs font-semibold text-white hover:underline"
            >
              Reset filters
            </button>
          </div>
        ) : (
          filteredItems.map(item => {
            const isChecked = Boolean(settings[item.key]);

            return (
              <div
                key={item.key}
                onClick={() => handleToggle(item.key)}
                className="flex items-center justify-between p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.06] border border-white/8 hover:border-white/15 transition-all select-none cursor-pointer"
              >
                <div className="min-w-0 pr-3">
                  <p className="text-xs font-semibold text-white">{item.title}</p>
                  <p className="text-[11px] text-zinc-400 mt-0.5 leading-snug">{item.subtitle}</p>
                </div>

                {/* Minimalist Monochrome Switch */}
                <div
                  className={`w-11 h-6 rounded-full transition-all duration-200 relative flex items-center px-0.5 shrink-0 ${
                    isChecked ? 'bg-white shadow-sm' : 'bg-zinc-800 border border-white/10'
                  }`}
                >
                  <div
                    className={`w-5 h-5 rounded-full transition-transform duration-200 transform ${
                      isChecked ? 'translate-x-5 bg-zinc-950' : 'translate-x-0 bg-zinc-400'
                    }`}
                  />
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
