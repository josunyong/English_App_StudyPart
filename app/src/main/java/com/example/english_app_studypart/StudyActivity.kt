package com.example.english_app_studypart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.english_app_studypart.databinding.ActivityStudyBinding // 사용자의 MainActivity 코드에 맞춰 바인딩 클래스 유지 또는 수정 필요
import com.example.english_app_studypart.datas.Word

class StudyActivity : AppCompatActivity() {
    // XML 파일명(activity_study.xml 가정)에 맞춘 바인딩
    private lateinit var binding: ActivityStudyBinding
    private var currentWord: Word? = null

    companion object { const val EXTRA_WORD_ID = "extra_word_id"; private const val TAG = "StudyActivity"}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // study_layout.xml 또는 activity_study.xml 에 맞는 바인딩 사용
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate called.")

        val wordId = intent.getIntExtra(EXTRA_WORD_ID, -1)
        if (wordId == -1) { /* 오류 처리 */ finish(); return }
        currentWord = WordManager.findWordById(wordId)
        if (currentWord == null) { /* 오류 처리 */ finish(); return }

        displayStudyWord(currentWord!!)

        binding.btnNext.setOnClickListener {
            Log.d(TAG, "Next clicked for ID: ${currentWord?.id}")
            goToNextScreen()
        }
    }

    private fun displayStudyWord(word: Word) {
        binding.tvWord.text = word.word; binding.tvMeaning.text = word.meaning
        // 로그에 노출 횟수 표시 (디버깅용)
        Log.d(TAG, "Display Study: ID ${word.id} ('${word.word}') (App: ${word.studyScreenAppearances})")
        WordManager.markAsStudied(word.id) // hasStudied 설정
    }

    // 다음 학습/퀴즈 화면 또는 메인 화면으로 이동
    private fun goToNextScreen() {
        // ⭐ 수정: WordManager의 getNextIntent 호출
        val nextIntent = WordManager.getNextIntent(this, null)

        if (nextIntent != null) {
            startActivity(nextIntent)
            finish() // 현재 액티비티 종료
        } else {
            // 다음 화면이 없는 경우 (학습 완료 또는 오류)
            if (WordManager.isLearningComplete()) {
                // 학습 완료 시 MainActivity로 이동
                Log.i(TAG, "Learning complete! Returning to MainActivity.")
                Toast.makeText(this, "오늘의 학습 완료!", Toast.LENGTH_LONG).show()
                val mainIntent = Intent(this, MainActivity::class.java)
                mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(mainIntent)
                finish()
            } else {
                // 완료 아닌데 다음 인텐트 없는 경우 (오류 가능성)
                Log.e(TAG, "Error: No next intent but learning not complete.")
                Toast.makeText(this, "오류: 다음 진행 불가", Toast.LENGTH_SHORT).show()
                finish() // 오류 시에도 일단 종료
            }
        }
    }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy.") }
}