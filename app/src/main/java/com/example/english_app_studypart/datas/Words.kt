package com.example.english_app_studypart.datas

data class Word(
    val id: Int,
    val word: String,
    val meaning: String,
    var correctCount: Int = 0,
    var hasStudied: Boolean = false,
    // var needsReviewToday: Boolean = false, // 복습 기능 제외
    var studyScreenAppearances: Int = 0 // ⭐ 추가: 랜덤 학습 화면 노출 횟수
) {
    // 학습 후보 조건 수정: 노출 횟수 < 3 추가
    val isLearningCandidate: Boolean
        get() = correctCount == 0 && studyScreenAppearances < 3 // ⭐ 수정

    // 퀴즈 후보 조건 (변경 없음)
    val isQuizCandidate: Boolean
        get() = hasStudied && correctCount in 0..2
}