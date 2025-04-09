package com.example.english_app_studypart.datas

// import android.os.Parcelable // ID만 전달하면 Parcelable 필요 없음
// import kotlinx.parcelize.Parcelize // ID만 전달하면 Parcelable 필요 없음

// @Parcelize // ID만 전달하면 Parcelable 필요 없음
data class Word(
    val id: Int, // 고유 식별자
    val word: String, // 영어 단어
    val meaning: String, // 뜻
    var correctCount: Int = 0,  // 퀴즈에서 맞힌 횟수 (시나리오의 quizCount 역할, 최대 3)
    var hasStudied: Boolean = false  // 학습 화면에 노출된 적이 있는지 (Quiz 후보 조건에 사용)
)
// : Parcelable // ID만 전달하면 Parcelable 필요 없음
{
    // --- 시나리오 반영 편의 프로퍼티 (선택 사항) ---
    // 학습 후보인지 확인 (correctCount == 0)
    val isLearningCandidate: Boolean
        get() = correctCount == 0

    // 퀴즈 후보인지 확인 (hasStudied == true && 0 <= correctCount <= 2)
    val isQuizCandidate: Boolean
        get() = hasStudied && correctCount in 0..2
}