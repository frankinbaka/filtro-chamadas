// Criação By Frankin + Claude
package br.filtro.chamadas

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * O Android chama este serviço antes de o telefone tocar.
 * Ele decide se a ligação toca normalmente ou é recusada.
 */
class FiltroService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        val dados = Dados(this)

        // Só filtra ligações recebidas
        if (details.callDirection != Call.Details.DIRECTION_INCOMING || !dados.ativo) {
            permitir(details); return
        }

        val numero = details.handle?.schemeSpecificPart.orEmpty()

        if (numero.isBlank()) {
            if (dados.bloquearOcultos) bloquear(details, dados, "Número oculto", "Número oculto")
            else permitir(details)
            return
        }

        if (estaNosContatos(numero)) { permitir(details); return }

        val analise = Regras.analisar(numero)
        if (Regras.deveBloquear(analise, dados.ddds)) {
            bloquear(details, dados, numero, Regras.motivo(analise))
        } else {
            permitir(details)
        }
    }

    private fun permitir(details: Call.Details) {
        respondToCall(details, CallResponse.Builder().build())
    }

    private fun bloquear(details: Call.Details, dados: Dados, numero: String, motivo: String) {
        respondToCall(
            details,
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
        )
        dados.registrar(numero, motivo)
    }

    private fun estaNosContatos(numero: String): Boolean {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numero))
            contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
