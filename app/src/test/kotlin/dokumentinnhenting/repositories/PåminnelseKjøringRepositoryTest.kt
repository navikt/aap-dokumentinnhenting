package dokumentinnhenting.repositories

import dokumentinnhenting.WithFakes
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate


@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PåminnelseKjøringRepositoryTest {
    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `skal lagre og hente sist kjørt for dato`() {
        val sistKjørtForDato = dataSource.transaction { connection ->
            val repository = PåminnelseKjøringRepository(connection)
            repository.lagreKjøringForDato(dato = LocalDate.now().minusDays(24))
            repository.lagreKjøringForDato(dato = LocalDate.now().minusDays(23))
            repository.hentSistKjørtForDato()

        }
        assertThat(sistKjørtForDato).isNotNull
        assertThat(sistKjørtForDato).isEqualTo(LocalDate.now().minusDays(23))
    }
}