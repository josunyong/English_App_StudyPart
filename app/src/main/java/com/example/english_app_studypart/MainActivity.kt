package com.example.english_app_studypart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.english_app_studypart.databinding.ActivityMainBinding
import com.example.english_app_studypart.datas.WordData // WordData import

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private companion object { private const val TAG = "MainActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: Initializing application.")

        // --- 시나리오 반영: 앱 시작 시 WordManager 초기화 ---
        initializeWordManager()

        // --- 시나리오 반영: 버튼 클릭 시 첫 학습/퀴즈 화면 시작 ---
        // Study 버튼 클릭 시 학습 프로세스 시작
        binding.btnStudy.setOnClickListener {
            Log.d(TAG, "Study button clicked. Starting learning process.")
            startFirstScreen()
        }

        // --- 앱 시작 시 바로 학습 시작하려면 아래 주석 해제 ---
        // startFirstScreen()
        // finish() // MainActivity는 시작 역할만 하고 종료
    }

    // WordManager를 초기화하는 함수
    private fun initializeWordManager() {
        Log.d(TAG, "Initializing WordManager with data from WordData...")
        // WordData에 있는 단어 목록으로 WordManager 초기화
        WordManager.initialize(WordData.localWords.toList()) // 방어적 복사본 전달
    }

    // 첫 학습/퀴즈 화면을 시작하는 함수
    private fun startFirstScreen() {
        Log.d(TAG, "Requesting first intent from WordManager...")
        // WordManager에게 다음에 보여줄 화면의 Intent 요청 (오답 정보 없으므로 null)
        val firstIntent = WordManager.getNextIntent(this, null)

        if (firstIntent != null) {
            // 다음 화면 Intent가 있으면 실행
            startActivity(firstIntent)
            Log.d(TAG, "Starting activity: ${firstIntent.component?.className}")
            // finish() // 첫 화면 시작 후 MainActivity 종료 (선택 사항)
        } else {
            // 시작할 화면이 없는 경우 (단어가 없거나, 이미 다 완료된 경우)
            Log.w(TAG, "No initial activity determined. Learning might be complete or word list is empty.")
            if (WordManager.isLearningComplete()) {
                Toast.makeText(this, "모든 단어를 학습했습니다!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "학습할 단어가 없습니다.", Toast.LENGTH_SHORT).show()
            }
            // finish() // 이 경우에도 MainActivity 종료 결정 필요
        }
    }
}