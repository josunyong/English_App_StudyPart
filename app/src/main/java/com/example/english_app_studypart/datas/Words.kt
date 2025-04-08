// Word.kt
package com.example.english_app_studypart.datas

data class Word(
    val id: Int,
    val word: String,
    val meaning: String,
    var correctCount: Int = 0,  // 퀴즈에서 맞힌 횟수를 기록 (최대 3)
    var hasStudied: Boolean = false  // 학습 화면에 노출된 적이 있는지 체크
    // dayword -> 망각곡선을 구현하기 위한


)