package org.schabi.newpipe.player.event

import com.google.android.exoplayer2.PlaybackException

/** [PlayerEventListener] that also gets called for
 * application-specific events like screen rotation or UI changes.
 */
interface PlayerServiceEventListener : PlayerEventListener {
    fun onViewCreated()

    fun onFullscreenStateChanged(fullscreen: Boolean)

    fun onFullscreenToggleButtonClicked()

    fun onMoreOptionsLongClicked()

    fun onPlayerError(error: PlaybackException, isCatchableException: Boolean)

    fun hideSystemUiIfNeeded()
}
