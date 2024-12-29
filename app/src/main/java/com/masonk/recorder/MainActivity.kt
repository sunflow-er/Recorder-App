package com.masonk.recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.Settings

import com.masonk.recorder.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity(), OnTimerTickListener {
    private lateinit var binding: ActivityMainBinding

    // 권한 요청 결과를 식벽하기 위한 코드 (static-final)
    companion object {
        private const val REQUEST_RECORD_AUDIO_CODE = 200
    }

    // 현재 상태를 나타내는 열거형 상수를 그룹화한 enum 클래스
    // RELEASE -> RECORDING -> RELEASE
    // RELEASE -> PLAYING -> RELEASE
    private enum class State {
        RELEASE, RECORDING, PLAYING
    }

    // 초기 state는 RELEASE
    private var state: State = State.RELEASE

    // 오디오(또는 비디오)를 녹음하는데 사용되는 객체, 초기는 null
    private var recorder: MediaRecorder? = null

    // 오디오(또는 비디오)를 재생하는 데 사용되는 객체, 초기는 null
    private var player: MediaPlayer? = null

    // 녹음된 파일의 경로를 나타내는 변수
    private var filePath: String = ""

    // 타이머 객채
    private lateinit var timer : Timer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 녹음을 저장할 파일의 경로
        // 외부 저장소에 위치, 앱별로 분리된 디렉토리, 임시 데이터 저장에 사용, 앱 제거 시 디렉토리 내용도 함께 제거
        filePath = "${externalCacheDir?.absolutePath}/audio_record_test.3gp"

        // Timer 객체 생성
        timer = Timer(this)

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

                else -> { // 현재 PLAYING 상태이면
                    // 아무것도 안함
                }
            }
        }

        // play 버튼을 클릭했을 때
        binding.playButton.setOnClickListener {
            when (state) {
                State.RELEASE -> {
                    // 재생 시작
                    startPlaying()
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
                    stopPlaying()
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
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> { // 권한이 이미 부여된 경우
                // 녹음 시작
                startRecording()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            ) -> { // 사용자가 이전에 권한 요청 거부했지만, 권한 요청 이유를 설명해야 하는 경우
                // 권한이 필요한 이유를 설명하는 다이얼로그 표시
                showRequestPermissionRationaleDialog()
            }
            else -> { // 권한이 부여되지 않았고, 설명이 필요하지 않은 경우
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO_CODE
                )
            }
        }
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
            } catch (e: IOException) {
                Log.e("APP", "MediaRecorder prepare() failed $e")
            }

            // 녹음 시작, 오디오 데이터를 캡처하고 파일에 저장
            start()
        }

        // 진폭 데이터 초기화
        binding.waveFormView.clearData()
        
        // 타이머 시작
        timer.start()

        // 녹음 버튼 설정
        binding.recordButton.apply {
            // 정지 이미지로 변경
            setImageDrawable(
                ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.baseline_stop_24
                )
            )

            // 이미지 색을 검정색으로 변경
            imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.black))
        }

        // 재생 버튼 비활성화
        disableButton(binding.playButton)

        // (재생) 정지 버튼 비활성화
        disableButton(binding.stopButton)
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

        // 타이머 정지
        timer.stop()

        // 녹음 버튼 설정
        binding.recordButton.apply {
            // 녹음 이미지로 복구
            setImageDrawable(
                ContextCompat.getDrawable(
                    this@MainActivity,
                    R.drawable.baseline_fiber_manual_record_24
                )
            )

            // 이미지 색을 빨간색으로 복구
            imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, R.color.red))
        }

        // 재생 버튼 활성화
        enableButton(binding.playButton)

        // (재생) 정지 버튼 활성화
        enableButton(binding.stopButton)
    }

    // 재생 시작
    private fun startPlaying() {
        // 현재 상태를 PLAYING으로 변경
        state = State.PLAYING

        // player 객체 설정 및 재생, 재생 완료 시 종료
        player = MediaPlayer().apply {
            try {
                // 재생할 녹음 파일의 경로 설정
                setDataSource(filePath)

                // 녹음 파일 준비
                prepare()
            } catch (e: IOException) {
                Log.e("APP", "MediaPlayer prepare() failed $e")
            }

            // 재생 시작
            start()

            // 재생 완료 시 종료
            setOnCompletionListener {
                stopPlaying()
            }
        }

        // 녹음 파형 초기화
        binding.waveFormView.clearWave()

        // 타이머 시작
        timer.start()

        // 녹음 버튼 비활성화
        disableButton(binding.recordButton)
    }

    // 재생 종료
    private fun stopPlaying() {
        // 현재 상태를 RELEASE로 변경
        state = State.RELEASE

        // 객체가 사용하던 자원 해제
        player?.release()

        player = null

        // 타이머 정지
        timer.stop()

        // 녹음 버튼 활성화
        enableButton(binding.recordButton)
    }

    // 버튼을 활성화하는 함수
    private fun enableButton(button: View) {
        button.apply {
            isEnabled = true
            alpha = 1.0f
        }
    }

    // 버튼을 비활성화하는 함수
    private fun disableButton(button: View) {
        button.apply {
            isEnabled = false
            alpha = 0.3f
        }
    }

    override fun onTick(duration: Long) {
        val milliSecond = duration % 1000
        val second = (duration / 1000) % 60
        val minute = (duration / 1000) / 60

        binding.timerTextView.text = String.format("%02d:%02d:%02d", minute, second, milliSecond / 10)

        when(state) {
            State.RECORDING -> {
                binding.waveFormView.addAmplitude(recorder?.maxAmplitude?.toFloat() ?: 0f)
            }
            State.PLAYING -> {
                binding.waveFormView.replayAmplitude()
            }
            else -> {

            }
        }
    }

    // 권한 요청 결과를 받는 함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 권한 요청 결과
        val isGranted =
            (requestCode == REQUEST_RECORD_AUDIO_CODE) && (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)

        if (isGranted) { // 승인
            startRecording() // 녹음 시작
        } else { // 거절
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
            ) { // 권한이 필요한 이유를 설명하는 AlertDialog를 띄워야 하면
                // 관련 AlertDialog 띄우기
                showRequestPermissionRationaleDialog()
            } else {
                // 앱 설정 화면으로 직접 이동하는 다이얼로그 띄우기
                showRequestPermissionSettingDialog()
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
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO_CODE
                )
            }
            .setNegativeButton(getString(R.string.request_permission_rationale_negative)) { dialogInterface, _ ->
                // 취소
                dialogInterface.cancel()
            }.show()
    }

    // 앱 설정 화면으로 직접 이동하는 다이얼로그 띄우기
    private fun showRequestPermissionSettingDialog() {
        AlertDialog.Builder(this)
            // Builder 패턴
            // set, set, set, ... -> build / show
            .setMessage(getString(R.string.request_permission_setting_message))
            .setPositiveButton(getString(R.string.request_permission_setting_positive)) { _, _ ->
                // 앱 설정으로 이동
                navigateToAppSetting()
            }
            .setNegativeButton(getString(R.string.request_permission_setting_negative)) { dialogInterface, _ ->
                // 취소
                dialogInterface.cancel()
            }.show()
    }

    // 앱 설정 화면(액티비티)로 이동
    private fun navigateToAppSetting() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS // 특정 애플리케이션의 세부 정보 화면을 나타내는 상수
        ).apply {
            // 특정 애플리케이션 설정
            data = Uri.fromParts(
                "package", // URI의 스키마
                packageName, // 현재 애플리케이션의 패키지 이름
                null // URI의 나머지 부분
            )
        }

        // 액티비티 이동
        startActivity(intent)
    }
}