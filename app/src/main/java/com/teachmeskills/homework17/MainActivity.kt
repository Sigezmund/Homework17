package com.teachmeskills.homework17

import android.content.Context
import android.os.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var startTimeInMillis: Long = 0L
    private var durationInMillis: Long = 0L
    private var timerState = TimerState.CREATED
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val timerText by lazy { findViewById<TextView>(R.id.text) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        savedInstanceState?.let {
            startTimeInMillis = savedInstanceState.getLong(EXTRA_START)
            durationInMillis = savedInstanceState.getLong(EXTRA_DURATION)
            timerState = savedInstanceState.getSerializable(EXTRA_STATE) as TimerState
            when (timerState) {
                TimerState.STARTED -> launchTimerCountdown(startTimeInMillis, durationInMillis)
                TimerState.PAUSED -> printTime(durationInMillis)
                TimerState.ENDED -> printEndMessage()
                TimerState.CREATED -> Unit
            }
        }
        findViewById<Button>(R.id.start).setOnClickListener {
            when (timerState) {
                TimerState.PAUSED -> start(durationInMillis)
                TimerState.ENDED, TimerState.CREATED -> start(
                    TimeUnit.SECONDS.toMillis(TIMER_DURATION_IN_SECONDS)
                )
                TimerState.STARTED -> {
                    Toast.makeText(this, R.string.timer_already_active, Toast.LENGTH_SHORT).show()
                }
            }

        }

        findViewById<Button>(R.id.pause).setOnClickListener {
            pause()
        }

        findViewById<Button>(R.id.reset).setOnClickListener {
            reset()
        }

    }

    private fun reset() {
        timerState = TimerState.CREATED
        startTimeInMillis = 0
        durationInMillis = 0
        timerText.text = ""
        job?.cancel()
    }

    private fun pause() {
        if (timerState == TimerState.STARTED) {
            durationInMillis = durationInMillis - SystemClock.elapsedRealtime() + startTimeInMillis
            startTimeInMillis = 0
            timerState = TimerState.PAUSED
            job?.cancel()
            printTime(durationInMillis)
        }
    }

    private fun start(durationInMillis: Long) {
        this.startTimeInMillis = SystemClock.elapsedRealtime()
        this.durationInMillis = durationInMillis
        this.timerState = TimerState.STARTED
        launchTimerCountdown(startTimeInMillis, durationInMillis)
    }

    private fun launchTimerCountdown(startTimeInMillis: Long, durationInMillis: Long) {
        job?.cancel()
        job = scope.launch {
            while (startTimeInMillis > SystemClock.elapsedRealtime() - durationInMillis) {
                printTime(startTimeInMillis - SystemClock.elapsedRealtime() + durationInMillis)
                delay(TimeUnit.SECONDS.toMillis(1))
            }

            handleTimerEnd()

        }
    }

    private fun printTime(timeInMillis: Long) {
        timerText.text =
            (TimeUnit.MILLISECONDS
                .toSeconds(timeInMillis) + 1)
                .toString()
    }

    private fun printEndMessage() {
        timerText.text = this@MainActivity.getString(R.string.timer_end)
    }

    private fun handleTimerEnd() {
        printEndMessage()
        timerState = TimerState.ENDED
        vibrate()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator;
        } else {
            this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    500,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            );
        } else {
            //deprecated in API 26
            vibrator.vibrate(500);
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // Остановка выполнения таймера
        scope.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_START, startTimeInMillis)
        outState.putLong(EXTRA_DURATION, durationInMillis)
        outState.putSerializable(EXTRA_STATE, timerState)
    }


    companion object {
        private const val TIMER_DURATION_IN_SECONDS = 10L
        private const val EXTRA_START = "extra_start"
        private const val EXTRA_DURATION = "extra_duration"
        private const val EXTRA_STATE = "extra_state"
    }
}