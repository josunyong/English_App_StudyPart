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
 * 1. Study 후보: 단어의 correctCount가 0인 단어들만 Study 화면에 노출됨.
 * 2. Quiz 후보: 이미 Study 화면에 노출된 단어 중( hasStudied == true ),
 *    correctCount가 0, 1, 또는 2 인 단어들로 진행.
 * 3. 단어가 Quiz에서 정답 처리되면 correctCount가 증가하여 Study 후보에서 제외됨.
 *
 * 강제학습(ForcedStudy)은 주석 처리되어 있으므로, 오답 처리 시 대신 studyList를 재계산하도록 함.
 */
class StudyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudyBinding

    // 초기 Study 후보: correctCount가 0인 단어들만
    private var studyList: MutableList<Word> = mutableListOf()
    private var currentIndex = 0
    private var lastQuizWord: Word? = null

    // Study 화면에서 Quiz로 전환할 확률 (예: 40%)
    private val QUIZ_PROBABILITY = 0.4
    private var isProcessing = false

    companion object {
        private const val TAG = "StudyActivity"
    }

    // QuizActivity 결과 수신 launcher
    private val quizActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            isProcessing = false
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
                                // 정답 시 해당 단어는 Study 후보에서 제거됨
                                removeWord(word)
                                displayNextWord()
                            }
                            "wrong" -> {
                                Log.d(TAG, "Word '${word.word}' answered wrongly (count=${word.correctCount}).")
                                // 강제학습은 주석 처리되었으므로, 오답 시에도 studyList의 최신 상태를 반영하여 후보를 재계산
                                if (word.correctCount > 0) {
                                    removeWord(word)
                                }
                                displayNextWord()
                            }
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 Study 후보 풀 구성 (correctCount가 0인 단어들)
        studyList = WordData.localWords.filter { it.correctCount == 0 }.toMutableList()

        // 전체 단어 중 암기 대상( correctCount < 3 )이 없으면 종료
        if (WordData.localWords.filter { it.correctCount < 3 }.isEmpty()) {
            Toast.makeText(this, "오늘의 학습완료!", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        currentIndex = 0

        if (studyList.isNotEmpty()) {
            displayStudyWord(studyList[currentIndex])
        } else {
            // Study 후보가 없으면 Quiz 후보 풀에서 진행
            val quizCandidates = WordData.localWords.filter { it.hasStudied && it.correctCount in 0..2 }
            if (quizCandidates.isNotEmpty()) {
                launchQuiz(quizCandidates.random())
            } else {
                Toast.makeText(this, "오늘의 학습완료!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        binding.btnNext.setOnClickListener {
            if (isProcessing) return@setOnClickListener

            // 전체 단어 중 학습 대상이 없으면 종료
            if (WordData.localWords.filter { it.correctCount < 3 }.isEmpty()) {
                Toast.makeText(this, "오늘의 학습완료!", Toast.LENGTH_LONG).show()
                finish()
                return@setOnClickListener
            }
            isProcessing = true

            // 현재 화면에 보여지는 단어가 최신 상태( correctCount == 0 )인지 확인
            val currentWord = studyList.getOrNull(currentIndex)
            if (currentWord == null || currentWord.correctCount > 0) {
                // 만약 현재 단어가 더 이상 Study 대상이 아니면 후보를 재계산
                displayNextWord()
                isProcessing = false
                return@setOnClickListener
            }

            if (studyList.isNotEmpty()) {
                if (Math.random() < QUIZ_PROBABILITY) {
                    Log.d(TAG, "Launching QuizActivity for studied word '${currentWord.word}'")
                    launchQuiz(currentWord)
                } else {
                    currentIndex++
                    if (currentIndex < studyList.size) {
                        displayStudyWord(studyList[currentIndex])
                    } else {
                        // 모든 후보 소진 시 재계산
                        displayNextWord()
                    }
                }
            } else {
                // Study 후보가 없으면 Quiz 후보 풀에서 진행
                val quizCandidates = WordData.localWords.filter { it.hasStudied && it.correctCount in 0..2 }
                if (quizCandidates.isNotEmpty()) {
                    launchQuiz(quizCandidates.random())
                }
            }
            isProcessing = false
        }
    }

    override fun onResume() {
        super.onResume()
        isProcessing = false
        // onResume 시에도 최신 Study 후보를 반영하도록 업데이트
        val newStudyList = WordData.localWords.filter { it.correctCount == 0 }
        if (newStudyList.isNotEmpty()) {
            studyList = newStudyList.toMutableList()
            if (currentIndex >= studyList.size) {
                currentIndex = 0
            }
            displayStudyWord(studyList[currentIndex])
        } else {
            // Study 후보가 없으면 Quiz 후보 풀에서 진행
            val quizCandidates = WordData.localWords.filter { it.hasStudied && it.correctCount in 0..2 }
            if (quizCandidates.isNotEmpty()) {
                launchQuiz(quizCandidates.random())
            } else {
                Toast.makeText(this, "오늘의 학습완료!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun displayStudyWord(word: Word) {
        word.hasStudied = true
        binding.tvWord.text = word.word
        binding.tvMeaning.text = word.meaning
        Log.d(TAG, "Displaying Study word: '${word.word}' (correctCount=${word.correctCount}, hasStudied=${word.hasStudied})")
    }

    private fun launchQuiz(word: Word) {
        lastQuizWord = word
        val intent = Intent(this, QuizActivity::class.java).apply {
            putExtra("wordId", word.id)
        }
        quizActivityLauncher.launch(intent)
    }

    private fun removeWord(word: Word) {
        val index = studyList.indexOfFirst { it.id == word.id }
        if (index != -1) {
            studyList.removeAt(index)
            if (index <= currentIndex && currentIndex > 0) {
                currentIndex--
            }
        }
    }

    private fun displayNextWord() {
        // 최신 Study 후보를 재계산 (correctCount가 0인 단어들)
        val newStudyList = WordData.localWords.filter { it.correctCount == 0 }
        if (newStudyList.isNotEmpty()) {
            studyList = newStudyList.toMutableList()
            currentIndex = 0
            displayStudyWord(studyList[currentIndex])
        } else {
            // Study 후보가 없으면 Quiz 후보로 전환
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
