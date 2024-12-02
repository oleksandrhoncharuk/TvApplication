package com.example.tvapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.tvapplication.ui.theme.TVApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout


private const val HOME_ROUTE = "home"
private const val VIDEO_PLAYER_ROUTE = "videoPlayer/"
private const val VIDEO_PATH = "videoPath"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            TVApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentIndex by remember { mutableIntStateOf(0) }
                    val videoNamesList = viewModel.getVideoNamesList()

                    NavHost(navController, startDestination = HOME_ROUTE) {
                        composable(HOME_ROUTE) {
                            HomePage(videos = videoNamesList) { videoPath ->
                                // Navigate to video player screen with the video path
                                navController.navigate("$VIDEO_PLAYER_ROUTE$videoPath")
                            }
                        }
                        composable(
                            "$VIDEO_PLAYER_ROUTE{$VIDEO_PATH}",
                            arguments = listOf(navArgument(VIDEO_PATH) {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val videoPath = backStackEntry.arguments?.getString(VIDEO_PATH)
                                ?: return@composable
                            // Find the index of the selected video
                            currentIndex = videoNamesList.indexOf(videoPath)

                            // Pass the list of videos to the VideoPlayer composable
                            VideoPlayer(playingIndex = currentIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomePage(videos: List<String>, onPlaySequence: (String) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = getString(context, R.string.previews),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f) // Take up the space to the left
            )

            // Divider below the text
            HorizontalDivider(
                thickness = 2.dp, color = Color.White,
                modifier = Modifier
                    .weight(5f) // Divider takes up the remaining space
                    .height(1.dp),
            )
        }

        // Video Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // 3 columns in the grid
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos) { videoPath ->
                VideoGridItem(videoPath = videoPath, onClick = { onPlaySequence(videoPath) })
            }
        }
    }
}


@Composable
fun VideoGridItem(
    viewModel: MainViewModel = hiltViewModel(),
    videoPath: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbnail = remember(videoPath) {
        viewModel.generateVideoThumbnail(context, videoPath)
    }

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth() // Ensures the item takes the full width of the grid
            .aspectRatio(1f) // Keeps the item square
            .clip(RoundedCornerShape(8.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth() // Ensures the image takes full width
                .weight(1f) // Makes the image take as much space as possible
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = getString(context, R.string.thumbnail),
                    modifier = Modifier
                        .fillMaxSize() // Stretches the image to fill the Box
                        .align(Alignment.Center) // Centers the image within the Box
                        .clip(RoundedCornerShape(8.dp)) // Optional, rounds the corners of the image
                        .graphicsLayer {
                            scaleX = 1f // Optional: adjust scaling if necessary
                            scaleY = 1f
                        },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(getString(context, R.string.no_preview), color = Color.White)
                }
            }
        }

        // Text overlay at the bottom of the image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(vertical = 4.dp, horizontal = 8.dp) // Adds padding around the text
        ) {
            Text(
                text = videoPath.substringAfterLast('/'), // Display only the file name
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun VideoPlayer(
    viewModel: MainViewModel = hiltViewModel(),
    playingIndex: Int
) {
    val context = LocalContext.current

    var currentIndex by remember { mutableIntStateOf(playingIndex) }
    val videoListSize = viewModel.getVideoNamesList().size
    val mediaPlayer = remember { viewModel.mediaPlayer }

    DisposableEffect(Unit) {
        val listener = MediaPlayer.EventListener { event ->
            if (event.type == MediaPlayer.Event.EndReached) {
                currentIndex = (currentIndex + 1) % videoListSize
                viewModel.setMedia(currentIndex)
                viewModel.playMedia()
            }
        }
        viewModel.setMediaPlayerListener(listener)
        viewModel.setMedia(currentIndex)
        viewModel.playMedia()

        onDispose {
            viewModel.releasePlayer()
        }
    }

    AndroidView(
        factory = {
            VLCVideoLayout(context).apply {
                mediaPlayer.attachViews(this, null, false, false)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
