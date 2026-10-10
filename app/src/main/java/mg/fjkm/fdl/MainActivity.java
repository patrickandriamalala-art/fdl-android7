package mg.fjkm.fdl;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.InputStream;

/**
 * Affiche la page « Mon planning » (assets/index.html) dans une WebView.
 * Le programme est en lecture seule ; l'application ajoute :
 *  - des notifications natives (rappels la veille et le jour de la participation) ;
 *  - le chargement d'un nouveau programme depuis un fichier reçu (FDL_Mon_Planning.html).
 */
public class MainActivity extends Activity {

    private static final String PAGE = "file:///android_asset/index.html";
    private static final int REQ_IMPORT = 41;
    private static final int REQ_NOTIF = 42;

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Rappels.creerCanal(this);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // on reste dans la page ; aucun site externe
                return url == null || !url.startsWith("file:");
            }
        });
        web.addJavascriptInterface(new Pont(), "AndroidFDL");
        web.loadUrl(PAGE);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new Exception("lecture impossible");
            }
            String texte = Stockage.decoder(Stockage.lireTout(in, 8 * 1024 * 1024));
            String json = Stockage.extraire(texte);
            if (json == null) {
                Toast.makeText(this, Langue.t(this,
                        "Aucun programme trouvé dans ce fichier. Choisissez le fichier FDL_Mon_Planning.html "
                                + "(ou programme.json) créé par l'application FDL.",
                        "Tsy nahita fandaharana tao amin'ity rakitra ity. Safidio ny rakitra FDL_Mon_Planning.html "
                                + "(na programme.json) noforonin'ny rindranasa FDL."), Toast.LENGTH_LONG).show();
                return;
            }
            if (!Stockage.ecrire(this, json)) {
                Toast.makeText(this, Langue.t(this, "Impossible d'enregistrer le programme",
                        "Tsy afaka nitahiry ny fandaharana"), Toast.LENGTH_LONG).show();
                return;
            }
            getSharedPreferences("fdl", MODE_PRIVATE).edit()
                    .putString("import_ms", String.valueOf(System.currentTimeMillis())).apply();
            String periode = Stockage.periode(json);
            Toast.makeText(this, Langue.t(this, "Programme chargé", "Voaray ny fandaharana")
                    + (periode.isEmpty() ? "" : " : " + periode), Toast.LENGTH_LONG).show();
            web.loadUrl(PAGE);   // la page relit le programme et reprogramme les rappels
        } catch (Exception e) {
            Toast.makeText(this, Langue.t(this, "Lecture du fichier impossible", "Tsy afaka namaky ny rakitra"),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Fonctions appelables depuis la page : window.AndroidFDL.xxx() */
    private class Pont {

        @JavascriptInterface
        public String donnees() {
            return Stockage.lire(MainActivity.this);
        }

        @JavascriptInterface
        public void programmerRappels(String json) {
            Rappels.programmer(getApplicationContext(), json);
        }

        /** Enregistre un programme reçu par internet (la page n'a pas besoin d'être rechargée). */
        @JavascriptInterface
        public void enregistrerProgramme(String json) {
            String ok = Stockage.extraire(json);
            if (ok != null) {
                Stockage.ecrire(MainActivity.this, ok);
                // programme reçu par internet : il remplace le fichier chargé à la main
                getSharedPreferences("fdl", MODE_PRIVATE).edit().remove("import_ms").apply();
            }
        }

        /** Instant du dernier chargement manuel d'un fichier ("" s'il n'y en a pas, ou si une mise à jour en ligne l'a remplacé). */
        @JavascriptInterface
        public String importManuel() {
            return getSharedPreferences("fdl", MODE_PRIVATE).getString("import_ms", "");
        }

        /** Télécharge une adresse https en arrière-plan puis rappelle window.__retourReseau(jeton, contenu). */
        @JavascriptInterface
        public void telecharger(final String url, final int jeton) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String corps = Reseau.lire(url);
                    final String js = "window.__retourReseau(" + jeton + "," + JSONObject.quote(corps) + ")";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            web.evaluateJavascript(js, null);
                        }
                    });
                }
            }).start();
        }

        /** La page indique la langue du programme ("fr" ou "mg") pour les messages natifs. */
        @JavascriptInterface
        public void definirLangue(String langue) {
            Langue.definir(getApplicationContext(), langue);
            Rappels.creerCanal(getApplicationContext());   // met à jour le nom du canal de notifications
        }

        @JavascriptInterface
        public int nbRappels() {
            return Rappels.compter(getApplicationContext());
        }

        @JavascriptInterface
        public void importerProgramme() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.setType("*/*");
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(i, Langue.t(MainActivity.this, "Choisir le fichier du programme",
                            "Fidio ny rakitra fandaharana")), REQ_IMPORT);
                }
            });
        }
    }
}
