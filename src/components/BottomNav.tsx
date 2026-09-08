import React from 'react';
import { motion } from 'motion/react';
import { Home, Sliders, Terminal, HelpCircle } from 'lucide-react';
import { ActiveTab } from '../types';
import { useApp } from '../context/AppContext';

interface BottomNavProps {
  activeTab: ActiveTab;
  onTabChange: (tab: ActiveTab) => void;
}

export const BottomNav: React.FC<BottomNavProps> = ({ activeTab, onTabChange }) => {
  const { hasStagedChanges, stagedChanges } = useApp();
  const stagedCount = Object.keys(stagedChanges).length;

  const tabs = [
    { id: 'home' as ActiveTab, label: 'Home', icon: Home },
    { id: 'features' as ActiveTab, label: 'Features', icon: Sliders, badge: hasStagedChanges ? stagedCount : null },
    { id: 'logs' as ActiveTab, label: 'Logs', icon: Terminal },
    { id: 'help' as ActiveTab, label: 'Help', icon: HelpCircle },
  ];

  return (
    <div className="fixed bottom-5 left-1/2 -translate-x-1/2 z-40 max-w-sm w-[calc(100%-2rem)] pointer-events-auto">
      <nav
        id="floating-navpill"
        className="relative flex items-center justify-between p-1.5 rounded-full bg-zinc-950/80 backdrop-blur-2xl border border-white/12 shadow-[0_16px_40px_-10px_rgba(0,0,0,0.85),0_0_0_1px_rgba(255,255,255,0.05)]"
      >
        {tabs.map(tab => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;

          return (
            <button
              key={tab.id}
              id={`nav-tab-${tab.id}`}
              onClick={() => onTabChange(tab.id)}
              className="relative flex-1 flex flex-col items-center justify-center py-2 px-1 rounded-full select-none transition-all duration-200 outline-none cursor-pointer group"
            >
              {/* Animated Floating Pill Background */}
              {isActive && (
                <motion.div
                  layoutId="activeNavPillIndicator"
                  className="absolute inset-0 rounded-full bg-white/12 border border-white/20 shadow-[0_2px_12px_rgba(255,255,255,0.08)]"
                  transition={{ type: 'spring', stiffness: 450, damping: 36 }}
                />
              )}

              {/* Icon with subtle glow when active */}
              <div className="relative z-10 flex items-center justify-center">
                <Icon
                  className={`w-4 h-4 transition-all duration-200 ${
                    isActive
                      ? 'text-white scale-110 drop-shadow-[0_0_8px_rgba(255,255,255,0.5)]'
                      : 'text-zinc-400 group-hover:text-zinc-200'
                  }`}
                />

                {/* Staged Changes Badge */}
                {tab.badge && (
                  <span className="absolute -top-1.5 -right-2.5 min-w-4 h-4 bg-white text-zinc-950 text-[9px] font-extrabold rounded-full flex items-center justify-center px-1 shadow-[0_0_10px_rgba(255,255,255,0.8)] animate-pulse">
                    {tab.badge}
                  </span>
                )}
              </div>

              {/* Label */}
              <span
                className={`relative z-10 text-[10px] tracking-tight mt-1 transition-colors duration-200 ${
                  isActive ? 'font-semibold text-white' : 'font-normal text-zinc-400 group-hover:text-zinc-300'
                }`}
              >
                {tab.label}
              </span>
            </button>
          );
        })}
      </nav>
    </div>
  );
};
