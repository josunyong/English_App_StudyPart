package com.example.english_app_studypart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.english_app_studypart.databinding.ActivityQuizBinding
import com.example.english_app_studypart.datas.Word
import com.example.english_app_studypart.datas.WordData
import com.example.english_app_studypart.R

/**
 * QuizActivity:
 * - Quiz 후보 풀은 이미 Study 화면에 한 번이라도 노출된(hasStudied == true) 단어들 중,
 *   correctCount가 0, 1, 또는 2 인 단어들을 대상으로 합니다.
 * - 정답 시 correctCount가 1 증가, 오답 시 1 감소(최소 0)하여 학습 상태를 변경합니다.
 * - 결과("correct"/"wrong") 및 wordId를 StudyActivity에 반환합니다.
 */
class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private var currentQuizWord: Word? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadRandomQuiz()
        setupOptionButtons()
    }

    private fun loadRandomQuiz() {
        // Quiz 후보: 이미 Study 화면에 나온(hasStudied == true) 단어 중 correctCount < 3 (즉, 0~2인 단어들)
        val candidateWords = WordData.localWords.filter { it.hasStudied && it.correctCount < 3 }
        if (candidateWords.isEmpty()) {
            finish()
            return
        }
        currentQuizWord = candidateWords.random()
        currentQuizWord?.let { quizWord ->
            binding.tvQuizCounter.text = "맞춘 횟수: ${quizWord.correctCount}/3"
            val correctAnswer = quizWord.meaning

            // 오답 옵션: 후보 단어들에서 정답이 아닌 의미들을 무작위로 3개 선정
            var wrongMeanings = candidateWords.map { it.meaning }
                .filter { it != correctAnswer }
                .shuffled()
                .take(3)
            // 후보가 부족하면 전체 단어 목록에서 추가 (중복 제거)
            if (wrongMeanings.size < 3) {
                val additional = WordData.localWords.map { it.meaning }
                    .filter { it != correctAnswer && it !in wrongMeanings }
                    .shuffled()
                wrongMeanings = (wrongMeanings + additional).distinct().take(3)
            }
            // 여전히 부족하면 임시 옵션("???")로 보충
            while (wrongMeanings.size < 3) {
                wrongMeanings = wrongMeanings + listOf("???")
            }
            val options = (wrongMeanings + correctAnswer).shuffled()
            // 최종 옵션이 4개 미만이면 보충
            val finalOptions = if (options.size < 4) {
                options + List(4 - options.size) { "???" }
            } else {
                options
            }
            binding.tvQuizWord.text = quizWord.word
            binding.btnOption1.text = finalOptions[0]
            binding.btnOption2.text = finalOptions[1]
            binding.btnOption3.text = finalOptions[2]
            binding.btnOption4.text = finalOptions[3]
        }
    }

    private fun setupOptionButtons() {
        binding.btnOption1.setOnClickListener { checkAnswer(binding.btnOption1.text.toString()) }
        binding.btnOption2.setOnClickListener { checkAnswer(binding.btnOption2.text.toString()) }
        binding.btnOption3.setOnClickListener { checkAnswer(binding.btnOption3.text.toString()) }
        binding.btnOption4.setOnClickListener { checkAnswer(binding.btnOption4.text.toString()) }
    }

    private fun checkAnswer(selectedMeaning: String) {
        val quizWord = currentQuizWord ?: return
        val isCorrect = quizWord.meaning == selectedMeaning

        if (isCorrect) {
            // 정답: correctCount 1 증가
            quizWord.correctCount++
            binding.tvQuizCounter.text = "맞춘 횟수: ${quizWord.correctCount}/3"
            binding.ivResultMark.setImageResource(R.drawable.ic_correct)
        } else {
            // 오답: correctCount 1 감소 (최소 0 유지)
            quizWord.correctCount = if (quizWord.correctCount > 0) quizWord.correctCount - 1 else 0
            binding.tvQuizCounter.text = "맞춘 횟수: ${quizWord.correctCount}/3"
            binding.ivResultMark.setImageResource(R.drawable.ic_wrong)
        }
        binding.ivResultMark.visibility = android.view.View.VISIBLE

        // 결과("correct"/"wrong") 및 wordId를 Intent에 담아 StudyActivity에 반환합니다.
        val resultIntent = Intent().apply {
            putExtra("wordId", quizWord.id)
            putExtra("quiz_result", if (isCorrect) "correct" else "wrong")
        }
        setResult(Activity.RESULT_OK, resultIntent)

        // 사용자에게 결과를 잠시 보여준 후 1초 후에 종료합니다.
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 1000)
    }
}
