package com.example.english_app_studypart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.english_app_studypart.databinding.ActivityStudyBinding
import com.example.english_app_studypart.datas.Word
import com.example.english_app_studypart.datas.WordData

/**
 * StudyActivity:
 *
 * [학습 규칙]
 * 1. Study 후보: 단어의 correctCount가 **0**인 단어들 → Study 화면에만 노출됨.
 *    - 단어가 Study 화면에 한 번이라도 표시되면 displayStudyWord()에서 hasStudied를 true로 설정합니다.
 * 2. Quiz 후보: 단어가 한 번이라도 Study 화면에 나타난(hasStudied == true) 단어 중,
 *    correctCount가 **0 ≤ correctCount ≤ 2** 인 단어들 → Quiz로 진행됩니다.
 * 3. 완전 암기: 단어가 hasStudied == true이고 correctCount가 **3**이면 학습 대상에서 제외됩니다.
 *
 * [랜덤 전개 구조]
 * - 버튼 클릭 시, Study 후보(단어의 correctCount == 0) 중 현재 단어를 보여주다가,
 *   일정 확률(QUIZ_PROBABILITY)로 해당 단어를 Quiz로 전환하여 정답/오답에 따라 상태가 변하게 합니다.
 * - (수정 1) 만약 Quiz에서 오답이면 ForcedStudy를 통해 바로 그 단어를 보여줍니다.
 * - (수정 2) Study 후보 풀은 오직 correctCount가 0인 단어만 포함됩니다.
 *   만약 후보 풀이 없으면 (예: 모든 단어의 correctCount가 0보다 클 때) Quiz 화면으로만 진행합니다.
 */
class StudyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudyBinding

    // Study 후보 풀: 단어의 correctCount가 0인 단어들만(초기 구성 시 필터링)
    private var studyList: MutableList<Word> = mutableListOf()
    private var currentIndex = 0
    private var lastQuizWord: Word? = null

    // 확률: Study 화면에서 넘기기 대신 Quiz를 실행할 확률
    private val QUIZ_PROBABILITY = 0.4
    private var isProcessing = false

    companion object {
        private const val TAG = "StudyActivity"
    }

    // QuizActivity 결과 수신 launcher
    private val quizActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val quizResult = data?.getStringExtra("quiz_result")
                val returnedWordId = data?.getIntExtra("wordId", -1) ?: -1

                lastQuizWord?.let { word ->
                    if (word.id != returnedWordId) {
                        Log.e(TAG, "Returned wordId ($returnedWordId) does not match lastQuizWord.id (${word.id})")
                    } else {
                        when (quizResult) {
                            "correct" -> {
                                Log.d(TAG, "Word '${word.word}' answered correctly (count=${word.correctCount}).")
                                // Quiz 정답 처리된 단어는 Study 후보에서 제거합니다.
                                removeWord(word)
                                displayNextWord()
                            }
                            "wrong" -> {
                                Log.d(TAG, "Word '${word.word}' answered wrongly (count=${word.correctCount}). Launching ForcedStudy.")
                                // (수정 1) 오답이면 무조건 해당 단어를 ForcedStudy를 통해 바로 보여줍니다.
                                launchForcedStudy(word)
                            }
                        }
                    }
                }
            }
            isProcessing = false
        }

    // ForcedStudyActivity 결과 수신 launcher
    private val forcedStudyLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            lastQuizWord?.let { word ->
                Log.d(TAG, "ForcedStudy returned for '${word.word}' (count=${word.correctCount}).")
                // (수정 1) 오답 후 ForcedStudy에서 종료되면, 무조건 바로 그 단어를 Study 화면에 보여줍니다.
                displayStudyWord(word)
            }
            isProcessing = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Study 후보 초기 구성: correctCount가 0인 단어들만 포함합니다.
        studyList = WordData.localWords.filter { it.correctCount == 0 }.toMutableList()

        // 만약 전체 단어 중 correctCount가 3 미만인 단어가 없다면 학습을 종료합니다.
        if (WordData.localWords.filter { it.correctCount < 3 }.isEmpty()) {
            Toast.makeText(this, "오늘의 학습완료!", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        currentIndex = 0

        // Study 후보가 비어 있으면 바로 Quiz 후보에서 단어를 선택해 Quiz로 진행합니다.
        if (studyList.isNotEmpty()) {
            displayStudyWord(studyList[currentIndex])
        } else {
            val quizCandidates = WordData.localWords.filter { it.hasStudied && it.correctCount in 0..2 }
            if (quizCandidates.isNotEmpty()) {
                launchQuiz(quizCandidates.random())
            } else {
                Toast.makeText(this, "오늘의 학습완료!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        // 버튼 클릭 시 Study 모드 또는 Quiz 모드를 결정하는 랜덤 처리
        binding.btnNext.setOnClickListener {
            if (isProcessing) return@setOnClickListener

            // 만약 모든 단어가 암기되어 있다면 종료.
            if (WordData.localWords.filter { it.correctCount < 3 }.isEmpty()) {
                Toast.makeText(this, "오늘의 학습완료!", Toast.LENGTH_LONG).show()
                finish()
                return@setOnClickListener
            }
            isProcessing = true

            if (studyList.isNotEmpty()) {
                val currentWord = studyList[currentIndex]
                if (Math.random() < QUIZ_PROBABILITY) {
                    Log.d(TAG, "Launching QuizActivity for studied word '${currentWord.word}'")
                    launchQuiz(currentWord)
                } else {
                    // "넘기기" 처리: 다음 Study 후보를 표시
                    currentIndex++
                    if (currentIndex < studyList.size) {
                        displayStudyWord(studyList[currentIndex])
                    } else {
                        // (수정 2) Study 후보가 모두 소진되었으면, 재계산하여 오직 correctCount가 0인 단어만을 Study 후보로 사용.
                        displayNextWord()
                    }
                    isProcessing = false
                }
            } else {
                // Study 후보가 없다면 Quiz 후보 풀에서 단어를 선택합니다.
                val quizCandidates = WordData.localWords.filter { it.hasStudied && it.correctCount in 0..2 }
                if (quizCandidates.isNotEmpty()) {
                    launchQuiz(quizCandidates.random())
                }
                isProcessing = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isProcessing = false
        if (currentIndex >= studyList.size) {
            currentIndex = 0
        }
    }

    /**
     * displayStudyWord:
     * - 현재 Study 후보 단어(correctCount == 0)를 Study 화면에 표시합니다.
     * - 단어가 Study 화면에 노출되면 hasStudied 플래그를 true로 설정하여,
     *   이후 Quiz 후보 풀에 포함되도록 합니다.
     */
    private fun displayStudyWord(word: Word) {
        word.hasStudied = true
        binding.tvWord.text = word.word
        binding.tvMeaning.text = word.meaning
        Log.d(TAG, "Displaying Study word: '${word.word}' (correctCount=${word.correctCount}, hasStudied=${word.hasStudied})")
    }

    /**
     * launchQuiz:
     * - Quiz 후보 풀은 "hasStudied == true && correctCount in 0..2"인 단어들을 대상으로 합니다.
     * - 이 메소드는 해당 단어에 대해 QuizActivity를 실행하여 Quiz 모드로 전환합니다.
     */
    private fun launchQuiz(word: Word) {
        lastQuizWord = word
        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("wordId", word.id)
        }
        quizActivityLauncher.launch(intent)
    }

    /**
     * launchForcedStudy:
     * - Quiz에서 오답 처리된 단어에 대해 ForcedStudyActivity를 실행합니다.
     * - (수정 1) 오답 처리된 단어는 반드시 ForcedStudy를 통해 바로 그 단어를 보여줍니다.
     */
    private fun launchForcedStudy(word: Word) {
        lastQuizWord = word
        val intent = Intent(this, ForcedStudyActivity::class.java).apply {
            putExtra("wordId", word.id)
        }
        forcedStudyLauncher.launch(intent)
    }

    /**
     * removeWord:
     * - Study 후보 풀에서 해당 단어를 제거합니다.
     * - Study 후보 풀은 오직 correctCount가 0인 단어들이므로,
     *   Quiz를 통해 정답 처리되어 correctCount가 0보다 커지면 제거합니다.
     */
    private fun removeWord(word: Word) {
        val index = studyList.indexOfFirst { it.id == word.id }
        if (index != -1) {
            studyList.removeAt(index)
            if (index <= currentIndex && currentIndex > 0) {
                currentIndex--
            }
        }
    }

    /**
     * displayNextWord:
     * - Study 후보 풀을 재계산하여, 오직 correctCount == 0인 단어들만 포함합니다.
     * - 만약 재계산된 Study 후보 풀이 비어 있으면, Quiz 후보 풀( hasStudied == true && correctCount in 0..2)에서 단어를 선택해 Quiz로 전환합니다.
     * - 모든 단어가 완전 암기( correctCount == 3)되면 학습 세션을 종료합니다.
     */
    private fun displayNextWord() {
        // (수정 2) 현재 상태에서 다시 Study 후보 풀을 재계산
        val newStudyList = WordData.localWords.filter { it.correctCount == 0 }
        if (newStudyList.isNotEmpty()) {
            studyList = newStudyList.toMutableList()
            currentIndex = 0
            displayStudyWord(studyList[currentIndex])
        } else {
            // Study 후보가 없으면, Quiz 후보 풀에서 단어를 선택합니다.
            val quizCandidates = WordData.localWords.filter { it.hasStudied && it.correctCount in 0..2 }
            if (quizCandidates.isNotEmpty()) {
                launchQuiz(quizCandidates.random())
            } else {
                Toast.makeText(this, "오늘의 학습완료!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
