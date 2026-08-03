package dev.devtoolbox.core.tools

import dev.devtoolbox.core.Category
import dev.devtoolbox.core.Row
import dev.devtoolbox.core.Tool
import dev.devtoolbox.core.ToolBody
import dev.devtoolbox.core.ToolInput
import dev.devtoolbox.core.ToolOutput
import dev.devtoolbox.core.util.BrazilianPhone
import dev.devtoolbox.core.util.Cnpj
import dev.devtoolbox.core.util.Cpf
import dev.devtoolbox.core.util.ValidationResult

/** Base comum dos três validadores: só muda a função de validação e a copy. */
private abstract class ValidatorTool(
    override val id: String,
    override val name: String,
    override val icon: String,
    override val description: String,
    defaultValue: String,
) : Tool {
    override val category = Category.Validators
    override val defaultInput = ToolInput.Text(defaultValue)
    override val singleLineInput = true

    abstract fun validate(text: String): ValidationResult

    override fun run(input: ToolInput): ToolOutput {
        val text = (input as? ToolInput.Text)?.value ?: return ToolOutput.Failure("Entrada inválida.")
        if (text.isBlank()) {
            return ToolOutput.Success(ToolBody.Validate("", valid = false, rows = emptyList()))
        }
        val result = validate(text)
        return ToolOutput.Success(
            ToolBody.Validate(
                value = result.formatted,
                valid = result.valid,
                rows = result.details.map { (label, value) -> Row(label, value) },
            ),
        )
    }
}

val CpfTool: Tool = object : ValidatorTool(
    id = "cpf",
    name = "Validador de CPF",
    icon = "identification-card",
    description = "Valide o formato e os dígitos verificadores de um CPF.",
    defaultValue = "529.982.247-25",
) {
    override fun validate(text: String) = Cpf.validate(text)
}

val CnpjTool: Tool = object : ValidatorTool(
    id = "cnpj",
    name = "Validador de CNPJ",
    icon = "buildings",
    description = "Valide o formato e os dígitos verificadores de um CNPJ.",
    defaultValue = "11.222.333/0001-81",
) {
    override fun validate(text: String) = Cnpj.validate(text)
}

val PhoneTool: Tool = object : ValidatorTool(
    id = "phone",
    name = "Validador de Telefone (BR)",
    icon = "phone",
    description = "Valide e formate números de telefone brasileiros, com DDD e tipo.",
    defaultValue = "(11) 98765-4321",
) {
    override fun validate(text: String) = BrazilianPhone.validate(text)
}
