package org.tigase.messenger.interfaces;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hyperether.toolbox.HyperLog;

import java.util.HashMap;

import static android.content.Context.MODE_PRIVATE;

/**
 * Shared prefs overlay class
 *
 * @author Slobodan Prijic
 */
public class SharedPrefs {
    private static final String TAG = SharedPrefs.class.getSimpleName();
    private static final String USER_PREF = "userSettings";
    private static final String PREFS_KEY_CURRENT_USER = "currentUser";

    public static void saveRooms(String jid, String name, Context context) {
        HashMap<String, String> map = getRooms(context);
        map.put(jid, name);
        String hashMapString = new Gson().toJson(map);
        HashMap<String, String> shareMap = new HashMap<>();
        shareMap.put("roomsPref", hashMapString);
        savePrefString(shareMap, USER_PREF, context);
    }

    public static HashMap<String, String> getRooms(Context context) {
        String hashMapString = getPrefString("roomsPref", USER_PREF, context);
        java.lang.reflect.Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        HashMap<String, String> map = new Gson().fromJson(hashMapString, type);
        if (map == null)
            map = new HashMap<>();
        return map;
    }

    /**
     * save String
     *
     * @param map map
     * @param prefName Shared preference name
     */
    public static void savePrefString(HashMap<String, String> map, String prefName,
                                      Context context) {
        try {
            SharedPreferences prefs = getUserSharedPrefs(prefName, context);
            SharedPreferences.Editor editor = prefs.edit();
            for (String key : map.keySet()) {
                editor.putString(key, map.get(key));
            }
            editor.apply();
        } catch (Exception e) {
            HyperLog.getInstance().e(TAG, "savePrefString", e);
        }
    }

    /**
     * get String
     *
     * @param key map
     * @param prefName Shared preference name
     */
    public static String getPrefString(String key, String prefName, Context context) {
        try {
            SharedPreferences prefs = getUserSharedPrefs(prefName, context);
            return prefs.getString(key, "");
        } catch (Exception e) {
            HyperLog.getInstance().e(TAG, "getPrefString", e);
        }
        return "";
    }

    /**
     * getSharedPrefs for User
     *
     * @param prefName prefName
     *
     * @return shared preference
     */
    private static SharedPreferences getUserSharedPrefs(String prefName, Context context) {
        String username = "";
        try {
            username = getCurrentUser(context);
        } catch (Exception e) {
            HyperLog.getInstance().e(TAG, "getUserSharedPrefs", e);
        }
        return context.getSharedPreferences(username + prefName, MODE_PRIVATE);
    }


    /**
     * getCurrentUser
     *
     * @return user
     */
    private static String getCurrentUser(Context context) {
        try {
            SharedPreferences prefs = context
                    .getSharedPreferences(PREFS_KEY_CURRENT_USER, Context.MODE_PRIVATE);
            return prefs.getString(PREFS_KEY_CURRENT_USER, "");
        } catch (Exception e) {
            HyperLog.getInstance().e(TAG, "getCurrentUser", e);
        }
        return "";
    }

}
