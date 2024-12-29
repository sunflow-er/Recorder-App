package com.masonk.recorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveFormView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // 진폭 데이터를 담을 리스트
    private val ampList = mutableListOf<Float>()

    // 진폭 데이터를 바탕으로 생성된 rectF 객체들을 담을 리스트
    private val rectFList = mutableListOf<RectF>() // RectF : four float coordinates for a rectangle.

    // rectF 한 개의 너비
    private val rectFWidth = 15f

    // tick
    private var tick = 0
    
    // 빨간색 페인트
    private val redPaint = Paint().apply {
        color = Color.RED
    }

    // 뷰 그리기
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (rectF in rectFList) {
            canvas.drawRect(rectF, redPaint)
        }
    }

    // 녹음 시 파형 그리기
    fun addAmplitude(maxAmplitude: Float) {
        // 화면(View)에서 보여줄 수 있는 rectF의 최대 개수
        val maxRectFCount = (this.width / rectFWidth).toInt()

        // 진폭값 보정
        val amplitude = (maxAmplitude / Short.MAX_VALUE) * this.height * 0.8f

        // 진폭 데이터 리스트에 추가
        ampList.add(amplitude)
        
        // 최근 maxRectFCount 개의 진폭 데이터 가져오기
        val amps = ampList.takeLast(maxRectFCount)

        // rectF 리스트 초기화
        rectFList.clear()

        // 최근 진폭 데이터를 바탕으로 rectF 생성 및 리스트에 추가
        for ((i, amp) in amps.withIndex()) {
            val rectF = contructRectF(i, amp)

            rectFList.add(rectF)
        }

        // 뷰 다시 그리기, onDraw() 호출
        invalidate()
    }

    fun replayAmplitude() {
        // 화면(View)에서 보여줄 수 있는 rectF의 최대 개수
        val maxRectFCount = (this.width / rectFWidth).toInt()

        val amps = ampList
            .take(tick) // tick 개수만큼의 요소를 가져온 뒤
            .takeLast(maxRectFCount) // 가져온 요소들 중 마지막 maxRectFCount 개의 요소를 가져옴

        // rectF 리스트 초기화
        rectFList.clear()

        for ((i, amp) in amps.withIndex()) {
            val rectF = contructRectF(i, amp)

            rectFList.add(rectF)
        }

        tick++
        
        // 뷰 다시 그리기
        invalidate()
    }

    // 진폭 데이터 리스트 초기화
    fun clearData() {
        ampList.clear()
    }

    // 녹음 파형 초기화
    fun clearWave() {
        rectFList.clear()
        tick = 0
        invalidate()
    }

    // 주어진 인덱스와 진폭 정보를 활용하여 위치를 지정한 RectF 객체를 반환
    fun contructRectF(i: Int, amp: Float) : RectF {
        return RectF().apply {
            top = (this@WaveFormView.height / 2) - (amp / 2) + 5
            bottom = this.top + amp + 5
            left = i * rectFWidth
            right = this.left + (rectFWidth - 5f)
        }
    }
}