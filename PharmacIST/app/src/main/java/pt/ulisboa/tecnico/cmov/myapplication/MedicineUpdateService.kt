package pt.ulisboa.tecnico.cmov.pharmacist

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MedicineUpdateService : Service() {

    private val binder = LocalBinder()

    private val previousAmounts = mutableMapOf<String, MutableMap<String, Long>>()
    private val listenerRegistrations = mutableMapOf<String, ListenerRegistration>()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var medicinesWithNotification : MedicineWithNotificationRepository

    companion object {
        private const val CHANNEL_ID = "medicine_availability_channel"
        private const val NOTIFICATION_ID = 1
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        Log.d("MedicineUpdateService", "Service is running")
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        medicinesWithNotification = MedicineWithNotificationRepository(this)

        addCollection()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service will run until explicitly stopped
        return START_STICKY
    }

    inner class LocalBinder : Binder() {
        fun getService(): MedicineUpdateService = this@MedicineUpdateService
    }

    // The rest of your service implementation

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Medicine Availability Service")
            .setContentText("Listening for medicine availability updates.")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Medicine Availability"
            val descriptionText = "Notifications for medicine availability"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(medicineName: String, pharmacyName:String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .setContentTitle("Medicine Available")
            .setContentText("$medicineName is now available in $pharmacyName.")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for heads-up notification
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MedicineUpdateService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private fun addCollectionListener(pharmacyName: String){
        val listenerRegistration = db.collection("pharmacies")
            .document(pharmacyName)
            .collection("medicines")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FirestoreListener", "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    val newData = dc.document.data
                    val newAmount = newData["stock"] as? Long ?: 0L
                    val medicineName = newData["name"] as String


                    when (dc.type) {
                        DocumentChange.Type.MODIFIED -> {}

                        DocumentChange.Type.ADDED -> {
                            if (newAmount > 0L && medicinesWithNotification.isMedicineWithNotification(medicineName)) {
                                sendNotification(medicineName, pharmacyName)
                            }
                        }

                        DocumentChange.Type.REMOVED -> {}
                    }
                }
            }
        listenerRegistrations[pharmacyName] = listenerRegistration
    }


    private fun addCollection() {
        val favoritePharmacies = FavoritePharmaciesRepository(this).getFavoritePharmacies()
        for (pharmacyName in favoritePharmacies) {
            addCollectionListener(pharmacyName)
        }
    }

    fun addNewPharmacyCollection(pharmacyName: String) {
        addCollectionListener(pharmacyName)
    }

    fun removePharmacyCollection(pharmacyName: String) {
        listenerRegistrations[pharmacyName]?.remove()
        listenerRegistrations.remove(pharmacyName)
    }
}