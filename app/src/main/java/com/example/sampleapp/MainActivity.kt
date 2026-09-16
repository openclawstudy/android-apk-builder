package com.example.sampleapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 綁定介面上的元件
        val ivReceipt = findViewById<ImageView>(R.id.ivReceipt)
        val btnScan = findViewById<Button>(R.id.btnScan)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        // 點擊「Scan Receipt」按鈕時的反應
        btnScan.setOnClickListener {
            // TODO: 之後在這裡加入開啟相機的程式碼
            // 暫時先讓「Confirm」按鈕顯示出來，方便我們測試
            btnConfirm.visibility = View.VISIBLE
        }

        // 點擊「Confirm」按鈕時的反應
        btnConfirm.setOnClickListener {
            // TODO: 之後在這裡加入上傳到 Google Sheet 的程式碼
        }
    }
}