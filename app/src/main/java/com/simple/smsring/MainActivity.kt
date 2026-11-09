package com.simple.smsring

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvRingtone: TextView

    // 接收选择铃声的结果
    private val pickRingtoneLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                getSharedPreferences("cfg", MODE_PRIVATE).edit().putString("custom_ringtone", uri.toString()).apply()
                tvRingtone.text = "当前铃声: 已自定义"
                Toast.makeText(this, "铃声设置成功！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val tvTip = TextView(this).apply {
            text = "输入要监控的发件人号码（例如 95588）："
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

        // 选择铃声按钮
        tvRingtone = TextView(this).apply {
            text = "当前铃声: 系统默认闹钟"
            textSize = 14f
            setPadding(0, 30, 0, 10)
        }
        val btnPickRingtone = Button(this).apply {
            text = "选择专属铃声"
            setOnClickListener {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                }
                pickRingtoneLauncher.launch(intent)
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
        layout.addView(tvRingtone)
        layout.addView(btnPickRingtone)
        layout.addView(btnStop)
        setContentView(layout)
        
        etNumber.setText(getSharedPreferences("cfg", MODE_PRIVATE).getString("target", ""))

               val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.POST_NOTIFICATIONS)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (need.isNotEmpty()) ActivityCompat.requestPermissions(this, need.toTypedArray(), 1)

        // 检查悬浮窗权限，没有就引导用户去开
        checkOverlayPermission()
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("重要权限提醒")
                    .setMessage("为了保证切到后台也能正常响铃，必须开启“显示在其他应用上层”权限！")
                    .setPositiveButton("去开启") { _, _ ->
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }
    }
}
