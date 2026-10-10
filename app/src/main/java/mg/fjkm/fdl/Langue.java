package mg.fjkm.fdl;

import android.content.Context;

/** Langue des messages natifs (la page indique la langue du programme chargé : "fr" ou "mg"). */
final class Langue {

    private static final String PREFS = "fdl";
    private static final String CLE = "langue";

    private Langue() {
    }

    static void definir(Context c, String langue) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(CLE, "mg".equals(langue) ? "mg" : "fr").apply();
    }

    static boolean malagasy(Context c) {
        return "mg".equals(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLE, "fr"));
    }

    /** Retourne le texte français ou malagasy selon la langue du programme. */
    static String t(Context c, String fr, String mg) {
        return malagasy(c) ? mg : fr;
    }
}
