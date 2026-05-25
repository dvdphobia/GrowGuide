package com.growguide.app.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.growguide.app.R
import com.growguide.app.models.LogEntry
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Displays a line chart of plant growth metrics over time.
 * Supports height (cm) and leaf count from log entries.
 */
class PlantGrowthChartActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var lineChart: LineChart
    private lateinit var emptyChartText: TextView

    private var plantId: String = ""
    private var plantName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_growth_chart)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        plantId = intent.getStringExtra("PLANT_ID") ?: ""
        plantName = intent.getStringExtra("PLANT_NAME") ?: ""

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.growth_chart)

        lineChart = findViewById(R.id.growthLineChart)
        emptyChartText = findViewById(R.id.emptyChartText)

        setupChart()
        loadGrowthData()
    }

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.setDragEnabled(true)
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            private val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return formatter.format(Date(value.toLong()))
            }
        }

        lineChart.axisLeft.setDrawGridLines(true)
        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = true
    }

    private fun loadGrowthData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("plants").document(plantId)
            .collection("logs")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { snapshot ->
                val logs = mutableListOf<LogEntry>()
                for (doc in snapshot.documents) {
                    val log = doc.toObject(LogEntry::class.java)
                    if (log != null) {
                        log.id = doc.id
                        logs.add(log)
                    }
                }
                displayChart(logs)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading chart: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayChart(logs: List<LogEntry>) {
        if (logs.isEmpty()) {
            lineChart.visibility = View.GONE
            emptyChartText.visibility = View.VISIBLE
            return
        }

        val heightEntries = mutableListOf<Entry>()
        val leafEntries = mutableListOf<Entry>()
        var hasHeight = false
        var hasLeaves = false

        for (log in logs) {
            val time = log.createdAt?.toDate()?.time?.toFloat() ?: continue
            if (log.heightCm > 0) {
                heightEntries.add(Entry(time, log.heightCm.toFloat()))
                hasHeight = true
            }
            if (log.leafCount > 0) {
                leafEntries.add(Entry(time, log.leafCount.toFloat()))
                hasLeaves = true
            }
        }

        if (!hasHeight && !hasLeaves) {
            lineChart.visibility = View.GONE
            emptyChartText.visibility = View.VISIBLE
            emptyChartText.text = "No numeric growth data yet. Add height or leaf count to your log entries."
            return
        }

        lineChart.visibility = View.VISIBLE
        emptyChartText.visibility = View.GONE

        val dataSets = mutableListOf<LineDataSet>()

        if (hasHeight) {
            val heightDataSet = LineDataSet(heightEntries, "Height (cm)").apply {
                color = getColor(R.color.primary)
                setCircleColor(getColor(R.color.primary))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
            }
            dataSets.add(heightDataSet)
        }

        if (hasLeaves) {
            val leafDataSet = LineDataSet(leafEntries, "Leaf Count").apply {
                color = getColor(R.color.secondary)
                setCircleColor(getColor(R.color.secondary))
                lineWidth = 2f
                circleRadius = 4f
                setDrawValues(false)
            }
            dataSets.add(leafDataSet)
        }

        lineChart.data = LineData(dataSets.toList())
        lineChart.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
