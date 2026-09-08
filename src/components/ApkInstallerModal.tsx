import React, { useState } from 'react';
import { Download, CheckCircle2, ShieldCheck, Sparkles, Copy, ExternalLink, X, Smartphone, HardDrive, Send, Layers, ArrowRight, FileCheck } from 'lucide-react';
import { useApp } from '../context/AppContext';

interface ApkInstallerModalProps { isOpen: boolean; onClose: () => void; }

export const ApkInstallerModal: React.FC<ApkInstallerModalProps> = ({ isOpen, onClose }) => {
  const { versionInfo, showToast, addLog } = useApp();
  const [opened, setOpened] = useState(false);
  const [copiedHash, setCopiedHash] = useState(false);
  if (!isOpen) return null;

  const url = versionInfo.apkDownloadUrl;
  const fileName = versionInfo.apkFileName || 'instaeclipse-2.0.0.apk';
  const size = versionInfo.apkFileSize || '18.5 MB';
  const sha256 = versionInfo.sha256Hash;

  const openRelease = () => {
    if (!url) { showToast('No verified APK release is configured'); return; }
    window.open(url, '_blank', 'noopener,noreferrer');
    setOpened(true);
    addLog('SYNC', `Opened verified APK release: ${fileName}`);
    showToast('Verified release opened. Install the downloaded APK when Android prompts you.');
  };
  const copyHash = async () => {
    if (!sha256) { showToast('Checksum is not published yet'); return; }
    try { await navigator.clipboard.writeText(sha256); setCopiedHash(true); showToast('SHA-256 copied'); window.setTimeout(() => setCopiedHash(false), 2000); } catch { showToast('Unable to copy checksum'); }
  };

  return <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
    <div className="relative w-full max-w-xl max-h-[92vh] overflow-y-auto glass-card rounded-3xl border border-white/15 p-5 sm:p-6 shadow-2xl space-y-5 custom-scrollbar">
      <div className="flex items-start justify-between gap-3 pb-3 border-b border-white/10">
        <div className="flex items-center gap-3"><div className="w-10 h-10 rounded-2xl bg-white/10 border border-white/20 flex items-center justify-center text-white"><Smartphone className="w-5 h-5" /></div><div><div className="flex items-center gap-2 flex-wrap"><h3 className="text-base font-bold text-white">InstaEclipse v2.0 Release APK</h3><span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-white text-zinc-950">v2.0.0 • Build 20</span></div><p className="text-xs text-zinc-400 mt-0.5">Signed release with the embedded Web Manager and Android runtime bridge.</p></div></div>
        <button id="close-apk-modal" onClick={onClose} className="p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-white/10"><X className="w-4 h-4" /></button>
      </div>

      <div className="rounded-2xl bg-white/[0.04] border border-white/12 p-4 space-y-4">
        <div className="flex items-start justify-between gap-3"><div className="space-y-1"><div className="flex items-center gap-2"><HardDrive className="w-4 h-4 text-zinc-300" /><span className="font-bold text-white text-sm">{fileName}</span></div><p className="text-xs text-zinc-400">Size: <span className="text-zinc-200 font-mono">{size}</span> • Signed Android APK</p></div><span className="text-[10px] font-mono px-2 py-0.5 rounded-md bg-white/10 text-zinc-300 border border-white/10">Verified Release</span></div>
        <button id="start-apk-download-btn" onClick={openRelease} className="w-full py-2.5 px-4 rounded-xl bg-white text-zinc-950 hover:bg-zinc-200 font-bold text-xs flex items-center justify-center gap-2"><Download className="w-4 h-4" /><span>{opened ? 'Open Release APK Again' : 'Download Release APK'}</span></button>
        {opened && <div className="p-3 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-zinc-300 flex items-start gap-2"><CheckCircle2 className="w-4 h-4 shrink-0 text-white" /><span>Download was handed to the system/browser. The app does not fabricate an APK or claim installation before Android confirms it.</span></div>}
        <div className="pt-2 border-t border-white/8 flex items-center justify-between gap-2 text-[11px]"><div className="min-w-0 flex items-center gap-1.5 text-zinc-400"><FileCheck className="w-3.5 h-3.5 shrink-0" /><span className="truncate font-mono">SHA-256: {sha256 ? `${sha256.slice(0,16)}...${sha256.slice(-8)}` : 'published with release'}</span></div><button onClick={copyHash} className="text-zinc-300 hover:text-white px-2 py-0.5 rounded hover:bg-white/10 flex items-center gap-1 shrink-0">{copiedHash ? <><CheckCircle2 className="w-3 h-3" />Copied</> : <><Copy className="w-3 h-3" />Copy</>}</button></div>
      </div>

      <div className="rounded-2xl bg-white/[0.02] border border-white/8 p-3.5 flex items-center justify-between gap-3"><div className="min-w-0"><div className="flex items-center gap-2"><p className="text-xs font-semibold text-white">Target Instagram APK</p><span className="text-[9px] font-mono px-1.5 py-0.5 rounded bg-white/10 text-zinc-300">443.x+</span></div><p className="text-[11px] text-zinc-400 truncate">Use the compatible base APK for the selected Instagram target.</p></div><a id="apkmirror-download-btn" href="https://www.apkmirror.com/uploads/?appcategory=instagram-instagram" target="_blank" rel="noreferrer" className="px-3 py-1.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white font-medium text-xs flex items-center gap-1.5 shrink-0"><span>APKMirror</span><ExternalLink className="w-3 h-3" /></a></div>

      <div className="space-y-2"><h4 className="text-xs font-bold uppercase tracking-wider text-zinc-300 flex items-center gap-1.5"><Layers className="w-3.5 h-3.5" />Merged Features</h4><div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs"><div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/8"><div className="flex items-center gap-1.5 text-white font-semibold"><ShieldCheck className="w-3.5 h-3.5" />Ghost Privacy</div><p className="text-[11px] text-zinc-400 mt-1">Runtime privacy hooks and stealth features from the stable core.</p></div><div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/8"><div className="flex items-center gap-1.5 text-white font-semibold"><Download className="w-3.5 h-3.5" />Media Downloader</div><p className="text-[11px] text-zinc-400 mt-1">Media-resolution and downloader hooks from the Android core.</p></div><div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/8"><div className="flex items-center gap-1.5 text-white font-semibold"><Sparkles className="w-3.5 h-3.5" />DexKit Hooks</div><p className="text-[11px] text-zinc-400 mt-1">Dynamic runtime resolution infrastructure for supported Instagram builds.</p></div><div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/8"><div className="flex items-center gap-1.5 text-white font-semibold"><Smartphone className="w-3.5 h-3.5" />Web Manager</div><p className="text-[11px] text-zinc-400 mt-1">The redesigned manager is embedded directly in this APK.</p></div></div></div>

      <div className="p-3.5 rounded-2xl bg-black/50 border border-white/10 space-y-2"><p className="text-[11px] font-bold uppercase tracking-wider text-zinc-300">Installation</p><div className="space-y-1.5 text-xs text-zinc-300"><div>1. Download the signed release APK.</div><div>2. Install it through Android's package installer.</div><div>3. Enable the module in LSPosed/Magisk/KernelSU and select the Instagram scope.</div><div>4. Restart Instagram and verify hooks in Logs.</div></div></div>

      <div className="flex items-center justify-between pt-2"><a href="https://t.me/InstaEclipsechat" target="_blank" rel="noreferrer" className="text-xs text-zinc-400 hover:text-white flex items-center gap-1"><Send className="w-3 h-3" />Need help? Join community<ArrowRight className="w-3 h-3" /></a><button onClick={onClose} className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-white font-semibold text-xs">Close</button></div>
    </div>
  </div>;
};
