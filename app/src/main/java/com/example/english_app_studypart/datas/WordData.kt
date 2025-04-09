package com.example.english_app_studypart.datas

// 앱 전체에서 사용할 단어 데이터 목록 (싱글톤 객체)
object WordData {
    // 앱 실행 시 로드될 초기 단어 목록 (실제 앱에서는 DB 등에서 로드)
    val localWords = mutableListOf(
        Word(1, "apple", "사과"),
        Word(2, "banana", "바나나"),
        Word(3, "cherry", "체리"),
        Word(4, "orange", "오렌지"),
        // --- 테스트용 단어 추가 ---
        Word(5, "grape", "포도")
    )
}