package com.example.english_app_studypart // 패키지명 확인

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.english_app_studypart.datas.Word
import kotlin.random.Random

// 앱의 단어 데이터 관리 및 학습/퀴즈 로직 중앙 처리 (싱글톤)
object WordManager {

    private val words = mutableListOf<Word>() // 관리 대상 단어 목록
    private const val TAG = "WordManager" // 로그 태그

    // WordData로부터 단어 목록을 받아 초기화
    fun initialize(initialWords: List<Word>) {
        words.clear()
        words.addAll(initialWords)
        Log.d(TAG, "WordManager initialized with ${words.size} words.")
    }

    // ID로 특정 Word 객체 찾기
    fun findWordById(id: Int): Word? {
        return words.find { it.id == id }
    }

    // 전체 단어 목록 반환 (읽기 전용)
    fun getAllWords(): List<Word> = words.toList()

    // --- 시나리오 반영: 단어 상태 업데이트 ---

    // 학습 완료 처리: hasStudied 플래그를 true로 설정
    fun markAsStudied(wordId: Int) {
        findWordById(wordId)?.let {
            if (!it.hasStudied) {
                it.hasStudied = true
                Log.d(TAG, "Word ID $wordId ('${it.word}') marked as studied.")
            }
        }
    }

    // 퀴즈 결과 반영: correctCount 업데이트 후 업데이트된 단어 반환
    fun updateQuizCount(wordId: Int, isCorrect: Boolean): Word? {
        val word = findWordById(wordId)
        word?.let {
            if (isCorrect) {
                // 정답: correctCount 증가 (최대 3)
                if (it.correctCount < 3) {
                    it.correctCount++
                    Log.d(TAG, "Word ID $wordId ('${it.word}') quiz count incremented to ${it.correctCount}.")
                }
            } else {
                // 오답: correctCount 감소 (최소 0)
                if (it.correctCount > 0) {
                    it.correctCount--
                    Log.d(TAG, "Word ID $wordId ('${it.word}') quiz count decremented to ${it.correctCount}.")
                } else {
                    Log.d(TAG, "Word ID $wordId ('${it.word}') quiz count is already 0.")
                }
            }
            // 퀴즈 시도 시 hasStudied는 항상 true여야 함
            if (!it.hasStudied) {
                it.hasStudied = true
                Log.d(TAG,"Word ID $wordId ('${it.word}') marked studied after quiz attempt")
            }
        }
        return word // 업데이트된 단어 반환 (오답 시 강제 학습 및 카운터 즉시 업데이트에 사용)
    }

    // --- 시나리오 반영: 후보 단어 선정 ---

    // 학습 후보 목록 가져오기 (correctCount == 0)
    fun getLearningCandidates(): List<Word> {
        return words.filter { it.isLearningCandidate } // Word 클래스의 편의 프로퍼티 사용
    }

    // 퀴즈 후보 목록 가져오기 (hasStudied == true && 0 <= correctCount <= 2)
    fun getQuizCandidates(): List<Word> {
        return words.filter { it.isQuizCandidate } // Word 클래스의 편의 프로퍼티 사용
    }

    // --- 시나리오 반영: 학습 완료 확인 ---

    // 모든 단어 학습 완료 여부 확인 (모든 단어의 correctCount == 3)
    fun isLearningComplete(): Boolean {
        val complete = words.isNotEmpty() && words.all { it.correctCount == 3 }
        if(complete) Log.d(TAG, "Learning complete!")
        return complete
    }

    // --- 시나리오 반영: 다음 화면 결정 로직 ---

    /**
     * 다음에 보여줄 Activity를 결정하고 해당 Intent를 생성합니다.
     * @param context 현재 Context
     * @param lastIncorrectWord 직전에 틀린 단어 객체 (null이 아니면 이 단어의 학습 화면 강제)
     * @return 다음에 시작할 Activity의 Intent, 학습 완료 시 null 반환
     */
    fun getNextIntent(context: Context, lastIncorrectWord: Word? = null): Intent? {
        // 1. 학습 완료 체크
        if (isLearningComplete()) {
            Log.d(TAG, "All words mastered. No next intent.")
            return null // 완료 시 null 반환
        }

        // 2. 오답 직후 강제 학습 처리
        if (lastIncorrectWord != null) {
            Log.d(TAG, "Forcing study for previously incorrect word ID: ${lastIncorrectWord.id} ('${lastIncorrectWord.word}')")
            // StudyActivity로 보내고, 대상 단어 ID 전달
            val intent = Intent(context, StudyActivity::class.java)
            intent.putExtra(StudyActivity.EXTRA_WORD_ID, lastIncorrectWord.id)
            return intent
        }

        // 3. 학습 및 퀴즈 후보 목록 가져오기
        val learningCandidates = getLearningCandidates()
        val quizCandidates = getQuizCandidates()

        Log.d(TAG, "Candidates - Learning: ${learningCandidates.size} (${learningCandidates.map { it.word }}), Quiz: ${quizCandidates.size} (${quizCandidates.map { it.word }})")

        // 4. 보여줄 화면 종류 결정 (랜덤)
        val possibleActivities = mutableListOf<Class<*>>()
        if (learningCandidates.isNotEmpty()) possibleActivities.add(StudyActivity::class.java)
        if (quizCandidates.isNotEmpty()) possibleActivities.add(QuizActivity::class.java)

        // 후보가 없는 예외 상황 처리 (이론상 완료 상태 외엔 없어야 함)
        if (possibleActivities.isEmpty()) {
            Log.w(TAG, "No candidates available, but learning is not complete? Check logic.")
            return null // 오류 또는 완료로 간주
        }

        // 5. Activity 종류 랜덤 선택 및 해당 후보 단어 랜덤 선택
        val nextActivityClass = possibleActivities.random() // Study 또는 Quiz 랜덤 선택
        val intent = Intent(context, nextActivityClass)
        val wordToShow: Word?

        if (nextActivityClass == StudyActivity::class.java) {
            // 학습 화면: 학습 후보 중 랜덤 선택
            wordToShow = learningCandidates.random()
            Log.d(TAG, "Next Activity: Study - Word ID: ${wordToShow.id} ('${wordToShow.word}')")
            intent.putExtra(StudyActivity.EXTRA_WORD_ID, wordToShow.id) // 단어 ID 전달
        } else { // QuizActivity
            // 퀴즈 화면: 퀴즈 후보 중 랜덤 선택
            wordToShow = quizCandidates.random()
            Log.d(TAG, "Next Activity: Quiz - Word ID: ${wordToShow.id} ('${wordToShow.word}')")
            intent.putExtra(QuizActivity.EXTRA_WORD_ID, wordToShow.id) // 단어 ID 전달
        }

        return intent
    }
}