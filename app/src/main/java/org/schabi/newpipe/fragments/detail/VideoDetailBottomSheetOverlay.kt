package org.schabi.newpipe.fragments.detail

import android.text.TextUtils
import android.view.View
import com.google.android.material.appbar.AppBarLayout
import org.schabi.newpipe.R
import org.schabi.newpipe.databinding.FragmentVideoOverlayBinding
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.util.image.CoilHelper.loadDetailsThumbnail
import kotlin.math.min

data class OverlayListeners(
    val overlay: () -> Unit,
    val close: () -> Unit,
    val openPlayQueue: () -> Unit,
    val playPause: () -> Unit,
    val longClick: () -> View.OnLongClickListener,
)

/**
 * This is the UI that appears when you pull down the video,
 * allowing you to interact with the rest of the app while
 * the main player is running.
 */
class VideoDetailBottomSheetOverlay(val binding: FragmentVideoOverlayBinding) {

    companion object {
        const val MAX_OVERLAY_ALPHA = 0.9f
    }

    fun setListeners(listeners: OverlayListeners) {
        binding.overlayThumbnail.setOnClickListener(View.OnClickListener { listeners.overlay() })
        binding.overlayMetadataLayout.setOnClickListener(View.OnClickListener { listeners.overlay() })
        binding.overlayButtonsLayout.setOnClickListener(View.OnClickListener { listeners.overlay() })
        binding.overlayCloseButton.setOnClickListener(View.OnClickListener { listeners.close() })
        binding.overlayPlayQueueButton.setOnClickListener(View.OnClickListener { listeners.openPlayQueue() })
        binding.overlayPlayPauseButton.setOnClickListener(View.OnClickListener { listeners.playPause() })
        binding.overlayThumbnail.setOnLongClickListener(listeners.longClick())
        binding.overlayMetadataLayout.setOnLongClickListener(listeners.longClick())
    }

    fun setAlpha(alpha: Float) {
        binding.overlayLayout.alpha = alpha
    }

    fun setOverlayElementsClickable(enable: Boolean) {
        binding.overlayThumbnail.isClickable = enable
        binding.overlayThumbnail.isLongClickable = enable
        binding.overlayMetadataLayout.isClickable = enable
        binding.overlayMetadataLayout.isLongClickable = enable
        binding.overlayButtonsLayout.isClickable = enable
        binding.overlayPlayQueueButton.isClickable = enable
        binding.overlayPlayPauseButton.isClickable = enable
        binding.overlayCloseButton.isClickable = enable
    }

    fun updateOverlayData(
        overlayTitle: String?,
        uploader: String?,
        thumbnails: MutableList<Image>
    ) {
        binding.overlayTitleTextView.text = if (TextUtils.isEmpty(overlayTitle)) "" else overlayTitle
        binding.overlayChannelTextView.text = if (TextUtils.isEmpty(uploader)) "" else uploader
        binding.overlayThumbnail.setImageDrawable(null)
        loadDetailsThumbnail(binding.overlayThumbnail, thumbnails)
    }

    fun setOverlayPlayPauseImage(playerIsPlaying: Boolean) {
        val drawable = if (playerIsPlaying)
            R.drawable.ic_pause
        else
            R.drawable.ic_play_arrow
        binding.overlayPlayPauseButton.setImageResource(drawable)
    }

    fun setOverlayLook(
        appBar: AppBarLayout,
        behavior: AppBarLayout.Behavior?,
        slideOffset: Float,
        thumbnailHeight: Int
    ) {
        // SlideOffset < 0 when mini player is about to close via swipe.
        // Stop animation in this case
        if (behavior == null || slideOffset < 0) {
            return
        }
        setAlpha(
            min(
                MAX_OVERLAY_ALPHA,
                1 - slideOffset
            )
        )
        // These numbers are not special. They just do a cool transition
        behavior.setTopAndBottomOffset(
            (-thumbnailHeight * 2 * (1 - slideOffset) / 3).toInt()
        )
        appBar.requestLayout()
    }
}
