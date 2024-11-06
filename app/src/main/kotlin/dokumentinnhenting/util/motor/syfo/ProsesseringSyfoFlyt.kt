package dokumentinnhenting.util.motor.syfo

import dokumentinnhenting.util.motor.syfo.syfosteg.SyfoSteg

class ProsesseringSyfoFlyt private constructor(
    private val rekkefølge: List<SyfoSteg>,
    private val stegTilUtfall: HashMap<SyfoSteg, ProsesseringSyfoStatus>,
    private val utfallTilSteg: HashMap<ProsesseringSyfoStatus, SyfoSteg>,
) {

    fun fraStatus(prosesseringStatus: ProsesseringSyfoStatus?): List<SyfoSteg> {
        if (prosesseringStatus == null) {
            return rekkefølge
        }
        val stegForUtfall = utfallTilSteg[prosesseringStatus]
            ?: throw IllegalStateException("Uforventet oppslag av udefinert steg for status $prosesseringStatus")
        return rekkefølge.dropWhile { it != stegForUtfall }.drop(1)
    }

    fun utfall(steg: SyfoSteg): ProsesseringSyfoStatus {
        return stegTilUtfall[steg]
            ?: throw IllegalStateException("Uforventet oppslag av udefinert utfall for steg $steg")
    }

    class Builder {
        private val rekkefølge = mutableListOf<SyfoSteg>()
        private val stegTilUtfall = mutableMapOf<SyfoSteg, ProsesseringSyfoStatus>()
        private val utfallTilSteg = mutableMapOf<ProsesseringSyfoStatus, SyfoSteg>()

        fun med(steg: SyfoSteg, utfall: ProsesseringSyfoStatus): Builder {
            if (rekkefølge.contains(steg)) {
                throw IllegalArgumentException("Steg $steg er allerede lagt til.")
            }
            if (utfallTilSteg.keys.contains(utfall)) {
                throw IllegalArgumentException("Utfall $utfall er allerede lagt til.")
            }
            rekkefølge.add(steg)
            stegTilUtfall.put(steg, utfall)
            utfallTilSteg.put(utfall, steg)

            return this
        }

        fun build(): ProsesseringSyfoFlyt {
            if (rekkefølge.isEmpty()) {
                throw IllegalStateException("Ingen steg å prosessere.")
            }

            return ProsesseringSyfoFlyt(
                rekkefølge = rekkefølge,
                stegTilUtfall = HashMap(stegTilUtfall),
                utfallTilSteg = HashMap(utfallTilSteg),
            )
        }
    }
}