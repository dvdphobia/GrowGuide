package com.growguide.app.activities

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.growguide.app.R
import com.growguide.app.adapters.PlantAdapter
import com.growguide.app.models.Plant

/**
 * Main screen showing the user's plant list.
 * Uses addSnapshotListener for real-time updates from Firestore.
 * Toolbar includes search, profile, and logout options; FAB navigates to AddPlantActivity.
 * Shows an offline banner when network is unavailable.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var plantAdapter: PlantAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: android.widget.TextView
    private lateinit var offlineBanner: android.widget.TextView
    private var plantList = mutableListOf<Plant>()
    private var allPlants = mutableListOf<Plant>()
    private var snapshotListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSupportActionBar(findViewById(R.id.toolbar))

        recyclerView = findViewById(R.id.plantRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        offlineBanner = findViewById(R.id.offlineBanner)
        val addPlantFab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.addPlantFab)

        recyclerView.layoutManager = LinearLayoutManager(this)

        plantAdapter = PlantAdapter(plantList) { plant ->
            val intent = Intent(this, PlantDetailActivity::class.java)
            intent.putExtra("PLANT_ID", plant.id)
            intent.putExtra("PLANT_NAME", plant.name)
            intent.putExtra("PLANT_TYPE", plant.type)
            intent.putExtra("PLANT_NOTES", plant.notes)
            intent.putExtra("PLANT_PHOTO_URL", plant.photoUrl)
            intent.putExtra("PLANT_LAST_WATERED", plant.lastWatered?.seconds ?: 0L)
            intent.putExtra("PLANT_WATERING_FREQ", plant.wateringFrequency)
            startActivity(intent)
        }
        recyclerView.adapter = plantAdapter

        addPlantFab.setOnClickListener {
            startActivity(Intent(this, AddPlantActivity::class.java))
        }

        monitorConnectivity()
    }

    override fun onStart() {
        super.onStart()
        listenForPlants()
    }

    override fun onStop() {
        super.onStop()
        snapshotListener?.remove()
    }

    private fun listenForPlants() {
        val userId = auth.currentUser?.uid ?: return

        snapshotListener = db.collection("users")
            .document(userId)
            .collection("plants")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.widget.Toast.makeText(this, "Error loading plants: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                allPlants.clear()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val plant = doc.toObject(Plant::class.java)
                        if (plant != null) {
                            plant.id = doc.id
                            allPlants.add(plant)
                        }
                    }
                }

                applyFilter("")
                updateEmptyState()
            }
    }

    private fun applyFilter(query: String) {
        plantList.clear()
        val q = query.lowercase().trim()
        if (q.isEmpty()) {
            plantList.addAll(allPlants)
        } else {
            plantList.addAll(allPlants.filter {
                it.name.lowercase().contains(q) || it.type.lowercase().contains(q)
            })
        }
        plantAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (plantList.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilter(newText ?: "")
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_logout -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun monitorConnectivity() {
        val cm = ContextCompat.getSystemService(this, ConnectivityManager::class.java) ?: return
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { offlineBanner.visibility = View.GONE }
            }
            override fun onLost(network: Network) {
                runOnUiThread { offlineBanner.visibility = View.VISIBLE }
            }
        })
    }
}
