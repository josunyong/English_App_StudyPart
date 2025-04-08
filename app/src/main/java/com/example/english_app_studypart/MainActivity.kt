package com.example.english_app_studypart

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.english_app_studypart.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Study 버튼 클릭 시 학습 화면으로 전환
        binding.btnStudy.setOnClickListener {
            val intent = Intent(this, StudyActivity::class.java)
            startActivity(intent)

        }
    }
}




