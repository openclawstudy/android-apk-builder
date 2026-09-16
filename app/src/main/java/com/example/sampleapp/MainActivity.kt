package com.example.sampleapp

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var ivReceipt: ImageView
    private lateinit var btnScan: Button
    private lateinit var btnConfirm: Button

    // 用來儲存照片的 URI
    private var photoUri: Uri? = null

    // 註冊拍照結果的處理器
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // 拍照成功，將照片顯示在 ImageView 上
            photoUri?.let { ivReceipt.setImageURI(it) }
            // 顯示 Confirm 按鈕
            btnConfirm.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "取消拍照", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivReceipt = findViewById(R.id.ivReceipt)
        btnScan = findViewById(R.id.btnScan)
        btnConfirm = findViewById(R.id.btnConfirm)

        btnScan.setOnClickListener {
            // 檢查相機權限
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                // 如果沒有權限，請求權限
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        btnConfirm.setOnClickListener {
            // TODO: 之後在這裡加入上傳到 Google Sheet 的程式碼
            Toast.makeText(this, "照片已確認，準備上傳！", Toast.LENGTH_SHORT).show()
        }
    }

    // 請求權限的處理器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "需要相機權限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    // 開啟相機的邏輯
    private fun openCamera() {
        // 建立一個暫存檔案來存放照片
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(cacheDir, "receipt_$timeStamp.jpg")
        
        // 透過 FileProvider 取得安全的 URI
        photoUri = FileProvider.getUriForFile(
            this,
            "com.example.sampleapp.fileprovider",
            imageFile
        )

        // 啟動相機 App
        takePictureLauncher.launch(photoUri)
    }
}