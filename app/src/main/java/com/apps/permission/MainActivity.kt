package com.apps.permission


import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isFlashOn = false
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isFlashing = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var phoneStateListener: PhoneStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toggleFlashButton: Button = findViewById(R.id.btn_location)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = PhoneStateListener()

        try {
            cameraId = cameraManager.cameraIdList.first { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show()
        }
        telephonyManager.listen(phoneStateListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)

        toggleFlashButton.setOnClickListener {
            if (cameraId != null) {
                if (isFlashing) {
                    stopFlashing()
                } else {
                    startFlashing()
                }
            } else {
                Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun startFlashing() {
        isFlashing = true
        handler.post(flashRunnable)
    }
    private fun stopFlashing() {
        isFlashing = false
        handler.removeCallbacks(flashRunnable)
        setFlashlight(false)
    }
    private val flashRunnable = object : Runnable {
        override fun run() {
            toggleFlashlight()
            if (isFlashing) {
                handler.postDelayed(this, 100)
            }
        }
    }

    private fun toggleFlashlight() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            return
        }

        try {
            cameraManager.setTorchMode(cameraId!!, !isFlashOn)
            isFlashOn = !isFlashOn
        } catch (e: Exception) {
            Toast.makeText(this, "Error toggling flash", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setFlashlight(state: Boolean) {
        try {
            cameraManager.setTorchMode(cameraId!!, state)
            isFlashOn = state
        } catch (e: Exception) {
            Toast.makeText(this, "Error toggling flash", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class PhoneStateListener : android.telephony.PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)

            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    if (cameraId != null) {
                        startFlashing()
                    }
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    stopFlashing()
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    stopFlashing()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleFlashlight()
        } else {
            Toast.makeText(this, "Camera permission required to use flashlight", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    override fun onDestroy() {
        super.onDestroy()
        telephonyManager.listen(phoneStateListener, android.telephony.PhoneStateListener.LISTEN_NONE)
        stopFlashing()
    }
}
