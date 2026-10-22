// Criação By Frankin + Claude
package br.filtro.chamadas

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var dados: Dados
    private lateinit var raiz: LinearLayout

    private val corPrimaria = 0xFF1B6E5A.toInt()
    private val corOk = 0xFF2E7D32.toInt()
    private val corAlerta = 0xFFC62828.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dados = Dados(this)
        raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(32))
        }
        setContentView(ScrollView(this).apply { addView(raiz) })
    }

    override fun onResume() {
        super.onResume()
        montarTela()
    }

    @Deprecated("Activity simples, sem AndroidX")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        montarTela()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        montarTela()
    }

    private fun temFuncao(): Boolean {
        val rm = getSystemService(RoleManager::class.java)
        return rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun temContatos() =
        checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun montarTela() {
        raiz.removeAllViews()

        titulo("Filtro de Chamadas", 26f)
        texto("Recusa automaticamente ligações de fora dos DDDs permitidos. Contatos salvos sempre tocam.")

        // ---------- Configuração ----------
        secao("Configuração")
        val funcaoOk = temFuncao()
        val contatosOk = temContatos()

        passo(
            "1. Definir como app de filtro de chamadas", funcaoOk,
            "Sem isso o app não consegue recusar ligações."
        ) {
            val rm = getSystemService(RoleManager::class.java)
            @Suppress("DEPRECATION")
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), 1)
        }
        passo(
            "2. Permitir acesso aos contatos", contatosOk,
            "Para nunca bloquear quem está salvo na sua agenda."
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 2)
        }

        // ---------- Filtro ----------
        secao("Filtro")
        interruptor("Filtro ligado", dados.ativo) { dados.ativo = it }
        interruptor("Bloquear também números ocultos", dados.bloquearOcultos) { dados.bloquearOcultos = it }

        val status = when {
            !funcaoOk -> "⚠ Conclua o passo 1 para o filtro funcionar."
            !dados.ativo -> "Filtro desligado: todas as ligações vão tocar."
            else -> "✓ Filtro funcionando."
        }
        texto(status).apply {
            setTextColor(if (funcaoOk && dados.ativo) corOk else corAlerta)
            setTypeface(typeface, Typeface.BOLD)
        }

        // ---------- DDDs ----------
        secao("DDDs permitidos")
        texto("Ligações destes DDDs tocam normalmente. As de qualquer outro DDD são recusadas.")
        dados.ddds.sorted().forEach { ddd ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(4))
            }
            linha.addView(TextView(this).apply {
                text = "DDD $ddd"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            linha.addView(Button(this).apply {
                text = "Remover"
                setOnClickListener { removerDdd(ddd) }
            })
            raiz.addView(linha)
        }

        val adicionar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        val campo = EditText(this).apply {
            hint = "Novo DDD (ex: 98)"
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(2))
        }
        adicionar.addView(campo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        adicionar.addView(Button(this).apply {
            text = "Adicionar"
            setOnClickListener { adicionarDdd(campo.text.toString()) }
        })
        raiz.addView(adicionar)

        // ---------- Histórico ----------
        val historico = dados.historico()
        secao("Chamadas bloqueadas (${historico.size})")
        if (historico.isEmpty()) {
            texto("Nenhuma ligação bloqueada ainda.")
        } else {
            val fmt = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
            historico.take(100).forEach {
                val linha = TextView(this).apply {
                    text = "${fmt.format(Date(it.quando))}   ${it.numero}\n${it.motivo}"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setPadding(0, dp(6), 0, dp(6))
                }
                raiz.addView(linha)
            }
            raiz.addView(Button(this).apply {
                text = "Limpar histórico"
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Apagar o histórico de chamadas bloqueadas?")
                        .setPositiveButton("Apagar") { _, _ -> dados.limparHistorico(); montarTela() }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            })
        }
    }

    private fun adicionarDdd(valor: String) {
        val ddd = valor.trim()
        if (ddd.length != 2 || ddd[0] == '0' || !ddd.all { it.isDigit() }) {
            Toast.makeText(this, "Digite um DDD com 2 números, ex: 98", Toast.LENGTH_SHORT).show()
            return
        }
        if (ddd in dados.ddds) {
            Toast.makeText(this, "O DDD $ddd já está na lista", Toast.LENGTH_SHORT).show()
            return
        }
        dados.ddds = dados.ddds + ddd
        Toast.makeText(this, "DDD $ddd adicionado", Toast.LENGTH_SHORT).show()
        montarTela()
    }

    private fun removerDdd(ddd: String) {
        if (dados.ddds.size == 1) {
            Toast.makeText(this, "Mantenha pelo menos um DDD na lista", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setMessage("Remover o DDD $ddd? Ligações dele passarão a ser recusadas.")
            .setPositiveButton("Remover") { _, _ -> dados.ddds = dados.ddds - ddd; montarTela() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Componentes ----------

    private fun titulo(t: String, tamanho: Float) = TextView(this).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, tamanho)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(corPrimaria)
        raiz.addView(this)
    }

    private fun secao(t: String) {
        raiz.addView(View(this).apply { setBackgroundColor(0x33888888) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(20); bottomMargin = dp(12)
            })
        TextView(this).apply {
            text = t
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(6))
            raiz.addView(this)
        }
    }

    private fun texto(t: String) = TextView(this).apply {
        text = t
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(0, dp(4), 0, dp(4))
        raiz.addView(this)
    }

    private fun passo(t: String, feito: Boolean, explicacao: String, acao: () -> Unit) {
        val caixa = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setStroke(dp(2), if (feito) corOk else corAlerta)
            }
        }
        caixa.addView(TextView(this).apply {
            text = (if (feito) "✓ " else "") + t
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        })
        caixa.addView(TextView(this).apply {
            text = if (feito) "Concluído" else explicacao
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        })
        if (!feito) {
            caixa.addView(Button(this).apply {
                text = "Fazer agora"
                setOnClickListener { acao() }
            })
        }
        raiz.addView(caixa, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
    }

    @Suppress("DEPRECATION")
    private fun interruptor(t: String, valor: Boolean, aoMudar: (Boolean) -> Unit) {
        Switch(this).apply {
            text = t
            isChecked = valor
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, marcado -> aoMudar(marcado); montarTela() }
            raiz.addView(this)
        }
    }

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
    ).toInt()
}
