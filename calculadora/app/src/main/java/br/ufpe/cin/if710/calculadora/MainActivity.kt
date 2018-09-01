package br.ufpe.cin.if710.calculadora

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    private var display: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateDisplay(savedInstanceState?.getString("display"))
        setAnswer(savedInstanceState?.getString("answer"))

        // Configura os listeners
        setupListeners()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString("display", display);
        outState?.putString("answer", text_info.text.toString())
    }

    private fun setupListeners() {
        // Adiciona os listeners aos botões que só adicionam texto ao display
        val buttons = arrayOf<String>(
                "btn_1", "btn_2", "btn_3", "btn_4", "btn_5",
                "btn_6", "btn_7", "btn_8", "btn_9", "btn_0",
                "btn_Divide", "btn_Multiply", "btn_Add", "btn_Subtract",
                "btn_Dot", "btn_Power", "btn_LParen", "btn_RParen"
        ).map {
            val id = applicationContext.resources.getIdentifier(it, "id", packageName)
            findViewById<Button>(id)
        }
        // Helper function para adicionar os listeners de fato
        setListeners(buttons)

        // Adiciona listeners aos botões com comportamento diferente
        btn_Clear.setOnClickListener {
            updateDisplay(value="")
        }

        btn_Equal.setOnClickListener {
            attemptCalculation()
        }
    }

    // Tenta avaliar a expressão ou trata a exceção caso não consiga
    private fun attemptCalculation() {
        try {
            val value = eval(display)
            setAnswer(value.toString())
        } catch(e: RuntimeException) {
            showAlert("Operação inválida")
            updateDisplay("")
        }
    }

    // Cada botão atualiza o display
    private fun setListeners(buttons: List<Button?>): Unit {
        buttons.forEach {
            val btnText = it?.text
            it?.setOnClickListener {
                display += btnText
                updateDisplay()
            }
        }
    }

    private fun setAnswer(value: String?) {
        text_info.text = value ?: text_info.text
    }

    // Faz update do display
    private fun updateDisplay(value: String? = null) {
        display = value ?: display
        text_calc.setText(display)
    }

    // Exibe um alerta com a mensagem e título passados
    private fun showAlert(message: String, title: String = "Erro") {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setNeutralButton("OK") {_,_ -> Unit}
        builder.create().show()
    }

    //Como usar a função:
    // eval("2+2") == 4.0
    // eval("2+3*4") = 14.0
    // eval("(2+3)*4") = 20.0
    //Fonte: https://stackoverflow.com/a/26227947
    fun eval(str: String): Double {
        return object : Any() {
            var pos = -1
            var ch: Char = ' '
            fun nextChar() {
                val size = str.length
                ch = if ((++pos < size)) str.get(pos) else (-1).toChar()
            }

            fun eat(charToEat: Char): Boolean {
                while (ch == ' ') nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Caractere inesperado: " + ch)
                return x
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            // | number | functionName factor | factor `^` factor
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'))
                        x += parseTerm() // adição
                    else if (eat('-'))
                        x -= parseTerm() // subtração
                    else
                        return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'))
                        x *= parseFactor() // multiplicação
                    else if (eat('/'))
                        x /= parseFactor() // divisão
                    else
                        return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+')) return parseFactor() // + unário
                if (eat('-')) return -parseFactor() // - unário
                var x: Double
                val startPos = this.pos
                if (eat('(')) { // parênteses
                    x = parseExpression()
                    eat(')')
                } else if ((ch in '0'..'9') || ch == '.') { // números
                    while ((ch in '0'..'9') || ch == '.') nextChar()
                    x = java.lang.Double.parseDouble(str.substring(startPos, this.pos))
                } else if (ch in 'a'..'z') { // funções
                    while (ch in 'a'..'z') nextChar()
                    val func = str.substring(startPos, this.pos)
                    x = parseFactor()
                    if (func == "sqrt")
                        x = Math.sqrt(x)
                    else if (func == "sin")
                        x = Math.sin(Math.toRadians(x))
                    else if (func == "cos")
                        x = Math.cos(Math.toRadians(x))
                    else if (func == "tan")
                        x = Math.tan(Math.toRadians(x))
                    else
                        throw RuntimeException("Função desconhecida: " + func)
                } else {
                    throw RuntimeException("Caractere inesperado: " + ch.toChar())
                }
                if (eat('^')) x = Math.pow(x, parseFactor()) // potência
                return x
            }
        }.parse()
    }
}
