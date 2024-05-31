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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MedicineUpdateService : Service() {

    private val binder = LocalBinder()

    private val previousAmounts = mutableMapOf<String, MutableMap<String, Long>>()
    private val listenerRegistrations = mutableMapOf<String, ListenerRegistration>()
    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val CHANNEL_ID = "medicine_availability_channel"
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        Log.d("MedicineUpdateService", "coisass ***")
        super.onCreate()
        createNotificationChannel()
        checkAndRequestNotificationPermission()
        startForeground(NOTIFICATION_ID, createNotification())

        addCollection()
        // Initialize with initial collections
        //val initialCollectionsToTrack = listOf("stock", "anotherCollection", "yetAnotherCollection")
        //for (collection in initialCollectionsToTrack) {
        //    addCollectionListener(collection)
        //}
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
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Ensure the notification makes sound and shows up
            .setAutoCancel(true) // Makes the notification dismissible when clicked
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

    private fun checkAndRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this as Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun addCollectionListener(medicineName: String, pharmacyName: String){
        val listenerRegistration = db.collection("medicines")
            .document(medicineName)
            .collection("pharmacies")
            .whereEqualTo("name", pharmacyName)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FirestoreListener", "listen:error", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    val docId = dc.document.id
                    val newData = dc.document.data
                    val newAmount = newData["amount"] as? Long ?: 0L

                    val previousCollectionAmounts =
                        previousAmounts.getOrPut(medicineName) { mutableMapOf() }
                    val previousAmount = previousCollectionAmounts[medicineName + "_" + pharmacyName] ?: 0L

                    when (dc.type) {
                        DocumentChange.Type.MODIFIED -> {
                            if (previousAmount == 0L && newAmount > 0L) {
                                Log.d(
                                    "FirestoreListener",
                                    "[$medicineName] Medicine amount updated from 0 to positive: $docId"
                                )
                                sendNotification(medicineName, pharmacyName)
                            }
                            previousCollectionAmounts[medicineName + "_" + pharmacyName] = newAmount
                        }

                        DocumentChange.Type.ADDED -> {
                            previousCollectionAmounts[medicineName + "_" + pharmacyName] = newAmount
                            sendNotification(medicineName, pharmacyName)
                        }

                        DocumentChange.Type.REMOVED -> {
                            previousCollectionAmounts.remove(medicineName + "_" + pharmacyName)
                        }
                    }
                }
            }
        listenerRegistrations[medicineName + "_" + pharmacyName] = listenerRegistration
    }

    private fun removeCollectionListener(medicineName: String, pharmacyName: String) {
        listenerRegistrations[medicineName + "_" + pharmacyName]?.remove()
        listenerRegistrations.remove(medicineName + "_" + pharmacyName)
        previousAmounts.remove(medicineName + "_" + pharmacyName)
    }

    private fun addCollection() {
        Log.d("***", "ubba")
        val medicinesWithNotification = FavoritePharmaciesRepository(this).getFavoritePharmacies()
        val favoritePharmacies = FavoritePharmaciesRepository(this).getFavoritePharmacies()
        for (medicineName in medicinesWithNotification) {
            Log.d("***", medicineName)
            for (pharmacyName in favoritePharmacies) {
                Log.d("***", pharmacyName)
                addCollectionListener(medicineName, pharmacyName)
            }
        }
    }

    fun addNewMedicineCollection(medicineName: String) {
        val favoritePharmacies = FavoritePharmaciesRepository(this).getFavoritePharmacies()
        for (pharmacyName in favoritePharmacies) {
            addCollectionListener(medicineName, pharmacyName)
        }
    }

    fun removeMedicineCollection(medicineName: String) {
        val favoritePharmacies = FavoritePharmaciesRepository(this).getFavoritePharmacies()
        for (pharmacyName in favoritePharmacies) {
            removeCollectionListener(medicineName, pharmacyName)
        }
    }

    fun addNewPharmacyCollection(pharmacyName: String) {
        val medicinesWithNotification = FavoritePharmaciesRepository(this).getFavoritePharmacies()
        for (medicineName in medicinesWithNotification) {
            addCollectionListener(medicineName, pharmacyName)
        }
    }

    fun removePharmacyCollection(pharmacyName: String) {
        val medicinesWithNotification = FavoritePharmaciesRepository(this).getFavoritePharmacies()
        for (medicineName in medicinesWithNotification) {
            addCollectionListener(medicineName, pharmacyName)
        }
    }
}