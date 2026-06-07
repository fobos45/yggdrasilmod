package eu.neilalexander.yggdrasilmod

import android.content.Context
import mobile.Mobile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ConfigurationProxy {
    private lateinit var json: JSONObject
    private lateinit var file: File

    // Полный список всех пиров (включая отключённые) хранится отдельно
    private lateinit var allPeersFile: File

    operator fun invoke(applicationContext: Context): ConfigurationProxy {
        file = File(applicationContext.filesDir, "yggdrasil.conf")
        allPeersFile = File(applicationContext.filesDir, "all_peers.json")
        if (!file.exists()) {
            val conf = Mobile.generateConfigJSON()
            if (file.createNewFile()) {
                file.writeBytes(conf)
            }
        }
        if (!allPeersFile.exists()) {
            // Инициализируем список всех пиров из текущего конфига
            val conf = JSONObject(file.readText(Charsets.UTF_8))
            val peers = conf.optJSONArray("Peers") ?: JSONArray()
            allPeersFile.writeText(peers.toString(), Charsets.UTF_8)
        }
        fix()
        return this
    }

    fun resetJSON() {
        val conf = Mobile.generateConfigJSON()
        file.writeBytes(conf)
        allPeersFile.writeText("[]", Charsets.UTF_8)
        fix()
    }

    fun resetKeys() {
        val newJson = JSONObject(String(Mobile.generateConfigJSON()))
        updateJSON { json ->
            json.put("PrivateKey", newJson.getString("PrivateKey"))
        }
    }

    fun setKeys(privateKey: String) {
        updateJSON { json ->
            json.put("PrivateKey", privateKey)
        }
    }

    // Возвращает ВСЕ пиры (активные + отключённые)
    fun getAllPeers(): List<String> {
        val arr = JSONArray(allPeersFile.readText(Charsets.UTF_8))
        return (0 until arr.length()).map { arr.getString(it) }
    }

    fun addPeer(uri: String) {
        val arr = JSONArray(allPeersFile.readText(Charsets.UTF_8))
        arr.put(uri)
        allPeersFile.writeText(arr.toString(), Charsets.UTF_8)
    }

    fun removePeer(uri: String) {
        val arr = JSONArray(allPeersFile.readText(Charsets.UTF_8))
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            if (arr.getString(i) != uri) newArr.put(arr.getString(i))
        }
        allPeersFile.writeText(newArr.toString(), Charsets.UTF_8)
    }

    fun updateJSON(fn: (JSONObject) -> Unit) {
        json = JSONObject(file.readText(Charsets.UTF_8))
        fn(json)
        val str = json.toString()
        file.writeText(str, Charsets.UTF_8)
    }

    private fun fix() {
        updateJSON { json ->
            json.put("AdminListen", "none")
            json.put("IfName", "none")
            json.put("IfMTU", 65535)

            if (json.getJSONArray("MulticastInterfaces").get(0) is String) {
                val ar = JSONArray()
                ar.put(0, JSONObject("""
                    {
                        "Regex": ".*",
                        "Beacon": true,
                        "Listen": true,
                        "Password": ""
                    }
                """.trimIndent()))
                json.put("MulticastInterfaces", ar)
            }
        }
    }

    fun getJSON(): JSONObject {
        fix()
        return json
    }

    fun getJSONByteArray(): ByteArray {
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    var multicastListen: Boolean
        get() = (json.getJSONArray("MulticastInterfaces").get(0) as JSONObject).getBoolean("Listen")
        set(value) {
            updateJSON { json ->
                (json.getJSONArray("MulticastInterfaces").get(0) as JSONObject).put("Listen", value)
            }
        }

    var multicastBeacon: Boolean
        get() = (json.getJSONArray("MulticastInterfaces").get(0) as JSONObject).getBoolean("Beacon")
        set(value) {
            updateJSON { json ->
                (json.getJSONArray("MulticastInterfaces").get(0) as JSONObject).put("Beacon", value)
            }
        }

    var multicastPassword: String
        get() = (json.getJSONArray("MulticastInterfaces").get(0) as JSONObject).optString("Password")
        set(value) {
            updateJSON { json ->
                (json.getJSONArray("MulticastInterfaces").get(0) as JSONObject).put("Password", value)
            }
        }
}
