package `in`.bitlogger.ankidroidsync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "1"
    private var syncBtn: Button? = null
    private var errSyncBtn: Button? = null
    private var isSyncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        syncBtn = findViewById(R.id.start_sync)
        errSyncBtn = findViewById(R.id.sync_error)

        createNotificationChannel()
        doAction(intent.getStringExtra("SYNC_NOTIFICATION"))

        /**
        * It is used to build the sync notification.
        */
        val builder1 = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Anki Sync")
            .setContentText("Description")
            .setOngoing(true)
            .setContentIntent(bringActivityToFront())
            .setPriority(NotificationCompat.PRIORITY_MAX)

        syncBtn!!.setOnClickListener {
            if (!isSyncing) {
                isSyncing = true
                startNotification(builder1)
                syncBtn!!.text = "Stop Sync"
            }else {
                cancelSyncNotification()
                isSyncing = false
                syncBtn!!.text = "Start Sync"
            }
        }

        errSyncBtn!!.setOnClickListener {
            startNotification(builder1)
            val handler = Handler()
            var i = 0;
            val mStatusChecker: Runnable = object : Runnable {
                override fun run() {
                    if (i==3) {
                        return
                    }
                    if (i==1) {
                        cancelSyncNotification()
                        showErrNotification()
                        i++
                    }
                    i++
                    handler.postDelayed(this, 2000)
                }
            }
            mStatusChecker.run()
        }
    }

    /**
    * It is used to cancel sync notification.
    */
    private fun cancelSyncNotification() {
        NotificationManagerCompat.from(this).apply{
            this.cancel(12)
        }
    }

    /**
    * It is used to display the notification if their is error in sync.
    */
    private fun showErrNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Deck Sync Error!!")
            .setContentText("Error 403: Unable to connect with internet")
            .setContentIntent(bringActivityToFront())
            .setPriority(NotificationCompat.PRIORITY_MAX)

        NotificationManagerCompat.from(this).apply {
            notify(11, builder.build())
        }
    }

    /**
    * It is used to display the notification after sync is completed.
    */
    private fun showSyncCompleteNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sync Complete")
            .setContentText("Deck List updated.")
            .setContentIntent(bringActivityToFront())
            .setPriority(NotificationCompat.PRIORITY_MAX)

        NotificationManagerCompat.from(this).apply {
            notify(14, builder.build())
        }
    }

    /**
    * It is used to start the sync notification
    */
    private fun startNotification(builder: NotificationCompat.Builder) {
        NotificationManagerCompat.from(this).apply {
            builder.setOngoing(true)
            builder.setProgress(0, 0, true)
            notify(12, builder.build())


            // Do the job here that tracks the progress.
            // Usually, this should be in a
            // worker thread
            // To show progress, update PROGRESS_CURRENT and update the notification with:
            // builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
            // notificationManager.notify(notificationId, builder.build());

            // When done, update the notification one more time to remove the progress bar
            val index = arrayOf("Connecting...", "Downloading...", "Uploading...", "Writing Changes...", "Finalizing...")
            val handler = Handler()
            var i = 0;
            val mStatusChecker: Runnable = object : Runnable {
                override fun run() {
                    if (i==5) {
                        this@apply.cancel(12)
                        isSyncing = false
                        syncBtn!!.text = "Start Sync"
                        showSyncCompleteNotification()
                        return
                    }
                    Log.e("ABC", " " + isSyncing)
                    if (isSyncing) {
                        builder.setContentText(index[i])
                        val notification = builder.build()
                        notification.flags = Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_ONLY_ALERT_ONCE
                        notify(12, notification)
                        i++
                        handler.postDelayed(this, 2000)
                    }
                }
            }
            mStatusChecker.run()
        }
    }

    private fun doAction(action: String?) {
        if (action == "ACTION_CANCEL") {
            Toast.makeText(this, "Notification canceled", Toast.LENGTH_SHORT).show()
        }
    }

    /**
    * TODO: Not yet implemented. 
    * REASON: unable to find the correct way to use the function present activity without creating the new activity using pending Intent
    * It is used to cancel the sync from notification. with the help of action button in notification.
    */
    private fun cancelPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(this, 0, intent.apply {
            putExtra("SYNC_NOTIFICATION", "ACTION_CANCEL")
        }, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
    *  This peice of code is used to create the notification channel. 
    */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
    *  This is used to bring the current running activity to front. 
    *  It is used when user clicks on the notification and app is in background.
    */
    private fun bringActivityToFront(): PendingIntent {
        val notificationIntent: Intent = this.packageManager.getLaunchIntentForPackage(this.packageName)!!
        notificationIntent.setPackage(null)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        return PendingIntent.getActivity(this, 0, notificationIntent, 0)
    }
}