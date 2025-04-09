package com.example.english_app_studypart

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.english_app_studypart.datas.Word
import kotlin.random.Random

// 학습/퀴즈 흐름만 관리 (복습 제외 버전)
object WordManager {

    private val words = mutableListOf<Word>()
    private const val TAG = "WordManager"

    // 초기화: studyScreenAppearances도 0으로 리셋
    fun initialize(initialWords: List<Word>) {
        words.clear()
        // ⭐ 수정: studyScreenAppearances도 0으로 초기화, needsReviewToday 제거
        words.addAll(initialWords.map { it.copy(studyScreenAppearances = 0) })
        Log.d(TAG, "WordManager initialized. Appearances reset.")
    }

    fun findWordById(id: Int): Word? = words.find { it.id == id }
    fun getAllWords(): List<Word> = words.toList() // 전체 단어 목록 (읽기 전용)

    fun markAsStudied(wordId: Int) { /* 변경 없음 */
        findWordById(wordId)?.let{if(!it.hasStudied){it.hasStudied=true;Log.d(TAG,"ID $wordId studied.")}}
    }

    // 퀴즈 결과 반영 (복습 상태 설정 로직 제거)
    fun updateQuizCount(wordId: Int, isCorrect: Boolean): Word? {
        val word = findWordById(wordId)
        word?.let {
            // val oldCount = it.correctCount // 복습 로직 없으므로 제거 가능
            if (isCorrect) { if (it.correctCount < 3) it.correctCount++ }
            else { if (it.correctCount > 0) it.correctCount-- }
            Log.d(TAG, "ID $wordId quiz count updated to ${it.correctCount}.")
            // ⭐ 복습 상태 설정 로직 제거
            // if (oldCount < 3 && it.correctCount == 3) { ... }
            if (!it.hasStudied) { it.hasStudied = true; Log.d(TAG,"ID $wordId studied after quiz.")}
        }
        return word
    }

    // 학습 후보 목록 (count=0, appearances<3)
    fun getLearningCandidates(): List<Word> {
        val candidates = words.filter { it.isLearningCandidate } // isLearningCandidate 조건 사용
        Log.d(TAG, "Learning candidates (count=0, appearances<3): ${candidates.size}")
        return candidates
    }

    // 퀴즈 후보 목록 (변경 없음)
    fun getQuizCandidates(): List<Word> {
        val candidates = words.filter { it.isQuizCandidate }; Log.d(TAG, "Quiz candidates: ${candidates.size}"); return candidates
    }

    // --- 복습 관련 함수 모두 제거 ---
    // fun getReviewCandidates()...
    // fun markAsReviewed()...
    // fun isReviewAvailable()...
    // fun isReviewComplete()...

    // 학습 완료 확인 (모든 단어 count=3)
    fun isLearningComplete(): Boolean = words.isNotEmpty() && words.all { it.correctCount == 3 }

    // ⭐ 추가: 랜덤 학습 화면 노출 횟수 증가 함수
    private fun incrementStudyAppearance(wordId: Int) { // private으로 변경 가능
        findWordById(wordId)?.let {
            // 조건 검사 강화: count가 0이고, 아직 3 미만일 때만 증가
            if (it.correctCount == 0 && it.studyScreenAppearances < 3) {
                it.studyScreenAppearances++
                Log.d(TAG, "ID $wordId ('${it.word}') study appearances incremented to ${it.studyScreenAppearances}.")
            } else {
                Log.w(TAG, "Attempted to increment study appearances for ID $wordId invalidly (count=${it.correctCount}, app=${it.studyScreenAppearances}).")
            }
        }
    }

    // ⭐ 수정: 함수명 및 로직 변경 (학습/퀴즈 흐름만 담당)
    // 다음에 보여줄 학습/퀴즈 Activity 결정
    fun getNextIntent(context: Context, lastIncorrectWord: Word? = null): Intent? {
        // 1. 학습 완료 체크
        if (isLearningComplete()) {
            Log.d(TAG, "Learning complete. No next intent.")
            return null // 완료 시 null 반환 -> Activity에서 MainActivity로 이동 처리
        }

        // 2. 강제 학습 처리 (횟수 증가 X)
        if (lastIncorrectWord != null) {
            Log.d(TAG, "Forcing study for ID: ${lastIncorrectWord.id}")
            val intent = Intent(context, StudyActivity::class.java)
            intent.putExtra(StudyActivity.EXTRA_WORD_ID, lastIncorrectWord.id)
            return intent // 바로 반환
        }

        // 3. 일반적인 다음 화면 결정 (학습/퀴즈 후보 확인)
        val learningCandidates = getLearningCandidates() // appearances < 3 조건 포함
        val quizCandidates = getQuizCandidates()
        Log.d(TAG, "Candidates - Learning: ${learningCandidates.size}, Quiz: ${quizCandidates.size}")

        val possibleActivities = mutableListOf<Class<*>>()
        if (learningCandidates.isNotEmpty()) possibleActivities.add(StudyActivity::class.java)
        if (quizCandidates.isNotEmpty()) possibleActivities.add(QuizActivity::class.java)

        if (possibleActivities.isEmpty()) {
            // 학습/퀴즈 후보가 없는데 완료도 아닌 경우 (이론상 거의 발생 안 함)
            // 모든 단어가 count > 0 이면서 (LC 없음), hasStudied=F 이거나 count=3 인 경우 (QC 없음)
            Log.w(TAG, "No candidates available, but learning not complete? Check logic.")
            // isLearningComplete 가 false 인 상황에서 이 분기에 오면 로직 오류 가능성 있음
            // 안전하게 null 반환 -> Activity에서 완료로 간주하고 MainActivity로 갈 수 있음
            return null
        }

        // 4. Activity 종류 및 단어 랜덤 선택 후 Intent 생성
        val nextActivityClass = possibleActivities.random()
        val intent = Intent(context, nextActivityClass)
        val wordToShow: Word

        if (nextActivityClass == StudyActivity::class.java) {
            // ⭐ 랜덤 학습 선택 시: 횟수 증가시키고 Intent에 담기
            wordToShow = learningCandidates.random()
            incrementStudyAppearance(wordToShow.id) // <--- 여기서 횟수 증가
            Log.d(TAG, "Next: Random Study - ID: ${wordToShow.id} (App: ${wordToShow.studyScreenAppearances})")
            intent.putExtra(StudyActivity.EXTRA_WORD_ID, wordToShow.id)
        } else { // QuizActivity
            // 퀴즈 선택 시: 횟수 증가 없음
            wordToShow = quizCandidates.random()
            Log.d(TAG, "Next: Random Quiz - ID: ${wordToShow.id}")
            intent.putExtra(QuizActivity.EXTRA_WORD_ID, wordToShow.id)
        }
        return intent
    }
}