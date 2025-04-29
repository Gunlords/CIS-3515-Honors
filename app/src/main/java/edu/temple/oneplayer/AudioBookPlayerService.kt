package edu.temple.oneplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class AudioBookPlayerService : Service() {

    private val binder = AudioBookPlayerBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var progressCallback: ((Int, Int) -> Unit)? = null

    private val CHANNEL_ID = "AudioBookPlayerServiceChannel"
    private val NOTIFICATION_ID = 1

    inner class AudioBookPlayerBinder : Binder() {
        fun getService(): AudioBookPlayerService = this@AudioBookPlayerService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun play(book: Book, startPosition: Int = 0) {
        mediaPlayer?.release()

        val url = "https://kamorris.com/lab/audlibplayer/${book.book_id}.mp3"
        Log.d("AudioBookPlayerService", "Audio URL: $url")

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(url)

            setOnPreparedListener {
                seekTo(startPosition)
                start()
                startProgressUpdates()
                startForegroundService()
            }

            setOnCompletionListener {
                progressCallback?.invoke(0, duration)
                stopForegroundService()
            }

            setOnErrorListener { _, what, extra ->
                Log.e("AudioBookPlayerService", "MediaPlayer error: what=$what, extra=$extra")
                stopForegroundService()
                true
            }

            prepareAsync()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        stopForegroundService()
    }

    fun seekTo(progress: Int) {
        mediaPlayer?.seekTo(progress)
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        progressCallback?.invoke(0, 1)
        stopForegroundService()
    }

    fun setProgressCallback(callback: (current: Int, total: Int) -> Unit) {
        this.progressCallback = callback
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForegroundService()
    }

    private fun startProgressUpdates() {
        Thread {
            while (mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                val total = mediaPlayer?.duration ?: 1
                progressCallback?.invoke(current, total)
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Audio Book Player Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Book Player")
            .setContentText("Playing audio book")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
