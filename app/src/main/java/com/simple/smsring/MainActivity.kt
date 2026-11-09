package com.simple.smsring

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 极简界面代码
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val tvTip = android.widget.TextView(this).apply {
            text = "输入要监控的发件人号码（例如 95588）\n收到该号码短信将一直响铃："
            textSize = 16f
        }
        
        val etNumber = EditText(this).apply {
            hint = "输入号码"
        }
        
        val btnSave = Button(this).apply {
            text = "保存号码并开启监控"
            setOnClickListener {
                val number = etNumber.text.toString().trim()
                if (number.isNotEmpty()) {
                    getSharedPreferences("cfg", MODE_PRIVATE).edit().putString("target", number).apply()
                    Toast.makeText(this@MainActivity, "已保存监控号码: $number", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        val btnStop = Button(this).apply {
            text = "停止当前响铃"
            setOnClickListener {
                startService(Intent(this@MainActivity, RingService::class.java).setAction("STOP"))
            }
        }

        layout.addView(tvTip)
        layout.addView(etNumber)
        layout.addView(btnSave)
        layout.addView(btnStop)
        setContentView(layout)
        
        // 初始化显示已保存的号码
        etNumber.setText(getSharedPreferences("cfg", MODE_PRIVATE).getString("target", ""))

        // 请求权限
        val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isNotEmpty()) ActivityCompat.requestPermissions(this, need.toTypedArray(), 1)
    }
}
