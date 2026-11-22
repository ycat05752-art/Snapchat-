package com.example.data.model

import kotlin.math.abs

data class SimulatedFriendProfile(
    val id: String,
    val name: String,
    val username: String,
    val gender: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val bio: String,
    val pnPoints: Long,
    val onlineStatus: String,
    val avatarColorArgb: Long,
    val storyCaption: String,
    val storyThemeEmoji: String,
    val storyBgHex: Long,
    val isDefaultFriend: Boolean
)

object SimulatedFriendsCatalog {

    private val priorityFriends = listOf(
        Triple("Rahul Kumar", "M", "Mumbai"),
        Triple("Aman Singh", "M", "Delhi"),
        Triple("Rohit Sharma", "M", "Jaipur"),
        Triple("Aditya Verma", "M", "Bengaluru"),
        Triple("Priya Singh", "F", "Lucknow"),
        Triple("Anjali Kumari", "F", "Patna"),
        Triple("Sneha Sharma", "F", "Pune"),
        Triple("Neha Gupta", "F", "Gurugram"),
        Triple("Arjun Yadav", "M", "Noida"),
        Triple("Shivam Kumar", "M", "Varanasi"),
        Triple("Pooja Verma", "F", "Chandigarh"),
        Triple("Riya Kumari", "F", "Kolkata"),
        Triple("Ayush Raj", "M", "Ranchi"),
        Triple("Vikas Singh", "M", "Indore"),
        Triple("Saurabh Kumar", "M", "Bhopal"),
        Triple("Khushi Sharma", "F", "Ahmedabad"),
        Triple("Deepak Yadav", "M", "Kanpur"),
        Triple("Karan Singh", "M", "Ludhiana"),
        Triple("Muskan Gupta", "F", "Surat"),
        Triple("Akash Kumar", "M", "Hyderabad")
    )

    private val maleFirstNames = listOf(
        "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Ayaan", "Krishna", "Ishaan",
        "Shaurya", "Atharv", "Kabir", "Rohan", "Dev", "Harsh", "Yash", "Nikhil", "Pranav", "Siddharth",
        "Manish", "Gaurav", "Abhishek", "Tarun", "Varun", "Kunal", "Ankit", "Mohit", "Sumit", "Kartik",
        "Tushar", "Mayank", "Piyush", "Harshit", "Utkarsh", "Rishabh", "Shubham", "Uday", "Lakshay", "Omkar",
        "Chirag", "Tanmay", "Parth", "Nitin", "Sagar", "Pankaj", "Naveen", "Dheeraj", "Pradeep", "Rajesh"
    )

    private val femaleFirstNames = listOf(
        "Aadhya", "Diya", "Ananya", "Pari", "Saanvi", "Myra", "Aarohi", "Ira", "Navya", "Kiara",
        "Riya", "Kavya", "Shruti", "Nandini", "Tanvi", "Kritika", "Meera", "Aditi", "Simran", "Palak",
        "Sakshi", "Ishita", "Divya", "Komal", "Swati", "Nisha", "Prachi", "Jyoti", "Pallavi", "Bhavna",
        "Shreya", "Tanya", "Aarti", "Radhika", "Gauri", "Mansi", "Kajal", "Sonal", "Rashmi", "Nikita",
        "Ritika", "Surbhi", "Payal", "Garima", "Saloni", "Avni", "Charvi", "Zoya", "Mahima", "Vanshika"
    )

    private val lastNames = listOf(
        "Kumar", "Singh", "Sharma", "Verma", "Gupta", "Yadav", "Patel", "Mishra", "Tiwari", "Pandey",
        "Chauhan", "Joshi", "Nair", "Iyer", "Reddy", "Rao", "Das", "Banerjee", "Chatterjee", "Kulkarni",
        "Deshmukh", "Jain", "Agarwal", "Mehta", "Shah", "Malhotra", "Kapoor", "Khanna", "Bhatia", "Choudhary",
        "Thakur", "Saxena", "Srivastava", "Dubey", "Shukla", "Bansal", "Goyal", "Mittal", "Arora", "Saini"
    )

    private data class CityCoord(val name: String, val lat: Double, val lng: Double)

    private val cities = listOf(
        CityCoord("New Delhi", 28.6139, 77.2090),
        CityCoord("Mumbai", 19.0760, 72.8777),
        CityCoord("Bengaluru", 12.9716, 77.5946),
        CityCoord("Jaipur", 26.9124, 75.7873),
        CityCoord("Pune", 18.5204, 73.8567),
        CityCoord("Hyderabad", 17.3850, 78.4867),
        CityCoord("Lucknow", 26.8467, 80.9462),
        CityCoord("Kolkata", 22.5726, 88.3639),
        CityCoord("Chandigarh", 30.7333, 76.7794),
        CityCoord("Ahmedabad", 23.0225, 72.5714),
        CityCoord("Patna", 25.5941, 85.1376),
        CityCoord("Indore", 22.7196, 75.8577),
        CityCoord("Varanasi", 25.3176, 82.9739),
        CityCoord("Kochi", 9.9312, 76.2673),
        CityCoord("Surat", 21.1702, 72.8311),
        CityCoord("Bhopal", 23.2599, 77.4126)
    )

    private val bios = listOf(
        "Chai lover | Snap streaks daily | AI Sim Friend",
        "College vibes, cricket & late night Maggi | AI Sim",
        "Sunset chaser & Golden Hour filter fan | AI Sim",
        "Desi foodie exploring street momos & golgappe | AI Sim",
        "Music, road trips & weekend stories | AI Sim",
        "Coding by day, snapping neon nights | AI Sim",
        "Festival lights, DSLR portraits & PN Points | AI Sim",
        "Monsoon rain + hot cutting chai = perfection | AI Sim"
    )

    private val storyScenes = listOf(
        Triple("Cutting chai on the college terrace!", "☕", 0xFFFF8F00L),
        Triple("Evening metro rush with sunset glow", "🌇", 0xFFE65100L),
        Triple("Late night hostel Maggi party!", "🍜", 0xFF6A1B9AL),
        Triple("Cricket match hype with the squad", "🏏", 0xFF1565C0L),
        Triple("Marigold lights & festive vibes at home", "🪔", 0xFFD81B60L),
        Triple("Weekend highway drive playlist on repeat", "🚗", 0xFF00695CL),
        Triple("Rainy window view + Lo-Fi beats", "🌧️", 0xFF283593L),
        Triple("Street samosa & jalebi craving unlocked", "🥟", 0xFFAD1457L)
    )

    val incomingSnapThemes = listOf(
        Pair("Chai & Monsoon", "Tapri wali garma-garam chai in the rain! ☕🌧️"),
        Pair("College Canteen", "Canteen samosa break between lectures 🥟📚"),
        Pair("Golden Hour Selfie", "Checking out this Golden Hour filter ✨🌅"),
        Pair("Cricket Night", "Gully cricket match under street lights 🏏🔥"),
        Pair("Desi Dinner", "Maa ke haath ka rajma chawal tonight ❤️🍛"),
        Pair("Travel Ride", "Window seat view on the expressway 🚆🌄"),
        Pair("Festival Spark", "Fairy lights & rangoli prep mode ON 🪔🎨"),
        Pair("Gym & Hustle", "Evening workout done! Streak maintained 💪⚡"),
        Pair("City Sunset", "Sky turned pure Orange & Teal today 🌇🧡"),
        Pair("Funny Moment", "Tried the Big Head filter and can't stop laughing 😂🎈")
    )

    private val avatarColors = listOf(
        0xFFFFFC00L, 0xFF00E5FFL, 0xFFFF3B5CL, 0xFF18E07FL,
        0xFF7C4DFFL, 0xFFFF9100L, 0xFFFF4081L, 0xFF448AFFL
    )

    // Total = 560 unique Indian simulated profiles
    val allFriends: List<SimulatedFriendProfile> by lazy {
        val result = ArrayList<SimulatedFriendProfile>(560)
        val usedNames = HashSet<String>(600)

        // First 20 exact requested profiles
        priorityFriends.forEachIndexed { idx, (name, gender, cityName) ->
            usedNames.add(name)
            val city = cities.firstOrNull { it.name.contains(cityName) } ?: cities[idx % cities.size]
            result.add(
                buildProfile(
                    index = idx,
                    name = name,
                    gender = gender,
                    city = city,
                    isDefaultFriend = true
                )
            )
        }

        // Generate 540 additional unique Indian male & female profiles (total 560)
        var counter = 0
        var idx = 20
        while (result.size < 560 && counter < 5000) {
            val isFemale = (counter % 2 == 1)
            val firstPool = if (isFemale) femaleFirstNames else maleFirstNames
            val fIdx = (counter / 2) % firstPool.size
            val lIdx = ((counter / 2) / firstPool.size + (if (isFemale) 7 else 0) + (counter % 13)) % lastNames.size
            val fullName = "${firstPool[fIdx]} ${lastNames[lIdx]}"
            if (usedNames.add(fullName)) {
                val city = cities[idx % cities.size]
                result.add(
                    buildProfile(
                        index = idx,
                        name = fullName,
                        gender = if (isFemale) "F" else "M",
                        city = city,
                        isDefaultFriend = idx < 45
                    )
                )
                idx++
            }
            counter++
        }
        result
    }

    private fun buildProfile(
        index: Int,
        name: String,
        gender: String,
        city: CityCoord,
        isDefaultFriend: Boolean
    ): SimulatedFriendProfile {
        val id = "sim_${(index + 1).toString().padStart(3, '0')}"
        val cleanHandle = name.lowercase().replace(" ", "_") + "_${(index + 11) % 99}"
        val scene = storyScenes[index % storyScenes.size]
        val jitterLat = ((abs(name.hashCode()) % 100) - 50) * 0.004
        val jitterLng = ((abs(cleanHandle.hashCode()) % 100) - 50) * 0.004
        val statuses = listOf("Active now", "Snapping • 2m ago", "Online", "On streak 🔥", "5m ago")

        return SimulatedFriendProfile(
            id = id,
            name = name,
            username = "@$cleanHandle",
            gender = gender,
            city = city.name,
            latitude = city.lat + jitterLat,
            longitude = city.lng + jitterLng,
            bio = bios[index % bios.size],
            pnPoints = 12_000L + ((index * 1373L) % 95_000L),
            onlineStatus = statuses[index % statuses.size],
            avatarColorArgb = avatarColors[index % avatarColors.size],
            storyCaption = scene.first,
            storyThemeEmoji = scene.second,
            storyBgHex = scene.third,
            isDefaultFriend = isDefaultFriend
        )
    }
}
