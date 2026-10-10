package mg.fjkm.fdl;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Programme et annule les rappels (notifications) de participation. */
final class Rappels {

    static final String CANAL = "rappels";
    static final String ACTION = "mg.fjkm.fdl.RAPPEL";
    private static final String PREFS = "fdl";
    private static final String CLE = "rappels";

    private Rappels() {
    }

    static void creerCanal(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(CANAL,
                    Langue.t(c, "Rappels de participation", "Fampahatsiahivana anjara"),
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription(Langue.t(c, "Rappel la veille et le jour de votre participation",
                    "Fampahatsiahivana ny andro aloha sy ny andro hanaovanao anjara"));
            nm.createNotificationChannel(ch);
        }
    }

    private static int immuable() {
        return Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
    }

    private static PendingIntent intention(Context c, int id, int drapeaux) {
        Intent i = new Intent(c, RappelReceiver.class);
        i.setAction(ACTION);
        return PendingIntent.getBroadcast(c, id, i, drapeaux | immuable());
    }

    /** json = [{"id":..., "at":millisecondes, "titre":"...", "texte":"..."}, ...] fourni par la page. */
    static void programmer(Context c, String json) {
        annuler(c);
        JSONArray garde = new JSONArray();
        long maintenant = System.currentTimeMillis();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.getLong("at") > maintenant) {
                    planifier(c, o);
                    garde.put(o);
                }
            }
        } catch (Exception e) {
            // liste illisible : on ne programme rien
        }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CLE, garde.toString()).apply();
    }

    private static void planifier(Context c, JSONObject o) throws JSONException {
        int id = o.getInt("id");
        long at = o.getLong("at");
        Intent i = new Intent(c, RappelReceiver.class);
        i.setAction(ACTION);
        i.putExtra("id", id);
        i.putExtra("titre", o.optString("titre", Langue.t(c, "Rappel FDL", "Fampahatsiahivana FDL")));
        i.putExtra("texte", o.optString("texte", ""));
        PendingIntent pi = PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | immuable());
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);   // pas d'autorisation spéciale ; peut avoir quelques minutes de retard
        } else {
            am.set(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    private static JSONArray stockes(Context c) {
        try {
            return new JSONArray(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLE, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    static void annuler(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        JSONArray arr = stockes(c);
        for (int i = 0; i < arr.length(); i++) {
            int id = arr.optJSONObject(i) == null ? 0 : arr.optJSONObject(i).optInt("id", 0);
            PendingIntent pi = intention(c, id, PendingIntent.FLAG_NO_CREATE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }

    static int compter(Context c) {
        JSONArray arr = stockes(c);
        long maintenant = System.currentTimeMillis();
        int n = 0;
        for (int i = 0; i < arr.length(); i++) {
            if (arr.optJSONObject(i) != null && arr.optJSONObject(i).optLong("at", 0) > maintenant) {
                n++;
            }
        }
        return n;
    }

    /** Après un redémarrage du téléphone, Android oublie les alarmes : on les reprogramme. */
    static void reprogrammer(Context c) {
        JSONArray arr = stockes(c);
        long maintenant = System.currentTimeMillis();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            try {
                if (o != null && o.getLong("at") > maintenant) {
                    planifier(c, o);
                }
            } catch (JSONException e) {
                // rappel illisible : ignoré
            }
        }
    }
}
