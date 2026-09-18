package com.example.ibizacustommap

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ObdManager(private val context: Context) {

    companion object {
        private const val TAG = "ObdManager"
        private val OBD_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val DEFAULT_TIMEOUT_MS = 1500L
        const val FAST_TIMEOUT_MS = 500L // For optional PIDs that may not be supported
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    var isConnected = false
        private set

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    @SuppressLint("MissingPermission")
    fun connect(): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth is off or unavailable.")
            return false
        }

        val pairedDevices = bluetoothAdapter!!.bondedDevices
        val obdDevice = pairedDevices.find { device ->
            val name = device.name ?: ""
            name.contains("OBD", ignoreCase = true) ||
                    name.contains("V-LINK", ignoreCase = true) ||
                    name.contains("ELM327", ignoreCase = true) ||
                    name.contains("Vgate", ignoreCase = true)
        }

        if (obdDevice == null) return false

        return try {
            Log.d(TAG, "Connecting to: ${obdDevice.name}...")
            socket = obdDevice.createRfcommSocketToServiceRecord(OBD_UUID)
            bluetoothAdapter?.cancelDiscovery()
            socket?.connect()

            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
            isConnected = true

            Log.d(TAG, "Tunnel open. We do not initialize here, MainScreen will do it.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect: ${e.message}")
            closeConnection()
            false
        }
    }

    fun closeConnection() {
        try {
            socket?.close()
            inputStream?.close()
            outputStream?.close()
            isConnected = false
            Log.d(TAG, "Connection closed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing: ${e.message}")
        }
    }

    fun sendCommand(command: String) {
        if (!isConnected || outputStream == null) return
        try {
            Log.d(TAG, "TX (Sending): $command")
            val cmdWithReturn = "$command\r"
            outputStream?.write(cmdWithReturn.toByteArray())
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command: ${e.message}")
            closeConnection()
        }
    }

    fun readResponse(timeoutMs: Long = DEFAULT_TIMEOUT_MS): String {
        if (!isConnected || inputStream == null) return ""
        try {
            val buffer = ByteArray(1024)
            val responseBuilder = java.lang.StringBuilder()

            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (inputStream!!.available() > 0) {
                    val bytesRead = inputStream!!.read(buffer)
                    if (bytesRead == -1) break

                    val chunk = String(buffer, 0, bytesRead)
                    responseBuilder.append(chunk)

                    if (chunk.contains(">")) {
                        break // End of successful read
                    }
                } else {
                    Thread.sleep(10) // Avoid saturating the CPU while waiting
                }
            }

            val rawResponse = responseBuilder.toString()
            Log.d(TAG, "RX (Raw response): $rawResponse")

            if (!rawResponse.contains(">")) {
                Log.w(TAG, "TIMEOUT! The scanner did not return '>' in time.")
                return "TIMEOUT"
            }

            val cleanResponse = rawResponse
                .replace(">", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace(" ", "")
                .replace("SEARCHING...", "")
                .replace("SEARCHING", "")
                .replace("STOPPED", "")
                .replace("BUSINIT", "")
                .replace("BUS INIT", "")
                .trim()

            Log.d(TAG, "RX (Clean response): $cleanResponse")

            return cleanResponse

        } catch (e: Exception) {
            Log.e(TAG, "Error reading: ${e.message}")
            closeConnection()
            return "ERROR"
        }
    }
}