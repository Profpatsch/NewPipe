package org.schabi.newpipe.fragments.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.transformLatest
import org.schabi.newpipe.R
import kotlin.time.Duration.Companion.seconds

data class VideoStreamProgress(
    val currentTime: String,
    val percentage: Float
)

data class VideoStreamInfo(
    val streamType: VideoStreamType,
    val streamDuration: StreamDuration,
)

enum class VideoStreamType { Video, Audio }

sealed class StreamDuration {
    data object LiveStream : StreamDuration()
    data class Stream(val duration: String) : StreamDuration()
}

data class TriggerHoldToXIndicator(
    val messageText: String
)

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun VideoDetailFragmentThumbnail(
    modifier: Modifier = Modifier,
    streamProgress: StateFlow<VideoStreamProgress?>,
    streamInfo: StateFlow<VideoStreamInfo?>,
    triggerHoldToXIndicator: Flow<TriggerHoldToXIndicator>
) {
    val progress by streamProgress.collectAsStateWithLifecycle()
    val info by streamInfo.collectAsStateWithLifecycle()

    class HoldToX(
        val alpha: Float,
        val lastVal: TriggerHoldToXIndicator
    )

    // We want to display the “Hold To” message for one second and then fade out.
    val holdToX by remember {
        triggerHoldToXIndicator.transformLatest { t ->
            emit(HoldToX(1f, t))
            delay(1.seconds)
            emit(HoldToX(0f, t))
        }
    }.collectAsStateWithLifecycle(HoldToX(0f, TriggerHoldToXIndicator("")))

    val holdToXAlpha by animateFloatAsState(
        holdToX.alpha,
        label = "HoldToXAlpha"
    )

    Column(modifier) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Centered on the thumbnail
            // play indicator (either audio or video)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                info?.let {
                    when (it.streamType) {
                        VideoStreamType.Video -> Image(
                            painter = painterResource(R.drawable.ic_play_arrow_shadow),
                            contentDescription = "Play arrow",
                            modifier = Modifier.size(64.dp)
                        )

                        VideoStreamType.Audio -> Image(
                            painter = painterResource(R.drawable.ic_headset_shadow),
                            contentDescription = "Headset",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // “hold to enqueue/append” indicator triggered to indicate long-press actions for buttons
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                Text(
                    text = holdToX.lastVal.messageText,
                    color = Color.White,
                    modifier = Modifier
                        .alpha(holdToXAlpha)
                        .background(colorResource(R.color.video_overlay_color))
                        .padding(vertical = 10.dp, horizontal = 30.dp)

                )
            }

            // Progress section in the bottom of thumbnail
            progress?.let {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    text = it.currentTime,
                    color = Color.White,
                )
            }
            info?.let {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color.hsl(0f, 1f, 0f, 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    text = when (it.streamDuration) {
                        is StreamDuration.LiveStream -> stringResource(R.string.duration_live)
                        is StreamDuration.Stream -> it.streamDuration.duration
                    },
                    color = Color.White
                )
            }
        }

        // progress indicator right below thumbnail
        progress?.let {
            LinearProgressIndicator(
                progress = { it.percentage },
                modifier = Modifier
                    .requiredHeight(5.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
@Preview
fun VideoDetailFragmentThumbnailPreview() {
    Box(modifier = Modifier.height(100.dp)) {
        VideoDetailFragmentThumbnail(
            streamProgress = MutableStateFlow(
                VideoStreamProgress(
                    currentTime = "01:15", percentage = 0.11f
                )
            ),
            streamInfo = MutableStateFlow(
                VideoStreamInfo(
                    streamType = VideoStreamType.Video,
                    streamDuration = StreamDuration.Stream("10:41")
                )
            ),
            triggerHoldToXIndicator = emptyFlow()
        )
    }
}
