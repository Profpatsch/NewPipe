package org.schabi.newpipe.fragments.detail

import org.schabi.newpipe.player.PlayerService
import org.schabi.newpipe.player.ui.MainPlayerUi
import org.schabi.newpipe.player.ui.VideoPlayerUi

class VideoDetailFragmentPlayer(
    val playerService: PlayerService
) {
    val player by playerService::player

    /** the VideoPlayerUi, if any */
    val videoPlayerUi
        get() = player.UIs().get(VideoPlayerUi::class.java)

    /** the root of our VideoPlayerUi, if any */
    val videoPlayerUiRoot
        get() = videoPlayerUi?.binding?.root

    /** The player is fullscreen if we have a VideoPlayerUI and it is in fullscreen mode */
    val isFullscreen
        get() = videoPlayerUi?.isFullscreen == true

    /** the MainPlayerUi, if any */
    val mainPlayerUi
        get() = player.UIs().get(MainPlayerUi::class.java)
}
