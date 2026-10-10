package mg.fjkm.fdl;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Reçoit l'alarme et affiche la notification de rappel. */
public class RappelReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("id", 0);
        String titre = intent.getStringExtra("titre");
        String texte = intent.getStringExtra("texte");
        if (titre == null) {
            titre = Langue.t(context, "Rappel FDL", "Fampahatsiahivana FDL");
        }
        if (texte == null) {
            texte = "";
        }

        Rappels.creerCanal(context);

        Intent ouvrir = new Intent(context, MainActivity.class);
        ouvrir.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int imm = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(context, id, ouvrir, PendingIntent.FLAG_UPDATE_CURRENT | imm);

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) {
            nb = new Notification.Builder(context, Rappels.CANAL);
        } else {
            nb = new Notification.Builder(context);
            nb.setPriority(Notification.PRIORITY_HIGH);
            nb.setDefaults(Notification.DEFAULT_ALL);
        }
        nb.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(titre)
                .setContentText(texte.split("\n")[0])
                .setStyle(new Notification.BigTextStyle().bigText(texte))
                .setContentIntent(pi)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, nb.build());
    }
}
