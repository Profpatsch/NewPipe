package org.schabi.newpipe.fragments.detail

import org.schabi.newpipe.player.PlayerService
import org.schabi.newpipe.player.ui.VideoPlayerUi

class VideoDetailFragmentPlayer(
    val playerService: PlayerService
) {
    val player by playerService::player

    val videoPlayerRoot
        get() = player.UIs().get(VideoPlayerUi::class.java)?.binding?.root
}
