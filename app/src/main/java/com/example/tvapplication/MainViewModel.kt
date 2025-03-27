package com.example.tvapplication

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.MediaPlayer.EventListener
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(@ApplicationContext val context: Context): ViewModel() {

    // To check how this work with multiple videos, change the list to videoNamesList
    private val videoNamesList = listOf(VIDEO_1_MP_4, VIDEO_2_MP_4, VIDEO_3_MP_4)
    // To check how this work with one video, change the list to oneVideoList
    private val oneVideoList = listOf(PANDA_VIDEO)

    private val libVLC: LibVLC by lazy {
        LibVLC(context)
    }
    val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer(libVLC)
    }

    fun getVideoNamesList(): List<String> {
        return oneVideoList
    }

    fun setMedia(videoListIndex: Int) {
        mediaPlayer.media = Media(libVLC, copyAssetToFile(oneVideoList[videoListIndex]).absolutePath).apply {
            addOption(":loop")
        }
    }

    fun setMediaPlayerListener(listener: EventListener) {
        mediaPlayer.setEventListener(listener)
    }

    fun playMedia() {
        mediaPlayer.play()
    }

    fun releasePlayer() {
        mediaPlayer.release()
        libVLC.release()
    }

    // Function to copy asset file to local storage
    private fun copyAssetToFile(assetFileName: String): File {
        val file = File(context.cacheDir, assetFileName)

        if (!file.exists()) {
            try {
                val inputStream: InputStream = context.assets.open(assetFileName)
                val outputStream = file.outputStream()
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            } catch (e: IOException) {
                Log.e("VLC", "Error copying asset to file", e)
            }
        }

        return file
    }

    fun generateVideoThumbnail(context: Context, assetPath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val assetFileDescriptor = context.assets.openFd(assetPath)
            retriever.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            return retriever.getFrameAtTime(SECOND)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    companion object {
        private const val SECOND = 1000000L
        private const val VIDEO_1_MP_4 = "video1.mp4"
        private const val VIDEO_2_MP_4 = "video2.mp4"
        private const val VIDEO_3_MP_4 = "video3.mp4"
        private const val PANDA_VIDEO = "Panda.mp4"
    }
}