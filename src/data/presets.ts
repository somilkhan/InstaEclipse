import { ThemePalette, ThemePresetData } from '../types';

export function intToHex(colorInt: number): string {
  const unsigned = (colorInt >>> 0) & 0xffffff;
  return '#' + unsigned.toString(16).padStart(6, '0');
}

function makePreset(
  id: number,
  name: string,
  bg: number,
  surface: number,
  primary: number,
  secondary: number,
  accent: number,
  icon: number,
  divider: number,
  nav: number,
  link: number,
  destructive: number
): ThemePresetData {
  const bgHex = intToHex(bg);
  const surfaceHex = intToHex(surface);
  const primaryHex = intToHex(primary);
  const secondaryHex = intToHex(secondary);
  const accentHex = intToHex(accent);
  const iconHex = intToHex(icon);
  const dividerHex = intToHex(divider);
  const navHex = intToHex(nav);
  const linkHex = intToHex(link);
  const destructiveHex = intToHex(destructive);

  const palette: ThemePalette = {
    background: bgHex,
    surface: surfaceHex,
    primaryText: primaryHex,
    secondaryText: secondaryHex,
    accent: accentHex,
    button: accentHex,
    icon: iconHex,
    glyph: iconHex,
    divider: dividerHex,
    border: dividerHex,
    statusBar: navHex,
    navigation: bgHex,
    link: linkHex,
    error: destructiveHex,
    destructive: destructiveHex,
  };

  return { id, name, palette };
}

// 0xFF000000 in Java ViewCompat.MEASURED_STATE_MASK is -16777216
const BLACK = -16777216;

export const THEME_PRESETS: ThemePresetData[] = [
  makePreset(0, 'Monochrome Glass', -16119285, -15527148, -460552, -7631989, -1, -460552, -14408668, -16119285, -2368549, -1226410),
  makePreset(1, 'Midnight Eclipse', -16118249, -15459539, -1512204, -7629400, -12285185, -1512204, -14801096, -16118249, -10773761, -44462),
  makePreset(2, 'Pure OLED', BLACK, -15921907, -657931, -9211021, -16738826, -657931, -15066598, BLACK, -16738826, -1226410),
  makePreset(3, 'Arctic Frost', -460036, -1, -15788246, -10193781, -14326805, -15788246, -1906448, -460036, -14326805, -2349530),
  makePreset(4, 'Rose Gold', -15068651, -14016732, -661782, -4680290, -1531724, -661782, -12768461, -15068651, -1531724, -38015),
  makePreset(5, 'Ocean Deep', -16507349, -16109500, -2034433, -9722946, -16730920, -2034433, -15584441, -16507349, -12006684, -38037),
  makePreset(6, 'Forest Canopy', -16049649, -15456232, -1509911, -8279934, -11751600, -1509911, -14798046, -16049649, -10044566, -1739917),
  makePreset(7, 'Sunset Amber', -15069176, -14018034, -3104, -4680338, -26624, -3104, -12769256, -15069176, -18611, -43230),
  makePreset(8, 'Lavender Dream', -15396833, -14542797, -792321, -5729084, -6543440, -792321, -13753792, -15396833, -4560696, -44462),
  makePreset(9, 'Cherry Blossom', -14740968, -13755868, -3851, -3894626, -40816, -3851, -12572624, -14740968, -28757, -47273),
  makePreset(10, 'Cyber Neon', -16119278, -15592930, -2031617, -9531762, -16711681, -2031617, -15066578, -16119278, -65281, -65434),
  makePreset(11, 'Coffee Mocha', -15068144, -14016488, -659996, -5729670, -7508381, -659996, -12767192, -15068144, -6190977, -2604267),
  makePreset(12, 'Royal Purple', -15594977, -14806477, -1185802, -6982195, -8630785, -1185802, -14018496, -15594977, -6982195, -44462),
  makePreset(13, 'Mint Fresh', -16115180, -15587296, -1507339, -8732768, -16725866, -1507339, -15060184, -16115180, -14233432, -38037),
  makePreset(14, 'Slate Pro', -14800581, -13418155, -920071, -7035976, -12877066, -920071, -12102295, -14800581, -10443270, -1096636),
  makePreset(15, 'Coral Reef', -15068656, -14016998, -2576, -4221304, -37023, -2576, -12768732, -15068656, -30080, -44462),
  makePreset(16, 'Nordic Ice', -1249292, -1, -13749184, -8681311, -10583636, -13749184, -2564375, -1249292, -8281663, -4234902),
  makePreset(17, 'Golden Hour', -15067640, -14015984, -1823, -4677536, -16121, -1823, -12766696, -15067640, -10929, -36797),
  makePreset(18, 'Matrix Green', -16774656, -16772096, -16711871, -16740591, -16711871, -16711871, -16768512, -16774656, -12976364, -65472),
  makePreset(19, 'Amethyst Night', -15857640, -15200216, -989441, -7311184, -5635841, -989441, -14411720, -15857640, -3238952, -44462),
  makePreset(20, 'Peach Soft', -2578, -1, -12768481, -6388624, -21615, -12768481, -661798, -2578, -30107, -1754827),
  makePreset(21, 'Steel Gray', -14606047, -13619152, -328966, -6381922, -8875876, -328966, -12434878, -14606047, -7297874, -1092784),
  makePreset(22, 'Tropical Teal', -16115174, -15718360, -2031617, -10573142, -16742021, -2031617, -15189960, -16115174, -14244198, -36797),
  makePreset(23, 'Wine Burgundy', -15070704, -14019552, -203540, -5214080, -7860657, -203540, -12773336, -15070704, -4056997, -44462),
  makePreset(24, 'Sky Blue', -1838339, -1, -15906911, -10711360, -15108398, -15906911, -4464901, -1838339, -12409355, -1754827),
  makePreset(25, 'Sand Dune', -659224, -1035, -12701146, -6386050, -2841228, -12701146, -1515568, -659224, -4412764, -2604267),
  makePreset(26, 'Violet Storm', -15726568, -15069144, -1185802, -8363872, -10354454, -1185802, -14149568, -15726568, -8630785, -44462),
  makePreset(27, 'Emerald City', -16246256, -15718376, -1509137, -10444672, -16725933, -1509137, -15189976, -16246256, -9834322, -44462),
  makePreset(28, 'Blush Pink', -3851, -1, -11919312, -5209968, -749647, -11919312, -469024, -3851, -1294214, -1754827),
  makePreset(29, 'Carbon Fiber', -15592942, -14803426, -2039584, -9079435, -16718337, -2039584, -13882324, -15592942, -16718337, -59580),
  makePreset(30, 'Aurora Borealis', -16117728, -15722448, -1509121, -10452832, -16718218, -1509121, -15196096, -16117728, -15138817, -49023),
];
