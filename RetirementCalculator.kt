package br.com.aposentfacil.aposentapp.tools

import android.util.Log
import br.com.aposentfacil.aposentapp.entities.PeriodRetire
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 * Calculadora de tempos de aposentadoria
 *
 * Essa classe é a responsável pelos calculos do aplicativo AposentApp.
 *
 * @author Rafael Antonio Ribeiro
 *
 * @param gender sexo do usuário, usado para diferenciar o tempo de contribuição
 * @param birthdate data de nascimento do usuário
 * @param periodsList lista de periodos em que o usuário contribuiu (trabalhou)
 *
 */

class RetirementCalculator(
    gender: String,
    birthdate: String,
    periodsList: List<PeriodRetire>?,
    private var periodos : MutableList<Array<String>> = arrayListOf(),
    private var periodosEspeciais : MutableList<Array<String>> = arrayListOf(),
    private var diasDoPeriodoEspecial : MutableList<Int> = arrayListOf(),
    private var diasDoPeriodo : MutableList<Int> = arrayListOf(),
    private var diasContribuidos: Int = 0,
    private var diasContribuidosConvertidos: Int = 0,
    private var diasQueFaltamParaAposentar: Int = 0,
    private var diasQueFaltamParaAposentarConvertidos: Int = 0,
    private var anosQueFaltamParaAposentarPorIdade: Int = 0
)
{
    private val idadeDoUsuario = getAge(toArrDate(birthdate))
    private val generoDoUsuario = gender
    private var dataQueCompletaraTempoDeContribuicao: String = ""
    private var dataQueCompletaraTempoDeContribuicaoConvertida: String = ""
    private var dataQueCompletaraTempoDeContribuicaoSeAtividadeEspecial: String = ""
    private var tempoContribuido: String = ""
    private var tempoContribuidoComConversao: String = ""
    private var dataQueAposentaraPorIdade: String = ""
    private var formula86e96 : Int = 0

    init {

        periodsList?.forEachIndexed { _, periodRetire ->
            if (periodRetire.tipo == "Sim") {
                periodosEspeciais.add(arrayOf(periodRetire.inicio, periodRetire.fim))
            }else {
                periodos.add(arrayOf(periodRetire.inicio, periodRetire.fim))
            }
        }

        periodos = concatPeriods(periodos)
        periodosEspeciais = concatPeriods(periodosEspeciais)

        periodos.forEachIndexed { _, value ->
            diasDoPeriodo.add(toDays(value[1]) - toDays(value[0]))
            diasContribuidos += toDays(value[1]) - toDays(value[0])
            diasContribuidosConvertidos += toDays(value[1]) - toDays(value[0])
        }

        periodosEspeciais.forEachIndexed { _, value ->
            diasDoPeriodoEspecial.add(toDays(value[1]) - toDays(value[0]))
            diasContribuidos += toDays(value[1]) - toDays(value[0])
            diasContribuidosConvertidos += if (generoDoUsuario == GENDER_MALE) {
                ((toDays(value[1]) - toDays(value[0])) * 1.4).toInt()
            } else {
                ((toDays(value[1]) - toDays(value[0])) * 1.2).toInt()
            }
        }

        tempoContribuido = toBrDate(diasContribuidos)
        tempoContribuidoComConversao = toBrDate(diasContribuidosConvertidos)

        if( generoDoUsuario == GENDER_MALE ) {
            diasQueFaltamParaAposentarConvertidos = INT_35_YEARS.minus(diasContribuidosConvertidos)
            diasQueFaltamParaAposentar = INT_35_YEARS.minus(diasContribuidos)
        } else {
            diasQueFaltamParaAposentarConvertidos = INT_30_YEARS.minus(diasContribuidosConvertidos)
            diasQueFaltamParaAposentar = INT_30_YEARS.minus(diasContribuidos)
        }

        dataQueCompletaraTempoDeContribuicaoSeAtividadeEspecial = if (generoDoUsuario == GENDER_MALE) {
            sumTodayOf(toArrDate((diasQueFaltamParaAposentarConvertidos / 1.4 ).toInt()))
        } else {
            sumTodayOf(toArrDate((diasQueFaltamParaAposentarConvertidos / 1.2 ).toInt()))
        }

        dataQueCompletaraTempoDeContribuicao = sumTodayOf(toArrDate(diasQueFaltamParaAposentar))
        dataQueCompletaraTempoDeContribuicaoConvertida = sumTodayOf(toArrDate(diasQueFaltamParaAposentarConvertidos))

        val anosContribuidosComConversao = diasContribuidosConvertidos / 365
        formula86e96 = idadeDoUsuario + anosContribuidosComConversao

        anosQueFaltamParaAposentarPorIdade = if (generoDoUsuario == GENDER_MALE) {
            INT_65_YEARS.minus(idadeDoUsuario)
        } else {
            INT_60_YEARS.minus(idadeDoUsuario)
        }

        val arrNascimento = toArrDate(birthdate)
        val decim = DecimalFormat("00")
        val dias = decim.format(arrNascimento[0].toLong())
        val meses = decim.format(arrNascimento[1].toLong())
        val anoAposentadoriaPorIdade = arrNascimento[2] + anosQueFaltamParaAposentarPorIdade + idadeDoUsuario
        dataQueAposentaraPorIdade = dias.toString() + "/" + meses.toString() + "/" + anoAposentadoriaPorIdade.toString()

        // TODO: Remover no build final
        log(
            diasDoPeriodo,
            diasDoPeriodoEspecial,
            tempoContribuido,
            tempoContribuidoComConversao,
            dataQueCompletaraTempoDeContribuicao,
            dataQueCompletaraTempoDeContribuicaoConvertida,
            dataQueCompletaraTempoDeContribuicaoSeAtividadeEspecial,
            anosQueFaltamParaAposentarPorIdade,
            dataQueAposentaraPorIdade
        )
    }

    /**
     * Concatena os períodos concomitantes
     * @param mutableListArray lista de períodos contribuidos
     * @return array de períodos em série
     */
    private fun concatPeriods(mutableListArray: MutableList<Array<String>>) : MutableList<Array<String>> {
        for ( index in mutableListArray.indices ) {
            val nextIndex = index + 1
            if ( mutableListArray.lastIndex >= nextIndex ) {
                if ( toDays(mutableListArray[index][1]) >= toDays(mutableListArray[nextIndex][0]) ) {
                    mutableListArray[index][1] = mutableListArray[nextIndex][1]
                    mutableListArray.removeAt(nextIndex)
                }
            }
        }
        return mutableListArray
    }

    /**
     * Soma a data de hoje com outra data
     * @param arrDate array de data onde as posições respectivamente são dia, mes, ano
     * @return data somada
     */
    private fun sumTodayOf(arrDate : IntArray) : String {
        val currentDate = Date()
        val c = Calendar.getInstance()
        c.time = currentDate
        c.add(Calendar.DATE, arrDate[0]) //same with c.add(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, arrDate[1])
        c.add(Calendar.YEAR, arrDate[2])
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        return sdf.format(c.time)
    }

    /**
     * Converte uma data no formato 00/00/0000 em dias
     * @param date String de data no formado 00/00/0000
     * @return dias
     */
    private fun toDays(date: String): Int {
        val arr = date.split("/")
        return Integer.parseInt(arr[0]) + (Integer.parseInt(arr[1]) *30) + ((Integer.parseInt(arr[2]) *12)*30)
    }

    /**
     * Converte dias em data com meses de 30 dias
     * @param days dias
     * @return data no formato 00/00/0000
     */
    private fun toBrDate(days: Int) : String {
        val anos: Int = days / 364
        val meses: Int = days % 364 / 30
        val dias: Int = ( days % 364 ) % 30
        return anos.toString() + " anos, " + meses.toString() + " meses, " + dias.toString() + " dias."
    }

    /**
     * Converte dias em array no formado dia, mes, ano
     * @param days dias
     * @return array respectivamente com os valores dia, mes, ano
     */
    private fun toArrDate(days: Int) : IntArray {
        return intArrayOf(( days % 364 ) % 30, days % 364 / 30, days / 364)
    }

    /**
     * Converte uma data em array no formado dia, mes, ano
     * @param date String de data no formato 00/00/0000
     * @return array de data respectivamente com os valores dia, mes, ano
     */
    private fun toArrDate(date: String) : IntArray {
        val arr = date.split("/")
        val anos: Int = arr[2].toInt()
        val meses: Int = arr[1].toInt()
        val dias: Int = arr[0].toInt()
        return intArrayOf(dias, meses, anos)
    }

    /**
     * Calcula a idade
     * @param arrDate array respectivamente com os valores dia, mes, ano
     * @return idade em anos
     */
    private fun getAge(arrDate: IntArray): Int {
        val dob = Calendar.getInstance()
        val today = Calendar.getInstance()
        dob.set(arrDate[2], arrDate[1], arrDate[0])
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    // TODO: Remover no build final
    private fun log(
            diasDoPeriodo: MutableList<Int>,
            diasDoPeriodoEspecial: MutableList<Int>,
            tempoContribuido: String,
            tempoContribuidoComConversao: String,
            anoQueCompletara35anosSemConversao: String,
            anoQueCompletara35anosComConversao: String,
            dataQueCompletaraTempoDeContribuicaoSeAtividadeEspecial: String,
            anosQueFaltamParaAposentarPorIdade: Int,
            dataQueAposentaraPorIdade: String
    ) {
        Log.d("console.log", "-- Períodos Normais -----------------------------------")
        diasDoPeriodo.forEachIndexed { index, value ->
            Log.d("console.log", "index: $index")
            Log.d("console.log", "dias: $value")
        }

        Log.d("console.log", "-- Períodos Especiais ---------------------------------")
        diasDoPeriodoEspecial.forEachIndexed { index, value ->
            Log.d("console.log", "index: $index")
            Log.d("console.log", "dias: $value")
        }

        Log.d("console.log", "-- Gênero ---------------------------------------------")
        Log.d("console.log", generoDoUsuario)

        Log.d("console.log", "-- Anos contribuídos sem conversão --------------------")
        Log.d("console.log", tempoContribuido)

        Log.d("console.log", "-- Anos contribuídos com conversão --------------------")
        Log.d("console.log", tempoContribuidoComConversao)

        Log.d("console.log", "-- Completará 35 anos em (com conversão): --------------------")
        Log.d("console.log", anoQueCompletara35anosComConversao)

        Log.d("console.log", "-- Completará 35 anos em (sem conversão): --------------------")
        Log.d("console.log", anoQueCompletara35anosSemConversao)

        Log.d("console.log", "-- Data Que Completará 35 ou 30 anos se a Atividade for Especial: --------------------")
        Log.d("console.log", dataQueCompletaraTempoDeContribuicaoSeAtividadeEspecial)

        Log.d("console.log", "-- Formula 86 / 96: --------------------")
        Log.d("console.log", formula86e96.toString())

        Log.d("console.log", "-- Anos que falta para aposentadoria por idade --------------------")
        Log.d("console.log", anosQueFaltamParaAposentarPorIdade.toString())

        Log.d("console.log", "-- Data da aposentadoria por idade --------------------")
        Log.d("console.log", dataQueAposentaraPorIdade)
    }

    fun getTempoContribuidoSemConversao(): String {
        return tempoContribuido
    }
    fun getTempoContribuidoComConversao(): String {
        return tempoContribuidoComConversao
    }
    fun getDataQueCompletara35anosSemConversao(): String {
        return dataQueCompletaraTempoDeContribuicao
    }
    fun getDataQueCompletara35anosComConversao(): String {
        return dataQueCompletaraTempoDeContribuicaoConvertida
    }
    fun getDataQueCompletara35ou30anosSeAtividadeForEspecial(): String {
        return dataQueCompletaraTempoDeContribuicaoSeAtividadeEspecial
    }
    fun getAnosQueFaltamParaAposentarPorIdade(): String {
        return anosQueFaltamParaAposentarPorIdade.toString()
    }
    fun getDataQueAposentaraPorIdade(): String {
        return dataQueAposentaraPorIdade
    }
    fun getformula86e96(): String {
        return formula86e96.toString()
    }

    companion object {
        private const val GENDER_MALE = "Masculino"
        private const val INT_35_YEARS = 12775
        private const val INT_30_YEARS = 10950
        private const val INT_60_YEARS = 60
        private const val INT_65_YEARS = 65
    }
}
