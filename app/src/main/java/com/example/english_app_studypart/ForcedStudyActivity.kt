package com.example.english_app_studypart

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.english_app_studypart.databinding.ActivityForcedStudyBinding
import com.example.english_app_studypart.datas.Word
import com.example.english_app_studypart.datas.WordData

/**
 * ForcedStudyActivity:
 * - Quiz에서 오답 처리된 단어에 대해 강제 복습(Forced Study)을 진행합니다.
 * - 반드시 오답 처리된 그 단어를 즉시 보여줍니다.
 */
class ForcedStudyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForcedStudyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForcedStudyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 전달받은 wordId로 단어를 검색합니다.
        val wordId = intent.getIntExtra("wordId", -1)
        val word: Word? = WordData.localWords.find { it.id == wordId }
        word?.let {
            binding.tvWord.text = it.word
            binding.tvMeaning.text = it.meaning
        }

        // "넘기기" 버튼 클릭 시 ForcedStudy를 종료하고 결과를 반환
        binding.btnNext.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
