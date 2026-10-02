package dokumentinnhenting

internal object TestConfig {

    fun default(fakes: Fakes): Config {
        return Config(
            dbConfig = DbConfig(
                url = "jdbc:h2:mem:test_db;MODE=PostgreSQL",
                username = "sa",
                password = ""
            ),
            unleash = UnleashConfig(
                apiUrl = "http://localhost",
                apiToken = "test",
                environment = "test"
            ),
        )
    }
}