package com.baiviet.core.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.EnumMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Hiệu ứng âm thanh trong game. */
enum class Sfx { TAP, DEAL, FLIP, PLAY, CHIP, WIN, LOSE, BIG_EVENT, TICK }

/**
 * Âm thanh tổng hợp bằng [AudioTrack] — không cần file asset, nhẹ, chạy offline.
 *
 * - Hiệu ứng: dựng sẵn PCM 16-bit mono một lần (lười), phát qua track tĩnh.
 * - Nhạc nền: vòng giai điệu ngũ cung nhẹ nhàng, sinh liên tục trên một luồng riêng.
 *
 * Bật/tắt theo Cài đặt qua [sfxEnabled] / [musicEnabled]; app gọi [onForeground] / [onBackground]
 * theo vòng đời Activity để dừng nhạc khi vào nền.
 */
object GameAudio {
    private const val RATE = 22_050

    @Volatile var sfxEnabled: Boolean = true

    @Volatile var musicEnabled: Boolean = false
        set(value) {
            field = value
            refreshMusic()
        }

    @Volatile private var foreground = false

    private val pcm = EnumMap<Sfx, ShortArray>(Sfx::class.java)
    private val sfxExecutor = Executors.newFixedThreadPool(3) { r -> Thread(r, "bv-sfx").apply { isDaemon = true } }
    private val lastPlayed = EnumMap<Sfx, Long>(Sfx::class.java)

    @Volatile private var musicThread: Thread? = null

    /** Phát một hiệu ứng (bỏ qua nếu tắt, hoặc cùng loại vừa phát < 45ms để tránh chồng tiếng). */
    fun play(sfx: Sfx) {
        if (!sfxEnabled) return
        val now = System.currentTimeMillis()
        synchronized(lastPlayed) {
            if (now - (lastPlayed[sfx] ?: 0L) < 45L) return
            lastPlayed[sfx] = now
        }
        sfxExecutor.execute { runCatching { playNow(sfx) } }
    }

    fun onForeground() {
        foreground = true
        refreshMusic()
    }

    fun onBackground() {
        foreground = false
        refreshMusic()
    }

    private fun playNow(sfx: Sfx) {
        val data = synchronized(pcm) { pcm.getOrPut(sfx) { synth(sfx) } }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
        try {
            track.write(data, 0, data.size)
            track.play()
            Thread.sleep(data.size * 1000L / RATE + 30L)
        } finally {
            track.release()
        }
    }

    // ───────────────────────── Tổng hợp hiệu ứng ─────────────────────────

    private fun synth(sfx: Sfx): ShortArray = when (sfx) {
        Sfx.TAP -> tone(listOf(Note(880.0, 0, 45)), volume = 0.25)
        Sfx.TICK -> tone(listOf(Note(1320.0, 0, 35)), volume = 0.2)
        Sfx.DEAL -> noise(durationMs = 90, volume = 0.35)
        Sfx.FLIP -> mix(noise(durationMs = 60, volume = 0.25), tone(listOf(Note(620.0, 10, 50)), volume = 0.15))
        Sfx.PLAY -> mix(noise(durationMs = 70, volume = 0.3), tone(listOf(Note(240.0, 0, 80)), volume = 0.25))
        Sfx.CHIP -> tone(
            listOf(Note(2600.0, 0, 60), Note(3150.0, 45, 60), Note(2800.0, 95, 70)),
            volume = 0.18,
        )
        Sfx.WIN -> tone(
            listOf(Note(523.25, 0, 140), Note(659.25, 110, 140), Note(783.99, 220, 160), Note(1046.5, 340, 320)),
            volume = 0.3,
        )
        Sfx.LOSE -> tone(
            listOf(Note(392.0, 0, 180), Note(349.23, 160, 180), Note(293.66, 320, 380)),
            volume = 0.28,
        )
        Sfx.BIG_EVENT -> mix(
            tone(
                listOf(
                    Note(523.25, 0, 120), Note(659.25, 90, 120), Note(783.99, 180, 120),
                    Note(1046.5, 270, 500), Note(1318.5, 270, 500),
                ),
                volume = 0.3,
            ),
            tone((0 until 8).map { Note(2093.0 + it * 180, 320 + it * 45, 90) }, volume = 0.08),
        )
    }

    private data class Note(val freq: Double, val startMs: Int, val durMs: Int)

    /** Các nốt sine + họa âm nhẹ, envelope tấn công nhanh – tắt dần. */
    private fun tone(notes: List<Note>, volume: Double): ShortArray {
        val totalMs = notes.maxOf { it.startMs + it.durMs }
        val out = DoubleArray(totalMs * RATE / 1000 + 1)
        for (n in notes) {
            val start = n.startMs * RATE / 1000
            val len = n.durMs * RATE / 1000
            for (i in 0 until len) {
                val t = i.toDouble() / RATE
                val attack = (i / (RATE * 0.005)).coerceAtMost(1.0)
                val env = attack * exp(-4.0 * i / len)
                val s = sin(2 * PI * n.freq * t) + 0.3 * sin(4 * PI * n.freq * t)
                val idx = start + i
                if (idx < out.size) out[idx] += s * env
            }
        }
        return toPcm(out, volume)
    }

    /** Tiếng "xoẹt" — nhiễu trắng lọc thông thấp, tắt dần. */
    private fun noise(durationMs: Int, volume: Double): ShortArray {
        val len = durationMs * RATE / 1000
        val rnd = Random(7)
        val out = DoubleArray(len)
        var last = 0.0
        for (i in 0 until len) {
            last = last * 0.55 + (rnd.nextDouble() * 2 - 1) * 0.45
            val env = sin(PI * i / len) * exp(-2.5 * i / len)
            out[i] = last * env * 2.2
        }
        return toPcm(out, volume)
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val out = ShortArray(maxOf(a.size, b.size))
        for (i in out.indices) {
            val v = (a.getOrElse(i) { 0 }.toInt() + b.getOrElse(i) { 0 }.toInt())
            out[i] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun toPcm(samples: DoubleArray, volume: Double): ShortArray {
        val peak = samples.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 0 } ?: 1.0
        return ShortArray(samples.size) { (samples[it] / peak * volume * Short.MAX_VALUE).toInt().toShort() }
    }

    // ───────────────────────── Nhạc nền ─────────────────────────

    @Synchronized
    private fun refreshMusic() {
        val shouldPlay = musicEnabled && foreground
        val running = musicThread
        if (shouldPlay && running == null) {
            val thread = Thread(::musicLoop, "bv-music").apply { isDaemon = true }
            musicThread = thread
            thread.start()
        } else if (!shouldPlay && running != null) {
            musicThread = null
            running.interrupt()
        }
    }

    /** Vòng nhạc ngũ cung (C D E G A) — 4 ô nhịp hợp âm rải + giai điệu, lặp vô hạn. */
    private fun musicLoop() {
        val me = Thread.currentThread()
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBuf * 4)
                .build()
        }.getOrNull() ?: return
        try {
            val loop = musicPcm()
            track.setVolume(0f)
            track.play()
            var fade = 0f
            val chunk = RATE / 10
            var pos = 0
            val buf = ShortArray(chunk)
            while (musicThread === me && !me.isInterrupted) {
                for (i in 0 until chunk) {
                    buf[i] = loop[pos]
                    pos = (pos + 1) % loop.size
                }
                if (fade < 1f) {
                    fade = (fade + 0.05f).coerceAtMost(1f)
                    track.setVolume(fade)
                }
                if (track.write(buf, 0, chunk) < 0) break
            }
        } catch (_: InterruptedException) {
            // Dừng nhạc
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private val musicCache: ShortArray by lazy { buildMusic() }

    private fun musicPcm(): ShortArray = musicCache

    private fun buildMusic(): ShortArray {
        val bpm = 84.0
        val beat = 60.0 / bpm
        val bars = 8
        val total = (bars * 4 * beat * RATE).toInt()
        val out = DoubleArray(total)
        // Hợp âm mỗi ô nhịp (tần số gốc): C, Am, F, G, C, Em, F, G
        val chords = listOf(
            listOf(261.63, 329.63, 392.0), listOf(220.0, 261.63, 329.63),
            listOf(174.61, 220.0, 261.63), listOf(196.0, 246.94, 293.66),
            listOf(261.63, 329.63, 392.0), listOf(164.81, 196.0, 246.94),
            listOf(174.61, 220.0, 261.63), listOf(196.0, 246.94, 293.66),
        )
        val pentatonic = listOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.5)
        val melody = listOf(2, 3, 4, 3, 2, 1, 0, 1, 2, 4, 5, 4, 3, 2, 3, -1)
        fun addNote(freq: Double, startSec: Double, durSec: Double, amp: Double, soft: Boolean) {
            val start = (startSec * RATE).toInt()
            val len = (durSec * RATE).toInt()
            for (i in 0 until len) {
                val idx = (start + i) % total
                val t = i.toDouble() / RATE
                val attack = (i / (RATE * if (soft) 0.04 else 0.01)).coerceAtMost(1.0)
                val env = attack * exp(-(if (soft) 1.6 else 3.2) * i / len)
                out[idx] += amp * env * (sin(2 * PI * freq * t) + 0.15 * sin(6 * PI * freq * t))
            }
        }
        for (bar in 0 until bars) {
            val barStart = bar * 4 * beat
            val chord = chords[bar]
            // Bass
            addNote(chord[0] / 2, barStart, 4 * beat, 0.35, soft = true)
            // Hợp âm rải nốt móc đơn
            for (k in 0 until 8) {
                addNote(chord[k % 3] * if (k % 4 == 3) 2.0 else 1.0, barStart + k * beat / 2, beat * 0.9, 0.12, soft = false)
            }
            // Giai điệu 2 nốt/ô nhịp
            for (k in 0 until 2) {
                val m = melody[(bar * 2 + k) % melody.size]
                if (m >= 0) addNote(pentatonic[m], barStart + k * 2 * beat, 2 * beat, 0.16, soft = true)
            }
        }
        return toPcm(out, 0.22)
    }
}
