package com.example.academic.code

import com.example.data.models.CodeLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCodeAnalyzerTest {
    @Test
    fun `runs hello world from a canvas code card`() {
        val result = LocalCodeAnalyzer.run("print(\"Hello world\")", CodeLanguage.PYTHON)

        assertTrue(result.isSuccess)
        assertEquals("Hello world", result.output)
    }

    @Test
    fun `runs basic Python assignments and print`() {
        val result = LocalCodeAnalyzer.run(
            """
                topic = "Thermodynamics"
                temperature = 20 + 5 * 2
                print(topic, temperature)
            """.trimIndent(),
            CodeLanguage.PYTHON
        )

        assertTrue(result.isSuccess)
        assertEquals("Thermodynamics 30", result.output)
    }

    @Test
    fun `runs basic C printf formatting`() {
        val result = LocalCodeAnalyzer.run(
            """
                #include <stdio.h>
                int main() {
                    double energy = 12.5;
                    printf("E = %.1f J\\n", energy);
                    return 0;
                }
            """.trimIndent(),
            CodeLanguage.C
        )

        assertTrue(result.isSuccess)
        assertEquals("E = 12.5 J", result.output)
    }

    @Test
    fun `runs basic C plus plus cout`() {
        val result = LocalCodeAnalyzer.run(
            """
                int a = 4;
                int b = 3;
                std::cout << "sum=" << a + b << std::endl;
            """.trimIndent(),
            CodeLanguage.CPP
        )

        assertTrue(result.isSuccess)
        assertEquals("sum=7", result.output)
    }

    @Test
    fun `reports unknown variables without crashing`() {
        val result = LocalCodeAnalyzer.run("print(missing + 1)", CodeLanguage.PYTHON)

        assertFalse(result.isSuccess)
        assertTrue(result.diagnostics.single().message.contains("Unknown variable"))
    }

    @Test
    fun `warns when control flow exceeds supported study subset`() {
        val result = LocalCodeAnalyzer.run("for i in range(3):\n    print(i)", CodeLanguage.PYTHON)

        assertTrue(result.diagnostics.any { it.severity == CodeDiagnostic.Severity.WARNING })
    }
}
