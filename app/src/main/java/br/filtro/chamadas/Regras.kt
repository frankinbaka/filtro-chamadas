// Criação By Frankin + Claude
package br.filtro.chamadas

/**
 * Decide o que fazer com uma chamada a partir do número recebido.
 * Não depende do Android, para poder ser testado isoladamente.
 */
object Regras {

    enum class Tipo { NACIONAL, LOCAL_SEM_DDD, TELEMARKETING, ESPECIAL, INTERNACIONAL, DESCONHECIDO }

    data class Analise(val tipo: Tipo, val ddd: String? = null)

    fun analisar(numeroBruto: String): Analise {
        val bruto = numeroBruto.trim()
        val d = bruto.filter { it.isDigit() }
        if (d.isEmpty()) return Analise(Tipo.DESCONHECIDO)

        // Formato internacional com "+"
        if (bruto.startsWith("+")) {
            if (!d.startsWith("55")) return Analise(Tipo.INTERNACIONAL)
            return porResto(d.substring(2))
        }

        if (d.startsWith("00")) {
            // 00 + operadora + código do país
            return if (d.length > 4 && d.substring(4).startsWith("55")) porResto(d.substring(6))
            else Analise(Tipo.INTERNACIONAL)
        }

        if (d.startsWith("0303")) return Analise(Tipo.TELEMARKETING)
        if (d.startsWith("0800") || d.startsWith("0300") || d.startsWith("0500") || d.startsWith("0900")) {
            return Analise(Tipo.ESPECIAL)
        }

        if (d.startsWith("0")) {
            return when (d.length) {
                11, 12 -> porResto(d.substring(1))        // 0 + DDD + número
                13, 14 -> porResto(d.substring(3))        // 0 + operadora + DDD + número
                else -> Analise(Tipo.DESCONHECIDO)
            }
        }

        if (d.startsWith("55") && (d.length == 12 || d.length == 13)) return porResto(d.substring(2))

        return when (d.length) {
            10, 11 -> porResto(d)
            8, 9 -> if (d.startsWith("4004") || d.startsWith("3003")) Analise(Tipo.ESPECIAL)
                    else Analise(Tipo.LOCAL_SEM_DDD)
            else -> Analise(Tipo.DESCONHECIDO)
        }
    }

    private fun porResto(resto: String): Analise {
        if (resto.startsWith("303")) return Analise(Tipo.TELEMARKETING)
        if (resto.length != 10 && resto.length != 11) return Analise(Tipo.DESCONHECIDO)
        val ddd = resto.substring(0, 2)
        if (ddd[0] == '0') return Analise(Tipo.DESCONHECIDO)
        return Analise(Tipo.NACIONAL, ddd)
    }

    /** true = bloquear. Contatos salvos são liberados antes de chegar aqui. */
    fun deveBloquear(a: Analise, dddsPermitidos: Set<String>): Boolean = when (a.tipo) {
        Tipo.NACIONAL -> a.ddd !in dddsPermitidos
        Tipo.TELEMARKETING -> true
        Tipo.INTERNACIONAL -> true
        // Na dúvida, deixa tocar para não perder ligação importante
        Tipo.LOCAL_SEM_DDD, Tipo.ESPECIAL, Tipo.DESCONHECIDO -> false
    }

    fun motivo(a: Analise): String = when (a.tipo) {
        Tipo.NACIONAL -> "DDD ${a.ddd}"
        Tipo.TELEMARKETING -> "Telemarketing (0303)"
        Tipo.INTERNACIONAL -> "Internacional"
        else -> ""
    }
}
