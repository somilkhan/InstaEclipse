package ps.reso.instaeclipse.mods.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.core.DexKitCache;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.i18n.I18n;
import ps.reso.instaeclipse.utils.log.ModuleLog;

public class ReelDownloadHook {
    private static Class<?> controllerClass;
    private static Method hookMethod;
    private static Method buttonAdderMethod;
    private static Field activityField;
    private static Field cachedOuterField = null;
    private static Field cachedInnerField = null;
    private static final AtomicBoolean OPTIONS_PATCH_INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean BUTTON_ADDER_PATCH_INSTALLED = new AtomicBoolean(false);
    private static final ThreadLocal<Boolean> ADDING_MODULE_BUTTON = new ThreadLocal<>();

    public void install(DexKitBridge bridge, ClassLoader classLoader) {
        // Keep the list-level patch as a compatibility fallback, but also intercept the
        // final button insertion point below. Instagram 443 can still expose its native
        // Download action through a path that bypasses the option list.
        installRemoveNativeDownloadOption(bridge, classLoader);

        if (DexKitCache.isCacheValid()) {
            Method cached = DexKitCache.loadMethod("ReelDownload", classLoader);
            if (cached != null) {
                controllerClass = cached.getDeclaringClass();
                hookMethod = cached;
                cached.setAccessible(true);
                FeatureStatusTracker.setHooked("ReelDownload");
                XposedBridge.hookMethod(cached, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (FeatureFlags.enableReelDownload) onOptionsBuilt(p);
                    }
                });
                ModuleLog.line("(IE|Reel) ✅ hooked: " + hookMethod.getDeclaringClass().getName() + "." + hookMethod.getName());
                return;
            }
        }
        try {
            var methods = bridge.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create().usingStrings("ClipsOrganicMediaItemViewMoreOptionsController")));
            if (methods.isEmpty()) {
                ModuleLog.line("(IE|Reel) ❌ ClipsOrganicMediaItemViewMoreOptionsController not found");
                return;
            }
            controllerClass = methods.get(0).getMethodInstance(classLoader).getDeclaringClass();
            Method target = null;
            for (Method m : controllerClass.getDeclaredMethods()) {
                if (m.getReturnType() != void.class) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length >= 2
                        && params[0].getName().equals("com.instagram.feed.media.Media")
                        && !params[1].isPrimitive()
                        && params[1] != String.class) {
                    target = m;
                    break;
                }
            }
            if (target == null) {
                ModuleLog.line("(IE|Reel) ❌ hook method (Media, ButtonAdder)V not found");
                return;
            }
            target.setAccessible(true);
            hookMethod = target;
            DexKitCache.saveMethod("ReelDownload", target);
            FeatureStatusTracker.setHooked("ReelDownload");
            XposedBridge.hookMethod(target, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (FeatureFlags.enableReelDownload) onOptionsBuilt(p);
                }
            });
            ModuleLog.line("(IE|Reel) ✅ hooked: " + hookMethod.getDeclaringClass().getName() + "." + hookMethod.getName());
        } catch (Throwable t) {
            ModuleLog.line("(IE|Reel) ❌ install: " + t);
        }
    }

    private static void installRemoveNativeDownloadOption(DexKitBridge bridge, ClassLoader classLoader) {
        if (!OPTIONS_PATCH_INSTALLED.compareAndSet(false, true)) return;
        try {
            Class<?> optionClass = classLoader.loadClass("com.instagram.feed.media.mediaoption.MediaOption$Option");
            Object download = null;
            for (Object value : (Object[]) optionClass.getMethod("values").invoke(null)) {
                if ("DOWNLOAD".equals(value.toString())) {
                    download = value;
                    break;
                }
            }
            if (download == null) return;
            final Object nativeDownload = download;
            XC_MethodHook removeHook = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!FeatureFlags.enableReelDownload) return;
                    try {
                        Object result = p.getResult();
                        if (result instanceof List<?>) {
                            ((List<?>) result).remove(nativeDownload);
                        }
                    } catch (Throwable t) {
                        ModuleLog.line("(IE|Reel) ⚠️ native DOWNLOAD removal failed: " + t.getClass().getSimpleName());
                    }
                }
            };

            if (DexKitCache.isCacheValid()) {
                Method cached = DexKitCache.loadMethod("ReelOptionsListBuilder", classLoader);
                if (cached != null) {
                    XposedBridge.hookMethod(cached, removeHook);
                    return;
                }
            }

            String optionDescriptor = "Lcom/instagram/feed/media/mediaoption/MediaOption$Option;";
            var methods = bridge.findMethod(FindMethod.create().matcher(
                    MethodMatcher.create()
                            .returnType("java.util.ArrayList")
                            .addUsingField(optionDescriptor + "->PLAYBACK_CONTROLS:" + optionDescriptor)
                            .addUsingField(optionDescriptor + "->UNSAVE:" + optionDescriptor)));
            if (methods.isEmpty()) {
                ModuleLog.line("(IE|Reel) ⚠️ native DOWNLOAD option builder not found");
                OPTIONS_PATCH_INSTALLED.set(false);
                return;
            }
            Method target = methods.get(0).getMethodInstance(classLoader);
            target.setAccessible(true);
            XposedBridge.hookMethod(target, removeHook);
            DexKitCache.saveMethod("ReelOptionsListBuilder", target);
            ModuleLog.line("(IE|Reel) ✅ native DOWNLOAD option suppressed");
        } catch (Throwable t) {
            OPTIONS_PATCH_INSTALLED.set(false);
            ModuleLog.line("(IE|Reel) ⚠️ native DOWNLOAD suppression unavailable: " + t.getClass().getSimpleName());
        }
    }

    private static void installNativeDownloadButtonGuard(Object adder) {
        if (adder == null || BUTTON_ADDER_PATCH_INSTALLED.get()) return;
        Method candidate = null;
        for (Method m : adder.getClass().getDeclaredMethods()) {
            Class<?>[] ps = m.getParameterTypes();
            if (ps.length == 4
                    && Context.class.isAssignableFrom(ps[0])
                    && View.OnClickListener.class.isAssignableFrom(ps[1])
                    && ps[2] == String.class
                    && ps[3] == int.class) {
                candidate = m;
                break;
            }
        }
        if (candidate == null) return;
        try {
            candidate.setAccessible(true);
            buttonAdderMethod = candidate;
            if (!BUTTON_ADDER_PATCH_INSTALLED.compareAndSet(false, true)) return;
            XposedBridge.hookMethod(candidate, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (Boolean.TRUE.equals(ADDING_MODULE_BUTTON.get())) return;
                    if (!FeatureFlags.enableReelDownload || p.args == null || p.args.length < 3) return;
                    Object labelArg = p.args[2];
                    if (!(labelArg instanceof String)) return;
                    String label = ((String) labelArg).trim();
                    if (isNativeDownloadLabel(label)) {
                        ModuleLog.line("(IE|Reel) ✅ intercepted native Download button");
                        p.setResult(null);
                    }
                }
            });
            ModuleLog.line("(IE|Reel) ✅ native Download button guard hooked: "
                    + candidate.getDeclaringClass().getName() + "." + candidate.getName());
        } catch (Throwable t) {
            BUTTON_ADDER_PATCH_INSTALLED.set(false);
            ModuleLog.line("(IE|Reel) ⚠️ native Download button guard unavailable: "
                    + t.getClass().getSimpleName());
        }
    }

    private static boolean isNativeDownloadLabel(String label) {
        if (label == null || label.isEmpty()) return false;
        String normalized = label.toLowerCase(java.util.Locale.ROOT).replace("_", " ").trim();
        return normalized.equals("download")
                || normalized.equals("download reel")
                || normalized.equals("download video")
                || normalized.startsWith("download ");
    }

    private static int findReelCarouselIndex(Object controller) {
        if(controller==null)return 0;
        if(cachedOuterField!=null&&cachedInnerField!=null)try{Object h=cachedOuterField.get(controller);if(h!=null)return cachedInnerField.getInt(h);}catch(Throwable ignored){}
        int best=Integer.MAX_VALUE;Field bo=null,bi=null;Class<?> c=controller.getClass();
        while(c!=null&&c!=Object.class){for(Field of:c.getDeclaredFields()){if(of.getType().isPrimitive())continue;String n=of.getType().getName();if(n.startsWith("android.")||n.startsWith("java.")||n.startsWith("androidx.")||n.startsWith("kotlin."))continue;of.setAccessible(true);Object nested;try{nested=of.get(controller);}catch(Throwable e){continue;}if(nested==null)continue;Field one=null;int count=0;Class<?> nc=nested.getClass();while(nc!=null&&nc!=Object.class){String nn=nc.getName();if(nn.startsWith("android.")||nn.startsWith("java.")||nn.startsWith("androidx.")||nn.startsWith("kotlin."))break;for(Field f:nc.getDeclaredFields())if(f.getType()==int.class){count++;one=f;if(count>1)break;}if(count>1)break;nc=nc.getSuperclass();}if(count==1&&one!=null){one.setAccessible(true);try{int idx=one.getInt(nested);if(idx>=0&&idx<200&&idx<best){best=idx;bo=of;bi=one;}}catch(Throwable ignored){}}}c=c.getSuperclass();}
        if(bo!=null){cachedOuterField=bo;cachedInnerField=bi;return best;}return 0;
    }
    static int findCarouselIndexFromView(Context ctx,int size){if(!(ctx instanceof Activity))return -1;try{List<Integer>m=new java.util.ArrayList<>();collectCarouselMatches(((Activity)ctx).getWindow().getDecorView(),size,m);return m.size()==1?m.get(0):-1;}catch(Throwable e){return -1;}}
    private static int adapterCount(Object a){try{return(int)a.getClass().getMethod("getItemCount").invoke(a);}catch(Throwable ignored){}try{return(int)a.getClass().getMethod("getCount").invoke(a);}catch(Throwable ignored){}return -1;}
    private static void collectCarouselMatches(View v,int size,List<Integer>out){String cn=v.getClass().getName();if(cn.contains("ViewPager"))try{Object a=v.getClass().getMethod("getAdapter").invoke(v);if(a!=null&&adapterCount(a)==size)for(String g:new String[]{"getCurrentItem","getCurrentDataIndex","getCurrentWrappedDataIndex","getCurrentRawDataIndex"})try{int p=(int)v.getClass().getMethod(g).invoke(v);if(p>=0){out.add(p);break;}}catch(NoSuchMethodException ignored){} }catch(Throwable ignored){} if(cn.contains("RecyclerView"))try{Object a=v.getClass().getMethod("getAdapter").invoke(v);if(a!=null&&adapterCount(a)==size){Object lm=v.getClass().getMethod("getLayoutManager").invoke(v);if(lm!=null){try{int o=(int)lm.getClass().getMethod("getOrientation").invoke(lm);if(o!=0)lm=null;}catch(Throwable ignored){}if(lm!=null){Integer p=null;try{int x=(int)lm.getClass().getMethod("findFirstCompletelyVisibleItemPosition").invoke(lm);if(x>=0)p=x;}catch(Throwable ignored){}if(p==null)try{int x=(int)lm.getClass().getMethod("findFirstVisibleItemPosition").invoke(lm);if(x>=0)p=x;}catch(Throwable ignored){}if(p!=null)out.add(p);}}}}catch(Throwable ignored){} if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collectCarouselMatches(g.getChildAt(i),size,out);}}

    private static void onOptionsBuilt(XC_MethodHook.MethodHookParam p){try{
        Object controller=p.thisObject, media=p.args[0], adder=p.args[1];
        if (adder == null) return;
        installNativeDownloadButtonGuard(adder);
        if(activityField==null)for(Field f:controller.getClass().getDeclaredFields())if(Activity.class.isAssignableFrom(f.getType())){f.setAccessible(true);activityField=f;break;}
        if(activityField==null)return;Activity activity=(Activity)activityField.get(controller);if(activity==null)return;
        if(buttonAdderMethod==null)for(Method m:adder.getClass().getDeclaredMethods()){Class<?>[] ps=m.getParameterTypes();if(ps.length==4&&Context.class.isAssignableFrom(ps[0])&&View.OnClickListener.class.isAssignableFrom(ps[1])&&ps[2]==String.class&&ps[3]==int.class){m.setAccessible(true);buttonAdderMethod=m;break;}}
        if(buttonAdderMethod==null)return;int icon=resolveDownloadIcon(activity);final Activity a=activity;final Object mc=media,cc=controller;
        try {
            ADDING_MODULE_BUTTON.set(Boolean.TRUE);
            buttonAdderMethod.invoke(adder,activity,(View.OnClickListener)v->showReelDownloadChooser(a,mc,cc),I18n.t(activity,R.string.ig_dl_title),icon);
        } finally {
            ADDING_MODULE_BUTTON.remove();
        }
    }catch(Throwable t){ModuleLog.line("(IE|Reel) ❌ onOptionsBuilt: "+t);}}

    private static void showReelDownloadChooser(Activity activity,Object media,Object controller){
        final String[] options={"Download Video","Download Image"};
        new AlertDialog.Builder(activity)
                .setTitle("Reel Download")
                .setItems(options,(dialog,which)->{
                    if(which==0) startReelVideoDownload(activity,media,controller);
                    else startReelImageDownload(activity,media,controller);
                })
                .show();
    }

    private static void startReelVideoDownload(Context ctx,Object media,Object controller){
        String user=FeedVideoDownloadHook.extractUsernameFromMediaObject(media);if(user==null)user="reel";String id="0";try{Object x=media.getClass().getMethod("getId").invoke(media);if(x instanceof String s&&!s.isEmpty())id=s;}catch(Throwable ignored){}
        String video=FeedVideoDownloadHook.bestVideoUrlFromMedia(media);
        if(video!=null){final String fn=FeedVideoDownloadHook.buildFilename(user,"reel",id,true);final String u=video;final String usr=user;Toast.makeText(ctx,I18n.t(ctx,R.string.ig_toast_downloading_reel),Toast.LENGTH_SHORT).show();FeedVideoDownloadHook.executor.submit(()->{try{boolean d=FeedVideoDownloadHook.downloadAndSave(ctx,u,fn,true,usr);if(!d)FeedVideoDownloadHook.mainHandler.post(()->Toast.makeText(ctx,I18n.t(ctx,R.string.ig_toast_reel_saved),Toast.LENGTH_SHORT).show());}catch(Throwable e){FeedVideoDownloadHook.mainHandler.post(()->Toast.makeText(ctx,I18n.t(ctx,R.string.ig_toast_reel_failed,e.getMessage()),Toast.LENGTH_SHORT).show());}});return;}
        List<String> urls=FeedVideoDownloadHook.extractAllUrlsFromMedia(ctx,media);if(urls.isEmpty()){Toast.makeText(ctx,I18n.t(ctx,R.string.ig_toast_reel_url_not_found),Toast.LENGTH_SHORT).show();return;}int vi=findCarouselIndexFromView(ctx,urls.size());int idx=vi>=0?vi:findReelCarouselIndex(controller);final String fu=user,fid=id;final int fi=idx;FeedVideoDownloadHook.mainHandler.post(()->FeedVideoDownloadHook.showPostDownloadDialog(ctx,urls,fu,fid,fi));
    }

    private static void startReelImageDownload(Context ctx,Object media,Object controller){try{String user=FeedVideoDownloadHook.extractUsernameFromMediaObject(media);if(user==null||user.isEmpty())user="reel";String id="0";try{Object x=media.getClass().getMethod("getId").invoke(media);if(x instanceof String s&&!s.isEmpty())id=s;}catch(Throwable ignored){}String image=FeedVideoDownloadHook.imageUrlFromMedia(ctx,media);if(image==null){List<String> urls=FeedVideoDownloadHook.extractAllUrlsFromMedia(ctx,media);for(String u:urls)if(isLikelyImageUrl(u)){image=u;break;}}if(image==null){Toast.makeText(ctx,"Reel image not available",Toast.LENGTH_SHORT).show();return;}final String url=image,fn=FeedVideoDownloadHook.buildFilename(user,"reel_image",id,false),usr=user;Toast.makeText(ctx,"Downloading reel image…",Toast.LENGTH_SHORT).show();FeedVideoDownloadHook.executor.submit(()->{try{boolean d=FeedVideoDownloadHook.downloadAndSave(ctx,url,fn,false,usr);if(!d)FeedVideoDownloadHook.mainHandler.post(()->Toast.makeText(ctx,"Reel image saved",Toast.LENGTH_SHORT).show());}catch(Throwable e){FeedVideoDownloadHook.mainHandler.post(()->Toast.makeText(ctx,"Reel image failed: "+e.getMessage(),Toast.LENGTH_SHORT).show());}});}catch(Throwable t){ModuleLog.line("(IE|Reel) Reel image download failed: "+t);}}
    private static boolean isLikelyImageUrl(String url){if(url==null||url.isEmpty())return false;String l=url.toLowerCase(java.util.Locale.ROOT);return l.contains("dst-jpg")||l.contains("dst-png")||l.contains("dst-webp")||l.endsWith(".jpg")||l.contains(".jpg?")||l.endsWith(".jpeg")||l.contains(".jpeg?")||l.endsWith(".png")||l.contains(".png?")||l.endsWith(".webp")||l.contains(".webp?");}
    private static int resolveDownloadIcon(Context ctx){try{Class<?> c=ctx.getClassLoader().loadClass("com.instagram.feed.media.mediaoption.MediaOption$Option");for(Object v:(Object[])c.getMethod("values").invoke(null))if(v.toString().contains("DOWNLOAD")){Field f=v.getClass().getField("iconDrawable");return(int)f.get(v);}}catch(Throwable ignored){}return 0;}
}