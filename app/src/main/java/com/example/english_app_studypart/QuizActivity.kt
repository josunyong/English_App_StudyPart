package com.example.english_app_studypart

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.example.english_app_studypart.databinding.ActivityQuizBinding
import com.example.english_app_studypart.datas.Word

// ... (다른 import 동일)

class QuizActivity : AppCompatActivity() {
    // ... (변수 선언 동일) ...
    companion object { const val EXTRA_WORD_ID = "extra_word_id"; private const val TAG = "QuizActivity"; private const val FEEDBACK_DELAY_MS = 1000L }
    private lateinit var binding: ActivityQuizBinding // activity_quiz 또는 quiz_layout에 맞는 바인딩
    private var currentQuizWord: Word? = null
    private val optionButtons = mutableListOf<Button>()
    private var correctAnswer: String? = null
    private var isAnswerable = true
    private val handler = Handler(Looper.getMainLooper())


    override fun onCreate(savedInstanceState: Bundle?) { /* 변경 없음 */
        super.onCreate(savedInstanceState); binding= ActivityQuizBinding.inflate(layoutInflater); setContentView(binding.root); optionButtons.addAll(listOf(binding.btnOption1,binding.btnOption2,binding.btnOption3,binding.btnOption4)); Log.d(TAG,"onCreate."); val wordId=intent.getIntExtra(EXTRA_WORD_ID,-1); if(wordId==-1){finish();return}; currentQuizWord=WordManager.findWordById(wordId); if(currentQuizWord==null){finish();return}; setupQuizUI(currentQuizWord!!); setupOptionButtonsListener()
    }
    private fun setupQuizUI(quizWord: Word) { /* 변경 없음 */
        Log.d(TAG,"Setting quiz..."); binding.tvQuizWord.text=quizWord.word; binding.tvQuizCounter.text="맞춘 횟수: ${quizWord.correctCount}/3"; correctAnswer=quizWord.meaning; var incorrectOptions=WordManager.getAllWords().filter{it.id!=quizWord.id}.map{it.meaning}.distinct().shuffled().take(3); if(incorrectOptions.size<3){val needed=3-incorrectOptions.size; val placeholders=List(needed){"오답 ${it+1}"}.filter{it!=correctAnswer}; incorrectOptions=(incorrectOptions+placeholders).distinct().take(3)}; val options=(incorrectOptions+correctAnswer!!).shuffled(); if(options.size>=optionButtons.size){optionButtons.forEachIndexed{ i, btn->btn.text=options[i];btn.isEnabled=true}} else {optionButtons.forEach{it.text="Err";it.isEnabled=false}}; binding.ivResultMark.visibility=View.GONE; isAnswerable=true
    }
    private fun setupOptionButtonsListener() { /* 변경 없음 */
        optionButtons.forEach{button->button.setOnClickListener{if(isAnswerable&&button.isEnabled){isAnswerable=false; checkAnswer(button.text.toString())}}}
    }

    // 정답 확인 및 결과 처리 (다음 화면 이동 로직 수정)
    private fun checkAnswer(selectedMeaning: String) {
        val quizWord = currentQuizWord ?: return
        val isCorrect = (selectedMeaning == correctAnswer)
        Log.d(TAG, "ID ${quizWord.id} - Correct: $isCorrect")

        val updatedWord = WordManager.updateQuizCount(quizWord.id, isCorrect)

        if (updatedWord != null) binding.tvQuizCounter.text = "맞춘 횟수: ${updatedWord.correctCount}/3" // 카운터 즉시 업데이트
        else Log.w(TAG, "Updated word null.")

        showFeedback(isCorrect) // O/X 표시

        handler.postDelayed({
            // ⭐ 수정: WordManager의 getNextIntent 호출
            val nextIntent = WordManager.getNextIntent(this, if (isCorrect) null else updatedWord)

            if (nextIntent != null) {
                startActivity(nextIntent)
                finish() // 현재 액티비티 종료
            } else {
                // 다음 화면 없음 (학습 완료 또는 오류)
                if (WordManager.isLearningComplete()) {
                    // 학습 완료 시 MainActivity로 이동
                    Log.i(TAG, "Learning complete! Returning to MainActivity.")
                    Toast.makeText(this, "오늘의 학습 완료!", Toast.LENGTH_LONG).show()
                    val mainIntent = Intent(this, MainActivity::class.java)
                    mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(mainIntent)
                    finish()
                } else {
                    // 완료 아닌데 인텐트 없는 오류 상황
                    Log.e(TAG, "Error: No next intent but learning not complete.")
                    Toast.makeText(this, "오류: 다음 진행 불가", Toast.LENGTH_SHORT).show()
                    finish() // 오류 시에도 종료
                }
            }
        }, FEEDBACK_DELAY_MS)
    }

    private fun showFeedback(isCorrect: Boolean) { /* 변경 없음 */
        binding.ivResultMark.setImageResource(if(isCorrect)R.drawable.ic_correct else R.drawable.ic_wrong); binding.ivResultMark.visibility=
            View.VISIBLE
    }
    override fun onDestroy() { /* 변경 없음 */ super.onDestroy(); handler.removeCallbacksAndMessages(null); Log.d(TAG,"onDestroy.") }
}