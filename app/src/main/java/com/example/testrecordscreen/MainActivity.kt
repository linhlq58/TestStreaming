package com.example.testrecordscreen

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.controls.Facing
import com.pedro.rtplibrary.view.OpenGlView
import net.ossrs.rtmp.ConnectCheckerRtmp


class MainActivity : AppCompatActivity(), ConnectCheckerRtmp {
    private lateinit var btnStartStop: Button
    private lateinit var webView: WebView

    private lateinit var notificationManager: NotificationManager
    private val camera: CameraView by lazy { findViewById(R.id.camera) }

    private val REQUEST_CODE_STREAM = 5236
    private val REQUEST_CODE_RECORD = 2316

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        btnStartStop = findViewById(R.id.btn_start_stop)
        webView = findViewById(R.id.web_view)

        setupWebview()
        camera.setLifecycleOwner(this)

        if (checkIfAlreadyHavePermission()) {
            camera.open()
            camera.facing = Facing.FRONT
        } else {
            requestPermission()
        }

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        getInstance()

        btnStartStop.setOnClickListener {
            if (!DisplayService.isStreaming()) {
                btnStartStop.setText(R.string.stop_button)
                startActivityForResult(DisplayService.sendIntent(), REQUEST_CODE_STREAM)
            } else {
                btnStartStop.setText(R.string.start_button)
                stopService(Intent(this, DisplayService::class.java))
            }

            if (!DisplayService.isStreaming() && !DisplayService.isRecording()) {
                stopNotification()
            }
        }

        if (DisplayService.isStreaming()) {
            btnStartStop.setText(R.string.stop_button)
        } else {
            btnStartStop.setText(R.string.start_button)
        }
    }

    private fun setupWebview() {
        webView.setBackgroundColor(0x00000000)
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.setBackgroundColor(0x00000000)
                view?.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
            }
        }

        webView.loadUrl("https://devchatbox.tk/botlive/index.html")
    }

    private fun getInstance() {
        DisplayService.init(this)
    }

    /**
     * This notification is to solve MediaProjection problem that only render surface if something
     * changed.
     * It could produce problem in some server like in Youtube that need send video and audio all time
     * to work.
     */
    private fun initNotification() {
        val notificationBuilder = Notification.Builder(this).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Streaming")
                .setContentText("Display mode stream")
                .setTicker("Stream in progress")
        notificationBuilder.setAutoCancel(true)
        if (notificationManager != null) notificationManager.notify(12345, notificationBuilder.build())
    }

    private fun stopNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(12345)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && (requestCode == REQUEST_CODE_STREAM
                        || requestCode == REQUEST_CODE_RECORD && resultCode == Activity.RESULT_OK)) {
            initNotification()
            DisplayService.setData(resultCode, data)

            val intent = Intent(this, DisplayService::class.java)
            intent.putExtra("endpoint", "rtmp://209.97.171.168:1935/myapp/mystream")
            startService(intent)
        } else {
            Toast.makeText(this, "No permissions available", Toast.LENGTH_SHORT).show()
            btnStartStop.setText(R.string.start_button);
        }
    }

    override fun onConnectionSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(
                    this,
                    "Connection success",
                    Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {
            Toast.makeText(this, "Connection failed. $reason", Toast.LENGTH_SHORT)
                    .show()
            stopNotification()
            stopService(Intent(this, DisplayService::class.java))
            btnStartStop.setText(R.string.start_button)
        }
    }

    override fun onNewBitrateRtmp(bitrate: Long) {

    }

    override fun onDisconnectRtmp() {
        runOnUiThread {
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthErrorRtmp() {
        runOnUiThread {
            Toast.makeText(this, "Auth error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkIfAlreadyHavePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                101
        )
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    camera.open()
                    camera.facing = Facing.FRONT
                } else {
                    finish()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}