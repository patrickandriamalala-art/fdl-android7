package mg.fjkm.fdl;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Enregistre le programme chargé par l'utilisateur (fichier interne à l'application). */
final class Stockage {

    private static final String FICHIER = "programme.json";

    private Stockage() {
    }

    /** Programme enregistré, ou "" si aucun (la page affiche alors « Aucun programme »). */
    static String lire(Context c) {
        File f = new File(c.getFilesDir(), FICHIER);
        if (!f.exists()) {
            return "";
        }
        try (InputStream in = new FileInputStream(f)) {
            return new String(lireTout(in, 8 * 1024 * 1024), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    static boolean ecrire(Context c, String json) {
        try (OutputStream out = new FileOutputStream(new File(c.getFilesDir(), FICHIER))) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static byte[] lireTout(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        int total = 0;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > max) {
                throw new IOException("fichier trop gros");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** Décode le fichier reçu : UTF-8 (avec ou sans BOM) ou UTF-16 (enregistrement « Unicode » du Bloc-notes). */
    static String decoder(byte[] b) {
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
            return new String(b, 2, b.length - 2, StandardCharsets.UTF_16LE);
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
            return new String(b, 2, b.length - 2, StandardCharsets.UTF_16BE);
        }
        return new String(b, StandardCharsets.UTF_8);
    }

    /** Période affichée dans le message de confirmation ("" si absente). */
    static String periode(String json) {
        try {
            return new JSONObject(json).optString("periode", "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Accepte un fichier .json ou la page .html exportée par l'application FDL :
     * retourne le JSON du programme, ou null si le fichier n'est pas reconnu.
     */
    static String extraire(String texte) {
        if (texte == null) {
            return null;
        }
        String t = texte.trim();
        if (t.startsWith("\uFEFF")) {
            t = t.substring(1).trim();
        }
        String json;
        if (t.startsWith("{")) {
            json = t;
        } else {
            int a = t.indexOf("id=\"donnees\"");
            if (a < 0) {
                return null;
            }
            a = t.indexOf('>', a);
            if (a < 0) {
                return null;
            }
            int e = t.indexOf("</script>", a);
            if (e < 0) {
                return null;
            }
            json = t.substring(a + 1, e).trim();
        }
        try {
            JSONObject o = new JSONObject(json);
            return (o.has("dimanches") && o.has("membres")) ? json : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
