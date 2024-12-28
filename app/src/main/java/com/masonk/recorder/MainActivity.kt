package com.masonk.recorder

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.masonk.recorder.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // 권한 요청 결과를 식벽하기 위한 코드 (static-final)
    companion object {
        private  const val  REQUEST_RECORD_AUDIO_CODE = 200
    }

    // 현재 상태를 나타내는 열거형 상수를 그룹화한 enum 클래스
    // RELEASE -> RECORDING -> RELEASE
    // RELEASE -> PLAYING -> RELEASE
    private enum class State {
        RELEASE, RECORDING, PLAYING
    }


    // 초기 state는 RELEASE
    private var state: State = State.RELEASE

    // 오디오를 녹음하는데 사용되는 객체, 초기는 null
    private var recorder: MediaRecorder? = null

    // 오디오를 재생하는 데 사용되는 객체, 초기는 null
    private var player: MediaPlayer? = null

    // 녹음된 파일의 경로를 나타내는 변수
    private var filePath: String = ""


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
                    stopRecording()
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

    // 현재 권한 허용 상태를 확인하고 권한을 요청, 녹음을 시작하는 함수
    private fun record() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> { // 권한이 이미 부여된 경우
                // 녹음 시작
                startRecording()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) -> { // 사용자가 이전에 권한 요청 거부했지만, 권한 요청 이유를 설명해야 하는 경우
                // 권한이 필요한 이유를 설명하는 다이얼로그 표시
                showRequestPermissionRationaleDialog()
            }
            else -> { // 권한이 부여되지 않았고, 설명이 필요하지 않은 경우
                // 권한 요청
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_CODE)
            }
        }
    }

    // 권한이 필요한 이유를 설명하는 AlertDialog 띄우기
    private fun showRequestPermissionRationaleDialog() {

        AlertDialog.Builder(this)
            // Builder 패턴
            // set, set, set, ... -> build / show
            .setMessage(getString(R.string.request_permission_rationale_message))
            .setPositiveButton(getString(R.string.request_permission_rationale_positive)) { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_CODE)
            }.setNegativeButton(getString(R.string.request_permission_rationale_negative)) { dialogInterface, _ ->
                dialogInterface.cancel()
            }.show()
    }

    // 녹음 시작
    private fun startRecording() {
        // 현재 상태를 RECORDING으로 변경
        state = State.RECORDING

        // recorder 객체 설정 및 녹음
        recorder = MediaRecorder().apply {
            // 오디소 소스 설정
            setAudioSource(MediaRecorder.AudioSource.MIC)

            // 녹음된 파일의 경로 설정
            setOutputFile(filePath)

            // 출력 형식 설정
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)

            // 오디오 인코더 설정
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                // 녹음 준비
                prepare()
            } catch(e: IOException) {
                Log.e("APP", "prepare() failed $e")
            }

            // 녹음 시작, 오디오 데이터를 캡처하고 파일에 저장
            start()
        }

        // 녹음 버튼 설정
        binding.recordButton.apply {
            // 정지 이미지로 변경
            setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.baseline_stop_24))

            // 이미지 색을 검정색으로 변경
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.black))
        }
        
        // 재생 버튼 설정
        binding.playButton.apply {
            // 버튼 비활성화
            isEnabled = false
            
            // 투명도 조절
            alpha = 0.3f
        }

    }
    
    // 녹음 정지 및 종료
    private fun stopRecording() {
        recorder?.apply {
            // 현재 진행 중인 녹음 중지, 녹음이 완료되고 파일 저장
            stop()
            
            // 객체가 사용하던 자원 해제
            release()
        }

        recorder = null

        // 현재 상태를 RELEASE로 변경
        state = State.RELEASE

        // 녹음 버튼 설정
        binding.recordButton.apply {
            // 녹음 이미지로 복구
            setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.baseline_fiber_manual_record_24))

            // 이미지 색을 빨간색으로 복구
            imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.red))
        }
        
        // 재생 버튼 설정
        binding.playButton.apply { 
            // 버튼 활성화
            isEnabled = true
            
            // 투명도 복구
            alpha = 1.0f
        }
    }

    
}