package tw.nekomimi.nekogram.helpers.remote;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.BuildConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tw.nekomimi.nekogram.NekoXConfig;

public class UpdateHelper extends BaseRemoteHelper {
    private static final class InstanceHolder {
        private static final UpdateHelper instance = new UpdateHelper();
    }

    public static UpdateHelper getInstance() {
        return InstanceHolder.instance;
    }

    private boolean updateAlways = false;

    @Override
    protected void onError(String text, Delegate delegate) {
        delegate.onTLResponse(null, text);
    }

    @Override
    protected String getTag() {
        return NekoXConfig.autoUpdateReleaseChannel >= 2 ? "updatetest" : "updatev2";
    }

    @SuppressWarnings("ConstantConditions")
    private int getPreferredAbiFile(Map<String, Integer> files) {
        for (String abi : Build.SUPPORTED_ABIS) {
            if (files.containsKey(abi)) {
                return files.get(abi);
            }
        }
        return files.get("arm64-v8a");
    }

    private Map<String, Integer> jsonToMap(JSONObject obj) {
        Map<String, Integer> map = new HashMap<>();
        List<String> abis = new ArrayList<>();
        abis.add("armeabi-v7a");
        abis.add("arm64-v8a");

        try {
            for(var abi: abis) {
                map.put(abi, obj.getInt(abi));
            }
        } catch (JSONException ignored) {}
        return map;
    }

    private Update getShouldUpdateVersion(List<JSONObject> responses) {
        long currentVersion = BuildConfig.VERSION_CODE;
        long currentTimestamp = BuildConfig.BUILD_TIMESTAMP;
        Update ref = null;
        for (var string : responses) {
            try {
                int versionCode = string.getInt("version_code");
                long timestamp = string.getLong("timestamp");
                if (versionCode > currentVersion
                        || (versionCode == currentVersion && timestamp > currentTimestamp)
                        || updateAlways) {
                    if (updateAlways) {
                        updateAlways = false;
                    }
                    ref = new Update(
                            string.getBoolean("can_not_skip"),
                            string.getString("version"),
                            versionCode,
                            timestamp,
                            string.getInt("sticker"),
                            string.getInt("message"),
                            jsonToMap(string.getJSONObject("gcm")),
                            string.getString("url")
                    );
                    break;
                }
            } catch (JSONException ignored) {}
        }
        return ref;
    }

    private void getNewVersionMessagesCallback(Delegate delegate, Update json,
                                               HashMap<String, Integer> ids, TLObject response) {
        var update = new TLRPC.TL_help_appUpdate();
        update.version = json.version;
        update.can_not_skip = json.canNotSkip;
        if (json.url != null) {
            if (json.url.contains("github.com") && !json.url.contains("Xlufoi/NyashkaGram")) {
                update.url = org.telegram.messenger.BuildVars.GITHUB_RELEASE_URL;
            } else {
                update.url = json.url;
            }
            update.flags |= 4;
        } else {
            update.url = org.telegram.messenger.BuildVars.GITHUB_RELEASE_URL;
            update.flags |= 4;
        }
        if (NekoXConfig.autoUpdateReleaseChannel == 0 && !update.can_not_skip) {
            delegate.onTLResponse(null, null);
            return;
        }
        if (response != null) {
            var res = (TLRPC.messages_Messages) response;
            getMessagesController().removeDeletedMessagesFromArray(CHANNEL_METADATA_ID, res.messages);
            var messages = new HashMap<Integer, TLRPC.Message>();
            for (var message : res.messages) {
                messages.put(message.id, message);
            }

            if (ids.containsKey("file")) {
                var file = messages.get(ids.get("file"));
                if (file != null && file.media != null) {
                    update.document = file.media.document;
                    update.flags |= 2;
                }
            }
            if (ids.containsKey("message")) {
                var message = messages.get(ids.get("message"));
                if (message != null) {
                    update.text = message.message;
                    update.entities = message.entities;
                }
            }
            if (ids.containsKey("sticker")) {
                var sticker = messages.get(ids.get("sticker"));
                if (sticker != null && sticker.media != null) {
                    update.sticker = sticker.media.document;
                    update.flags |= 8;
                }
            }
        }
        delegate.onTLResponse(update, null);
    }

    @Override
    protected void onLoadSuccess(ArrayList<JSONObject> responses, Delegate delegate) {
        var update = getShouldUpdateVersion(responses);
        if (update == null) {
            delegate.onTLResponse(null, null);
            return;
        }

        var ids = new HashMap<String, Integer>();
        if (update.message != null) {
            ids.put("message", update.message);
        }
        if (update.sticker != null) {
            ids.put("sticker", update.sticker);
        }
        if (update.gcm != null) {
            ids.put("file", getPreferredAbiFile(update.gcm));
        }

        if (ids.isEmpty()) {
            getNewVersionMessagesCallback(delegate, update, null, null);
        } else {
            var req = new TLRPC.TL_channels_getMessages();
            req.channel = getMessagesController().getInputChannel(CHANNEL_METADATA_ID);
            req.id = new ArrayList<>(ids.values());
            getConnectionsManager().sendRequest(req, (response1, error1) -> {
                if (error1 == null) {
                    getNewVersionMessagesCallback(delegate, update, ids, response1);
                } else {
                    delegate.onTLResponse(null, error1.text);
                }
            });
        }
    }

    private boolean isVersionNewer(String remoteVer, String currentVer) {
        if (remoteVer == null || currentVer == null) return false;
        remoteVer = remoteVer.replaceAll("^[vV]", "").trim();
        currentVer = currentVer.replaceAll("^[vV]", "").trim();
        String[] rParts = remoteVer.split("[.\\-_]");
        String[] cParts = currentVer.split("[.\\-_]");
        int len = Math.max(rParts.length, cParts.length);
        for (int i = 0; i < len; i++) {
            int r = 0, c = 0;
            if (i < rParts.length) {
                try { r = Integer.parseInt(rParts[i]); } catch (Exception ignored) {}
            }
            if (i < cParts.length) {
                try { c = Integer.parseInt(cParts[i]); } catch (Exception ignored) {}
            }
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }

    public void checkNewVersionAvailable(Delegate delegate) {
        checkNewVersionAvailable(delegate, false);
    }

    public void checkNewVersionAvailable(Delegate delegate, boolean updateAlways_) {
        updateAlways = updateAlways_;
        org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
            java.net.HttpURLConnection conn = null;
            try {
                java.net.URL url = new java.net.URL("https://api.github.com/repos/Xlufoi/NyashkaGram/releases/latest");
                conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "NyashkaGram-Android");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                int code = conn.getResponseCode();
                if (code == java.net.HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    JSONObject obj = new JSONObject(sb.toString());
                    String tagName = obj.optString("tag_name", "");
                    String body = obj.optString("body", "");
                    String htmlUrl = obj.optString("html_url", "https://github.com/Xlufoi/NyashkaGram/releases");
                    String downloadUrl = null;
                    org.json.JSONArray assets = obj.optJSONArray("assets");
                    if (assets != null) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.getJSONObject(i);
                            String assetName = asset.optString("name", "");
                            if (assetName.endsWith(".apk")) {
                                if (downloadUrl == null || assetName.contains("arm64") || assetName.contains("universal")) {
                                    downloadUrl = asset.optString("browser_download_url", htmlUrl);
                                }
                            }
                        }
                    }
                    if (downloadUrl == null) {
                        downloadUrl = htmlUrl;
                    }

                    boolean isNewer = isVersionNewer(tagName, BuildConfig.VERSION_NAME);
                    if (isNewer || updateAlways_) {
                        TLRPC.TL_help_appUpdate update = new TLRPC.TL_help_appUpdate();
                        update.version = tagName;
                        update.text = body;
                        update.url = downloadUrl;
                        update.flags |= 4;
                        org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> delegate.onTLResponse(update, null));
                        return;
                    } else {
                        org.telegram.messenger.AndroidUtilities.runOnUIThread(() -> delegate.onTLResponse(null, null));
                        return;
                    }
                }
            } catch (Throwable e) {
                org.telegram.messenger.FileLog.e(e);
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Throwable ignored) {}
                }
            }
            load(delegate);
        });
    }

    public static class Update {
        public Boolean canNotSkip;
        public String version;
        public Integer versionCode;
        public Long timeStamp;
        public Integer sticker;
        public Integer message;
        public Map<String, Integer> gcm;
        public String url;

        public Update(Boolean canNotSkip, String version, int versionCode, long timeStamp, int sticker, int message, Map<String, Integer> gcm, String url) {
            this.canNotSkip = canNotSkip;
            this.version = version;
            this.versionCode = versionCode;
            this.timeStamp = timeStamp;
            this.sticker = sticker;
            this.message = message;
            this.gcm = gcm;
            this.url = url;
        }
    }
}
