import React, { useState } from 'react';
import {
  Terminal,
  Search,
  Copy,
  Trash2,
  Download,
  Filter,
  Check,
  Shield,
  Layers,
  Sparkles,
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { LogEntry } from '../types';

export const LogsTab: React.FC = () => {
  const { logs, clearLogs, showToast, addLog } = useApp();
  const [filterTag, setFilterTag] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [copied, setCopied] = useState(false);

  const tags = ['ALL', 'HOOK', 'DEXKIT', 'SYNC', 'SETTINGS', 'INFO', 'ERROR'];

  const filteredLogs = logs.filter(log => {
    const matchTag = filterTag === 'ALL' || log.tag === filterTag;
    const matchSearch =
      searchQuery === '' ||
      log.message.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.tag.toLowerCase().includes(searchQuery.toLowerCase()) ||
      log.source.toLowerCase().includes(searchQuery.toLowerCase());
    return matchTag && matchSearch;
  });

  const handleCopyLogs = () => {
    const text = filteredLogs
      .map(l => `[${l.timestamp}] [${l.source}/${l.tag}] ${l.message}`)
      .join('\n');
    navigator.clipboard.writeText(text);
    setCopied(true);
    showToast('Logs copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleExportLogs = () => {
    const text = logs
      .map(l => `[${l.timestamp}] [${l.source}/${l.tag}] ${l.message}`)
      .join('\n');
    const blob = new Blob([text], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `instaeclipse_logs_${Date.now()}.log`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('Logs exported to file');
  };

  const getTagColor = (tag: LogEntry['tag']) => {
    switch (tag) {
      case 'HOOK':
        return 'bg-white/10 text-white border-white/20';
      case 'DEXKIT':
        return 'bg-white/15 text-zinc-100 border-white/25';
      case 'SYNC':
        return 'bg-white/10 text-zinc-200 border-white/20';
      case 'SETTINGS':
        return 'bg-white/10 text-zinc-300 border-white/20';
      case 'ERROR':
        return 'bg-red-500/20 text-red-200 border-red-500/30';
      default:
        return 'bg-white/[0.05] text-zinc-400 border-white/10';
    }
  };

  return (
    <div className="space-y-4 pb-28 max-w-3xl mx-auto">
      {/* Search & Actions Bar */}
      <div className="space-y-3">
        <div className="relative flex items-center">
          <Search className="w-4 h-4 text-zinc-400 absolute left-3.5 pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            placeholder="Filter logs by keyword, bytecode hook, or class..."
            className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white/[0.03] border border-white/10 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white/30 transition-all"
          />
        </div>

        {/* Tag Filters */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
          {tags.map(tag => (
            <button
              key={tag}
              onClick={() => setFilterTag(tag)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                filterTag === tag
                  ? 'bg-white text-zinc-950 shadow-sm font-bold'
                  : 'bg-white/[0.03] text-zinc-400 hover:text-white border border-white/8 hover:border-white/16'
              }`}
            >
              {tag}
            </button>
          ))}
        </div>

        {/* Action Controls */}
        <div className="flex items-center justify-between text-xs text-zinc-400 pt-1">
          <span>
            Showing <strong className="text-white">{filteredLogs.length}</strong> of{' '}
            <strong className="text-white">{logs.length}</strong> events
          </span>

          <div className="flex items-center gap-2">
            <button
              onClick={handleCopyLogs}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white border border-white/10 transition-colors cursor-pointer"
              title="Copy to clipboard"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-white" /> : <Copy className="w-3.5 h-3.5" />}
              <span>Copy</span>
            </button>

            <button
              onClick={handleExportLogs}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white border border-white/10 transition-colors cursor-pointer"
              title="Export to file"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Export</span>
            </button>

            <button
              onClick={clearLogs}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-zinc-400 hover:text-white border border-white/10 transition-colors cursor-pointer"
              title="Clear all logs"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Clear</span>
            </button>
          </div>
        </div>
      </div>

      {/* Terminal View */}
      <div className="rounded-2xl bg-zinc-950/80 border border-white/10 shadow-2xl overflow-hidden font-mono text-xs backdrop-blur-xl">
        {/* Terminal Titlebar */}
        <div className="px-4 py-3 bg-white/[0.03] border-b border-white/10 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-2.5 h-2.5 rounded-full bg-white/20" />
            <div className="w-2.5 h-2.5 rounded-full bg-white/30" />
            <div className="w-2.5 h-2.5 rounded-full bg-white/50" />
            <span className="text-[11px] text-zinc-400 font-sans ml-2">
              InstaEclipse Diagnostic Terminal
            </span>
          </div>

          <div className="flex items-center gap-1.5 text-[11px] text-zinc-400">
            <span className="w-2 h-2 rounded-full bg-white shadow-[0_0_6px_rgba(255,255,255,0.8)] animate-pulse" />
            <span className="text-zinc-300">Listening</span>
          </div>
        </div>

        {/* Terminal Body */}
        <div className="p-4 space-y-2 max-h-[500px] overflow-y-auto">
          {filteredLogs.length === 0 ? (
            <div className="py-12 text-center text-zinc-500 font-sans">
              <Terminal className="w-8 h-8 mx-auto mb-2 text-zinc-600" />
              <p className="text-xs">No logs found matching your filter criteria</p>
            </div>
          ) : (
            filteredLogs.map(log => (
              <div
                key={log.id}
                className="flex items-start gap-2.5 hover:bg-white/[0.03] p-1.5 rounded-xl transition-colors group"
              >
                <span className="text-zinc-500 select-none shrink-0 font-mono text-[11px]">
                  {log.timestamp}
                </span>

                <span
                  className={`px-1.5 py-0.2 rounded-md text-[10px] uppercase font-bold border shrink-0 ${getTagColor(
                    log.tag
                  )}`}
                >
                  {log.tag}
                </span>

                <span className="text-zinc-500 shrink-0 font-mono text-[11px]">
                  [{log.source}]
                </span>

                <span className="text-zinc-300 break-all leading-relaxed font-mono">
                  {log.message}
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
