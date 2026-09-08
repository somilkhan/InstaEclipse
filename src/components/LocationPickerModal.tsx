import React, { useState, useEffect } from 'react';
import {
  ArrowLeft,
  Search,
  MapPin,
  Check,
  X,
  Crosshair,
  Compass,
  Navigation,
  Globe,
  Loader2,
} from 'lucide-react';
import { useApp } from '../context/AppContext';

interface LocationPickerModalProps {
  onClose: () => void;
}

interface SearchResult {
  place_id: number;
  display_name: string;
  lat: string;
  lon: string;
}

const QUICK_PRESETS = [
  { name: 'Tokyo, Japan', lat: '35.6762', lng: '139.6503' },
  { name: 'New York, USA', lat: '40.7128', lng: '-74.0060' },
  { name: 'Paris, France', lat: '48.8566', lng: '2.3522' },
  { name: 'London, UK', lat: '51.5074', lng: '-0.1278' },
  { name: 'Sydney, Australia', lat: '-33.8688', lng: '151.2093' },
  { name: 'Dubai, UAE', lat: '25.2048', lng: '55.2708' },
  { name: 'Rio de Janeiro, Brazil', lat: '-22.9068', lng: '-43.1729' },
  { name: 'Cairo, Egypt', lat: '30.0444', lng: '31.2357' },
];

export const LocationPickerModal: React.FC<LocationPickerModalProps> = ({ onClose }) => {
  const { settings, updateSettingImmediately, showToast, addLog } = useApp();

  const [lat, setLat] = useState<string>(settings.spoofLat || '35.6895');
  const [lng, setLng] = useState<string>(settings.spoofLng || '139.6917');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [isSearching, setIsSearching] = useState<boolean>(false);

  // Convert lat/lng into SVG coordinates for the interactive world map
  // Lat: -90 to +90 -> Y: 100% to 0%
  // Lng: -180 to +180 -> X: 0% to 100%
  const numLat = parseFloat(lat) || 0;
  const numLng = parseFloat(lng) || 0;

  const pinX = ((numLng + 180) / 360) * 100;
  const pinY = ((90 - numLat) / 180) * 100;

  const handleMapClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    const percentX = clickX / rect.width;
    const percentY = clickY / rect.height;

    const newLng = (percentX * 360 - 180).toFixed(4);
    const newLat = (90 - percentY * 180).toFixed(4);

    setLat(newLat);
    setLng(newLng);
  };

  const handleSearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const q = searchQuery.trim();
    if (!q) return;

    setIsSearching(true);
    try {
      const res = await fetch(
        `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(q)}&limit=5`,
        {
          headers: {
            'Accept-Language': 'en',
          },
        }
      );
      if (res.ok) {
        const data = await res.json();
        setSearchResults(data);
      }
    } catch {
      // Fallback matching in local quick presets
      const filtered = QUICK_PRESETS.filter(p =>
        p.name.toLowerCase().includes(q.toLowerCase())
      ).map((p, idx) => ({
        place_id: idx,
        display_name: p.name,
        lat: p.lat,
        lon: p.lng,
      }));
      setSearchResults(filtered);
    } finally {
      setIsSearching(false);
    }
  };

  const selectSearchResult = (item: SearchResult) => {
    setLat(parseFloat(item.lat).toFixed(4));
    setLng(parseFloat(item.lon).toFixed(4));
    setSearchResults([]);
    setSearchQuery(item.display_name.split(',')[0]);
  };

  const applyLocation = () => {
    updateSettingImmediately('spoofLat', lat);
    updateSettingImmediately('spoofLng', lng);
    updateSettingImmediately('spoofLocation', true);
    addLog('INFO', `Spoof location coordinates updated to: (${lat}, ${lng})`);
    showToast(`Spoofed GPS coordinates set to ${lat}, ${lng}`);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 bg-[#070709]/95 backdrop-blur-2xl flex flex-col overflow-hidden animate-in fade-in duration-200">
      {/* Header */}
      <header className="sticky top-0 z-20 bg-zinc-950/80 backdrop-blur-xl border-b border-white/10 px-4 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={onClose}
            className="p-1.5 -ml-1 rounded-xl hover:bg-white/10 text-zinc-300 hover:text-white transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <div className="w-6 h-6 rounded-lg bg-white/10 border border-white/15 flex items-center justify-center">
                <MapPin className="w-3.5 h-3.5 text-white" />
              </div>
              Location Picker
            </h2>
            <p className="text-xs text-zinc-400">OpenStreetMap GPS Spoofing</p>
          </div>
        </div>

        <button
          onClick={applyLocation}
          className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-white hover:bg-zinc-200 text-zinc-950 text-xs font-bold shadow-lg shadow-white/10 transition-all active:scale-95 cursor-pointer"
        >
          <Check className="w-3.5 h-3.5 stroke-[3]" />
          <span>Use Location</span>
        </button>
      </header>

      {/* Main Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4 max-w-3xl mx-auto w-full pb-24">
        {/* Search Bar */}
        <form onSubmit={handleSearch} className="relative">
          <div className="relative flex items-center">
            <Search className="w-4 h-4 text-zinc-400 absolute left-3 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search place or city (e.g. Tokyo, Times Square, Eiffel Tower)..."
              className="w-full pl-9 pr-20 py-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white/30 transition-colors"
            />
            <div className="absolute right-2 flex items-center gap-1">
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchQuery('');
                    setSearchResults([]);
                  }}
                  className="p-1 text-zinc-400 hover:text-white"
                >
                  <X className="w-3.5 h-3.5" />
                </button>
              )}
              <button
                type="submit"
                disabled={isSearching}
                className="px-2.5 py-1 rounded-lg bg-white/10 hover:bg-white/15 text-xs font-medium text-white transition-colors cursor-pointer"
              >
                {isSearching ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : 'Search'}
              </button>
            </div>
          </div>

          {/* Search dropdown results */}
          {searchResults.length > 0 && (
            <div className="absolute top-full left-0 right-0 z-30 mt-1 rounded-2xl bg-zinc-950 border border-white/15 shadow-2xl overflow-hidden divide-y divide-white/8 backdrop-blur-xl">
              {searchResults.map(res => (
                <button
                  key={res.place_id}
                  type="button"
                  onClick={() => selectSearchResult(res)}
                  className="w-full text-left p-3 hover:bg-white/5 text-xs text-white transition-colors flex items-start gap-2 cursor-pointer"
                >
                  <MapPin className="w-3.5 h-3.5 text-white shrink-0 mt-0.5" />
                  <span className="truncate">{res.display_name}</span>
                </button>
              ))}
            </div>
          )}
        </form>

        {/* Current Active Pin Indicator */}
        <div className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Crosshair className="w-4 h-4 text-amber-400" />
            <span className="text-xs text-zinc-400">Target Coordinates:</span>
          </div>
          <div className="inline-flex items-center gap-2 font-mono text-xs font-semibold text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-lg border border-amber-500/20">
            <span>Lat: {parseFloat(lat).toFixed(4)}</span>
            <span>&bull;</span>
            <span>Lng: {parseFloat(lng).toFixed(4)}</span>
          </div>
        </div>

        {/* Interactive Map Canvas Container */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs text-zinc-400 px-1">
            <span className="flex items-center gap-1.5">
              <Globe className="w-3.5 h-3.5" />
              Tap anywhere on map to drop pin
            </span>
            <span className="text-[11px] text-zinc-500">Mercator Projection</span>
          </div>

          <div
            onClick={handleMapClick}
            className="relative w-full h-64 sm:h-80 rounded-2xl bg-zinc-950 border border-zinc-800 overflow-hidden cursor-crosshair group select-none shadow-inner"
            style={{
              backgroundImage: `radial-gradient(circle at 1px 1px, rgba(255,255,255,0.06) 1px, transparent 0)`,
              backgroundSize: '24px 24px',
            }}
          >
            {/* World Map SVG Vector Silhouette */}
            <svg
              className="w-full h-full opacity-40 group-hover:opacity-60 transition-opacity"
              viewBox="0 0 1000 500"
              preserveAspectRatio="none"
              fill="#52525b"
            >
              {/* Simplified world continents outlines */}
              {/* North America */}
              <path d="M150,80 Q200,60 250,80 T300,150 T280,240 T210,240 T170,180 Z" />
              {/* South America */}
              <path d="M280,260 Q340,280 320,380 T260,460 T240,360 T270,270 Z" />
              {/* Europe */}
              <path d="M460,70 Q520,60 560,90 T540,160 T480,160 T450,110 Z" />
              {/* Africa */}
              <path d="M470,170 Q560,180 580,260 T540,380 T480,360 T450,240 Z" />
              {/* Asia */}
              <path d="M580,70 Q750,50 850,120 T800,260 T680,240 T580,160 Z" />
              {/* Australia */}
              <path d="M780,320 Q860,310 880,380 T800,430 T750,380 Z" />
            </svg>

            {/* Latitude / Longitude Guide lines */}
            <div className="absolute top-1/2 left-0 right-0 h-px bg-zinc-800/60 border-t border-dashed border-zinc-700/50 pointer-events-none" />
            <div className="absolute left-1/2 top-0 bottom-0 w-px bg-zinc-800/60 border-l border-dashed border-zinc-700/50 pointer-events-none" />

            {/* Center Pin Indicator (#EB6D24 color from LocationPickerActivity.java) */}
            <div
              className="absolute pointer-events-none transition-all duration-150 transform -translate-x-1/2 -translate-y-full"
              style={{
                left: `${Math.max(2, Math.min(98, pinX))}%`,
                top: `${Math.max(4, Math.min(96, pinY))}%`,
              }}
            >
              <div className="flex flex-col items-center">
                <div className="w-8 h-8 rounded-full bg-[#EB6D24] shadow-lg shadow-orange-500/50 flex items-center justify-center text-white ring-4 ring-orange-500/20 animate-bounce">
                  <MapPin className="w-4 h-4 fill-current" />
                </div>
                <div className="w-2 h-2 rounded-full bg-[#EB6D24] mt-1 shadow" />
              </div>
            </div>
          </div>
        </div>

        {/* Manual Latitude & Longitude Inputs */}
        <div className="grid grid-cols-2 gap-2.5">
          <div className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 space-y-1">
            <label className="text-[11px] font-semibold text-zinc-400">Latitude (-90 to +90)</label>
            <input
              type="number"
              step="any"
              value={lat}
              onChange={e => setLat(e.target.value)}
              className="w-full px-2 py-1 rounded-lg bg-zinc-950 border border-zinc-800 text-xs font-mono text-zinc-200 focus:outline-none focus:border-amber-500"
            />
          </div>

          <div className="p-3 rounded-xl bg-zinc-900 border border-zinc-800 space-y-1">
            <label className="text-[11px] font-semibold text-zinc-400">Longitude (-180 to +180)</label>
            <input
              type="number"
              step="any"
              value={lng}
              onChange={e => setLng(e.target.value)}
              className="w-full px-2 py-1 rounded-lg bg-zinc-950 border border-zinc-800 text-xs font-mono text-zinc-200 focus:outline-none focus:border-amber-500"
            />
          </div>
        </div>

        {/* Quick City Presets */}
        <div className="space-y-2">
          <p className="text-xs font-bold uppercase tracking-wider text-zinc-400 px-1">
            Popular City Presets
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {QUICK_PRESETS.map(city => (
              <button
                key={city.name}
                type="button"
                onClick={() => {
                  setLat(city.lat);
                  setLng(city.lng);
                  showToast(`Selected ${city.name}`);
                }}
                className="p-2.5 rounded-xl bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 hover:border-zinc-700 text-left transition-all"
              >
                <p className="text-xs font-semibold text-zinc-200 truncate">{city.name}</p>
                <p className="text-[10px] font-mono text-zinc-500 mt-0.5">
                  {city.lat}, {city.lng}
                </p>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
