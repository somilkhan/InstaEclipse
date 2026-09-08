import { ContributorItem, InstagramPackage } from '../types';

export const LEAD_MAINTAINER: ContributorItem = {
  name: 'Zehen',
  role: 'Active Lead Maintainer & Continuator',
  badge: 'Project Revival',
  bio: 'Leading project continuation, Monochrome Glass UI transformation, web companion updates, and active release maintenance.',
  avatarInitials: 'ZH',
  githubUrl: null,
  linkedinUrl: null,
  telegramUrl: 'https://t.me/Zehen0i',
  isLead: true,
};

export const ORIGINAL_FOUNDER: ContributorItem = {
  name: 'Somil Khan (ReSo7200)',
  role: 'Original Founder & Module Architect',
  badge: 'Original Author',
  bio: 'Creator of the original InstaEclipse module architecture, DexKit hook infrastructure, and core codebase.',
  avatarInitials: 'SK',
  githubUrl: null,
  linkedinUrl: 'https://linkedin.com/in/abdalhaleem-altamimi',
  telegramUrl: 'https://t.me/InstaEclipsechat',
};

export const CONTRIBUTORS: ContributorItem[] = [
  LEAD_MAINTAINER,
  ORIGINAL_FOUNDER,
  { name: 'swakwork', role: 'Hook Engineer', avatarInitials: 'SW', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'isma3iloiso', role: 'DexKit Integration', avatarInitials: 'IO', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'Placeholder6', role: 'Core Logic', avatarInitials: 'P6', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'frknkrc44', role: 'AdBlock Rules', avatarInitials: 'FK', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'BrianML', role: 'UI Contributor', avatarInitials: 'BM', githubUrl: null, linkedinUrl: null, telegramUrl: 'https://t.me/instamoon_channel' },
  { name: 'silvzr', role: 'Media Downloader', avatarInitials: 'SZ', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'oct', role: 'Distraction Free', avatarInitials: 'OC', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'HalfManBear', role: 'Developer Options', avatarInitials: 'HB', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'ar5to', role: 'Ghost Hook Specialist', avatarInitials: 'AR', githubUrl: null, linkedinUrl: null, telegramUrl: 'https://t.me/ar5to' },
  { name: 'particle-box', role: 'Tester & CI', avatarInitials: 'PB', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'rsr', role: 'Security & Safety', avatarInitials: 'RS', githubUrl: null, linkedinUrl: null, telegramUrl: 'https://t.me/rsr1337' },
];

export const SPECIAL_THANKS: ContributorItem[] = [
  { name: 'xHookman', role: 'Android Hooking Research', avatarInitials: 'XH', githubUrl: null, linkedinUrl: null, telegramUrl: null },
  { name: 'Bluepapilte', role: 'Community Modding Support', avatarInitials: 'BP', githubUrl: null, linkedinUrl: null, telegramUrl: 'https://t.me/instasmashrepo' },
  { name: 'BdrcnAYYDIN', role: 'Localization & Testing', avatarInitials: 'BA', githubUrl: null, linkedinUrl: null, telegramUrl: 'https://t.me/BdrcnAYYDIN' },
  { name: 'Amàzing World', role: 'Feature Feedback', avatarInitials: 'AW', githubUrl: null, linkedinUrl: null, telegramUrl: null },
];

export const DETECTED_PACKAGES: InstagramPackage[] = [
  {
    pkg: 'com.instagram.android',
    label: 'Instagram (Official)',
    versionName: '443.0.0.48.82',
    versionCode: 443004882,
    isInstalled: true,
  },
  {
    pkg: 'com.instander.android',
    label: 'Instander Mod',
    versionName: '18.0',
    versionCode: 18000,
    isInstalled: false,
  },
  {
    pkg: 'com.aeroinsta.android',
    label: 'AeroInsta',
    versionName: '24.0.1',
    versionCode: 24001,
    isInstalled: false,
  },
  {
    pkg: 'com.instagram.lite',
    label: 'Instagram Lite',
    versionName: '380.0.0.7.109',
    versionCode: 3800007,
    isInstalled: false,
  },
];
