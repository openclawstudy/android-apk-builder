package com.example.sampleapp

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var ivReceipt: ImageView
    private lateinit var btnScan: Button
    private lateinit var btnConfirm: Button

    private var photoUri: Uri? = null

    // ⚠️ 換成你自己的阿里雲百煉 API Key
    private val dashscopeApiKey = "sk-ws-H.ERXIEDD.Wsdf.MEUCIQCUgfyO0Wc3bPphvO8i959k2Fb6hCSjg9ina3QvvTYfMgIgOvlXfljoNnSLs-xJBMhaoeN8OJCIx4wF5lyHWgYLa3w"

    // 拍照結果處理
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { ivReceipt.setImageURI(it) }
            btnConfirm.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "取消拍照", Toast.LENGTH_SHORT).show()
        }
    }

    // 權限請求處理
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(this, "需要相機權限才能拍照", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivReceipt = findViewById(R.id.ivReceipt)
        btnScan = findViewById(R.id.btnScan)
        btnConfirm = findViewById(R.id.btnConfirm)

        btnScan.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openCamera()
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        btnConfirm.setOnClickListener {
            photoUri?.let { uri ->
                btnConfirm.isEnabled = false
                btnConfirm.text = "分析中..."
                analyzeReceiptWithQwenVL(uri)
            }
        }
    }

    // 開啟相機
    private fun openCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(cacheDir, "receipt_$timeStamp.jpg")
        photoUri = FileProvider.getUriForFile(
            this,
            "com.example.sampleapp.fileprovider",
            imageFile
        )
        takePictureLauncher.launch(photoUri)
    }

    // 將照片轉為 Base64 字串
    private fun uriToBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // 呼叫 Qwen-VL API 進行收據辨識
    private fun analyzeReceiptWithQwenVL(uri: Uri) {
        Thread {
            try {
                val base64Image = uriToBase64(uri)

                // 建立請求 JSON（OpenAI 相容格式）
                val jsonBody = JSONObject().apply {
                    put("model", "qwen-vl-plus")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put(
                                        "text",
                                        "這是一張消費收據。請提取商家名稱、日期和總金額，嚴格按以下 JSON 格式回傳，不要包含任何其他文字：{\"store_name\":\"\",\"date\":\"\",\"total_amount\":\"\"}"
                                    )
                                })
                                put(JSONObject().apply {
                                    put("type", "image_url")
                                    put("image_url", JSONObject().apply {
                                        put("url", "data:image/jpeg;base64,$base64Image")
                                    })
                                })
                            })
                        })
                    })
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $dashscopeApiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                runOnUiThread {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Confirm"

                    if (response.isSuccessful && responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val choices = jsonResponse.getJSONArray("choices")
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val text = message.getString("content")
                        Toast.makeText(this, "辨識結果：\n$text", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "分析失敗：HTTP ${response.code}\n$responseBody", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Confirm"
                    Toast.makeText(this, "錯誤：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}