package com.app.weather.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import org.json.JSONArray
import java.net.URL
import android.content.Context
import com.app.weather.BuildConfig

// ─── Data Models ─────────────────────────────────────────────────────────────

enum class WeatherType(val title: String) {
    Thunderstorm("Stormy"),
    Drizzle("Drizzle"),
    Rain("Rainy"),
    Snow("Snowy"),
    Mist("Misty"),
    Smoke("Smoky"),
    Haze("Hazy"),
    Dust("Dusty"),
    Fog("Foggy"),
    Sand("Sandy"),
    Ash("Ashy"),
    Squall("Squally"),
    Tornado("Tornado"),
    Clear("Clear"),
    Clouds("Cloudy")
}

data class ForecastItem(val time: String, val temp: String, val type: WeatherType)

data class DailyForecastItem(
    val day: String,
    val temp: String,
    val tempMin: String = "__",
    val type: WeatherType,
    val rainMm: Double = 0.0,
    val pop: Int = 0  // precipitation probability %
)

data class WeatherData(
    val type: WeatherType,
    val temp: Int?,
    val feelsLike: Int?,
    val location: String,
    val description: String,
    val aqi: String,
    val aqiValue: Int?,
    val wind: String,
    val windDeg: Int?,
    val windGust: String,
    val visibility: String,
    val visibilityM: Int?,
    val humidity: String,
    val humidityValue: Int?,
    val rainProb: String,
    val precipProbMax: Int?,
    val pressure: Int?,
    val uvIndex: Double?,
    val rainfallLast24h: Double?,
    val rainfallNext24h: Double?,
    val sunriseEpoch: Long?,
    val sunsetEpoch: Long?,
    val moonPhase: Double?,   // 0..1  (0=new, 0.5=full)
    val lat: Double?,
    val lon: Double?,
    val feelsLikeDesc: String,
    val lastUpdated: String,
    val lastUpdatedMs: Long,
    val hourlyForecast: List<ForecastItem>,
    val dailyForecast: List<DailyForecastItem>
) {
    companion object {
        val Default = WeatherData(
            type = WeatherType.Clear,
            temp = null, feelsLike = null,
            location = "Locating...", description = "__",
            aqi = ".. AQI", aqiValue = null,
            wind = "__", windDeg = null, windGust = "__",
            visibility = "__", visibilityM = null,
            humidity = "__", humidityValue = null,
            rainProb = "__", precipProbMax = null,
            pressure = null,
            uvIndex = null,
            rainfallLast24h = null, rainfallNext24h = null,
            sunriseEpoch = null, sunsetEpoch = null,
            moonPhase = null,
            lat = null, lon = null,
            feelsLikeDesc = "Loading...",
            lastUpdated = "Connecting...",
            lastUpdatedMs = 0L,
            hourlyForecast = List(8) { ForecastItem("__", "__", WeatherType.Clear) },
            dailyForecast = List(10) { DailyForecastItem("__", "__", "__", WeatherType.Clear) }
        )
    }
}

// ─── Cache ────────────────────────────────────────────────────────────────────

object WeatherCache {
    fun save(context: Context, data: WeatherData) {
        val prefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
        val hourlyArr = JSONArray()
        data.hourlyForecast.forEach { h ->
            hourlyArr.put(JSONObject().put("time", h.time).put("temp", h.temp).put("type", h.type.name))
        }
        val dailyArr = JSONArray()
        data.dailyForecast.forEach { d ->
            dailyArr.put(JSONObject()
                .put("day", d.day).put("temp", d.temp).put("tempMin", d.tempMin)
                .put("type", d.type.name).put("rainMm", d.rainMm).put("pop", d.pop))
        }

        val json = JSONObject()
            .put("type", data.type.name)
            .put("temp", data.temp ?: -999)
            .put("feelsLike", data.feelsLike ?: -999)
            .put("location", data.location)
            .put("description", data.description)
            .put("aqi", data.aqi)
            .put("aqiValue", data.aqiValue ?: -1)
            .put("wind", data.wind)
            .put("windDeg", data.windDeg ?: -1)
            .put("windGust", data.windGust)
            .put("visibility", data.visibility)
            .put("visibilityM", data.visibilityM ?: -1)
            .put("humidity", data.humidity)
            .put("humidityValue", data.humidityValue ?: -1)
            .put("rainProb", data.rainProb)
            .put("precipProbMax", data.precipProbMax ?: -1)
            .put("pressure", data.pressure ?: -1)
            .put("uvIndex", data.uvIndex ?: -1.0)
            .put("rainfallLast24h", data.rainfallLast24h ?: -1.0)
            .put("rainfallNext24h", data.rainfallNext24h ?: -1.0)
            .put("sunriseEpoch", data.sunriseEpoch ?: -1L)
            .put("sunsetEpoch", data.sunsetEpoch ?: -1L)
            .put("moonPhase", data.moonPhase ?: -1.0)
            .put("lat", data.lat ?: 0.0)
            .put("lon", data.lon ?: 0.0)
            .put("feelsLikeDesc", data.feelsLikeDesc)
            .put("lastUpdated", data.lastUpdated)
            .put("lastUpdatedMs", data.lastUpdatedMs)
            .put("hourly", hourlyArr)
            .put("daily", dailyArr)

        prefs.edit().putString("data", json.toString()).apply()
    }

    fun load(context: Context): WeatherData? {
        val str = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
            .getString("data", null) ?: return null
        return try {
            val json = JSONObject(str)
            val hourlyArr = json.getJSONArray("hourly")
            val hourly = List(hourlyArr.length()) { i ->
                val obj = hourlyArr.getJSONObject(i)
                ForecastItem(obj.getString("time"), obj.getString("temp"), WeatherType.valueOf(obj.getString("type")))
            }
            val dailyArr = json.getJSONArray("daily")
            val daily = List(dailyArr.length()) { i ->
                val obj = dailyArr.getJSONObject(i)
                DailyForecastItem(
                    obj.getString("day"), obj.getString("temp"),
                    obj.optString("tempMin", "__"),
                    WeatherType.valueOf(obj.getString("type")),
                    obj.optDouble("rainMm", 0.0),
                    obj.optInt("pop", 0)
                )
            }
            WeatherData(
                type = WeatherType.valueOf(json.getString("type")),
                temp = json.getInt("temp").takeIf { it != -999 },
                feelsLike = json.getInt("feelsLike").takeIf { it != -999 },
                location = json.getString("location"),
                description = json.getString("description"),
                aqi = json.getString("aqi"),
                aqiValue = json.getInt("aqiValue").takeIf { it != -1 },
                wind = json.getString("wind"),
                windDeg = json.getInt("windDeg").takeIf { it != -1 },
                windGust = json.getString("windGust"),
                visibility = json.getString("visibility"),
                visibilityM = json.getInt("visibilityM").takeIf { it != -1 },
                humidity = json.getString("humidity"),
                humidityValue = json.getInt("humidityValue").takeIf { it != -1 },
                rainProb = json.getString("rainProb"),
                precipProbMax = json.getInt("precipProbMax").takeIf { it != -1 },
                pressure = json.getInt("pressure").takeIf { it != -1 },
                uvIndex = json.getDouble("uvIndex").takeIf { it != -1.0 },
                rainfallLast24h = json.getDouble("rainfallLast24h").takeIf { it != -1.0 },
                rainfallNext24h = json.getDouble("rainfallNext24h").takeIf { it != -1.0 },
                sunriseEpoch = json.getLong("sunriseEpoch").takeIf { it != -1L },
                sunsetEpoch = json.getLong("sunsetEpoch").takeIf { it != -1L },
                moonPhase = json.getDouble("moonPhase").takeIf { it != -1.0 },
                lat = json.getDouble("lat").takeIf { it != 0.0 },
                lon = json.getDouble("lon").takeIf { it != 0.0 },
                feelsLikeDesc = json.getString("feelsLikeDesc"),
                lastUpdated = json.getString("lastUpdated"),
                lastUpdatedMs = json.optLong("lastUpdatedMs", 0L),
                hourlyForecast = hourly,
                dailyForecast = daily
            )
        } catch (e: Exception) { null }
    }
}

// ─── Backend ──────────────────────────────────────────────────────────────────

object WeatherBackend {
    private var openWeatherApiKey = BuildConfig.OPENWEATHER_API_KEY
    private var currentProvider = "OpenWeather"

    suspend fun fetchWeather(city: String = "Jakarta"): WeatherData = withContext(Dispatchers.IO) {
        fetchFromApi("q=$city")
    }

    suspend fun fetchWeatherByLocation(lat: Double, lon: Double): WeatherData = withContext(Dispatchers.IO) {
        fetchFromApi("lat=$lat&lon=$lon")
    }

    private fun parseWeatherType(conditionId: Int): WeatherType {
        return when (conditionId) {
            in 200..232 -> WeatherType.Thunderstorm
            in 300..321 -> WeatherType.Drizzle
            in 500..531 -> WeatherType.Rain
            in 600..622 -> WeatherType.Snow
            701         -> WeatherType.Mist
            711         -> WeatherType.Smoke
            721         -> WeatherType.Haze
            731         -> WeatherType.Dust
            741         -> WeatherType.Fog
            751         -> WeatherType.Sand
            761         -> WeatherType.Dust
            762         -> WeatherType.Ash
            771         -> WeatherType.Squall
            781         -> WeatherType.Tornado
            800         -> WeatherType.Clear
            else        -> WeatherType.Clouds
        }
    }

    private fun epochToTime(epoch: Long): String {
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(epoch * 1000))
    }

    private suspend fun fetchFromApi(query: String): WeatherData = coroutineScope {
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        val lastUpdatedMs = System.currentTimeMillis()
        val updateTime = timeFormat.format(java.util.Date(lastUpdatedMs))

        if (currentProvider == "BMKG") {
            return@coroutineScope WeatherData.Default.copy(location = "Indonesia", description = "BMKG reports clouds", lastUpdated = updateTime, lastUpdatedMs = lastUpdatedMs)
        } else if (currentProvider == "Google") {
            return@coroutineScope WeatherData.Default.copy(location = "Mountain View", description = "Google Weather simulated", lastUpdated = updateTime, lastUpdatedMs = lastUpdatedMs)
        }

        try {
            // ── Parallel Tier 1: Current Weather & Forecast ──────────────────
            val currentDeferred = async { 
                val url = "https://api.openweathermap.org/data/2.5/weather?$query&appid=$openWeatherApiKey&units=metric"
                JSONObject(URL(url).readText())
            }
            val forecastDeferred = async {
                val url = "https://api.openweathermap.org/data/2.5/forecast?$query&appid=$openWeatherApiKey&units=metric"
                JSONObject(URL(url).readText())
            }

            val currentJson = currentDeferred.await()
            val coord = currentJson.getJSONObject("coord")
            val lat = coord.getDouble("lat")
            val lon = coord.getDouble("lon")

            // ── Parallel Tier 2: AQI & UV (Requires Lat/Lon) ─────────────────
            val aqiDeferred = async {
                try {
                    val waqiApiKey = BuildConfig.WAQI_API_KEY
                    val url = "https://api.waqi.info/feed/geo:$lat;$lon/?token=$waqiApiKey"
                    val jsonResponse = JSONObject(URL(url).readText())
                    if (jsonResponse.optString("status") == "ok") {
                        jsonResponse.getJSONObject("data").getInt("aqi")
                    } else null
                } catch (_: Exception) { null }
            }
            val uvDeferred = async {
                try {
                    val url = "https://api.openweathermap.org/data/2.5/uvi?lat=$lat&lon=$lon&appid=$openWeatherApiKey"
                    JSONObject(URL(url).readText()).getDouble("value")
                } catch (_: Exception) { null }
            }

            val main = currentJson.getJSONObject("main")
            val temp = main.getDouble("temp").toInt()
            val feelsLike = main.getDouble("feels_like").toInt()
            val humidityValue = main.getInt("humidity")
            val pressureValue = main.getInt("pressure")

            val windObj = currentJson.getJSONObject("wind")
            val windSpeedKm = (windObj.getDouble("speed") * 3.6).toInt()
            val windDeg = windObj.optInt("deg", 0)
            val windGustKm = if (windObj.has("gust")) "${(windObj.getDouble("gust") * 3.6).toInt()} km/h" else "--"

            val visibilityM = currentJson.optInt("visibility", 10000)
            val location = currentJson.getString("name")

            val weatherArray = currentJson.getJSONArray("weather").getJSONObject(0)
            val conditionId = weatherArray.getInt("id")
            val description = weatherArray.getString("description")
            val type = parseWeatherType(conditionId)

            val sys = currentJson.getJSONObject("sys")
            val sunriseEpoch = sys.getLong("sunrise")
            val sunsetEpoch = sys.getLong("sunset")

            val rainLast1h = currentJson.optJSONObject("rain")?.optDouble("1h", 0.0) ?: 0.0

            // ── 5-day / 3-hour forecast ───────────────────
            val forecastList = forecastDeferred.await().getJSONArray("list")

            val fmtIn = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val fmtHour = java.text.SimpleDateFormat("h a", java.util.Locale.getDefault())
            val fmtDay = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
            val fmtDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

            val hourly = mutableListOf<ForecastItem>()
            val dailyMap = linkedMapOf<String, MutableList<JSONObject>>()

            var rainNext24h = 0.0
            var maxPop = 0

            for (i in 0 until forecastList.length()) {
                val item = forecastList.getJSONObject(i)
                val dtText = item.getString("dt_txt")
                val fTemp = item.getJSONObject("main").getDouble("temp").toInt().toString() + "°"
                val fType = parseWeatherType(item.getJSONArray("weather").getJSONObject(0).getInt("id"))
                val date = fmtIn.parse(dtText)

                if (i < 8 && date != null) {
                    hourly.add(ForecastItem(if (i == 0) "Now" else fmtHour.format(date).lowercase(), fTemp, fType))
                }
                if (i < 8) {
                    rainNext24h += item.optJSONObject("rain")?.optDouble("3h", 0.0) ?: 0.0
                    val pop = (item.optDouble("pop", 0.0) * 100).toInt()
                    if (pop > maxPop) maxPop = pop
                }

                if (date != null) {
                    val dateKey = fmtDate.format(date)
                    dailyMap.getOrPut(dateKey) { mutableListOf() }.add(item)
                }
            }

            val daily = mutableListOf<DailyForecastItem>()
            var dayIdx = 0
            for ((_, items) in dailyMap) {
                if (daily.size >= 10) break
                val temps = items.map { it.getJSONObject("main").getDouble("temp") }
                val maxt = temps.maxOrNull()?.toInt() ?: 0
                val mint = temps.minOrNull()?.toInt() ?: 0
                val repItem = items[items.size / 2]
                val fType = parseWeatherType(repItem.getJSONArray("weather").getJSONObject(0).getInt("id"))
                val rain = items.sumOf { it.optJSONObject("rain")?.optDouble("3h", 0.0) ?: 0.0 }
                val pop = items.maxOf { (it.optDouble("pop", 0.0) * 100).toInt() }
                val dayDate = fmtIn.parse(repItem.getString("dt_txt"))
                val dayName = if (dayIdx == 0) "Today" else if (dayDate != null) fmtDay.format(dayDate) else "Day ${dayIdx + 1}"
                daily.add(DailyForecastItem(dayName, "${maxt}°", "${mint}°", fType, rain, pop))
                dayIdx++
            }

            // ── Air Quality & UV ──────────────────────────
            val aqiValue = aqiDeferred.await()
            val aqiLabel = if (aqiValue == null) ".. AQI" else "$aqiValue AQI"

            val uvIndex = uvDeferred.await()

            // ── Moon phase ──────────
            val moonPhase = approximateMoonPhase()

            // ── Feels-like description ──────────────────────────────────────
            val feelsLikeDesc = buildFeelsLikeDesc(feelsLike, temp, humidityValue, type)

            // ── Rain probability ────────────────────────────────────────────
            val rainProbLabel = when {
                maxPop >= 70 -> "High"
                maxPop >= 40 -> "Medium"
                else         -> "Low"
            }

            WeatherData(
                type = type, temp = temp, feelsLike = feelsLike,
                location = location, description = description,
                aqi = aqiLabel, aqiValue = aqiValue,
                wind = "$windSpeedKm km/h", windDeg = windDeg, windGust = windGustKm,
                visibility = "${visibilityM / 1000} km", visibilityM = visibilityM,
                humidity = "$humidityValue%", humidityValue = humidityValue,
                rainProb = rainProbLabel, precipProbMax = maxPop,
                pressure = pressureValue,
                uvIndex = uvIndex,
                rainfallLast24h = rainLast1h * 24.0,
                rainfallNext24h = rainNext24h,
                sunriseEpoch = sunriseEpoch, sunsetEpoch = sunsetEpoch,
                moonPhase = moonPhase,
                lat = lat, lon = lon,
                feelsLikeDesc = feelsLikeDesc,
                lastUpdated = updateTime,
                lastUpdatedMs = lastUpdatedMs,
                hourlyForecast = hourly,
                dailyForecast = daily
            )
        } catch (e: Exception) {
            WeatherData.Default.copy(location = "Jakarta", description = "Failed to load", lastUpdated = updateTime, lastUpdatedMs = lastUpdatedMs)
        }
    }

    private fun approximateMoonPhase(): Double {
        val referenceNewMoon = 1736726400000L // ms
        val synodicMs = 29.53 * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return ((now - referenceNewMoon) % synodicMs / synodicMs + 1.0) % 1.0
    }

    private fun buildFeelsLikeDesc(feelsLike: Int, temp: Int, humidity: Int, type: WeatherType): String {
        val diff = feelsLike - temp
        return when {
            type == WeatherType.Rain || type == WeatherType.Thunderstorm -> "Rain is making it feel colder."
            humidity > 75 && diff > 0 -> "Humidity is making it feel warmer."
            humidity > 75 && diff <= 0 -> "High humidity adds to the discomfort."
            diff <= -5 -> "Wind is making it feel colder."
            diff >= 5  -> "Direct sun is adding heat."
            else       -> "Similar to the actual temperature."
        }
    }

    fun setProvider(provider: String) { currentProvider = provider }
    fun setApiKey(key: String) { openWeatherApiKey = key }
}