@file:Suppress("SpellCheckingInspection")

package com.example.athensplus.domain.model

import com.google.android.gms.maps.model.LatLng

// WILL BE MOVED TO BACKEND LATER ON

object StationData {
    val metroLine1 = listOf(
        MetroStation("Πειραιάς", "Piraeus", LatLng(37.948263, 23.643233), isInterchange = true),
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
        MetroStation("Αεροδρόμιο", "Airport", LatLng(37.936942775310406, 23.944783037869243)),
        MetroStation("Κορωπί", "Koropi", LatLng(37.9131505428903, 23.8958794023863)),
        MetroStation("Παιανία-Κάντζα", "Paiania-Kantza", LatLng(37.98417062046167, 23.869756642430122)),
        MetroStation("Παλλήνη", "Pallini", LatLng(38.00601053761855, 23.869545425355156)),
        MetroStation("Δουκίσσης Πλακεντίας", "Doukissis Plakentias", LatLng(38.024189316487046, 23.833724220891263)),
        MetroStation("Χαλάνδρι", "Chalandri", LatLng(38.021437720256365, 23.82139210120635)),
        MetroStation("Αγία Παρασκευή", "Agia Paraskevi", LatLng(38.01773132519414, 23.81262417681338)),
        MetroStation("Νομισματοκοπείο", "Nomismatokopeio", LatLng(38.00975632474917, 23.805353216074298)),
        MetroStation("Χολαργός", "Cholargos", LatLng(38.00456995086606, 23.794701985734786)),
        MetroStation("Εθνική Άμυνα", "Ethniki Amyna", LatLng(38.00031275851294, 23.78568239514545)),
        MetroStation("Κατεχάκη", "Katehaki", LatLng(37.99370654358875, 23.776255284953653)),
        MetroStation("Πανόρμου", "Panormou", LatLng(37.99324246685057, 23.763372268004527)),
        MetroStation("Αμπελόκηποι", "Ambelokipi", LatLng(37.98736656071125, 23.75703357239967)),
        MetroStation("Μέγαρο Μουσικής", "Megaro Mousikis", LatLng(37.979289172647675, 23.75291481651404)),
        MetroStation("Ευαγγελισμός", "Evangelismos", LatLng(37.97644961902063, 23.747314144474466)),
        MetroStation("Σύνταγμα", "Syntagma", LatLng(37.97568408036298, 23.73535892766162), isInterchange = true),
        MetroStation("Μοναστηράκι", "Monastiraki", LatLng(37.976114278617565, 23.725633963810413), isInterchange = true),
        MetroStation("Κεραμεικός", "Kerameikos", LatLng(37.97855799587991, 23.711527839122848)),
        MetroStation("Ελαιώνας", "Elaionas", LatLng(37.98787205461006, 23.69420027290326)),
        MetroStation("Αιγάλεω", "Aigaleo", LatLng(37.99197010485269, 23.68176929794242)),
        MetroStation("Αγία Μαρίνα", "Agia Marina", LatLng(37.99722077049423, 23.667344819154156)),
        MetroStation("Αγία Βαρβάρα", "Agia Varvara", LatLng(37.9900618690817, 23.65932429578851)),
        MetroStation("Κορυδαλλός", "Korydallos", LatLng(37.97702502617706, 23.65035476643652)),
        MetroStation("Νίκαια", "Nikaia", LatLng(37.965641240168104, 23.647316509152287)),
        MetroStation("Μανιάτικα", "Maniatika", LatLng(37.959568185347536, 23.63969873316419)),
        MetroStation("Πειραιάς", "Piraeus", LatLng(37.948263, 23.643233), isInterchange = true),
        MetroStation("Δημοτικό Θέατρο", "Dimotiko Theatro", LatLng(37.942987138386535, 23.64761813894496))
    )

    val line3CurvedPoints = listOf(
        LatLng(37.942987138386535, 23.64761813894496), 
        LatLng(37.945, 23.645), 
        LatLng(37.94826317831643, 23.643233750670223), 
        LatLng(37.952, 23.641), 
        LatLng(37.959568185347536, 23.63969873316419), 
        LatLng(37.962, 23.643), 
        LatLng(37.965641240168104, 23.647316509152287), 
        LatLng(37.971, 23.649), 
        LatLng(37.97702502617706, 23.65035476643652), 
        LatLng(37.984, 23.654), 
        LatLng(37.9900618690817, 23.65932429578851), 
        LatLng(37.994, 23.663), 
        LatLng(37.99722077049423, 23.667344819154156), 
        LatLng(37.993, 23.674), 
        LatLng(37.99197010485269, 23.68176929794242), 
        LatLng(37.990, 23.688), 
        LatLng(37.98787205461006, 23.69420027290326), 
        LatLng(37.983, 23.703), 
        LatLng(37.97855799587991, 23.711527839122848), 
        LatLng(37.977, 23.718), 
        LatLng(37.976114278617565, 23.725633963810413), 
        LatLng(37.976, 23.730), 
        LatLng(37.97568408036298, 23.73535892766162), 
        LatLng(37.976, 23.741), 
        LatLng(37.97644961902063, 23.747314144474466), 
        LatLng(37.978, 23.750), 
        LatLng(37.979289172647675, 23.75291481651404), 
        LatLng(37.983, 23.754), 
        LatLng(37.98736656071125, 23.75703357239967), 
        LatLng(37.990, 23.760), 
        LatLng(37.99324246685057, 23.763372268004527), 
        LatLng(37.993, 23.770), 
        LatLng(37.99370654358875, 23.776255284953653), 
        LatLng(37.997, 23.781), 
        LatLng(38.00031275851294, 23.78568239514545), 
        LatLng(38.004, 23.790), 
        LatLng(38.00456995086606, 23.794701985734786), 
        LatLng(38.007, 23.800), 
        LatLng(38.00975632474917, 23.805353216074298), 
        LatLng(38.013, 23.809), 
        LatLng(38.01773132519414, 23.81262417681338), 
        LatLng(38.019, 23.817), 
        LatLng(38.021437720256365, 23.82139210120635), 
        LatLng(38.023, 23.827), 
        LatLng(38.024189316487046, 23.833724220891263), 
        LatLng(38.015, 23.851), 
        LatLng(38.00601053761855, 23.869545425355156), 
        LatLng(37.995, 23.869), 
        LatLng(37.98417062046167, 23.869756642430122), 
        LatLng(37.950, 23.883), 
        LatLng(37.9131505428903, 23.8958794023863), 
        LatLng(37.925, 23.920), 
        LatLng(37.936942775310406, 23.944783037869243) 
    )

    val line2CurvedPoints = listOf(
        LatLng(37.892649697635335, 23.74758851630924), 
        LatLng(37.898, 23.747), 
        LatLng(37.90312049238752, 23.745938957712156), 
        LatLng(37.911, 23.745), 
        LatLng(37.91813201276993, 23.744191423118377), 
        LatLng(37.924, 23.744), 
        LatLng(37.92980661834609, 23.744523084603305), 
        LatLng(37.935, 23.742), 
        LatLng(37.94057253079489, 23.740736185819), 
        LatLng(37.945, 23.739), 
        LatLng(37.949211541316096, 23.737265287277935), 
        LatLng(37.953, 23.736), 
        LatLng(37.95663952647803, 23.734630922893803), 
        LatLng(37.957, 23.731), 
        LatLng(37.957596771154535, 23.728486607161212), 
        LatLng(37.961, 23.727), 
        LatLng(37.964075081691085, 23.726455951266303), 
        LatLng(37.966, 23.728), 
        LatLng(37.968791278421335, 23.729646981926912), 
        LatLng(37.972, 23.732), 
        LatLng(37.97568408036298, 23.73535892766162), 
        LatLng(37.978, 23.734), 
        LatLng(37.98037292954861, 23.73307575719024), 
        LatLng(37.982, 23.731), 
        LatLng(37.98420036534532, 23.728709865494178), 
        LatLng(37.985, 23.725), 
        LatLng(37.986246898920776, 23.721171206907755), 
        LatLng(37.989, 23.721), 
        LatLng(37.99199512016312, 23.721114088402935), 
        LatLng(37.995, 23.721), 
        LatLng(37.99931374537682, 23.722117655786956), 
        LatLng(38.001, 23.718), 
        LatLng(38.0026582025189, 23.713618531964784), 
        LatLng(38.004, 23.706), 
        LatLng(38.00668188937669, 23.699473817226718), 
        LatLng(38.010, 23.697), 
        LatLng(38.01316415384895, 23.695482322472614), 
        LatLng(38.015, 23.693), 
        LatLng(38.017089551895666, 23.691134987138266) 
    )

    val line1CurvedPoints = listOf(
        LatLng(38.07373290671199, 23.808305136483852), 
        LatLng(38.070, 23.806), 
        LatLng(38.06591837925154, 23.804017818766344), 
        LatLng(38.061, 23.804), 
        LatLng(38.05619686161689, 23.80503609761142), 
        LatLng(38.050, 23.799), 
        LatLng(38.04484194992877, 23.792862889776107), 
        LatLng(38.043, 23.788), 
        LatLng(38.04330588326173, 23.783400691393396), 
        LatLng(38.045, 23.775), 
        LatLng(38.0462292394805, 23.76607388463279), 
        LatLng(38.042, 23.755), 
        LatLng(38.037165103011894, 23.75008681005388), 
        LatLng(38.035, 23.747), 
        LatLng(38.03266821407559, 23.744712039673153), 
        LatLng(38.028, 23.740), 
        LatLng(38.02382144738529, 23.736039815065507), 
        LatLng(38.022, 23.734), 
        LatLng(38.02012753169261, 23.731728912700053), 
        LatLng(38.016, 23.730), 
        LatLng(38.01160376209793, 23.728755698511065), 
        LatLng(38.009, 23.728), 
        LatLng(38.00692137614022, 23.72773410379398), 
        LatLng(38.003, 23.725), 
        LatLng(37.99931374537682, 23.722117655786956), 
        LatLng(37.996, 23.726), 
        LatLng(37.993070240716015, 23.730372320299832), 
        LatLng(37.988, 23.729), 
        LatLng(37.98420036534532, 23.728709865494178), 
        LatLng(37.980, 23.727), 
        LatLng(37.976114278617565, 23.725633963810413), 
        LatLng(37.976, 23.723), 
        LatLng(37.97673744990198, 23.72063841824465), 
        LatLng(37.972, 23.715), 
        LatLng(37.9686355751221, 23.709296406789832), 
        LatLng(37.965, 23.706), 
        LatLng(37.962439214458016, 23.703328766874794), 
        LatLng(37.961, 23.700), 
        LatLng(37.96038993809849, 23.69735115718288), 
        LatLng(37.957, 23.688), 
        LatLng(37.95503411820899, 23.679616546345812), 
        LatLng(37.950, 23.672), 
        LatLng(37.94503590091905, 23.665229397093032), 
        LatLng(37.947, 23.654), 
        LatLng(37.94806117043078, 23.643235606587858) 
    )

    val piraeusPortCenter = LatLng(37.9469, 23.6405) 
    const val PIRAEUS_PORT_ZOOM = 15.0f 

    val piraeusGates = listOf(
        Triple("E1", LatLng(37.9461, 23.6377), "To Crete"),
        Triple("E2", LatLng(37.9457, 23.6387), "To Dodecanese"),
        Triple("E3", LatLng(37.9452, 23.6402), "To Cyclades"),
        Triple("E4", LatLng(37.9450, 23.6420), "To Chios/Mytilene"),
        Triple("E5", LatLng(37.9452, 23.6440), "Port Entrance"),
        Triple("E6", LatLng(37.9460, 23.6455), "To Saronic"),
        Triple("E7", LatLng(37.9470, 23.6465), "To Saronic"),
        Triple("E8", LatLng(37.9477, 23.6450), "To Saronic"),
        Triple("E9", LatLng(37.9485, 23.6435), "To Saronic"),
        Triple("E10", LatLng(37.9490, 23.6415), "To Saronic"),
        Triple("E11", LatLng(37.9495, 23.6395), "To Saronic"),
        Triple("E12", LatLng(37.9500, 23.6375), "Cruise Terminal B")
    )
} 