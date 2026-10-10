package mg.fjkm.fdl;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Télécharge le fichier programme.json publié en ligne. */
final class Reseau {

    private Reseau() {
    }

    /** Contenu du fichier, ou "" en cas d'échec (pas de connexion, adresse invalide...). */
    static String lire(String adresse) {
        if (adresse == null || !adresse.startsWith("https://")) {
            return "";
        }
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(adresse).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            c.setUseCaches(false);
            c.setRequestProperty("Cache-Control", "no-cache");
            if (c.getResponseCode() != 200) {
                return "";
            }
            try (InputStream in = c.getInputStream()) {
                return new String(Stockage.lireTout(in, 8 * 1024 * 1024), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }
}
