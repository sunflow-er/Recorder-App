package com.masonk.recorder

import android.os.Handler
import android.os.Looper

class Timer(listener: OnTimerTickListener) {
    // 경과 시간
    private var duration = 0L

    private val handler = Handler( // 메시지와 실행 가능한 객체를 큐에 넣고, 특정 시점에 실행되도록 함
        Looper.getMainLooper() // 메인스레드의 Looper 반환, 큐에서 메시지를 꺼내 처리
    )

    // 스레드
    private val runnable: Runnable = object: Runnable {
        override fun run() {
            duration += 40L// 경과 시간 40ms추가
            listener.onTick(duration)

            handler.postDelayed(this, 40L) // 40ms 후에 다시 자기자신 실행
        }
    }

    // 타이머 시작
    fun start() {
        duration = 0

        handler.postDelayed(runnable, 40L)
    }

    // 타이머 종료
    fun stop() {
        // 메시지 큐에 있는 runnable 메시지 모두 삭제
        handler.removeCallbacks(runnable)

        // 경과 시간 초기화
        duration = 0
    }
}

interface OnTimerTickListener {
    fun onTick(duration: Long)
}