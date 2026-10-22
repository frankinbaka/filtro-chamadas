// Criação By Frankin + Claude
package br.filtro.chamadas

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Guarda as configurações e o histórico de chamadas bloqueadas no próprio celular. */
class Dados(context: Context) {

    private val prefs = context.getSharedPreferences("filtro", Context.MODE_PRIVATE)

    var ativo: Boolean
        get() = prefs.getBoolean("ativo", true)
        set(v) = prefs.edit().putBoolean("ativo", v).apply()

    var bloquearOcultos: Boolean
        get() = prefs.getBoolean("bloquear_ocultos", false)
        set(v) = prefs.edit().putBoolean("bloquear_ocultos", v).apply()

    var ddds: Set<String>
        get() = prefs.getStringSet("ddds", null)?.toSet() ?: setOf("99")
        set(v) = prefs.edit().putStringSet("ddds", v.toSet()).apply()

    data class Bloqueio(val numero: String, val motivo: String, val quando: Long)

    fun historico(): List<Bloqueio> {
        val arr = JSONArray(prefs.getString("historico", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Bloqueio(o.getString("n"), o.getString("m"), o.getLong("t"))
        }
    }

    @Synchronized
    fun registrar(numero: String, motivo: String) {
        val atual = historico().toMutableList()
        atual.add(0, Bloqueio(numero, motivo, System.currentTimeMillis()))
        val arr = JSONArray()
        atual.take(300).forEach {
            arr.put(JSONObject().put("n", it.numero).put("m", it.motivo).put("t", it.quando))
        }
        prefs.edit().putString("historico", arr.toString()).apply()
    }

    fun limparHistorico() = prefs.edit().putString("historico", "[]").apply()
}
