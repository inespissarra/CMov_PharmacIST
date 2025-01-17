package pt.ulisboa.tecnico.cmov.pharmacist
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

object ConnectivityUtils {

    private lateinit var wifiDialog: AlertDialog
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    fun initialize(activity: AppCompatActivity) {
        setupWifiDialog(activity)
        setupNetworkCallback(activity)
        connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
        checkInitialWifiState(activity)
    }

    private fun checkInitialWifiState(activity: AppCompatActivity) {
        if (!isWifiConnected(activity)) {
            showWifiDialog(activity)
        }
    }

    private fun isWifiConnected(activity: AppCompatActivity): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun setupWifiDialog(activity: AppCompatActivity) {
        wifiDialog = AlertDialog.Builder(activity)
            .setTitle(R.string.wifi_off)
            .setMessage(R.string.wifi_not_on)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                activity.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            }.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    private fun setupNetworkCallback(activity: AppCompatActivity) {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                activity.runOnUiThread {
                    if (isWifiConnected(activity)) {
                        if (wifiDialog.isShowing) {
                            wifiDialog.dismiss()
                        }
                    }
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                activity.runOnUiThread {
                    if (!isWifiConnected(activity)) {
                        if (!wifiDialog.isShowing) {
                            showWifiDialog(activity)
                        }
                    }
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun showWifiDialog(activity: AppCompatActivity) {
        wifiDialog.show()
    }
}