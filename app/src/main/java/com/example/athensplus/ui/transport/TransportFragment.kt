@file:Suppress("SpellCheckingInspection")

package com.example.athensplus.ui.transport

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.athensplus.R
import com.example.athensplus.databinding.FragmentTransportBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteStep(
    val startLocation: LatLng,
    val endLocation: LatLng,
    val instructions: String,
    val distance: String,
    val duration: String
)

data class TransitInstruction(
    val type: String,
    val details: String,
    val duration: String
)

class TransportFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentTransportBinding? = null
    private val binding get() = _binding!!
    
    private var googleMap: GoogleMap? = null
    private var directionsButton: Button? = null
    private var currentTransitInstructions: List<TransitStep> = emptyList()
    private lateinit var geocoder: Geocoder
    private val apiKey = "AIzaSyAbCaNy9okak33ITCpb1MWR_Idu6wqQq14"
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationPermissionGranted = false
    private var selectedLocation: LatLng? = null
    private var currentMarkers: MutableList<Marker> = mutableListOf()
    private var selectedMode: String = "Metro"
    private var selectedLine: String = "All Lines"
    private var selectedStartStation: MetroStation? = null
    private var selectedEndStation: MetroStation? = null
    private var startStationMarker: Marker? = null
    private var endStationMarker: Marker? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    data class TransitStep(
        val mode: String, // "WALKING", "BUS", "METRO", "TRAM"
        val instruction: String,
        val duration: String,
        val line: String? = null,
        val departureStop: String? = null,
        val arrivalStop: String? = null,
        val nextArrival: String? = null,
        val walkingDistance: String? = null
    )

    data class MetroStation(
        val nameGreek: String,
        val nameEnglish: String,
        val coords: LatLng,
        val isInterchange: Boolean = false
    )

    data class TramStation(
        val name: String,
        val coords: LatLng
    )

    data class BusStop(
        val name: String,
        val coords: LatLng
    )

    data class TimetableTable(
        val direction: String,
        val headers: List<String>,
        val rows: List<List<String>>
    )

    // Official Athens Metro lines data (partial, for brevity; full list should be used in production)
    val metroLine1 = listOf(
        MetroStation("Πειραιάς", "Piraeus", LatLng(37.94806117043078, 23.643235606587858)),
        MetroStation("Φάληρο", "Faliro", LatLng(37.94503590091905, 23.665229397093032)),
        MetroStation("Μοσχάτο", "Moschato", LatLng(37.95503411820899, 23.679616546345812)),
        MetroStation("Καλλιθέα", "Kallithea", LatLng(37.96038993809849, 23.69735115718288)),
        MetroStation("Ταύρος", "Tavros", LatLng(37.962439214458016, 23.703328766874794)),
        MetroStation("Πετράλωνα", "Petralona", LatLng(37.9686355751221, 23.709296406789832)),
        MetroStation("Θησείο", "Thiseio", LatLng(37.97673744990198, 23.72063841824465)),
        MetroStation("Μοναστηράκι", "Monastiraki", LatLng(37.976114278617565, 23.725633963810413), isInterchange = true),
        MetroStation("Ομόνοια", "Omonia", LatLng(37.98420036534532, 23.728709865494178), isInterchange = true),
        MetroStation("Βικτώρια", "Victoria", LatLng(37.993070240716015, 23.730372320299832)),
        MetroStation("Αττική", "Attiki", LatLng(37.99931374537682, 23.722117655786956), isInterchange = true),
        MetroStation("Άγιος Νικόλαος", "Agios Nikolaos", LatLng(38.00692137614022, 23.72773410379398)),
        MetroStation("Κάτω Πατήσια", "Kato Patisia", LatLng(38.01160376209793, 23.728755698511065)),
        MetroStation("Άγιος Ελευθέριος", "Agios Eleftherios", LatLng(38.02012753169261, 23.731728912700053)),
        MetroStation("Άνω Πατήσια", "Ano Patisia", LatLng(38.02382144738529, 23.736039815065507)),
        MetroStation("Περισσός", "Perissos", LatLng(38.03266821407559, 23.744712039673153)),
        MetroStation("Πευκάκια", "Pefkakia", LatLng(38.037165103011894, 23.75008681005388)),
        MetroStation("Νέα Ιωνία", "Nea Ionia", LatLng(38.041430, 23.754835)),
        MetroStation("Ηράκλειο", "Irakleio", LatLng(38.0462292394805, 23.76607388463279)),
        MetroStation("Ειρήνη", "Eirini", LatLng(38.04330588326173, 23.783400691393396)),
        MetroStation("Νερατζιώτισσα", "Neratziotissa", LatLng(38.04484194992877, 23.792862889776107)),
        MetroStation("Μαρούσι", "Marousi", LatLng(38.05619686161689, 23.80503609761142)),
        MetroStation("Κ.Α.Τ.", "K.A.T.", LatLng(38.06591837925154, 23.804017818766344)),
        MetroStation("Κηφισιά", "Kifisia", LatLng(38.07373290671199, 23.808305136483852))
    )

    val metroLine2 = listOf(
        MetroStation("Ελληνικό", "Elliniko", LatLng(37.892649697635335, 23.74758851630924)),
        MetroStation("Αργυρούπολη", "Argyroupoli", LatLng(37.90312049238752, 23.745938957712156)),
        MetroStation("Άλιμος", "Alimos", LatLng(37.91813201276993, 23.744191423118377)),
        MetroStation("Ηλιούπολη", "Ilioupoli", LatLng(37.92980661834609, 23.744523084603305)),
        MetroStation("Άγιος Δημήτριος", "Agios Dimitrios", LatLng(37.94057253079489, 23.740736185819)),
        MetroStation("Δάφνη", "Dafni", LatLng(37.949211541316096, 23.737265287277935)),
        MetroStation("Άγιος Ιωάννης", "Agios Ioannis", LatLng(37.95663952647803, 23.734630922893803)),
        MetroStation("Νέος Κόσμος", "Neos Kosmos", LatLng(37.957596771154535, 23.728486607161212)),
        MetroStation("Συγγρού-Φιξ", "Syngrou-Fix", LatLng(37.964075081691085, 23.726455951266303)),
        MetroStation("Ακρόπολη", "Acropolis", LatLng(37.968791278421335, 23.729646981926912)),
        MetroStation("Σύνταγμα", "Syntagma", LatLng(37.97568408036298, 23.73535892766162), isInterchange = true),
        MetroStation("Πανεπιστήμιο", "Panepistimio", LatLng(37.98037292954861, 23.73307575719024)),
        MetroStation("Ομόνοια", "Omonia", LatLng(37.98420036534532, 23.728709865494178), isInterchange = true),
        MetroStation("Μεταξουργείο", "Metaxourgeio", LatLng(37.986246898920776, 23.721171206907755)),
        MetroStation("Σταθμός Λαρίσης", "Larissa Station", LatLng(37.99199512016312, 23.721114088402935)),
        MetroStation("Αττική", "Attiki", LatLng(37.99931374537682, 23.722117655786956), isInterchange = true),
        MetroStation("Σεπόλια", "Sepolia", LatLng(38.0026582025189, 23.713618531964784)),
        MetroStation("Άγιος Αντώνιος", "Agios Antonios", LatLng(38.00668188937669, 23.699473817226718)),
        MetroStation("Περιστέρι", "Peristeri", LatLng(38.01316415384895, 23.695482322472614)),
        MetroStation("Ανθούπολη", "Anthoupoli", LatLng(38.017089551895666, 23.691134987138266))
    )

    val metroLine3 = listOf(
        MetroStation("Δημοτικό Θέατρο", "Dimotiko Theatro", LatLng(37.942987138386535, 23.64761813894496)),
        MetroStation("Πειραιάς", "Piraeus", LatLng(37.94826317831643, 23.643233750670223)),
        MetroStation("Μανιάτικα", "Maniatika", LatLng(37.959568185347536, 23.63969873316419)),
        MetroStation("Νίκαια", "Nikaia", LatLng(37.965641240168104, 23.647316509152287)),
        MetroStation("Κορυδαλλός", "Korydallos", LatLng(37.97702502617706, 23.65035476643652)),
        MetroStation("Αγία Βαρβάρα", "Agia Varvara", LatLng(37.9900618690817, 23.65932429578851)),
        MetroStation("Αγία Μαρίνα", "Agia Marina", LatLng(37.99722077049423, 23.667344819154156)),
        MetroStation("Αιγάλεω", "Aigaleo", LatLng(37.99197010485269, 23.68176929794242)),
        MetroStation("Ελαιώνας", "Elaionas", LatLng(37.98787205461006, 23.69420027290326)),
        MetroStation("Κεραμεικός", "Kerameikos", LatLng(37.97855799587991, 23.711527839122848)),
        MetroStation("Μοναστηράκι", "Monastiraki", LatLng(37.976114278617565, 23.725633963810413), isInterchange = true),
        MetroStation("Σύνταγμα", "Syntagma", LatLng(37.97568408036298, 23.73535892766162), isInterchange = true),
        MetroStation("Ευαγγελισμός", "Evangelismos", LatLng(37.97644961902063, 23.747314144474466)),
        MetroStation("Μέγαρο Μουσικής", "Megaro Mousikis", LatLng(37.979289172647675, 23.75291481651404)),
        MetroStation("Αμπελόκηποι", "Ambelokipi", LatLng(37.98736656071125, 23.75703357239967)),
        MetroStation("Πανόρμου", "Panormou", LatLng(37.99324246685057, 23.763372268004527)),
        MetroStation("Κατεχάκη", "Katehaki", LatLng(37.99370654358875, 23.776255284953653)),
        MetroStation("Εθνική Άμυνα", "Ethniki Amyna", LatLng(38.00031275851294, 23.78568239514545)),
        MetroStation("Χολαργός", "Cholargos", LatLng(38.00456995086606, 23.794701985734786)),
        MetroStation("Νομισματοκοπείο", "Nomismatokopeio", LatLng(38.00975632474917, 23.805353216074298)),
        MetroStation("Αγία Παρασκευή", "Agia Paraskevi", LatLng(38.01773132519414, 23.81262417681338)),
        MetroStation("Χαλάνδρι", "Chalandri", LatLng(38.021437720256365, 23.82139210120635)),
        MetroStation("Δουκίσσης Πλακεντίας", "Doukissis Plakentias", LatLng(38.024189316487046, 23.833724220891263)),
        MetroStation("Παλλήνη", "Pallini", LatLng(38.00601053761855, 23.869545425355156)),
        MetroStation("Παιανία-Κάντζα", "Paiania-Kantza", LatLng(37.98417062046167, 23.869756642430122)),
        MetroStation("Κορωπί", "Koropi", LatLng(37.9131505428903, 23.8958794023863)),
        MetroStation("Αεροδρόμιο", "Airport", LatLng(37.936942775310406, 23.944783037869243))
    )

    private val tramStations = listOf(
        TramStation("Syntagma", LatLng(37.9755, 23.7348)),
        TramStation("Zappeio", LatLng(37.9717, 23.7372)),
        TramStation("Neos Kosmos", LatLng(37.9578, 23.7289)),
        TramStation("Agios Ioannis", LatLng(37.9539, 23.7250)),
        TramStation("Dafni", LatLng(37.9500, 23.7211)),
        TramStation("Agios Dimitrios", LatLng(37.9461, 23.7172)),
        TramStation("Alimos", LatLng(37.9422, 23.7133)),
        TramStation("Elliniko", LatLng(37.9383, 23.7094)),
        TramStation("Asklipio Voulas", LatLng(37.9344, 23.7055)),
        TramStation("Voula", LatLng(37.9306, 23.7017))
    )

    private val busStops = listOf(
        BusStop("Syntagma", LatLng(37.9755, 23.7348)),
        BusStop("Omonia", LatLng(37.9833, 23.7283)),
        BusStop("Monastiraki", LatLng(37.9767, 23.7250)),
        BusStop("Thissio", LatLng(37.9767, 23.7217)),
        BusStop("Kerameikos", LatLng(37.9767, 23.7183)),
        BusStop("Gazi", LatLng(37.9767, 23.7150)),
        BusStop("Kallithea", LatLng(37.9550, 23.7000)),
        BusStop("Nea Smyrni", LatLng(37.9450, 23.7100)),
        BusStop("Palaio Faliro", LatLng(37.9350, 23.7000)),
        BusStop("Glyfada", LatLng(37.8650, 23.7500))
    )

    val line3CurvedPoints = listOf(
        LatLng(37.942987138386535, 23.64761813894496), // Dimotiko Theatro
        LatLng(37.945, 23.645), // control point
        LatLng(37.94826317831643, 23.643233750670223), // Pireus
        LatLng(37.952, 23.641), // control point
        LatLng(37.959568185347536, 23.63969873316419), // Maniatika
        LatLng(37.962, 23.643), // control point
        LatLng(37.965641240168104, 23.647316509152287), // Nikaia
        LatLng(37.971, 23.649), // control point
        LatLng(37.97702502617706, 23.65035476643652), // Koridalos
        LatLng(37.984, 23.654), // control point
        LatLng(37.9900618690817, 23.65932429578851), // Agia Barbara
        LatLng(37.994, 23.663), // control point
        LatLng(37.99722077049423, 23.667344819154156), // Agia Marina
        LatLng(37.993, 23.674), // control point
        LatLng(37.99197010485269, 23.68176929794242), // Aigaleo
        LatLng(37.990, 23.688), // control point
        LatLng(37.98787205461006, 23.69420027290326), // Elaionas
        LatLng(37.983, 23.703), // control point
        LatLng(37.97855799587991, 23.711527839122848), // Keramikos
        LatLng(37.977, 23.718), // control point
        LatLng(37.976114278617565, 23.725633963810413), // Monastiraki
        LatLng(37.976, 23.730), // control point
        LatLng(37.97568408036298, 23.73535892766162), // Syntagma
        LatLng(37.976, 23.741), // control point
        LatLng(37.97644961902063, 23.747314144474466), // Evangelismos
        LatLng(37.978, 23.750), // control point
        LatLng(37.979289172647675, 23.75291481651404), // Megaro Mousikis
        LatLng(37.983, 23.754), // control point
        LatLng(37.98736656071125, 23.75703357239967), // Ameplokipoi
        LatLng(37.990, 23.760), // control point
        LatLng(37.99324246685057, 23.763372268004527), // Panormou
        LatLng(37.993, 23.770), // control point
        LatLng(37.99370654358875, 23.776255284953653), // Katexaki
        LatLng(37.997, 23.781), // control point
        LatLng(38.00031275851294, 23.78568239514545), // Ethniki Amyna
        LatLng(38.004, 23.790), // control point
        LatLng(38.00456995086606, 23.794701985734786), // Holargos
        LatLng(38.007, 23.800), // control point
        LatLng(38.00975632474917, 23.805353216074298), // Nomismatokopeio
        LatLng(38.013, 23.809), // control point
        LatLng(38.01773132519414, 23.81262417681338), // Agia Paraskevi
        LatLng(38.019, 23.817), // control point
        LatLng(38.021437720256365, 23.82139210120635), // Xalandri
        LatLng(38.023, 23.827), // control point
        LatLng(38.024189316487046, 23.833724220891263), // Doukisis Plakentias
        LatLng(38.015, 23.851), // control point
        LatLng(38.00601053761855, 23.869545425355156), // Pallini
        LatLng(37.995, 23.869), // control point
        LatLng(37.98417062046167, 23.869756642430122), // Paiania-Katza
        LatLng(37.950, 23.883), // control point
        LatLng(37.9131505428903, 23.8958794023863), // Koropi
        LatLng(37.925, 23.920), // control point
        LatLng(37.936942775310406, 23.944783037869243) // Airport
    )

    val line2CurvedPoints = listOf(
        LatLng(37.892649697635335, 23.74758851630924), // Elliniko
        LatLng(37.898, 23.747), // control point
        LatLng(37.90312049238752, 23.745938957712156), // Argyroupoli
        LatLng(37.911, 23.745), // control point
        LatLng(37.91813201276993, 23.744191423118377), // Alimos
        LatLng(37.924, 23.744), // control point
        LatLng(37.92980661834609, 23.744523084603305), // Hlioupoli
        LatLng(37.935, 23.742), // control point
        LatLng(37.94057253079489, 23.740736185819), // Agios Dimitrios
        LatLng(37.945, 23.739), // control point
        LatLng(37.949211541316096, 23.737265287277935), // Dafni
        LatLng(37.953, 23.736), // control point
        LatLng(37.95663952647803, 23.734630922893803), // Agios Ioanis
        LatLng(37.957, 23.731), // control point
        LatLng(37.957596771154535, 23.728486607161212), // Neos Kosmos
        LatLng(37.961, 23.727), // control point
        LatLng(37.964075081691085, 23.726455951266303), // Sugrou-Fix
        LatLng(37.966, 23.728), // control point
        LatLng(37.968791278421335, 23.729646981926912), // Akropoli
        LatLng(37.972, 23.732), // control point
        LatLng(37.97568408036298, 23.73535892766162), // Syntagma
        LatLng(37.978, 23.734), // control point
        LatLng(37.98037292954861, 23.73307575719024), // Panepistimio
        LatLng(37.982, 23.731), // control point
        LatLng(37.98420036534532, 23.728709865494178), // Omonoia
        LatLng(37.985, 23.725), // control point
        LatLng(37.986246898920776, 23.721171206907755), // Metaksourgeio
        LatLng(37.989, 23.721), // control point
        LatLng(37.99199512016312, 23.721114088402935), // Stathmos Larisis
        LatLng(37.995, 23.721), // control point
        LatLng(37.99931374537682, 23.722117655786956), // Attiki
        LatLng(38.001, 23.718), // control point
        LatLng(38.0026582025189, 23.713618531964784), // Sepolia
        LatLng(38.004, 23.706), // control point
        LatLng(38.00668188937669, 23.699473817226718), // Agios Antonios
        LatLng(38.010, 23.697), // control point
        LatLng(38.01316415384895, 23.695482322472614), // Peristeri
        LatLng(38.015, 23.693), // control point
        LatLng(38.017089551895666, 23.691134987138266) // Anthoupoli
    )

    val line1CurvedPoints = listOf(
        LatLng(38.07373290671199, 23.808305136483852), // Kifisia
        LatLng(38.070, 23.806), // control point
        LatLng(38.06591837925154, 23.804017818766344), // KAT
        LatLng(38.061, 23.804), // control point
        LatLng(38.05619686161689, 23.80503609761142), // Marousi
        LatLng(38.050, 23.799), // control point
        LatLng(38.04484194992877, 23.792862889776107), // Neratziotisa
        LatLng(38.043, 23.788), // control point
        LatLng(38.04330588326173, 23.783400691393396), // Eirini
        LatLng(38.045, 23.775), // control point
        LatLng(38.0462292394805, 23.76607388463279), // Hrakleio
        LatLng(38.042, 23.755), // control point
        LatLng(38.037165103011894, 23.75008681005388), // Peukakia
        LatLng(38.035, 23.747), // control point
        LatLng(38.03266821407559, 23.744712039673153), // Perissos
        LatLng(38.028, 23.740), // control point
        LatLng(38.02382144738529, 23.736039815065507), // Ano Patisia
        LatLng(38.022, 23.734), // control point
        LatLng(38.02012753169261, 23.731728912700053), // Agios Eleftherios
        LatLng(38.016, 23.730), // control point
        LatLng(38.01160376209793, 23.728755698511065), // Kato Patisia
        LatLng(38.009, 23.728), // control point
        LatLng(38.00692137614022, 23.72773410379398), // Agios Nikolaos
        LatLng(38.003, 23.725), // control point
        LatLng(37.99931374537682, 23.722117655786956), // Attiki
        LatLng(37.996, 23.726), // control point
        LatLng(37.993070240716015, 23.730372320299832), // Victoria
        LatLng(37.988, 23.729), // control point
        LatLng(37.98420036534532, 23.728709865494178), // Omonoia
        LatLng(37.980, 23.727), // control point
        LatLng(37.976114278617565, 23.725633963810413), // Monastiraki
        LatLng(37.976, 23.723), // control point
        LatLng(37.97673744990198, 23.72063841824465), // Thiseio
        LatLng(37.972, 23.715), // control point
        LatLng(37.9686355751221, 23.709296406789832), // Petralona
        LatLng(37.965, 23.706), // control point
        LatLng(37.962439214458016, 23.703328766874794), // Tavros
        LatLng(37.961, 23.700), // control point
        LatLng(37.96038993809849, 23.69735115718288), // Kallithea
        LatLng(37.957, 23.688), // control point
        LatLng(37.95503411820899, 23.679616546345812), // Moschato
        LatLng(37.950, 23.672), // control point
        LatLng(37.94503590091905, 23.665229397093032), // Neo Faliro
        LatLng(37.947, 23.654), // control point
        LatLng(37.94806117043078, 23.643235606587858) // Piraeus
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTransportBinding.inflate(inflater, container, false)

        try {
            // Initialize MapView
            binding.mapView.onCreate(savedInstanceState)
            // Initialize location services
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            // Get location permission
            getLocationPermission()
            // Initialize geocoder
            geocoder = Geocoder(requireContext(), Locale.getDefault())
            // Setup selection controls
            setupSelectionControls()
            Log.d("TransportFragment", "Fragment view created successfully")
        } catch (e: Exception) {
            Log.e("TransportFragment", "Error in onCreateView", e)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.getMapAsync(this)
        // Set up directions button click listener
        binding.buttonDirections.setOnClickListener {
            showTransitDirections()
        }
        // Request location permission
        getLocationPermission()

        // Set up mode picker button
        binding.buttonModePicker.setOnClickListener {
            showModePickerMenu()
        }
        // Set up line picker button
        binding.buttonLinePicker.setOnClickListener {
            showLinePickerMenu()
        }
        
        // Set up reset map button
        binding.resetMapButton.setOnClickListener {
            resetMap()
        }
        
        // Set up airport timetable button
        binding.airportTimetableButton.setOnClickListener {
            if (selectedStartStation != null) {
                showAirportTimetable(selectedStartStation!!)
            }
        }
        
        // Initialize UI state - hide reset button initially
        updateSelectionIndicator()
    }

    // Add MapView lifecycle methods
    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideDirectionsButton()
        binding.mapView.onDestroy()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
            updateLocationUI()
            getDeviceLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                    updateLocationUI()
                    getDeviceLocation()
                }
            }
        }
    }

    private fun updateLocationUI() {
        if (googleMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                googleMap?.isMyLocationEnabled = true
                googleMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                googleMap?.isMyLocationEnabled = false
                googleMap?.uiSettings?.isMyLocationButtonEnabled = false
            }
        } catch (e: SecurityException) {
            Log.e("TransportFragment", "Error updating location UI", e)
        }
    }

    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        googleMap?.animateCamera(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(userLatLng, 15f)
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("TransportFragment", "Error getting device location", e)
            // Fallback to Athens center
            val athensCenter = LatLng(37.9755, 23.7348)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(athensCenter, 15f))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        Log.d("TransportFragment", "Map is ready")

        // Force custom map style
        try {
            val style = MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            googleMap.setMapStyle(style)
            Log.d("TransportFragment", "Custom map style applied")
        } catch (e: Exception) {
            Log.e("TransportFragment", "Error applying map style", e)
        }

        // Disable all overlays and UI elements
        googleMap.isTrafficEnabled = false
        googleMap.isBuildingsEnabled = true
        googleMap.isIndoorEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.uiSettings.isCompassEnabled = false
        googleMap.uiSettings.isIndoorLevelPickerEnabled = false
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isTiltGesturesEnabled = false
        googleMap.uiSettings.isRotateGesturesEnabled = false

        // Only add schematic's stations and lines based on selection
        updateMapForSelection()
        updateStationMarkers(googleMap.cameraPosition.zoom)
        googleMap.setOnCameraIdleListener {
            val zoom = googleMap.cameraPosition.zoom
            updateStationMarkers(zoom)
        }

        // No other overlays or listeners
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
            googleMap?.isMyLocationEnabled = true // Show blue dot
            moveToCurrentLocationIfPossible()
        } else {
            // Request permissions if not granted (optional: handle this if not already)
        }

        // Add marker click listener
        googleMap.setOnMarkerClickListener { marker ->
            showStationMenu(marker)
            true
        }
    }

    private fun moveToCurrentLocationIfPossible() {
        if (locationPermissionGranted) {
            try {
                fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        googleMap?.animateCamera(
                            com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(userLatLng, 15f)
                        )
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun createStationCircleMarker(context: Context, outlineColor: Int, outlineWidth: Float = 12f, radius: Float = 24f, isSelected: Boolean = false): BitmapDescriptor {
        val actualRadius = if (isSelected) radius * 1.5f else radius
        val actualOutlineWidth = if (isSelected) outlineWidth * 1.5f else outlineWidth
        val size = ((actualRadius + actualOutlineWidth) * 2).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        // Draw outline
        val outlinePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = actualOutlineWidth
            color = outlineColor
        }
        canvas.drawCircle(center, center, actualRadius, outlinePaint)

        // Draw fill - black for selected stations, white for others
        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = if (isSelected) Color.BLACK else Color.WHITE
        }
        canvas.drawCircle(center, center, actualRadius - actualOutlineWidth / 2, fillPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun createStationLabel(context: Context, station: MetroStation, color: Int, textOffset: Float = 0f, isFirstOrLast: Boolean = false): BitmapDescriptor {
        val greekText = station.nameGreek
        val englishText = station.nameEnglish
        val greekFontSize = 54f
        val englishFontSize = 40f
        val padding = 10f
        val lineSpacing = 6f
        val labelOffset = 10f // bring text closer to the circle

        val greekTypeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
        val englishTypeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)

        val greekPaint = Paint().apply {
            isAntiAlias = true
            textSize = greekFontSize
            typeface = greekTypeface
            textAlign = Paint.Align.LEFT
        }

        val englishPaint = Paint().apply {
            isAntiAlias = true
            textSize = englishFontSize
            typeface = englishTypeface
            textAlign = Paint.Align.LEFT
        }

        val greekBounds = Rect()
        val englishBounds = Rect()
        greekPaint.getTextBounds(greekText, 0, greekText.length, greekBounds)
        englishPaint.getTextBounds(englishText, 0, englishText.length, englishBounds)

        val textWidth = maxOf(greekPaint.measureText(greekText), englishPaint.measureText(englishText))
        val textHeight = greekBounds.height() + englishBounds.height() + lineSpacing
        val width = (textWidth + 2 * padding).toInt()
        val height = if (isFirstOrLast) {
            (textHeight + 2 * padding).toInt()
        } else {
            (textHeight + 2 * padding + textOffset).toInt()
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (isFirstOrLast) {
            val bgPaint = Paint().apply {
                this.color = color
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 24f, 24f, bgPaint)
            greekPaint.color = Color.WHITE
            englishPaint.color = Color.WHITE
        } else if (station.isInterchange) {
            greekPaint.color = Color.BLACK
            englishPaint.color = Color.BLACK
        } else {
            greekPaint.color = color
            englishPaint.color = color
        }

        // Draw Greek text
        canvas.drawText(
            greekText,
            padding,
            padding + greekBounds.height() - labelOffset,
            greekPaint
        )

        // Draw English text
        canvas.drawText(
            englishText,
            padding,
            padding + greekBounds.height() + lineSpacing + englishBounds.height() - labelOffset,
            englishPaint
        )

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun updateStationMarkers(zoom: Float) {
        currentMarkers.forEach { it.remove() }
        currentMarkers.clear()
        val showAll = zoom >= 12.5f
        if (!showAll) return

        // Update the map lines first
        updateMapForSelection()

        fun addMarkers(line: List<MetroStation>, color: Int) {
            // Keep track of existing labels for overlap checking
            val existingLabels = mutableListOf<Pair<LatLng, String>>()
            
            line.forEachIndexed { index, station ->
                // Get interchange station if it exists
                val interchangeStation = if (selectedStartStation != null && selectedEndStation != null) {
                    findInterchangeStation(selectedStartStation!!, selectedEndStation!!)
                } else null

                // Skip stations that are not between selected start and end stations when both are selected
                if (selectedStartStation != null && selectedEndStation != null) {
                    val isOnRoute = if (interchangeStation != null) {
                        // Check if station is on either segment of the route
                        isStationBetween(station, selectedStartStation!!, interchangeStation) ||
                        isStationBetween(station, interchangeStation, selectedEndStation!!)
                    } else {
                        isStationBetween(station, selectedStartStation!!, selectedEndStation!!)
                    }
                    if (!isOnRoute && station != selectedStartStation && station != selectedEndStation && station != interchangeStation) {
                        return@forEachIndexed
                    }
                }

                val isSelected = station == selectedStartStation || station == selectedEndStation || station == interchangeStation
                val isFirstOrLast = (index == 0 || index == line.size - 1)
                
                val circleMarker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(station.coords)
                        .title("${station.nameGreek} / ${station.nameEnglish}")
                        .icon(createStationCircleMarker(requireContext(), color, isSelected = isSelected))
                        .anchor(0.5f, 0.5f)
                        .zIndex(if (isSelected) 3f else if (station.isInterchange) 2f else 1f)
                )
                
                circleMarker?.let { 
                    currentMarkers.add(it)
                    // Store references to selected station markers
                    when (station) {
                        selectedStartStation -> startStationMarker = it
                        selectedEndStation -> endStationMarker = it
                    }
                }

                // Find optimal position for text that doesn't overlap with lines
                val textPosition = findOptimalTextPosition(
                    station.coords,
                    station.nameEnglish,
                    existingLabels,
                    listOf(line1CurvedPoints, line2CurvedPoints, line3CurvedPoints)
                )
                
                val textMarker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(textPosition)
                        .icon(createStationLabel(requireContext(), station, color, 0f, isFirstOrLast))
                        .anchor(0f, 0.5f)
                        .zIndex(1f)
                )
                textMarker?.let { 
                    currentMarkers.add(it)
                    existingLabels.add(Pair(textPosition, station.nameEnglish))
                }
            }
        }
        when (selectedLine) {
            "All Lines" -> {
                addMarkers(metroLine1, Color.parseColor("#009640"))
                addMarkers(metroLine2, Color.parseColor("#e30613"))
                addMarkers(metroLine3, Color.parseColor("#0057a8"))
            }
            "Line 1" -> addMarkers(metroLine1, Color.parseColor("#009640"))
            "Line 2" -> addMarkers(metroLine2, Color.parseColor("#e30613"))
            "Line 3" -> addMarkers(metroLine3, Color.parseColor("#0057a8"))
        }
    }

    private fun isStationBetween(station: MetroStation, start: MetroStation, end: MetroStation): Boolean {
        // Find which line contains both start and end stations
        val line = when {
            metroLine1.contains(start) && metroLine1.contains(end) -> metroLine1
            metroLine2.contains(start) && metroLine2.contains(end) -> metroLine2
            metroLine3.contains(start) && metroLine3.contains(end) -> metroLine3
            else -> return false // Stations are on different lines
        }

        val startIndex = line.indexOf(start)
        val endIndex = line.indexOf(end)
        val stationIndex = line.indexOf(station)

        // Check if station index is between start and end indices (inclusive)
        return if (startIndex <= endIndex) {
            stationIndex in startIndex..endIndex
        } else {
            stationIndex in endIndex..startIndex
        }
    }

    private fun calculateTextOffset(station: MetroStation, line: List<MetroStation>, index: Int): Float {
        var offset = 0f
        
        // Check if text would overlap with metro lines
        val isOnLine = isStationOnLine(station.coords)
        if (isOnLine) {
            offset += 40f // Move text below the line
        }
        
        // Check nearby stations for potential text overlap
        val nearbyStations = mutableListOf<MetroStation>()
        
        // Look at previous and next stations
        if (index > 0) nearbyStations.add(line[index - 1])
        if (index < line.size - 1) nearbyStations.add(line[index + 1])
        
        // Calculate if text might overlap with nearby stations
        nearbyStations.forEach { nearby ->
            val distance = calculateDistance(station.coords, nearby.coords)
            if (distance < 0.005) { // If stations are very close (about 500m)
                offset += 30f
            }
        }
        
        return offset
    }

    private fun isStationOnLine(stationCoords: LatLng): Boolean {
        // Check if station is on any line segment
        val allLines = listOf(line1CurvedPoints, line2CurvedPoints, line3CurvedPoints)
        for (line in allLines) {
            for (i in 0 until line.size - 1) {
                if (isPointNearLineSegment(stationCoords, line[i], line[i + 1])) {
                    return true
                }
            }
        }
        return false
    }

    private fun isPointNearLineSegment(point: LatLng, lineStart: LatLng, lineEnd: LatLng): Boolean {
        val tolerance = 0.0001 // Approximately 10 meters
        
        // Calculate the distance from point to line segment
        val a = point.latitude - lineStart.latitude
        val b = point.longitude - lineStart.longitude
        val c = lineEnd.latitude - lineStart.latitude
        val d = lineEnd.longitude - lineStart.longitude
        
        val dot = a * c + b * d
        val lenSq = c * c + d * d
        
        var param = -1.0
        if (lenSq != 0.0) param = dot / lenSq
        
        var nearestPoint = LatLng(0.0, 0.0)
        if (param < 0) {
            nearestPoint = lineStart
        } else if (param > 1) {
            nearestPoint = lineEnd
        } else {
            nearestPoint = LatLng(
                lineStart.latitude + param * c,
                lineStart.longitude + param * d
            )
        }
        
        val distance = calculateDistance(point, nearestPoint)
        return distance < tolerance
    }

    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)
        
        val dlon = lon2 - lon1
        val dlat = lat2 - lat1
        
        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        
        // Radius of earth in kilometers
        val r = 6371
        
        return c * r
    }

    private fun addTransitStationsToMap() {
        try {
            Log.d("TransportFragment", "Drawing Athens Metro lines and stations")
            googleMap?.clear()
            // Draw Metro Line 1 (Green)
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(line1CurvedPoints)
                    .color(Color.parseColor("#009640"))
                    .width(22f)
                    .geodesic(false)  // Disable geodesic line drawing
                    .jointType(JointType.ROUND)  // Use round joints for smoother connections
            )
            // Draw Metro Line 2 (Red)
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(line2CurvedPoints)
                    .color(Color.parseColor("#e30613"))
                    .width(22f)
                    .geodesic(false)  // Disable geodesic line drawing
                    .jointType(JointType.ROUND)  // Use round joints for smoother connections
            )
            // Draw Metro Line 3 (Blue)
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(line3CurvedPoints)
                    .color(Color.parseColor("#0057a8"))
                    .width(22f)
                    .geodesic(false)  // Disable geodesic line drawing
                    .jointType(JointType.ROUND)  // Use round joints for smoother connections
            )
        } catch (e: Exception) {
            Log.e("TransportFragment", "Error drawing metro map", e)
        }
    }

    private fun showTransitDirections() {
        val toText = binding.editTo.text.toString().trim()
        if (toText.isEmpty()) {
            Toast.makeText(context, "Please enter a destination", Toast.LENGTH_SHORT).show()
            return
        }
        // Clear previous markers and polylines
        googleMap?.clear()
        hideDirectionsButton()
        // Show loading message
        Toast.makeText(context, "Finding best route...", Toast.LENGTH_SHORT).show()
        // Get directions from Google Directions API
        lifecycleScope.launch {
            try {
                val directionsResult = getGoogleDirections("Athens, Greece", toText)
                withContext(Dispatchers.Main) {
                    if (directionsResult != null) {
                        displayDirectionsResult(directionsResult)
                        Toast.makeText(context, "Route found!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Could not find route to destination", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error finding route: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("TransportFragment", "Error getting directions", e)
                }
            }
        }
    }
    
    private suspend fun getGoogleDirections(origin: String, destination: String): DirectionsResult? {
        return withContext(Dispatchers.IO) {
            try {
                val originEncoded = URLEncoder.encode("$origin, Athens, Greece", "UTF-8")
                val destinationEncoded = URLEncoder.encode("$destination, Athens, Greece", "UTF-8")
                
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=$originEncoded" +
                        "&destination=$destinationEncoded" +
                        "&mode=transit" +
                        "&transit_mode=bus|subway" +
                        "&region=gr" +
                        "&language=en" +
                        "&key=$apiKey"
                
                Log.d("TransportFragment", "Directions URL: $url")
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                Log.d("TransportFragment", "Response code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()
                    
                    Log.d("TransportFragment", "API Response: ${response.take(500)}...")
                    
                    parseDirectionsResponse(response)
                } else {
                    Log.e("TransportFragment", "HTTP Error: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e("TransportFragment", "Error calling Directions API", e)
                null
            }
        }
    }
    
    private fun parseDirectionsResponse(response: String): DirectionsResult? {
        try {
            val json = JSONObject(response)
            val status = json.getString("status")
            
            if (status != "OK") {
                Log.e("TransportFragment", "Directions API error: $status")
                return null
            }
            
            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                Log.e("TransportFragment", "No routes found")
                return null
            }
            
            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs")
            val leg = legs.getJSONObject(0)
            
            // Get start and end locations
            val startLocation = leg.getJSONObject("start_location")
            val endLocation = leg.getJSONObject("end_location")
            val startLatLng = LatLng(startLocation.getDouble("lat"), startLocation.getDouble("lng"))
            val endLatLng = LatLng(endLocation.getDouble("lat"), endLocation.getDouble("lng"))
            
            // Parse steps
            val steps = leg.getJSONArray("steps")
            val transitSteps = mutableListOf<TransitStep>()
            val routePoints = mutableListOf<LatLng>()
            
            routePoints.add(startLatLng)
            
            for (i in 0 until steps.length()) {
                val step = steps.getJSONObject(i)
                val travelMode = step.getString("travel_mode")
                val duration = step.getJSONObject("duration").getString("text")
                val instructions = step.getString("html_instructions")
                    .replace("<[^>]*>".toRegex(), "") // Remove HTML tags
                    .replace("&nbsp;", " ")
                
                // Add step end location to route
                val stepEndLocation = step.getJSONObject("end_location")
                routePoints.add(LatLng(stepEndLocation.getDouble("lat"), stepEndLocation.getDouble("lng")))
                
                when (travelMode) {
                    "WALKING" -> {
                        transitSteps.add(TransitStep("WALKING", instructions, duration))
                    }
                    "TRANSIT" -> {
                        val transitDetails = step.getJSONObject("transit_details")
                        val line = transitDetails.getJSONObject("line")
                        val vehicle = line.getJSONObject("vehicle")
                        val vehicleType = vehicle.getString("type")
                        val lineName = if (line.has("short_name")) line.getString("short_name") else line.getString("name")
                        
                        val departureStop = transitDetails.getJSONObject("departure_stop").getString("name")
                        val arrivalStop = transitDetails.getJSONObject("arrival_stop").getString("name")
                        
                        val mode = when (vehicleType) {
                            "SUBWAY", "METRO_RAIL" -> "METRO"
                            "BUS" -> "BUS"
                            "TRAM" -> "TRAM"
                            else -> "TRANSIT"
                        }
                        
                        val detailedInstruction = "Take $lineName from $departureStop to $arrivalStop"
                        transitSteps.add(TransitStep(mode, detailedInstruction, duration, lineName, departureStop, arrivalStop))
                    }
                }
            }
            
            return DirectionsResult(
                startLocation = startLatLng,
                endLocation = endLatLng,
                routePoints = routePoints,
                steps = transitSteps,
                totalDuration = leg.getJSONObject("duration").getString("text"),
                totalDistance = leg.getJSONObject("distance").getString("text")
            )
            
        } catch (e: Exception) {
            Log.e("TransportFragment", "Error parsing directions response", e)
            return null
        }
    }
    
    data class DirectionsResult(
        val startLocation: LatLng,
        val endLocation: LatLng,
        val routePoints: List<LatLng>,
        val steps: List<TransitStep>,
        val totalDuration: String,
        val totalDistance: String
    )
    
    private fun displayDirectionsResult(result: DirectionsResult) {
        // Add markers for start and end points
        googleMap?.addMarker(
            MarkerOptions()
                .position(result.startLocation)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        
        googleMap?.addMarker(
            MarkerOptions()
                .position(result.endLocation)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        
        // Draw route on map
        drawRoute(result.routePoints, result.steps)
        
        // Store instructions for popup
        currentTransitInstructions = result.steps
        
        // Adjust camera to show the route
        val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
        result.routePoints.forEach { builder.include(it) }
        val bounds = builder.build()
        
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        
        // Show directions button
        showDirectionsButton()
    }
    
    private fun drawRoute(routePoints: List<LatLng>, steps: List<TransitStep>) {
        if (routePoints.size < 2) return
        
        // Draw route segments with different colors based on transport mode
        var currentStepIndex = 0
        
        for (i in 0 until routePoints.size - 1) {
            val currentStep = if (currentStepIndex < steps.size) steps[currentStepIndex] else steps.lastOrNull()
            
            val color = when (currentStep?.mode) {
                "WALKING" -> android.graphics.Color.GREEN
                "METRO" -> android.graphics.Color.BLUE
                "BUS" -> android.graphics.Color.rgb(255, 165, 0) // Orange
                "TRAM" -> android.graphics.Color.MAGENTA
                else -> android.graphics.Color.GRAY
            }
            
            val polylineOptions = PolylineOptions()
                .add(routePoints[i])
                .add(routePoints[i + 1])
                .width(6f)
                .color(color)
            
            googleMap?.addPolyline(polylineOptions)
            
            // Move to next step when we've drawn enough segments for current step
            if (i > 0 && currentStepIndex < steps.size - 1) {
                currentStepIndex++
            }
        }
    }
    
    private fun showDirectionsButton() {
        hideDirectionsButton()
        
        directionsButton = Button(requireContext()).apply {
            text = "Directions"
            setBackgroundResource(R.drawable.rounded_directions_button)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(32, 16, 32, 16)
            textSize = 16f
            elevation = 8f
            setOnClickListener {
                val location = selectedLocation ?: LatLng(37.9755, 23.7348)
                showDirectionsPopup(location)
            }
        }
        
        // Only declare this ONCE:
        val mapContainer = binding.mapView.parent as ViewGroup
        
        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 32)
        }
        
        if (mapContainer is FrameLayout) {
            val frameParams = FrameLayout.LayoutParams(layoutParams)
            frameParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            directionsButton?.layoutParams = frameParams
        } else {
            directionsButton?.layoutParams = layoutParams
        }
        
        mapContainer.addView(directionsButton)
    }
    
    private fun hideDirectionsButton() {
        directionsButton?.let { button ->
            (button.parent as? ViewGroup)?.removeView(button)
        }
        directionsButton = null
    }
    
    private fun showDirectionsPopup(selectedLocation: LatLng) {
        try {
            Log.d("TransportFragment", "Showing directions popup for location: $selectedLocation")
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_directions)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        
            // Set up close button
            val closeButton = dialog.findViewById<Button>(R.id.button_close)
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Set up show all buses button
            val showAllBusesButton = dialog.findViewById<Button>(R.id.button_show_all_buses)
            showAllBusesButton.setOnClickListener {
                dialog.dismiss()
                showAllAvailableBuses(selectedLocation)
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("TransportFragment", "Error showing directions popup", e)
            Toast.makeText(
                requireContext(),
                "Error showing directions options",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showAllAvailableBuses(selectedLocation: LatLng) {
        try {
            Log.d("TransportFragment", "Showing all available buses for location: $selectedLocation")
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_bus_directions)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Set up close button
            val closeButton = dialog.findViewById<Button>(R.id.button_close)
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            // Get available buses
            getAvailableBuses(selectedLocation) { buses ->
                // Update UI with bus information
                // This will be implemented when we have the bus data
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("TransportFragment", "Error showing all available buses", e)
            Toast.makeText(
                requireContext(),
                "Error showing available buses",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getAvailableBuses(selectedLocation: LatLng, callback: (List<BusInfo>) -> Unit) {
        try {
            Log.d("TransportFragment", "Getting available buses for location: $selectedLocation")
            // This will be implemented when we have the bus data
            // For now, return an empty list
            callback(emptyList())
        } catch (e: Exception) {
            Log.e("TransportFragment", "Error getting available buses", e)
            callback(emptyList())
        }
    }

    data class BusInfo(
        val number: String,
        val destination: String,
        val arrivalTime: String,
        val status: String // "On Time", "Delayed", "Arriving", etc.
    )

    private fun showBusDirections(busInfo: BusInfo) {
        try {
            Log.d("TransportFragment", "Showing bus directions for bus: ${busInfo.number}")
            // Use the selected location or default to Syntagma Square
            val location = selectedLocation ?: LatLng(37.9755, 23.7348)
            showDirectionsPopup(location)
        } catch (e: Exception) {
            Log.e("TransportFragment", "Error showing bus directions", e)
            Toast.makeText(
                requireContext(),
                "Error showing bus directions",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun isTextOverlappingWithLines(position: LatLng, text: String, lines: List<List<LatLng>>): Boolean {
        val textBounds = getTextBounds(position, text)
        
        for (line in lines) {
            for (i in 0 until line.size - 1) {
                val start = line[i]
                val end = line[i + 1]
                
                // Check if line segment is close to the text bounds
                if (isLineCloseToRect(start, end, textBounds) || doesLineIntersectRect(start, end, textBounds)) {
                    return true
                }
            }
        }
        return false
    }

    private fun isLineCloseToRect(lineStart: LatLng, lineEnd: LatLng, rect: RectF): Boolean {
        // Minimal line detection buffer
        val buffer = 0.0001f
        
        val expandedRect = RectF(
            rect.left - buffer,
            rect.top - buffer,
            rect.right + buffer,
            rect.bottom + buffer
        )
        
        // Check if line endpoints are within the expanded rectangle
        val startPoint = PointF(lineStart.longitude.toFloat(), lineStart.latitude.toFloat())
        val endPoint = PointF(lineEnd.longitude.toFloat(), lineEnd.latitude.toFloat())
        
        return isPointInRect(startPoint, expandedRect) || isPointInRect(endPoint, expandedRect)
    }

    private fun isPointInRect(point: PointF, rect: RectF): Boolean {
        return point.x >= rect.left && point.x <= rect.right &&
               point.y >= rect.top && point.y <= rect.bottom
    }

    private fun isTextOverlappingWithOtherTexts(position: LatLng, text: String, existingLabels: List<Pair<LatLng, String>>): Boolean {
        val textBounds = getTextBounds(position, text)
        
        for (label in existingLabels) {
            val otherBounds = getTextBounds(label.first, label.second)
            if (doRectsOverlap(textBounds, otherBounds)) {
                return true
            }
        }
        return false
    }

    private fun getTextBounds(position: LatLng, text: String): RectF {
        // Minimal text dimensions - as close as possible to stations
        val textWidth = text.length * 0.0004  // Reduced from 0.0006
        val textHeight = 0.0003  // Reduced from 0.0004
        
        // Very minimal buffer
        val buffer = 0.0001
        
        return RectF(
            (position.longitude - (textWidth + buffer) / 2).toFloat(),
            (position.latitude - (textHeight + buffer) / 2).toFloat(),
            (position.longitude + (textWidth + buffer) / 2).toFloat(),
            (position.latitude + (textHeight + buffer) / 2).toFloat()
        )
    }

    private fun doesLineIntersectRect(lineStart: LatLng, lineEnd: LatLng, rect: RectF): Boolean {
        // Check if line intersects with any of the rectangle's edges
        val lines = listOf(
            Pair(PointF(rect.left, rect.top), PointF(rect.right, rect.top)),
            Pair(PointF(rect.right, rect.top), PointF(rect.right, rect.bottom)),
            Pair(PointF(rect.right, rect.bottom), PointF(rect.left, rect.bottom)),
            Pair(PointF(rect.left, rect.bottom), PointF(rect.left, rect.top))
        )

        val p1 = PointF(lineStart.longitude.toFloat(), lineStart.latitude.toFloat())
        val p2 = PointF(lineEnd.longitude.toFloat(), lineEnd.latitude.toFloat())

        for (rectLine in lines) {
            if (doLinesIntersect(p1, p2, rectLine.first, rectLine.second)) {
                return true
            }
        }
        return false
    }

    private fun doLinesIntersect(p1: PointF, p2: PointF, p3: PointF, p4: PointF): Boolean {
        val denominator = (p4.y - p3.y) * (p2.x - p1.x) - (p4.x - p3.x) * (p2.y - p1.y)
        if (denominator == 0f) return false

        val ua = ((p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x)) / denominator
        val ub = ((p2.x - p1.x) * (p1.y - p3.y) - (p2.y - p1.y) * (p1.x - p3.x)) / denominator

        return ua in 0f..1f && ub in 0f..1f
    }

    private fun doRectsOverlap(rect1: RectF, rect2: RectF): Boolean {
        return !(rect1.right < rect2.left ||
                rect1.left > rect2.right ||
                rect1.bottom < rect2.top ||
                rect1.top > rect2.bottom)
    }

    private fun findOptimalTextPosition(
        originalPosition: LatLng,
        text: String,
        existingLabels: List<Pair<LatLng, String>>,
        lines: List<List<LatLng>>
    ): LatLng {
        // Base offset value
        val baseOffset = 0.0015
        
        // Try positions with increasing vertical offsets
        for (multiplier in 1..3) {
            val currentOffset = baseOffset * multiplier
            
            // Try above/below first, then diagonals
            val offsets = listOf(
                Pair(0.0, currentOffset),  // above
                Pair(0.0, -currentOffset), // below
                Pair(currentOffset, currentOffset),     // diagonal top-right
                Pair(currentOffset, -currentOffset),    // diagonal bottom-right
                Pair(-currentOffset, currentOffset),    // diagonal top-left
                Pair(-currentOffset, -currentOffset)    // diagonal bottom-left
            )

            for (offset in offsets) {
                val newPosition = LatLng(
                    originalPosition.latitude + offset.second,
                    originalPosition.longitude + offset.first
                )
                
                if (!isTextOverlappingWithLines(newPosition, text, lines) &&
                    !isTextOverlappingWithOtherTexts(newPosition, text, existingLabels)) {
                    return newPosition
                }
            }
        }

        // If no position works, return a position well above the station
        return LatLng(
            originalPosition.latitude + baseOffset * 3,
            originalPosition.longitude
        )
    }

    private fun setupTransportControls() {
        val modeButton = binding.buttonModePicker
        val lineButton = binding.buttonLinePicker

        modeButton.setOnClickListener {
            showModePickerMenu()
        }

        lineButton.setOnClickListener {
            showLinePickerMenu()
        }
    }

    private fun showLinePickerMenu() {
        // Create the popup window layout
        val inflater = LayoutInflater.from(requireContext())
        val popupView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.dropdown_menu_background)
            elevation = resources.getDimension(R.dimen.cardview_default_elevation)
        }

        // Create the popup window
        val popupWindow = PopupWindow(
            popupView,
            binding.buttonLinePicker.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = resources.getDimension(R.dimen.cardview_default_elevation)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
        }

        val menuItems = listOf(
            Triple("All Lines", 0, "#663399"),
            Triple("Line 1", R.drawable.dropdown_dot_green, "#2ECC40"),
            Triple("Line 2", R.drawable.dropdown_dot_red, "#FF4136"),
            Triple("Line 3", R.drawable.dropdown_dot_blue, "#0074D9")
        )

        // Add menu items to the popup
        menuItems.forEach { (title, dotRes, color) ->
            val itemView = inflater.inflate(R.layout.item_line_dropdown, null)
            val dot = itemView.findViewById<View>(R.id.line_dot)
            val text = itemView.findViewById<TextView>(R.id.line_text)

            if (dotRes != 0) {
                dot.setBackgroundResource(dotRes)
                dot.visibility = View.VISIBLE
            } else {
                dot.visibility = View.GONE
            }

            text.text = title
            text.setTextColor(Color.parseColor(color))

            itemView.setOnClickListener {
                selectedLine = title
                binding.lineText.text = title
                binding.lineText.setTextColor(Color.parseColor(color))
                
                // Update the button's dot visibility and color based on selection
                val buttonDot = binding.buttonLinePicker.findViewById<View>(R.id.line_dot)
                if (dotRes != 0) {
                    buttonDot.setBackgroundResource(dotRes)
                    buttonDot.visibility = View.VISIBLE
                } else {
                    buttonDot.visibility = View.GONE
                }
                
                updateMapForSelection()
                popupWindow.dismiss()
            }

            popupView.addView(itemView)
        }

        // Measure the popup view to get its height
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = popupView.measuredHeight

        // Show the popup above the button
        popupWindow.showAsDropDown(
            binding.buttonLinePicker,
            0,
            -popupHeight - binding.buttonLinePicker.height
        )
    }

    private fun showModePickerMenu() {
        // Create the popup window layout
        val inflater = LayoutInflater.from(requireContext())
        val popupView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.dropdown_menu_background)
            elevation = resources.getDimension(R.dimen.cardview_default_elevation)
        }

        // Create the popup window
        val popupWindow = PopupWindow(
            popupView,
            binding.buttonModePicker.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = resources.getDimension(R.dimen.cardview_default_elevation)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
        }

        // Set button text and icon color to purple when menu opens
        binding.modeText.setTextColor(Color.parseColor("#663399"))
        binding.modeIcon.setColorFilter(Color.parseColor("#663399"))

        val menuItems = listOf(
            Triple("Metro", R.drawable.ic_metro, "#663399"),
            Triple("Bus Stops", R.drawable.ic_transport, "#663399"),
            Triple("Tram", R.drawable.ic_tram, "#663399")
        )

        // Add menu items to the popup
        menuItems.forEach { (title, iconRes, color) ->
            val itemView = inflater.inflate(R.layout.popup_menu_item, null).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    resources.getDimensionPixelSize(R.dimen.activity_vertical_margin) / 2,
                    resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                    resources.getDimensionPixelSize(R.dimen.activity_vertical_margin) / 2
                )
            }

            val icon = itemView.findViewById<ImageView>(R.id.menu_item_icon)
            val text = itemView.findViewById<TextView>(R.id.menu_item_text)

            icon.setImageResource(iconRes)
            icon.setColorFilter(Color.parseColor(color))
            text.text = title
            text.setTextColor(Color.parseColor(color))

            itemView.setOnClickListener {
                selectedMode = title
                binding.modeText.text = title
                binding.modeIcon.setImageResource(iconRes)
                // Always set text and icon color to purple after selection
                binding.modeText.setTextColor(Color.parseColor("#663399"))
                binding.modeIcon.setColorFilter(Color.parseColor("#663399"))
                updateMapForSelection()
                popupWindow.dismiss()
            }

            popupView.addView(itemView)
        }

        // Measure the popup view to get its height
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = popupView.measuredHeight

        // Show the popup above the button
        popupWindow.showAsDropDown(
            binding.buttonModePicker,
            0,
            -popupHeight - binding.buttonModePicker.height
        )
    }

    private fun updateMapForSelection() {
        googleMap?.clear()
        if (selectedMode == "Metro") {
            when (selectedLine) {
                "All Lines" -> {
                    // Draw lines based on selection state
                    if (selectedStartStation != null && selectedEndStation != null) {
                        // Only draw segments between selected stations
                        drawLineSegments(metroLine1, line1CurvedPoints, Color.parseColor("#009640"))
                        drawLineSegments(metroLine2, line2CurvedPoints, Color.parseColor("#e30613"))
                        drawLineSegments(metroLine3, line3CurvedPoints, Color.parseColor("#0057a8"))
                    } else {
                        // Draw all lines when no stations are selected
                        googleMap?.addPolyline(
                            PolylineOptions()
                                .addAll(line1CurvedPoints)
                                .color(Color.parseColor("#009640"))
                                .width(22f)
                                .geodesic(false)
                                .jointType(JointType.ROUND)
                        )
                        googleMap?.addPolyline(
                            PolylineOptions()
                                .addAll(line2CurvedPoints)
                                .color(Color.parseColor("#e30613"))
                                .width(22f)
                                .geodesic(false)
                                .jointType(JointType.ROUND)
                        )
                        googleMap?.addPolyline(
                            PolylineOptions()
                                .addAll(line3CurvedPoints)
                                .color(Color.parseColor("#0057a8"))
                                .width(22f)
                                .geodesic(false)
                                .jointType(JointType.ROUND)
                        )
                    }
                }
                "Line 1" -> {
                    if (selectedStartStation != null && selectedEndStation != null) {
                        drawLineSegments(metroLine1, line1CurvedPoints, Color.parseColor("#009640"))
                    } else {
                        googleMap?.addPolyline(
                            PolylineOptions()
                                .addAll(line1CurvedPoints)
                                .color(Color.parseColor("#009640"))
                                .width(22f)
                                .geodesic(false)
                                .jointType(JointType.ROUND)
                        )
                    }
                }
                "Line 2" -> {
                    if (selectedStartStation != null && selectedEndStation != null) {
                        drawLineSegments(metroLine2, line2CurvedPoints, Color.parseColor("#e30613"))
                    } else {
                        googleMap?.addPolyline(
                            PolylineOptions()
                                .addAll(line2CurvedPoints)
                                .color(Color.parseColor("#e30613"))
                                .width(22f)
                                .geodesic(false)
                                .jointType(JointType.ROUND)
                        )
                    }
                }
                "Line 3" -> {
                    if (selectedStartStation != null && selectedEndStation != null) {
                        drawLineSegments(metroLine3, line3CurvedPoints, Color.parseColor("#0057a8"))
                    } else {
                        googleMap?.addPolyline(
                            PolylineOptions()
                                .addAll(line3CurvedPoints)
                                .color(Color.parseColor("#0057a8"))
                                .width(22f)
                                .geodesic(false)
                                .jointType(JointType.ROUND)
                        )
                    }
                }
            }
        } else {
            // For Bus Stops and Tram, show nothing for now
        }
    }

    private fun drawLineSegments(stations: List<MetroStation>, curvedPoints: List<LatLng>, color: Int) {
        if (selectedStartStation == null || selectedEndStation == null) return

        // Check if we need to draw a direct route or an interchange route
        val interchangeStation = findInterchangeStation(selectedStartStation!!, selectedEndStation!!)
        
        if (interchangeStation != null) {
            // Draw first segment if it's on this line
            if (stations.contains(selectedStartStation) && stations.contains(interchangeStation)) {
                drawSegmentBetweenStations(selectedStartStation!!, interchangeStation, stations, curvedPoints, color)
            }
            // Draw second segment if it's on this line
            if (stations.contains(interchangeStation) && stations.contains(selectedEndStation)) {
                drawSegmentBetweenStations(interchangeStation, selectedEndStation!!, stations, curvedPoints, color)
            }
        } else {
            // Direct route on same line
            if (stations.contains(selectedStartStation) && stations.contains(selectedEndStation)) {
                drawSegmentBetweenStations(selectedStartStation!!, selectedEndStation!!, stations, curvedPoints, color)
            }
        }
    }

    private fun drawSegmentBetweenStations(start: MetroStation, end: MetroStation, stations: List<MetroStation>, curvedPoints: List<LatLng>, color: Int) {
        val startIndex = stations.indexOf(start)
        val endIndex = stations.indexOf(end)
        
        // Find the corresponding curved points for the selected stations
        val startPointIndex = findClosestCurvedPointIndex(start.coords, curvedPoints)
        val endPointIndex = findClosestCurvedPointIndex(end.coords, curvedPoints)

        // Create a sublist of curved points between the selected stations
        val segmentPoints = if (startPointIndex <= endPointIndex) {
            curvedPoints.subList(startPointIndex, endPointIndex + 1)
        } else {
            curvedPoints.subList(endPointIndex, startPointIndex + 1)
        }

        // Draw the line segment
        if (segmentPoints.size >= 2) {
            googleMap?.addPolyline(
                PolylineOptions()
                    .addAll(segmentPoints)
                    .color(color)
                    .width(22f)
                    .geodesic(false)
                    .jointType(JointType.ROUND)
            )
        }
    }

    private fun findClosestCurvedPointIndex(stationCoords: LatLng, curvedPoints: List<LatLng>): Int {
        var closestIndex = 0
        var minDistance = Double.MAX_VALUE

        curvedPoints.forEachIndexed { index, point ->
            val distance = calculateDistance(stationCoords, point)
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = index
            }
        }

        return closestIndex
    }

    private fun showStationMenu(marker: Marker) {
        val inflater = LayoutInflater.from(requireContext())
        val menuView = inflater.inflate(R.layout.station_menu_layout, null)

        // Set station names
        val greekName = marker.title?.substringBefore(" / ") ?: ""
        val englishName = marker.title?.substringAfter(" / ") ?: ""

        // Find the current station object
        val currentStation = listOf(metroLine1, metroLine2, metroLine3)
            .flatten()
            .find { it.nameGreek == greekName && it.nameEnglish == englishName }
            ?: return

        // Determine which line the station belongs to and get its color
        val lineColor = getStationColor(currentStation)

        // Set text colors
        val greekNameText = menuView.findViewById<TextView>(R.id.station_name_greek)
        val englishNameText = menuView.findViewById<TextView>(R.id.station_name_english)
        greekNameText.text = greekName
        englishNameText.text = englishName
        greekNameText.setTextColor(lineColor)
        englishNameText.setTextColor(lineColor)

        // Create popup window first
        val popupWindow = PopupWindow(
            menuView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = resources.getDimension(R.dimen.cardview_default_elevation)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
        }

        // Set card background colors and update button text/visibility based on selection state
        val cards = listOf(
            menuView.findViewById<CardView>(R.id.card_1),
            menuView.findViewById<CardView>(R.id.card_time),
            menuView.findViewById<CardView>(R.id.card_parking),
            menuView.findViewById<CardView>(R.id.card_airport),
            menuView.findViewById<CardView>(R.id.card_harbor),
            menuView.findViewById<CardView>(R.id.card_information)
        )

        // Configure buttons based on selection state
        val card1 = cards[0]
        val cardTime = cards[1]
        val cardParking = cards[2]
        val cardAirport = cards[3]
        val cardHarbor = cards[4]
        val cardInformation = cards[5]

        // Hide all cards initially
        cards.forEach { it.visibility = View.GONE }

        // Show airport, harbor, and information buttons for all stations
        cardAirport.visibility = View.VISIBLE
        cardHarbor.visibility = View.VISIBLE
        cardInformation.visibility = View.VISIBLE
        cardTime.visibility = View.VISIBLE
        cardParking.visibility = View.VISIBLE

        // Add click listeners for the new buttons
        cardTime.setOnClickListener {
            showStationTimetable(currentStation)
            popupWindow.dismiss()
        }

        cardAirport.setOnClickListener {
            selectedStartStation = currentStation
            selectedEndStation = metroLine3.find { it.nameEnglish == "Airport" }
            updateStationMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
            updateSelectionIndicator()
            popupWindow.dismiss()
        }

        cardHarbor.setOnClickListener {
            selectedStartStation = currentStation
            selectedEndStation = metroLine1.find { it.nameEnglish == "Piraeus" }
            updateStationMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
            updateSelectionIndicator()
            popupWindow.dismiss()
        }

        when {
            selectedStartStation == null -> {
                // No station selected - show "Set as start" button
                card1.visibility = View.VISIBLE
                val icon = card1.findViewById<ImageView>(R.id.card_icon)
                icon.setImageResource(R.drawable.ic_checkshalf)
                card1.setOnClickListener {
                    selectedStartStation = currentStation
                    updateStationMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                    updateSelectionIndicator()
                    popupWindow.dismiss()
                }
            }
            selectedStartStation != null && selectedEndStation == null && currentStation != selectedStartStation -> {
                // Start station selected - show "Set as destination" button
                card1.visibility = View.VISIBLE
                val icon = card1.findViewById<ImageView>(R.id.card_icon)
                icon.setImageResource(R.drawable.ic_checks)
                card1.setOnClickListener {
                    selectedEndStation = currentStation
                    updateStationMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                    updateSelectionIndicator()
                    popupWindow.dismiss()
                }
            }
            currentStation == selectedStartStation || currentStation == selectedEndStation -> {
                // This is a selected station - show "Clear selection" button
                card1.visibility = View.VISIBLE
                val icon = card1.findViewById<ImageView>(R.id.card_icon)
                icon.setImageResource(R.drawable.ic_clear)
                card1.setOnClickListener {
                    if (currentStation == selectedStartStation) {
                        selectedStartStation = null
                        selectedEndStation = null // Clear both if start is cleared
                    } else {
                        selectedEndStation = null
                    }
                    updateStationMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                    updateSelectionIndicator()
                    popupWindow.dismiss()
                }
            }
        }

        cards.forEach { card ->
            card.findViewById<LinearLayout>(android.R.id.content)?.setBackgroundColor(lineColor)
            if (card.childCount > 0 && card.getChildAt(0) is LinearLayout) {
                (card.getChildAt(0) as LinearLayout).setBackgroundColor(lineColor)
            }
        }

        // Calculate position to show popup below the marker
        val projection = googleMap?.projection
        val markerScreenPosition = projection?.toScreenLocation(marker.position)
        
        if (markerScreenPosition != null) {
            // Show popup below the marker
            popupWindow.showAtLocation(
                binding.mapView,
                Gravity.NO_GRAVITY,
                markerScreenPosition.x - 100, // Center horizontally (menu is ~200dp wide)
                markerScreenPosition.y + 20 // Show slightly below marker
            )
        }
    }

    private fun findInterchangeStation(start: MetroStation, end: MetroStation): MetroStation? {
        // If stations are on the same line, no interchange needed
        if (metroLine1.contains(start) && metroLine1.contains(end) ||
            metroLine2.contains(start) && metroLine2.contains(end) ||
            metroLine3.contains(start) && metroLine3.contains(end)) {
            return null
        }

        // Find all interchange stations
        val interchangeStations = listOf(metroLine1, metroLine2, metroLine3)
            .flatten()
            .filter { it.isInterchange }

        // Find an interchange station that connects the start and end lines
        return interchangeStations.find { interchange ->
            val isOnStartLine = metroLine1.contains(start) && metroLine1.contains(interchange) ||
                               metroLine2.contains(start) && metroLine2.contains(interchange) ||
                               metroLine3.contains(start) && metroLine3.contains(interchange)
            val isOnEndLine = metroLine1.contains(end) && metroLine1.contains(interchange) ||
                             metroLine2.contains(end) && metroLine2.contains(interchange) ||
                             metroLine3.contains(end) && metroLine3.contains(interchange)
            isOnStartLine && isOnEndLine
        }
    }

    private fun updateSelectionIndicator() {
        val startText = binding.startStationText
        val endText = binding.endStationText
        val swapButton = binding.swapStationsButton
        val enterButton = binding.enterButton
        val interchangeContainer = binding.interchangeContainer
        val interchangeText = binding.interchangeStationText
        val secondArrow = binding.secondArrow
        val resetButton = binding.resetMapButton
        val airportTimetableButton = binding.airportTimetableButton
        val harborGateMapButton = binding.harborGateMapButton

        when {
            selectedStartStation == null -> {
                startText.text = "Select Station"
                startText.setTextColor(Color.parseColor("#663399"))
                endText.text = "Select Station"
                endText.setTextColor(Color.parseColor("#663399"))
                swapButton.isEnabled = false
                enterButton.visibility = View.VISIBLE
                interchangeContainer.visibility = View.GONE
                secondArrow.visibility = View.GONE
                resetButton.visibility = View.GONE
                airportTimetableButton.visibility = View.GONE
                harborGateMapButton.visibility = View.GONE
            }
            selectedEndStation == null -> {
                // Start station selected, waiting for end station
                startText.text = selectedStartStation!!.nameEnglish
                startText.setTextColor(getStationColor(selectedStartStation!!))
                endText.text = "Select Station"
                endText.setTextColor(Color.parseColor("#663399"))
                swapButton.isEnabled = false
                enterButton.visibility = View.VISIBLE
                interchangeContainer.visibility = View.GONE
                secondArrow.visibility = View.GONE
                resetButton.visibility = View.VISIBLE
                airportTimetableButton.visibility = View.GONE
                harborGateMapButton.visibility = View.GONE
            }
            else -> {
                // Both stations selected
                startText.text = selectedStartStation!!.nameEnglish
                startText.setTextColor(getStationColor(selectedStartStation!!))
                endText.text = selectedEndStation!!.nameEnglish
                endText.setTextColor(getStationColor(selectedEndStation!!))
                swapButton.isEnabled = true
                enterButton.visibility = View.VISIBLE
                resetButton.visibility = View.VISIBLE

                // Show airport timetable button if end station is airport
                if (selectedEndStation!!.nameEnglish == "Airport") {
                    airportTimetableButton.visibility = View.VISIBLE
                } else {
                    airportTimetableButton.visibility = View.GONE
                }

                // Show harbor gate map button if end station is Piraeus
                if (selectedEndStation!!.nameEnglish == "Piraeus") {
                    harborGateMapButton.visibility = View.VISIBLE
                } else {
                    harborGateMapButton.visibility = View.GONE
                }

                // Check for interchange
                val interchangeStation = findInterchangeStation(selectedStartStation!!, selectedEndStation!!)
                if (interchangeStation != null) {
                    interchangeContainer.visibility = View.VISIBLE
                    secondArrow.visibility = View.VISIBLE
                    interchangeText.text = interchangeStation.nameEnglish
                    interchangeText.setTextColor(getStationColor(interchangeStation))
                } else {
                    interchangeContainer.visibility = View.GONE
                    secondArrow.visibility = View.GONE
                }
            }
        }
        
        // Update enter button colors based on selection state
        if (selectedStartStation != null && selectedEndStation != null) {
            // Both stations selected - purple background, white icon
            enterButton.setBackgroundResource(R.drawable.rounded_button_bg)
            enterButton.setColorFilter(Color.WHITE)
        } else {
            // Not both selected - gray circular background, purple icon
            enterButton.setBackgroundResource(R.drawable.rounded_button_bg_gray)
            enterButton.setColorFilter(Color.parseColor("#663399"))
        }
    }

    private fun getStationColor(station: MetroStation): Int {
        return when {
            metroLine1.contains(station) -> Color.parseColor("#009640")
            metroLine2.contains(station) -> Color.parseColor("#e30613")
            metroLine3.contains(station) -> Color.parseColor("#0057a8")
            else -> Color.parseColor("#663399")
        }
    }

    private fun setupSelectionControls() {
        binding.swapStationsButton.setOnClickListener {
            if (selectedStartStation != null && selectedEndStation != null) {
                val temp = selectedStartStation
                selectedStartStation = selectedEndStation
                selectedEndStation = temp
                updateStationMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
                updateSelectionIndicator()
            }
        }

        binding.enterButton.setOnClickListener {
            if (selectedStartStation != null && selectedEndStation != null) {
                // We'll implement directions functionality later
                Toast.makeText(context, "Directions coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.harborGateMapButton.setOnClickListener {
            showPiraeusGateMap()
        }
    }

    private fun resetMap() {
        // Clear selected stations
        selectedStartStation = null
        selectedEndStation = null
        
        // Clear station markers
        startStationMarker?.remove()
        endStationMarker?.remove()
        startStationMarker = null
        endStationMarker = null
        
        // Clear map and redraw metro lines and stations
        googleMap?.clear()
        updateMapForSelection()
        updateStationMarkers(googleMap?.cameraPosition?.zoom ?: 15f)
        updateSelectionIndicator()
        
        // Hide airport timetable button
        binding.airportTimetableButton.visibility = View.GONE
        binding.harborGateMapButton.visibility = View.GONE
    }

    private fun findNearestInterchangeToLine3(station: MetroStation): MetroStation? {
        val line3Interchanges = metroLine3.filter { it.isInterchange }

        val stationLine = when {
            metroLine1.contains(station) -> metroLine1
            metroLine2.contains(station) -> metroLine2
            else -> null
        }

        return stationLine?.let { line ->
            line3Interchanges.firstOrNull { interchange ->
                line.contains(interchange)
            }
        }
    }

    private fun showAirportTimetable(station: MetroStation) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val stationGreekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val stationEnglishNameText = dialog.findViewById<TextView>(R.id.station_name_english)
        val interchangeInfoText = dialog.findViewById<TextView>(R.id.interchange_info_text)
        
        val stationColor = getStationColor(station)
        stationGreekNameText.text = station.nameGreek
        stationEnglishNameText.text = station.nameEnglish
        stationGreekNameText.setTextColor(stationColor)
        stationEnglishNameText.setTextColor(stationColor)

        dialog.findViewById<ImageView>(R.id.close_button).setOnClickListener { dialog.dismiss() }

        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        val loadingView = TextView(requireContext()).apply {
            text = "Loading timetable..."
            textSize = 14f
            setTextColor(Color.parseColor("#663399"))
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        timetableContainer.addView(loadingView)
        dialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            var stationForTimetable = station
            var instructionText: String? = null

            if (!metroLine3.contains(station)) {
                val interchangeStation = findNearestInterchangeToLine3(station)
                if (interchangeStation != null) {
                    stationForTimetable = interchangeStation
                    instructionText = "Take Metro to ${interchangeStation.nameEnglish} for Airport Line (Directions shown on map)"
                } else {
                    instructionText = "No direct route to Airport Line found."
                }
            }
            
            val times = parseAirportTimetable(stationForTimetable)
            withContext(Dispatchers.Main) {
                timetableContainer.removeAllViews()

                if (instructionText != null) {
                    interchangeInfoText.text = instructionText
                    interchangeInfoText.visibility = View.VISIBLE
                } else {
                    interchangeInfoText.visibility = View.GONE
                }

                if (times.isNotEmpty()) {
                    val inflater = LayoutInflater.from(dialog.context)
                    val airportView = inflater.inflate(R.layout.item_airport_timetable, timetableContainer, false)
                    val title = airportView.findViewById<TextView>(R.id.direction_title)
                    val timesContainer = airportView.findViewById<LinearLayout>(R.id.times_container)

                    title.text = "Departures from ${stationForTimetable.nameEnglish} to Airport"
                    title.setTextColor(Color.parseColor("#0057a8")) // Line 3 color

                    val timesText = times.joinToString(separator = "  •  ")
                    
                    val cellTextView = TextView(dialog.context).apply {
                        text = timesText
                        setTextColor(Color.BLACK)
                        setPadding(16, 16, 16, 16)
                        typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)
                        gravity = Gravity.CENTER
                        isSingleLine = false
                    }
                    timesContainer.addView(cellTextView)
                    timetableContainer.addView(airportView)
                } else {
                     val errorView = TextView(requireContext()).apply {
                        text = "Timetable not available for this station."
                        setTextColor(Color.RED)
                        gravity = Gravity.CENTER
                        setPadding(16, 48, 16, 48)
                    }
                    timetableContainer.addView(errorView)
                }
            }
        }
    }

    private fun showStationTimetable(station: MetroStation) {
        // Create a custom dialog with the new layout
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_timetable)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Set station names
        val greekNameText = dialog.findViewById<TextView>(R.id.station_name_greek)
        val englishNameText = dialog.findViewById<TextView>(R.id.station_name_english)
        greekNameText.text = station.nameGreek
        englishNameText.text = station.nameEnglish

        // Set close button
        val closeButton = dialog.findViewById<ImageView>(R.id.close_button)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Show loading state
        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        val loadingView = TextView(requireContext()).apply {
            text = "Loading timetable..."
            textSize = 14f
            setTextColor(Color.parseColor("#663399"))
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        timetableContainer.addView(loadingView)

        dialog.show()

        // Fetch timetable data
        fetchStationTimetable(station, dialog)
    }

    private fun fetchStationTimetable(station: MetroStation, dialog: Dialog) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val timetableTables = when {
                    metroLine1.contains(station) -> parseLine1Timetable(station)
                    metroLine2.contains(station) -> parseLine2Timetable(station)
                    metroLine3.contains(station) -> parseLine3Timetable(station)
                    else -> emptyList()
                }

                withContext(Dispatchers.Main) {
                    if (timetableTables.isNotEmpty()) {
                        updateTimetableDialog(dialog, timetableTables, station)
                    } else {
                        dialog.findViewById<LinearLayout>(R.id.timetable_container).apply {
                            removeAllViews()
                            val errorView = TextView(requireContext()).apply {
                                text = "Timetable not available for this station."
                                setTextColor(Color.RED)
                                gravity = Gravity.CENTER
                                setPadding(16, 48, 16, 48)
                            }
                            addView(errorView)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("TransportFragment", "Error fetching timetable", e)
                    dialog.findViewById<LinearLayout>(R.id.timetable_container).apply {
                        removeAllViews()
                        val errorView = TextView(requireContext()).apply {
                            text = "Failed to load timetable."
                            setTextColor(Color.RED)
                            gravity = Gravity.CENTER
                            setPadding(16, 48, 16, 48)
                        }
                        addView(errorView)
                    }
                }
            }
        }
    }

    private fun updateTimetableDialog(dialog: Dialog, tables: List<TimetableTable>, station: MetroStation) {
        val container = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        container.removeAllViews()
        val lineColor = getStationColor(station)

        tables.forEach { tableData ->
            val inflater = LayoutInflater.from(dialog.context)
            val tableView = inflater.inflate(R.layout.item_timetable_table, container, false)
            
            val title = tableView.findViewById<TextView>(R.id.direction_title)
            val tableLayout = tableView.findViewById<TableLayout>(R.id.timetable_table_layout)

            title.text = tableData.direction
            title.setTextColor(lineColor)

            // Create Header Row
            val headerRow = TableRow(dialog.context)
            tableData.headers.forEach { headerText ->
                val headerTextView = TextView(dialog.context).apply {
                    text = headerText
                    setTextColor(lineColor)
                    setPadding(16, 16, 16, 16)
                    typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
                    gravity = Gravity.CENTER
                }
                headerRow.addView(headerTextView)
            }
            tableLayout.addView(headerRow)

            // Create Data Rows
            tableData.rows.forEach { rowData ->
                val tableRow = TableRow(dialog.context)
                rowData.forEach { cellData ->
                    val cellTextView = TextView(dialog.context).apply {
                        text = cellData
                        setTextColor(Color.BLACK)
                        setPadding(16, 16, 16, 16)
                        typeface = ResourcesCompat.getFont(context, R.font.montserrat_regular)
                        gravity = Gravity.CENTER
                    }
                    tableRow.addView(cellTextView)
                }
                tableLayout.addView(tableRow)
            }
            container.addView(tableView)
        }
    }

    private fun parseLine1Timetable(station: MetroStation): List<TimetableTable> {
        val inputStream = resources.openRawResource(R.raw.line1_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationBlock = text.split("STATION:").find { it.trim().startsWith(station.nameGreek) } ?: return emptyList()

        val lines = stationBlock.trim().split("\n").map { it.trim() }
        val result = mutableListOf<TimetableTable>()

        lines.forEach { line ->
            val parts = line.split(";")
            if (line.startsWith("TOWARDS_KIFISIA")) {
                val first = parts.getOrNull(2) ?: "-"
                val last = parts.getOrNull(4) ?: "-"
                val lastOmonia = parts.getOrNull(6)
                
                val rows = mutableListOf(listOf("Kifisia", first, last))
                lastOmonia?.let { rows.add(listOf("Omonia", "-", it)) }

                result.add(TimetableTable("Towards Kifisia", listOf("To", "First", "Last"), rows))
            } else if (line.startsWith("TOWARDS_PIRAEUS")) {
                val first = parts.getOrNull(2) ?: "-"
                val last = parts.getOrNull(4) ?: "-"
                result.add(TimetableTable("Towards Piraeus", listOf("To", "First", "Last"), listOf(listOf("Piraeus", first, last))))
            }
        }
        return result
    }

    private fun parseLine2Timetable(station: MetroStation): List<TimetableTable> {
        val inputStream = resources.openRawResource(R.raw.line2_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationBlock = text.split("STATION:").find { it.trim().startsWith(station.nameGreek) } ?: return emptyList()

        val lines = stationBlock.trim().split("\n").map { it.trim() }
        val result = mutableListOf<TimetableTable>()
        
        lines.forEach { line ->
            val parts = line.split(";")
            if (line.startsWith("TOWARDS_ELLINIKO")) {
                 val headers = listOf("First (Mon-Fri)", "First (Sat-Sun)", "Last (Mon-Thu, Sun)", "Last (Fri-Sat)")
                 val row = listOf(
                    parts.getOrNull(2) ?: "-",
                    parts.getOrNull(4) ?: "-",
                    parts.getOrNull(6) ?: "-",
                    parts.getOrNull(8) ?: "-"
                 )
                result.add(TimetableTable("Towards Elliniko", headers, listOf(row)))
            } else if (line.startsWith("TOWARDS_ANTHOUPOLI")) {
                val headers = listOf("First (All Days)", "Last (Mon-Thu, Sun)", "Last (Fri-Sat)")
                val row = listOf(
                    parts.getOrNull(2) ?: "-",
                    parts.getOrNull(4) ?: "-",
                    parts.getOrNull(6) ?: "-"
                )
                result.add(TimetableTable("Towards Anthoupoli", headers, listOf(row)))
            }
        }
        return result
    }

    private fun parseLine3Timetable(station: MetroStation): List<TimetableTable> {
        val inputStream = resources.openRawResource(R.raw.line3_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationBlock = text.split("STATION:").find { it.trim().startsWith(station.nameGreek) } ?: return emptyList()

        val lines = stationBlock.trim().split("\n").map { it.trim() }
        val result = mutableListOf<TimetableTable>()

        lines.forEach { line ->
            val parts = line.split(";")
            if (line.startsWith("TOWARDS_AIRPORT")) {
                val headers = listOf("To", "First", "Second", "Last", "Last (Mon-Thu, Sun)", "Last (Fri-Sat)")
                val rowAirport = listOf(
                    "Airport",
                    parts.getOrNull(4) ?: "-",
                    parts.getOrNull(6) ?: "-",
                    parts.getOrNull(8) ?: "-",
                    parts.getOrNull(8) ?: "-", // No separate last train on weekday
                    parts.getOrNull(8) ?: "-"  // No separate last train on weekend
                )
                val rowDPL = listOf(
                    "D. Plakentias",
                    parts.getOrNull(2) ?: "-",
                    "-", // No second train to DPL
                    "-", // No dedicated last train to DPL in this field
                    parts.getOrNull(10) ?: "-",
                    parts.getOrNull(12) ?: "-"
                )
                result.add(TimetableTable("Towards Airport / D. Plakentias", headers, listOf(rowAirport, rowDPL)))
            } else if (line.startsWith("TOWARDS_DIMOTIKO_THEATRO")) {
                val headers = listOf("First", "Last")
                val row = listOf(parts.getOrNull(2) ?: "-", parts.getOrNull(4) ?: "-")
                result.add(TimetableTable("Towards Dimotiko Theatro", headers, listOf(row)))
            }
        }
        return result
    }

    private fun parseAirportTimetable(station: MetroStation): List<String> {
        val inputStream = resources.openRawResource(R.raw.airport_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationLine = text.split("\n").find {
            val parts = it.split(";")
            // Use English name for matching, case-insensitively
            parts.size > 1 && parts[1].trim().equals(station.nameEnglish, ignoreCase = true)
        }

        return if (stationLine != null) {
            stationLine.split(";").drop(2).map { it.trim() }
        } else {
            emptyList()
        }
    }

    private fun getStationName(marker: Marker): String {
        val title = marker.title
        return title?.substringBefore(" / ") ?: ""
    }

    private fun readLine1Timetable(station: MetroStation): String {
        val inputStream = resources.openRawResource(R.raw.line1_timetable)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()
        reader.close()

        val stationName = station.nameGreek
        val stationData = StringBuilder()
        var foundStation = false
        var stationIndex = -1

        // First, find the line index for the requested station
        for (i in lines.indices) {
            if (lines[i].equals("STATION: $stationName", ignoreCase = true)) {
                foundStation = true
                stationIndex = i
                break
            }
        }

        if (foundStation) {
            // Now, look at the lines immediately following the station line
            var currentIndex = stationIndex + 1
            while (currentIndex < lines.size && !lines[currentIndex].startsWith("STATION:")) {
                val line = lines[currentIndex]
                val parts = line.split(";")

                if (line.startsWith("TOWARDS_PIRAEUS")) {
                    stationData.append("Towards Piraeus:\n")
                    val firstTime = parts.getOrNull(2) ?: "N/A"
                    val lastTime = parts.getOrNull(4) ?: "N/A"
                    stationData.append("  - First: $firstTime\n")
                    stationData.append("  - Last: $lastTime\n\n")
                } else if (line.startsWith("TOWARDS_KIFISIA")) {
                    stationData.append("Towards Kifisia:\n")
                    val firstTime = parts.getOrNull(2) ?: "N/A"
                    val lastTime = parts.getOrNull(4) ?: "N/A"
                    val lastOmoniaTime = parts.getOrNull(6)

                    stationData.append("  - First: $firstTime\n")
                    stationData.append("  - Last: $lastTime\n")
                    if (lastOmoniaTime != null) {
                        stationData.append("  - Last (to Omonia): $lastOmoniaTime\n")
                    }
                    stationData.append("\n")
                }
                currentIndex++
            }
        } else {
            return "Timetable not found for station: $stationName"
        }

        return stationData.toString().trim()
    }
    
    private fun readLine2Timetable(station: MetroStation): String {
        val inputStream = resources.openRawResource(R.raw.line2_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationBlock = text.split("STATION:").find { it.trim().startsWith(station.nameGreek) } ?: return "Timetable not found for station: ${station.nameGreek}"

        val lines = stationBlock.trim().split("\n").map { it.trim() }
        val stationData = StringBuilder()

        lines.forEach { line ->
            val parts = line.split(";")
            if (line.startsWith("TOWARDS_ELLINIKO")) {
                stationData.append("Towards Elliniko:\n")
                stationData.append("  - First (Mon-Fri): ${parts.getOrNull(2) ?: "-"}\n")
                stationData.append("  - First (Sat-Sun): ${parts.getOrNull(4) ?: "-"}\n")
                stationData.append("  - Last (Mon-Thu, Sun): ${parts.getOrNull(6) ?: "-"}\n")
                stationData.append("  - Last (Fri-Sat): ${parts.getOrNull(8) ?: "-"}\n\n")
            } else if (line.startsWith("TOWARDS_ANTHOUPOLI")) {
                stationData.append("Towards Anthoupoli:\n")
                stationData.append("  - First (All days): ${parts.getOrNull(2) ?: "-"}\n")
                stationData.append("  - Last (Mon-Thu, Sun): ${parts.getOrNull(4) ?: "-"}\n")
                stationData.append("  - Last (Fri-Sat): ${parts.getOrNull(6) ?: "-"}\n")
            }
        }

        return stationData.toString().trim()
    }
    
    private fun readLine3Timetable(station: MetroStation): String {
        val inputStream = resources.openRawResource(R.raw.line3_timetable)
        val text = inputStream.bufferedReader().use { it.readText() }
        val stationBlock = text.split("STATION:").find { it.trim().startsWith(station.nameGreek) } ?: return "Timetable not found for station: ${station.nameGreek}"

        val lines = stationBlock.trim().split("\n").map { it.trim() }
        val stationData = StringBuilder()

        lines.forEach { line ->
            val parts = line.split(";")
            if (line.startsWith("TOWARDS_AIRPORT")) {
                stationData.append("Towards Airport:\n")
                stationData.append("  - First (to D.Plakentias): ${parts.getOrNull(2) ?: "-"}\n")
                stationData.append("  - First (to Airport): ${parts.getOrNull(4) ?: "-"}\n")
                stationData.append("  - Second (to Airport): ${parts.getOrNull(6) ?: "-"}\n")
                stationData.append("  - Last (to Airport): ${parts.getOrNull(8) ?: "-"}\n")
                stationData.append("  - Last (to D.Plakentias, Mon-Thu, Sun): ${parts.getOrNull(10) ?: "-"}\n")
                stationData.append("  - Last (to D.Plakentias, Fri-Sat): ${parts.getOrNull(12) ?: "-"}\n\n")
            } else if (line.startsWith("TOWARDS_DIMOTIKO_THEATRO")) {
                stationData.append("Towards Dimotiko Theatro:\n")
                stationData.append("  - First: ${parts.getOrNull(2) ?: "-"}\n")
                stationData.append("  - Last: ${parts.getOrNull(4) ?: "-"}\n")
            }
        }

        return stationData.toString().trim()
    }

    private fun showPiraeusGateMap() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_timetable) // Reusing the same base dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Set station names and title
        dialog.findViewById<TextView>(R.id.station_name_greek).text = "Πειραιάς"
        dialog.findViewById<TextView>(R.id.station_name_english).text = "Piraeus"
        dialog.findViewById<ImageView>(R.id.close_button).setOnClickListener { dialog.dismiss() }

        val timetableContainer = dialog.findViewById<LinearLayout>(R.id.timetable_container)
        timetableContainer.removeAllViews() // Clear any previous content

        val messageText = TextView(requireContext()).apply {
            text = "Piraeus Gate Map Coming Soon!"
            textSize = 16f
            setTextColor(Color.parseColor("#009640"))
            gravity = Gravity.CENTER
            setPadding(32, 64, 32, 64)
        }
        timetableContainer.addView(messageText)

        dialog.show()
    }
}