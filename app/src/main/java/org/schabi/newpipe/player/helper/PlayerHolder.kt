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

data class Listeners(
    val listener: PlayerServiceEventListener,
    val holderListener: PlayerHolderLifecycleEventListener
)

private val DEBUG = MainActivity.DEBUG
private val TAG: String = PlayerHolder::class.java.getSimpleName()

/** Singleton that manages a `PlayerService`
 * and can be used to control the player instance through the service.  */
object PlayerHolder {
    private var listeners: Listeners? = null

    private var bound = false

    private var playerService: PlayerService? = null
    private var player: Player? = null
        get() = playerService?.player

    /**
     * Returns the current [PlayerType] of the [PlayerService] service,
     * otherwise `null` if no service is running.
     *
     * @return Current PlayerType
     */
    fun getType(): PlayerType? =
        player?.playerType

    fun isPlaying(): Boolean =
        player?.isPlaying == true

    fun isPlayerOpen(): Boolean =
        playerService != null

    /**
     * Use this method to only allow the user to manipulate the play queue (e.g. by enqueueing via
     * the stream long press menu) when there actually is a play queue to manipulate.
     * @return true only if the player is open and its play queue is ready (i.e. it is not null)
     */
    fun isPlayQueueReady(): Boolean =
        player?.playQueue != null

    fun isNotBoundYet(): Boolean {
        return !bound
    }

    fun getQueueSize(): Int =
        player?.playQueue?.size() ?: 0

    fun getQueuePosition(): Int =
        player?.playQueue?.index ?: 0

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
        newListener: PlayerServiceEventListener,
        newHolderListener: PlayerHolderLifecycleEventListener
    ) {
        val context = getCommonContext()
        listeners = Listeners(newListener, newHolderListener)

        // Force reload data from service
        player?.let { player ->
            newHolderListener.onServiceConnected(playerService, false)
            player.setFragmentListener(HolderPlayerServiceEventListener)
        }
        if (bound) {
            return
        }
        // startService() can be called concurrently and it will give a random crashes
        // and NullPointerExceptions inside the service because the service will be
        // bound twice. Prevent it with unbinding first
        unbind(context)
        ContextCompat.startForegroundService(context, Intent(context, PlayerService::class.java))
        PlayerServiceConnection.playAfterConnect = playAfterConnect

        if (DEBUG) {
            Log.d(TAG, "bind() called")
        }

        val serviceIntent = Intent(context, PlayerService::class.java)
        bound = context.bindService(
            serviceIntent, PlayerServiceConnection,
            Context.BIND_AUTO_CREATE
        )
        if (!bound) {
            context.unbindService(PlayerServiceConnection)
        }
    }

    fun stopService() {
        val context = getCommonContext()
        unbind(context)
        context.stopService(Intent(context, PlayerService::class.java))
    }

    /** Call [Context.unbindService] on our service
     * (does not necessarily stop the service right away).
     * Remove all our listeners and initialize them.
     * @param context shared context
     */
    private fun unbind(context: Context) {
        if (DEBUG) {
            Log.d(TAG, "unbind() called")
        }

        if (bound) {
            context.unbindService(PlayerServiceConnection)
            bound = false
            player?.removeFragmentListener(HolderPlayerServiceEventListener)
            playerService = null
            listeners?.holderListener?.onServiceDisconnected()
        }
    }

    fun unsetListeners() {
        listeners = null
    }

    internal object PlayerServiceConnection : ServiceConnection {
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
                "PlayerService.LocalBinder.getService() must never be null after the service connects"
            }
            listeners?.holderListener?.onServiceConnected(playerService, playAfterConnect)
            player?.setFragmentListener(HolderPlayerServiceEventListener)
        }
    }

    /** Delegate all [PlayerServiceEventListener] events to our current `listener` object.
     * Only difference is that if [PlayerServiceEventListener.onServiceStopped] is called,
     * it also calls [PlayerHolder.unbind].
     */
    private object HolderPlayerServiceEventListener : PlayerServiceEventListener {
        override fun onViewCreated() {
            listeners?.listener?.onViewCreated()
        }

        override fun onFullscreenStateChanged(fullscreen: Boolean) {
            listeners?.listener?.onFullscreenStateChanged(fullscreen)
        }

        override fun onScreenRotationButtonClicked() {
            listeners?.listener?.onScreenRotationButtonClicked()
        }

        override fun onMoreOptionsLongClicked() {
            listeners?.listener?.onMoreOptionsLongClicked()
        }

        override fun onPlayerError(
            error: PlaybackException?,
            isCatchableException: Boolean
        ) {
            listeners?.listener?.onPlayerError(error, isCatchableException)
        }

        override fun hideSystemUiIfNeeded() {
            listeners?.listener?.hideSystemUiIfNeeded()
        }

        override fun onQueueUpdate(queue: PlayQueue?) {
            listeners?.listener?.onQueueUpdate(queue)
        }

        override fun onPlaybackUpdate(
            state: Int,
            repeatMode: Int,
            shuffled: Boolean,
            parameters: PlaybackParameters?
        ) {
            listeners?.listener?.onPlaybackUpdate(state, repeatMode, shuffled, parameters)
        }

        override fun onProgressUpdate(
            currentProgress: Int,
            duration: Int,
            bufferPercent: Int
        ) {
            listeners?.listener?.onProgressUpdate(currentProgress, duration, bufferPercent)
        }

        override fun onMetadataUpdate(info: StreamInfo?, queue: PlayQueue?) {
            listeners?.listener?.onMetadataUpdate(info, queue)
        }

        override fun onServiceStopped() {
            listeners?.listener?.onServiceStopped()
            unbind(getCommonContext())
        }
    }
}
