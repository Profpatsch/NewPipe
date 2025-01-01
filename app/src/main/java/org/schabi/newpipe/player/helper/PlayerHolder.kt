package org.schabi.newpipe.player.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import org.schabi.newpipe.App
import org.schabi.newpipe.MainActivity
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.player.Player
import org.schabi.newpipe.player.PlayerService
import org.schabi.newpipe.player.PlayerService.LocalBinder
import org.schabi.newpipe.player.PlayerType
import org.schabi.newpipe.player.event.PlayerHolderLifecycleEventListener
import org.schabi.newpipe.player.event.PlayerServiceEventListener
import org.schabi.newpipe.player.helper.PlayerHolder.PlayerServiceConnection
import org.schabi.newpipe.player.playqueue.PlayQueue

/** Singleton that manages a `PlayerService`
 * and can be used to control the player instance through the service.  */
class PlayerHolder private constructor() {
    private var listener: PlayerServiceEventListener? = null
    private var holderListener: PlayerHolderLifecycleEventListener? = null

    private val serviceConnection = PlayerServiceConnection()
    private var bound = false

    private var playerService: PlayerService? = null
    private var player: Player? = null

    /**
     * Returns the current [PlayerType] of the [PlayerService] service,
     * otherwise `null` if no service is running.
     *
     * @return Current PlayerType
     */
    fun getType(): PlayerType? {
        if (player == null) {
            return null
        }
        return player!!.getPlayerType()
    }

    fun isPlaying(): Boolean {
        if (player == null) {
            return false
        }
        return player!!.isPlaying()
    }

    fun isPlayerOpen(): Boolean {
        return player != null
    }

    /**
     * Use this method to only allow the user to manipulate the play queue (e.g. by enqueueing via
     * the stream long press menu) when there actually is a play queue to manipulate.
     * @return true only if the player is open and its play queue is ready (i.e. it is not null)
     */
    fun isPlayQueueReady(): Boolean {
        return player != null && player!!.getPlayQueue() != null
    }

    fun isNotBoundYet(): Boolean {
        return !bound
    }

    fun getQueueSize(): Int {
        if (player == null || player!!.getPlayQueue() == null) {
            // player play queue might be null e.g. while player is starting
            return 0
        }
        return player!!.getPlayQueue()!!.size()
    }

    fun getQueuePosition(): Int {
        if (player == null || player!!.getPlayQueue() == null) {
            return 0
        }
        return player!!.getPlayQueue()!!.getIndex()
    }

    fun unsetListeners() {
        listener = null
        holderListener = null
    }

    /** Helper to handle context in common place as using the same
     * context to bind/unbind a service is crucial.
     *
     * @return the common context
     */
    private fun getCommonContext(): Context {
        return App.instance
    }

    /** Connect to (and if needed start) the [PlayerService]
     * and bind [PlayerServiceConnection] to it.
     * If the service is already started, only set the listener.
     * @param playAfterConnect If this holder’s service was already started,
     * start playing immediately
     * @param newListener set this listener
     * @param newHolderListener set this listener
     */
    fun startService(
        playAfterConnect: Boolean,
        newListener: PlayerServiceEventListener?,
        newHolderListener: PlayerHolderLifecycleEventListener?
    ) {
        val context = getCommonContext()
        listener = newListener
        holderListener = newHolderListener

        // Force reload data from service
        if (player != null) {
            holderListener!!.onServiceConnected(playerService, false)
            player!!.setFragmentListener(internalListener)
        }
        if (bound) {
            return
        }
        // startService() can be called concurrently and it will give a random crashes
        // and NullPointerExceptions inside the service because the service will be
        // bound twice. Prevent it with unbinding first
        unbind(context)
        ContextCompat.startForegroundService(context, Intent(context, PlayerService::class.java))
        serviceConnection.playAfterConnect = playAfterConnect

        if (DEBUG) {
            Log.d(TAG, "bind() called")
        }

        val serviceIntent = Intent(context, PlayerService::class.java)
        bound = context.bindService(
            serviceIntent, serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        if (!bound) {
            context.unbindService(serviceConnection)
        }
    }

    fun stopService() {
        val context = getCommonContext()
        unbind(context)
        context.stopService(Intent(context, PlayerService::class.java))
    }

    /** Call [Context.unbindService] on our service
     * (does not necessarily stop the service right away).
     * Remove all our listeners and deinitialize them.
     * @param context shared context
     */
    private fun unbind(context: Context) {
        if (DEBUG) {
            Log.d(TAG, "unbind() called")
        }

        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
            if (player != null) {
                player!!.removeFragmentListener(internalListener)
            }
            playerService = null
            player = null
            if (holderListener != null) {
                holderListener!!.onServiceDisconnected()
            }
        }
    }

    internal inner class PlayerServiceConnection : ServiceConnection {
        var playAfterConnect = false

        override fun onServiceDisconnected(compName: ComponentName?) {
            if (DEBUG) {
                Log.d(TAG, "Player service is disconnected")
            }

            val context = getCommonContext()
            unbind(context)
        }

        override fun onServiceConnected(compName: ComponentName?, service: IBinder?) {
            if (DEBUG) {
                Log.d(TAG, "Player service is connected")
            }
            val localBinder = service as LocalBinder

            playerService = localBinder.getService()
            requireNotNull(playerService) {
                (
                    "PlayerService.LocalBinder.getService() must never be" +
                        "null after the service connects"
                    )
            }
            player = playerService!!.player

            if (holderListener != null) {
                holderListener!!.onServiceConnected(playerService, playAfterConnect)
            }
            if (player != null) {
                player!!.setFragmentListener(internalListener)
            }
        }
    }

    /** Delegate all [PlayerServiceEventListener] events to our current `listener` object.
     * Only difference is that if [PlayerServiceEventListener.onServiceStopped] is called,
     * it also calls [PlayerHolder.unbind].
     */
    private val internalListener: PlayerServiceEventListener = object : PlayerServiceEventListener {
        override fun onViewCreated() {
            if (listener != null) {
                listener!!.onViewCreated()
            }
        }

        override fun onFullscreenStateChanged(fullscreen: Boolean) {
            if (listener != null) {
                listener!!.onFullscreenStateChanged(fullscreen)
            }
        }

        override fun onScreenRotationButtonClicked() {
            if (listener != null) {
                listener!!.onScreenRotationButtonClicked()
            }
        }

        override fun onMoreOptionsLongClicked() {
            if (listener != null) {
                listener!!.onMoreOptionsLongClicked()
            }
        }

        override fun onPlayerError(
            error: PlaybackException?,
            isCatchableException: Boolean
        ) {
            if (listener != null) {
                listener!!.onPlayerError(error, isCatchableException)
            }
        }

        override fun hideSystemUiIfNeeded() {
            if (listener != null) {
                listener!!.hideSystemUiIfNeeded()
            }
        }

        override fun onQueueUpdate(queue: PlayQueue?) {
            if (listener != null) {
                listener!!.onQueueUpdate(queue)
            }
        }

        override fun onPlaybackUpdate(
            state: Int,
            repeatMode: Int,
            shuffled: Boolean,
            parameters: PlaybackParameters?
        ) {
            if (listener != null) {
                listener!!.onPlaybackUpdate(state, repeatMode, shuffled, parameters)
            }
        }

        override fun onProgressUpdate(
            currentProgress: Int,
            duration: Int,
            bufferPercent: Int
        ) {
            if (listener != null) {
                listener!!.onProgressUpdate(currentProgress, duration, bufferPercent)
            }
        }

        override fun onMetadataUpdate(info: StreamInfo?, queue: PlayQueue?) {
            if (listener != null) {
                listener!!.onMetadataUpdate(info, queue)
            }
        }

        override fun onServiceStopped() {
            if (listener != null) {
                listener!!.onServiceStopped()
            }
            unbind(getCommonContext())
        }
    }

    companion object {
        private var instance: PlayerHolder? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): PlayerHolder {
            if (instance == null) {
                instance = PlayerHolder()
            }
            return instance!!
        }

        private val DEBUG = MainActivity.DEBUG
        private val TAG: String = PlayerHolder::class.java.getSimpleName()
    }
}
