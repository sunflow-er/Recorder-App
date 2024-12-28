package com.masonk.recorder

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.masonk.recorder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    // 현재 상태를 나타내는 열거형 상수를 그룹화한 enum 클래스
    // RELEASE -> RECORDING -> RELEASE
    // RELEASE -> PLAYING -> RELEASE
    private enum class State {
        RELEASE, RECORDING, PLAYING
    }

    private lateinit var binding: ActivityMainBinding

    // 초기 state는 RELEASE
    private var state: State = State.RELEASE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // record 버튼 클릭했을 때
        binding.recordButton.setOnClickListener {
            when (state) {
                State.RELEASE -> { // 현재 RELEASE 상태이면
                    // 녹음 권한 요청 및 녹음 시작
                    record()
                }

                State.RECORDING -> { // 현재 RECORDING 상태이면
                    // 녹음 종료
                    onRecord(false)
                }

                State.PLAYING -> { // 현재 PLAYING 상태이면

                }
            }
        }

        // play 버튼을 클릭했을 때
        binding.playButton.setOnClickListener {
            when (state) {
                State.RELEASE -> {
                    // 재생 시작
                    onPlay(true)
                }

                else -> { // RECORDING, PLAYING
                    // 아무것도 안함
                }
            }
        }

        // stop 버튼을 클릭했을 때
        binding.stopButton.setOnClickListener {
            when (state) {
                State.PLAYING -> {
                    // 재생 종료
                    onPlay(false)
                }

                else -> { // RELEASE, RECORDING
                    // 아무것도 안함
                }
            }
        }

    }

    private fun record() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.
            )
        }
    }
}