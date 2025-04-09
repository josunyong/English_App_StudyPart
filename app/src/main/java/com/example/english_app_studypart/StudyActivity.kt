package com.example.english_app_studypart

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.english_app_studypart.databinding.ActivityStudyBinding
import com.example.english_app_studypart.datas.Word

// 학습 화면: 전달받은 단어를 보여주고 '학습됨' 상태로 변경 후, 다음 화면으로 이동
class StudyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudyBinding
    private var currentWord: Word? = null // 현재 화면에 표시 중인 단어

    companion object {
        const val EXTRA_WORD_ID = "extra_word_id" // Intent로 전달받을 단어 ID 키
        private const val TAG = "StudyActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate called.")

        // Intent에서 표시할 단어 ID 가져오기
        val wordId = intent.getIntExtra(EXTRA_WORD_ID, -1) // ID는 Int 타입
        if (wordId == -1) {
            // ID가 전달되지 않은 경우 오류 처리
            Log.e(TAG, "Word ID not found in Intent extras.")
            Toast.makeText(this, "오류: 표시할 단어 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish() // 액티비티 종료
            return
        }

        // WordManager를 통해 해당 ID의 Word 객체 찾기
        currentWord = WordManager.findWordById(wordId)

        if (currentWord == null) {
            // Word 객체를 찾지 못한 경우 오류 처리
            Log.e(TAG, "Word object not found in WordManager for ID: $wordId")
            Toast.makeText(this, "오류: 단어를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- 시나리오 반영: 단어 표시 및 학습 상태 변경 ---
        displayStudyWord(currentWord!!) // 찾은 단어를 화면에 표시

        // '넘기기' 버튼 클릭 리스너 설정
        binding.btnNext.setOnClickListener {
            Log.d(TAG, "Next button clicked for Word ID: ${currentWord?.id}")
            // WordManager를 통해 다음 화면 결정 및 이동
            goToNextScreen()
        }
    }

    // 화면에 단어와 뜻을 표시하고, '학습됨'(hasStudied=true) 상태로 변경
    private fun displayStudyWord(word: Word) {
        binding.tvWord.text = word.word
        binding.tvMeaning.text = word.meaning
        Log.d(TAG, "Displaying Study word: ID ${word.id} ('${word.word}'), Meaning: ${word.meaning}")

        // --- 시나리오 반영: 학습 상태 업데이트 ---
        // 이 화면에서 단어를 봤으므로 WordManager를 통해 hasStudied = true 로 설정
        WordManager.markAsStudied(word.id)
    }

    // 다음 화면으로 이동하는 함수
    private fun goToNextScreen() {
        // --- 시나리오 반영: 다음 화면 결정은 WordManager에 위임 ---
        // getNextIntent 호출 (이전 오답 정보 없으므로 null 전달)
        val nextIntent = WordManager.getNextIntent(this, null)

        if (nextIntent != null) {
            // 다음 화면 Intent가 있으면 실행
            startActivity(nextIntent)
        } else {
            // 다음 화면 Intent가 null이면 학습 완료 또는 오류
            if (WordManager.isLearningComplete()) {
                Log.i(TAG, "Learning finished!")
                Toast.makeText(this, "오늘의 학습 완료!", Toast.LENGTH_LONG).show()
                // TODO: 학습 완료 화면으로 이동하거나 앱 종료 등의 로직 추가
                finishAffinity() // 예: 모든 관련 액티비티 종료
            } else {
                Log.e(TAG, "Error: Could not determine next activity from StudyActivity.")
                Toast.makeText(this, "오류: 다음 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
                // finish() // 오류 시 현재 액티비티만 종료할 수도 있음
            }
        }
        // 현재 StudyActivity 종료 (다음 화면으로 넘어갔으므로)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called.")
    }
}