package org.schabi.newpipe.fragments.detail

import org.schabi.newpipe.player.PlayerService
import org.schabi.newpipe.player.ui.MainPlayerUi
import org.schabi.newpipe.player.ui.PopupPlayerUi
import org.schabi.newpipe.player.ui.VideoPlayerUi

class VideoDetailFragmentPlayer(
    val playerService: PlayerService
) {
    val player by playerService::player

    /** the first VideoPlayerUi ([MainPlayerUi] or [PopupPlayerUi]), if any */
    val firstVideoPlayerUi
        get() = player.UIs().get(VideoPlayerUi::class.java)

    /** the root of our VideoPlayerUi, if any */
    val firstVideoPlayerUiRoot
        get() = firstVideoPlayerUi?.binding?.root

    /** the MainPlayerUi, if any */
    val mainPlayerUi
        get() = player.UIs().get(MainPlayerUi::class.java)

    /** The player is fullscreen if we have a VideoPlayerUI and it is in fullscreen mode */
    val isFullscreen
        get() = firstVideoPlayerUi?.isFullscreen == true
}
