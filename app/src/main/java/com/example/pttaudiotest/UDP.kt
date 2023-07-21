package com.example.pttaudiotest

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

import com.theeasiestway.opus.Constants
import com.theeasiestway.opus.Opus

class UDP {
    var audios = emptyArray<ByteArray>()

    private val remoteHost = "190.2.45.173"//""172.31.120.230"//"127.0.0.1"
    private val remotePort = 64749 //64739

    private val codec = Opus()
    private var running = false
    private val buffer = ByteArray(2048)
    private var socket: DatagramSocket? = null

    fun init() {
        socket = DatagramSocket()
        socket!!.broadcast = true

        codec.decoderInit(
            sampleRate = Constants.SampleRate._48000(),
            channels = Constants.Channels.mono()
        )

        running = true
    }

    fun reset() {
        audios = emptyArray<ByteArray>()
    }

    fun ping() {
        Thread(Runnable {
            Thread.sleep(500)
            pingLoop()
        }).start()
    }

    fun receive() {
        Thread(Runnable {
            Thread.sleep(500)
            receiveLoop()
        }).start()
    }

    private fun pingLoop() {
        while (running) {
            try {
                val ping = byteArrayOf(23.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
                val sendPacket =
                    DatagramPacket(ping, ping.size, InetAddress.getByName(remoteHost), remotePort)
                socket?.send(sendPacket)
                //println("UDP PING... ")
                Thread.sleep(1000 * 10)

            } catch (e: Exception) {
                println("UDP ERR PING " + e.message)
                Thread.sleep(100)
            }

        }
    }

    private fun receiveLoop() {
        while (running) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)

                socket?.receive(packet)

                if (packet.length < 10) {
                    //println("UDP ping? " + packet.data.slice(0 until packet.length).toByteArray().contentToString())
                    continue
                }

                val headerLen = packet.length - 40
                val header = packet.data.slice(0 until headerLen).toByteArray()
                val payload = packet.data.slice(headerLen until packet.length).toByteArray()

                //println("UDP " + header.contentToString())

                val decoded =
                    codec.decode(
                        bytes = payload,
                        frameSize = Constants.FrameSize._1920()
                    )

                if (decoded != null) {
                    audios += decoded
                } else {
                    println("UDP ERR decoded = null!!")
                }

            } catch (e: Exception) {
                println("UDP ERR " + e.message)
                Thread.sleep(100)
            }
        }
    }

    fun end() {
        running = false
        socket?.close()
    }

    fun isRunning(): Boolean {
        return running
    }
}