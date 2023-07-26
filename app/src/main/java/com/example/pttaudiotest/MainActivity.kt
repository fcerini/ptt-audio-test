package com.example.pttaudiotest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.pttaudiotest.ui.theme.PttAudioTestTheme

import kotlin.experimental.and
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private var udp = UDP()

    private var lastAudioSize = 0

    private lateinit var audioTrack: AudioTrack
    var play = false
    var replay = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PttAudioTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Escuchando canal 1 Soflex. Ver el Logcat...")
                }
            }
        }

        udp.init()
        udp.ping()
        udp.receive()

        genTone()

        Thread(Runnable {
            Thread.sleep(500)
            testAudio()
        }).start()
    }


    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {

        Column {
            ReplayButton()
            Text(
                text = "$name!",
                modifier = modifier,
                fontSize = 32.sp
            )
            ResetButton()
        }
    }

    @Composable
    fun ReplayButton() {
        Button(onClick = {
            Thread(Runnable {
                Thread.sleep(100)
                replay()
            }).start()

        }) {
            Text(text = "REPLAY", fontSize = 40.sp)
        }
    }

    @Composable
    fun ResetButton() {
        Button(onClick = {
            reset()
        }) {
            Text(text = "RESET", fontSize = 40.sp)
        }
    }

    private val duration = 1
    private val sampleRate = 48000
    private val numSamples = duration * sampleRate
    private val sample = DoubleArray(numSamples)
    private val generatedSnd = ByteArray(2 * numSamples)
    private fun genTone() {
        for (i in 0 until numSamples) {
            val freqOfTone = 400.0
            sample[i] =
                sin( Math.PI * i / (sampleRate / freqOfTone))
        }
        var idx = 0
        for (dVal in sample) {
            val `val` = (dVal * 32767).toInt().toShort()
            generatedSnd[idx++] = `val`.and(0x00ff).toByte()
            generatedSnd[idx++] = `val`.and(0xff00 ushr 8).toByte()
        }
    }

    private fun playSound() {
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
    }

    private fun initAudio() {
        val sampleRate = 48000
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM,
            0
        )

        audioTrack.play()
    }

    private fun reset() {
        udp.reset()
        lastAudioSize = 0
        initAudio()
        playSound()
    }

    private fun replay() {
        try {
            val audioSize = udp.audios.size
            val nuevosAudios = udp.audios.sliceArray(0 until audioSize)

            replay = true
            for (element in nuevosAudios) {
                Thread.sleep(20)
                if (play) {
                    replay = false
                    return
                }
                audioTrack.write(element, 0, element.size)
            }
        } catch (e: Exception) {
            println("ERR testAudio" + e.message)
        }

        replay = false

    }

    private fun testAudio() {

        try {
            initAudio()
            println("--- INICIO ----------------------------------------------------")

            while (udp.isRunning()) {
                val audioSize = udp.audios.size
                if (audioSize < 50) { //buffer inicial
                    continue
                }

                val nuevosAudios = udp.audios.sliceArray(lastAudioSize until audioSize)
                lastAudioSize = audioSize

                if (nuevosAudios.isNotEmpty()) {
                    println("AUDIOS " + nuevosAudios.size)
                }
                for (element in nuevosAudios) {
                    play = true
                    Thread.sleep(10)
                    audioTrack.write(element, 0, element.size)
                }
                play = false

                Thread.sleep(10)
            }

        } catch (e: Exception) {
            println("ERR testAudio" + e.message)
        }
    }

}
