package com.example.smsapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val SMS_PERMISSION_CODE = 101
    private val TEXT_LIMIT = 160  // Set your desired text limit
    private lateinit var subscriptionManager: SubscriptionManager
    private var simList: List<SubscriptionInfo> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val phoneNumberEditText = findViewById<EditText>(R.id.phoneNumberEditText)
        val messageEditText = findViewById<EditText>(R.id.messageEditText)
        val simSpinner = findViewById<Spinner>(R.id.simSpinner)
        val sendButton = findViewById<Button>(R.id.sendButton)

        subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        // Load available SIMs into the spinner
        loadSimOptions(simSpinner)

        sendButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            var message = messageEditText.text.toString().trim()

            if (phoneNumber.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please enter both phone number and message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Limit message to TEXT_LIMIT
            if (message.length > TEXT_LIMIT) {
                message = message.substring(0, TEXT_LIMIT)
                Toast.makeText(this, "Message truncated to $TEXT_LIMIT characters", Toast.LENGTH_SHORT).show()
            }

            // Check SMS permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
            } else {
                // Get the selected SIM
                val selectedSimIndex = simSpinner.selectedItemPosition
                if (simList.isNotEmpty()) {
                    val subscriptionId = simList[selectedSimIndex].subscriptionId
                    sendSMS(phoneNumber, message, subscriptionId)
                } else {
                    Toast.makeText(this, "No SIM cards available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadSimOptions(simSpinner: Spinner) {
        // Check permission to read SIM cards
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), SMS_PERMISSION_CODE)
        } else {
            // Get available SIMs
            simList = subscriptionManager.activeSubscriptionInfoList ?: listOf()

            if (simList.isNotEmpty()) {
                val simNames = simList.map { it.displayName.toString() }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, simNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                simSpinner.adapter = adapter
            } else {
                Toast.makeText(this, "No SIM cards found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSMS(phoneNumber: String, message: String, subscriptionId: Int) {
        try {
            // Use the SmsManager for the specific SIM (subscriptionId)
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
            } else {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }

            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS sent successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "SMS failed to send", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
