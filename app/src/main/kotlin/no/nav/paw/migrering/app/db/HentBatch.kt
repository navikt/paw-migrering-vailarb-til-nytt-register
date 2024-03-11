package no.nav.paw.migrering.app.db

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.Operations
import no.nav.paw.migrering.app.konfigurasjon.kafkaKonfigurasjon
import no.nav.paw.migrering.app.loggTid
import no.nav.paw.migrering.app.serde.HendelseSerde
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.select

context(Transaction, PrometheusMeterRegistry)
fun hentBatch(antall: Int): List<Pair<Long, Hendelse?>> =
    loggTid(Operations.BATCH_READ_FROM_DB) {
        HendelserTabell
            .select { HendelserTabell.groupId eq kafkaKonfigurasjon.klientKonfigurasjon.konsumerGruppeId }
            .orderBy(HendelserTabell.tidspunkt to SortOrder.ASC)
            .limit(antall)
            .map { resultRow ->
                resultRow[HendelserTabell.id] to HendelseSerde().deserializer().deserialize("", resultRow[HendelserTabell.hendelse])
            }
    }
