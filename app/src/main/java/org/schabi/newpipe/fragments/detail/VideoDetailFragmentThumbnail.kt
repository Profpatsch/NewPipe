package org.schabi.newpipe.fragments.detail

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.schabi.newpipe.R

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

@Composable
fun VideoDetailFragmentThumbnail(
    modifier: Modifier = Modifier,
    streamProgress: StateFlow<VideoStreamProgress?>,
    streamInfo: StateFlow<VideoStreamInfo?>
) {
    val progress by streamProgress.collectAsStateWithLifecycle()
    val info by streamInfo.collectAsStateWithLifecycle()
    Column(modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
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
            )
        )
    }
}
