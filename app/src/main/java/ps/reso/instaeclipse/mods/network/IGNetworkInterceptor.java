package ps.reso.instaeclipse.mods.network;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ps.reso.instaeclipse.mods.misc.FollowStatusHook;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.feature.FeatureStatusTracker;
import ps.reso.instaeclipse.utils.log.ModuleLog;

public class IGNetworkInterceptor {

    public void handleInterceptor(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            ClassLoader classLoader = lpparam.classLoader;

            Class<?> tigonClass = classLoader.loadClass("com.instagram.api.tigon.TigonServiceLayer");
            Method[] methods = tigonClass.getDeclaredMethods();

            Class<?> random_param_1 = null;
            Class<?> random_param_2 = null;
            Class<?> random_param_3 = null;
            String uriFieldName = null;

            for (Method method : methods) {
                if (method.getName().equals("startRequest") && method.getParameterCount() == 3) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    random_param_1 = paramTypes[0];
                    random_param_2 = paramTypes[1];
                    random_param_3 = paramTypes[2];
                    break;
                }
            }

            if (random_param_1 != null) {
                for (Field field : random_param_1.getDeclaredFields()) {
                    if (field.getType().equals(URI.class)) {
                        uriFieldName = field.getName();
                        break;
                    }
                }
            }

            if (random_param_1 != null && random_param_2 != null && random_param_3 != null && uriFieldName != null) {
                String finalUriFieldName = uriFieldName;
                XposedHelpers.findAndHookMethod("com.instagram.api.tigon.TigonServiceLayer", classLoader, "startRequest",
                        random_param_1, random_param_2, random_param_3, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                Object requestObj = param.args[0];
                                URI uri;
                                try {
                                    uri = (URI) XposedHelpers.getObjectField(requestObj, finalUriFieldName);
                                } catch (Throwable t) {
                                    ModuleLog.line("(InstaEclipse | Interceptor): request URI read failed: " + t.getMessage());
                                    return;
                                }

                                if (uri != null && uri.getPath() != null) {
                                    String path = uri.getPath();
                                    boolean shouldDrop = false;

                                    if (FeatureFlags.isGhostSeen) {
                                        shouldDrop |= path.contains("/threads/") && path.contains("/opened");
                                    }
                                    if (FeatureFlags.keepEphemeralMessages) {
                                        shouldDrop |= path.contains("/mark_ephemeral_item_ranges_viewed");
                                    }
                                    if (FeatureFlags.isGhostScreenshot) {
                                        shouldDrop |= path.endsWith("/screenshot/") || path.endsWith("/ephemeral_screenshot/");
                                    }
                                    if (FeatureFlags.isGhostViewOnce) {
                                        shouldDrop |= path.endsWith("/item_replayed/");
                                        shouldDrop |= (path.contains("/direct") && path.endsWith("/item_seen/"));
                                    }
                                    if (FeatureFlags.isGhostStory) {
                                        shouldDrop |= path.contains("/api/v2/media/seen/");
                                        FeatureStatusTracker.setHooked("GhostStories");
                                    }
                                    if (FeatureFlags.isGhostLive) {
                                        shouldDrop |= path.contains("/heartbeat_and_get_viewer_count/");
                                        FeatureStatusTracker.setHooked("GhostLive");
                                    }

                                    if (FeatureFlags.disableStories) {
                                        shouldDrop |= path.contains("/feed/reels_tray/")
                                                || path.contains("feed/get_latest_reel_media/")
                                                || path.contains("direct_v2/pending_inbox/?visual_message")
                                                || path.contains("stories/hallpass/")
                                                || path.contains("/api/v1/feed/reels_media_stream/");
                                    }
                                    if (FeatureFlags.disableFeed) {
                                        shouldDrop |= path.endsWith("/feed/timeline/");
                                    }
                                    if (FeatureFlags.disableReels && !FeatureFlags.disableReelsExceptDM) {
                                        shouldDrop |= path.endsWith("/qp/batch_fetch/")
                                                || path.contains("api/v1/clips")
                                                || path.contains("clips")
                                                || path.contains("mixed_media")
                                                || path.contains("mixed_media/discover/stream/");
                                    }
                                    if (FeatureFlags.disableReelsExceptDM) {
                                        if (path.startsWith("/api/v1/direct_v2/")) {
                                            return;
                                        }
                                        shouldDrop |= (path.startsWith("/api/v1/clips/") && uri.getQuery() != null
                                                && (uri.getQuery().contains("next_media_ids=")
                                                || uri.getQuery().contains("max_id=")))
                                                || path.contains("/clips/discover/")
                                                || path.contains("/mixed_media/discover/stream/");
                                    }
                                    if (FeatureFlags.disableExplore) {
                                        shouldDrop |= path.contains("/discover/topical_explore")
                                                || path.contains("/discover/topical_explore_stream")
                                                || (uri.getHost() != null && uri.getHost().contains("i.instagram.com") && path.contains("/api/v1/fbsearch/top_serp/"));
                                    }
                                    if (FeatureFlags.disableComments) {
                                        shouldDrop |= path.contains("/api/v1/media/") && path.contains("comments/");
                                    }

                                    if (FeatureFlags.isAdBlockEnabled) {
                                        shouldDrop |= path.contains("profile_ads/get_profile_ads/")
                                                || path.contains("/async_ads/")
                                                || path.contains("/feed/injected_reels_media/")
                                                || path.equals("/api/v1/ads/graphql/");
                                    }

                                    if (FeatureFlags.isAnalyticsBlocked) {
                                        shouldDrop |= (uri.getHost() != null && (uri.getHost().contains("graph.instagram.com")
                                                || uri.getHost().contains("graph.facebook.com")))
                                                || path.contains("/logging_client_events");
                                    }

                                    if (FeatureFlags.spoofLastSeen) {
                                        shouldDrop |= path.contains("/push/setForegroundState/")
                                                || path.contains("/accounts/update_active_status")
                                                || path.contains("/notes/create_note")
                                                || path.contains("/accounts/set_presence_disabled")
                                                || path.contains("/update_active_status")
                                                || path.contains("/banyan/banyan/")
                                                || path.endsWith("/last_active/")
                                                || path.contains("/presence/");
                                        FeatureStatusTracker.setHooked("SpoofLastSeen");
                                    }

                                    // Instagram's repost implementation has moved between
                                    // several request routes. The old /media/create_note/ check
                                    // is unrelated to reposting. Match the actual repost route
                                    // family without relying on an obfuscated method name.
                                    if (FeatureFlags.disableRepost) {
                                        String normalized = path.toLowerCase(java.util.Locale.ROOT);
                                        shouldDrop |= normalized.contains("/repost")
                                                || normalized.contains("/reposts/")
                                                || normalized.contains("/media/repost/")
                                                || normalized.contains("/media/reposts/")
                                                || normalized.contains("/create_repost/");
                                        FeatureStatusTracker.setHooked("DisableRepost");
                                    }

                                    if (FeatureFlags.disableDiscoverPeople) {
                                        shouldDrop |= path.contains("/discover/ayml/");
                                        shouldDrop |= path.contains("discover/chaining/");
                                        FeatureStatusTracker.setHooked("DisableDiscoverPeople");
                                    }

                                    if (shouldDrop) {
                                        try {
                                            URI fakeUri = new URI("https", "127.0.0.1", "/404", null);
                                            XposedHelpers.setObjectField(requestObj, finalUriFieldName, fakeUri);
                                        } catch (Throwable ignored) {}
                                    }

                                    if (FeatureFlags.showFollowerToast) {
                                        FeatureStatusTracker.setHooked("FollowerToast");
                                        FollowStatusHook.handleRequest(uri, param.args);
                                    }
                                }
                            }
                        }
                );
            } else {
                ModuleLog.line("(InstaEclipse | Interceptor): Could not resolve required classes or fields.");
            }

        } catch (Throwable e) {
            ModuleLog.line("(InstaEclipse | Interceptor): ❌ " + e.getMessage());
        }
    }
}
