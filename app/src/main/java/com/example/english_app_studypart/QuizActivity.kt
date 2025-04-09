package com.example.english_app_studypart

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.english_app_studypart.databinding.ActivityQuizBinding
import com.example.english_app_studypart.datas.Word

// 퀴즈 화면: 전달받은 단어로 퀴즈를 표시하고, 결과를 WordManager에 업데이트 후 다음 화면 이동
class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private var currentQuizWord: Word? = null // 현재 퀴즈 단어
    private val optionButtons = mutableListOf<Button>() // 선택지 버튼 리스트
    private var correctAnswer: String? = null // 정답 (뜻)
    private var isAnswerable = true // 중복 정답 체크 방지 플래그
    private val handler = Handler(Looper.getMainLooper()) // 결과 표시 후 다음 화면 이동 딜레이용

    companion object {
        const val EXTRA_WORD_ID = "extra_word_id" // Intent로 전달받을 단어 ID 키
        private const val TAG = "QuizActivity"
        private const val FEEDBACK_DELAY_MS = 1000L // 정답/오답 피드백 보여주는 시간 (1초)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)
        optionButtons.addAll(listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4))
        Log.d(TAG, "onCreate called.")

        // Intent에서 퀴즈 단어 ID 가져오기
        val wordId = intent.getIntExtra(EXTRA_WORD_ID, -1)
        if (wordId == -1) {
            Log.e(TAG, "Word ID not found in Intent extras.")
            Toast.makeText(this, "오류: 퀴즈 단어 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // WordManager를 통해 해당 ID의 Word 객체 찾기
        currentQuizWord = WordManager.findWordById(wordId)

        if (currentQuizWord == null) {
            Log.e(TAG, "Word object not found in WordManager for ID: $wordId")
            Toast.makeText(this, "오류: 퀴즈 단어를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- 시나리오 반영: 전달받은 단어로 퀴즈 구성 ---
        setupQuizUI(currentQuizWord!!) // 퀴즈 UI 설정 (단어 표시, 선택지 생성)
        setupOptionButtonsListener() // 버튼 클릭 리스너 설정
    }

    // 퀴즈 UI 설정 (단어 표시, 선택지 생성 및 배치)
    private fun setupQuizUI(quizWord: Word) {
        Log.d(TAG, "Setting up quiz for Word ID: ${quizWord.id} ('${quizWord.word}')")
        binding.tvQuizWord.text = quizWord.word
        // --- 시나리오 반영: 카운터 표시 (현재 단어의 상태) ---
        binding.tvQuizCounter.text = "맞춘 횟수: ${quizWord.correctCount}/3" // 기존 로직 유지
        correctAnswer = quizWord.meaning // 정답 저장

        // --- 시나리오 반영: 선택지 생성 로직 (기존 로직 활용) ---
        // 오답 후보: 전체 단어 목록에서 현재 단어 제외하고 뜻만 추출
        var incorrectOptions = WordManager.getAllWords()
            .filter { it.id != quizWord.id } // 현재 단어 제외
            .map { it.meaning } // 뜻만 추출
            .distinct() // 중복 뜻 제거
            .shuffled() // 랜덤 섞기
            .take(3) // 3개 선택

        // 오답 후보가 3개 미만일 경우 처리 (기존 로직 보완)
        if (incorrectOptions.size < 3) {
            Log.w(TAG, "Not enough distinct incorrect options available! Adding placeholders.")
            val needed = 3 - incorrectOptions.size
            val placeholders = List(needed) { "오답 ${it + 1}" } // 고유한 Placeholder 생성 시도
            incorrectOptions = (incorrectOptions + placeholders).distinct().take(3)
            // 만약 placeholder와 정답이 겹치는 극단적 경우 발생 가능성 있음
        }

        // 정답 + 오답 합쳐서 섞고 버튼에 표시
        val options = (incorrectOptions + correctAnswer!!).shuffled()
        if (options.size >= 4) { // 4개 이상이어야 함 (정상: 4개)
            optionButtons[0].text = options[0]
            optionButtons[1].text = options[1]
            optionButtons[2].text = options[2]
            optionButtons[3].text = options[3]
        } else {
            Log.e(TAG, "Cannot setup options, final options count is less than 4.")
            // 오류 처리: 버튼 비활성화 또는 기본 텍스트 설정
            optionButtons.forEach { it.text = "Error"; it.isEnabled = false }
        }

        // O/X 이미지 초기 상태: 숨김
        binding.ivResultMark.visibility = View.GONE
        isAnswerable = true // 다시 답변 가능 상태로
    }

    // 선택지 버튼 클릭 리스너 설정
    private fun setupOptionButtonsListener() {
        optionButtons.forEach { button ->
            button.setOnClickListener {
                if (isAnswerable) {
                    isAnswerable = false // 중복 클릭 방지
                    checkAnswer(button.text.toString())
                }
            }
        }
    }

    // 정답 확인 및 결과 처리
    private fun checkAnswer(selectedMeaning: String) {
        val quizWord = currentQuizWord ?: return // 현재 단어 없으면 처리 중단
        val isCorrect = (selectedMeaning == correctAnswer)

        Log.d(TAG, "Word ID ${quizWord.id} ('${quizWord.word}') - Selected: '$selectedMeaning', Correct: $isCorrect")

        // --- 시나리오 반영: WordManager 통해 상태 업데이트 ---
        // 정답 여부에 따라 WordManager의 correctCount 업데이트
        val updatedWord = WordManager.updateQuizCount(quizWord.id, isCorrect)

        // --- 시나리오 반영: 시각적 피드백 (O/X) ---
        showFeedback(isCorrect)

        // --- 시나리오 반영: 결과 반환 대신 다음 화면 직접 결정 및 실행 ---
        // 일정 시간(FEEDBACK_DELAY_MS) 후 다음 화면으로 이동
        handler.postDelayed({
            // WordManager에게 다음 Intent 요청
            // 오답인 경우: 방금 틀린 단어(updatedWord) 정보를 전달하여 강제 학습 유도
            // 정답인 경우: null을 전달하여 랜덤 진행
            val nextIntent = WordManager.getNextIntent(this, if (isCorrect) null else updatedWord)

            if (nextIntent != null) {
                // 다음 화면 시작
                startActivity(nextIntent)
            } else {
                // 학습 완료 또는 오류
                if (WordManager.isLearningComplete()) {
                    Log.i(TAG, "Learning finished from QuizActivity!")
                    Toast.makeText(this, "오늘의 학습 완료!", Toast.LENGTH_LONG).show()
                    finishAffinity() // 예: 모든 관련 액티비티 종료
                } else {
                    Log.e(TAG, "Error: Could not determine next activity from QuizActivity.")
                    Toast.makeText(this, "오류: 다음 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
                    // finish()
                }
            }
            // 현재 QuizActivity 종료
            finish()
        }, FEEDBACK_DELAY_MS)
    }

    // 정답/오답 피드백 표시 (O/X 이미지)
    private fun showFeedback(isCorrect: Boolean) {
        binding.ivResultMark.setImageResource(
            // R.drawable.ic_correct, R.drawable.ic_incorrect 리소스 필요
            if (isCorrect) R.drawable.ic_correct
            else R.drawable.ic_wrong
        )
        binding.ivResultMark.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        // Handler 메모리 누수 방지
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "onDestroy called.")
    }
}