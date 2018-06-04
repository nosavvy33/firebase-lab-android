package pe.mistreating.undying.firebaseapp2.classes;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import pe.mistreating.undying.firebaseapp2.MainActivity;
import pe.mistreating.undying.firebaseapp2.R;

/**
 * Created by Alumno on 29/05/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

    public static int NOTIFICATION_ID = 0;

    /**
     * Notification Only Foreground
     * Data Foreground/Background
     * @param remoteMessage
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e(TAG, "From: " + remoteMessage.getFrom());
        try {
            // Check if message contains a notification payload.
            if (remoteMessage.getNotification() != null) {

                Log.d(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());

                String title = remoteMessage.getNotification().getTitle();
                String body = remoteMessage.getNotification().getBody();
                String icon = remoteMessage.getNotification().getIcon();
                String color = remoteMessage.getNotification().getColor();
                String sound = remoteMessage.getNotification().getSound();

                // Notification Builder
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");

                // Icon
                int iconId = (icon == null)? R.mipmap.ic_launcher_round:this.getResources().getIdentifier(icon, "drawable", this.getPackageName());

                // Color
                int colorId = (color == null)? ContextCompat.getColor(this, R.color.colorPrimary): Color.argb(
                        255,
                        Integer.valueOf( color.substring( 1, 3 ), 16 ),
                        Integer.valueOf( color.substring( 3, 5 ), 16 ),
                        Integer.valueOf( color.substring( 5, 7 ), 16 )
                );

                // Sound
                Uri soundUri = (sound == null || "default".equals(sound))? RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION): Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + this.getPackageName() + "/raw/" + sound);

                // Intent
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                // Check if message contains a data payload.
                if (remoteMessage.getData().size() > 0) {
                    Log.d(TAG, "Data Payload: " + remoteMessage.getData().toString());

                    JSONObject data = new JSONObject(remoteMessage.getData().toString());
                    intent.putExtra("ACTION", data.getString("ACTION"));    // Additional data
                    // intent.putExtra("other", json.getString("other"));   // Other additional data
                }

                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

                // Notification
                Notification notification = builder
                        .setContentTitle(title)
                        .setContentText(body)
                        .setSmallIcon(iconId)
                        .setColor(colorId)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();

                // Notification manager
                NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID++, notification);

                // Play sound
                RingtoneManager.getRingtone(this, soundUri).play();

            }else if (remoteMessage.getData().size() > 0) {
                // Check if message contains ONLY data payload (Foreground or Background).
                Log.d(TAG, "Data Payload: " + remoteMessage.getData().toString());

//                JSONObject data = new JSONObject(remoteMessage.getData().toString());
//                String param = data.getString("param");
                // TODO: ...

            }

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

}
