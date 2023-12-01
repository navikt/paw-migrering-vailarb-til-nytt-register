package no.nav.paw.migrering.app

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.app.db.skrivBatchTilDb
import no.nav.paw.migrering.app.kafka.StatusConsumerRebalanceListener
import no.nav.paw.migrering.app.konfigurasjon.applikasjonKonfigurasjon
import no.nav.paw.migrering.app.mapping.situasjonMottat
import no.nav.paw.migrering.app.mapping.tilPeriode
import no.nav.paw.migrering.app.serde.hendelseTilBytes
import no.nav.paw.migrering.app.utils.nLimitFilter
import org.apache.kafka.clients.producer.KafkaProducer

fun prepareBatches(
    periodeHendelseMeldinger: Sequence<List<Pair<String, ArbeidssokerperiodeHendelseMelding>>>,
    besvarelseHendelser: Sequence<List<Pair<String, ArbeidssokerBesvarelseEvent>>>,
): Sequence<List<Hendelse>> {
    val utfoertAv = Bruker(
        type = BrukerType.SYSTEM,
        id = applikasjonKonfigurasjon.applicationName
    )
    return periodeHendelseMeldinger
        .map { batch -> batch.map { (_, periodeMelding) -> tilPeriode(utfoertAv, periodeMelding) } }
        .zip(besvarelseHendelser) { perioder, besvarelser ->
            perioder + besvarelser.map { (_, besvarelse) -> situasjonMottat(utfoertAv, besvarelse) }
        }
        .nLimitFilter(numberOfConsecutiveFalseBeforeForward = 3, Collection<Hendelse>::isNotEmpty)
}

context (PrometheusMeterRegistry)
fun Sequence<List<Hendelse>>.processBatches(
    consumerStatus: StatusConsumerRebalanceListener,
    eventlogTopic: String,
    producer: KafkaProducer<Long, Hendelse>,
    identitetsnummerTilKafkaKey: (String) -> Long,
) {
    forEach { batch ->
        when {
            consumerStatus.isReady() && batch.isEmpty() -> hentDataFraDbOgSendTilTopic(
                eventlogTopic,
                producer,
                identitetsnummerTilKafkaKey
            )

            batch.isNotEmpty() -> skrivBatchTilDb(serializer = hendelseTilBytes, batch = batch)
            else -> logger.info("Venter på at alle topics skal bli klare")
        }
    }
}
