package no.nav.paw.migrering.app

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.paw.migrering.ArbeidssokerperiodeHendelseMelding
import no.nav.paw.migrering.app.db.skrivBatchTilDb
import no.nav.paw.migrering.app.kafka.StatusConsumerRebalanceListener
import no.nav.paw.migrering.app.kafkakeys.KafkaKeysResponse
import no.nav.paw.migrering.app.konfigurasjon.applikasjonKonfigurasjon
import no.nav.paw.migrering.app.mapping.conditionallyAddOneMilliSecond
import no.nav.paw.migrering.app.mapping.situasjonMottat
import no.nav.paw.migrering.app.mapping.tilPeriode
import no.nav.paw.migrering.app.serde.HendelseDeserializer
import no.nav.paw.migrering.app.serde.HendelseSerde
import no.nav.paw.migrering.app.serde.HendelseSerializer
import no.nav.paw.migrering.app.utils.nLimitFilter
import org.apache.kafka.clients.producer.KafkaProducer

fun prepareBatches(
    periodeHendelseMeldinger: Sequence<List<Pair<String, ArbeidssokerperiodeHendelseMelding>>>,
    besvarelseHendelser: Sequence<List<Pair<String, ArbeidssokerBesvarelseEvent>>>,
    opplysningerFraVeilarbHendelser: Sequence<List<Pair<String, Hendelse>>>,
    numberOfConsecutiveEmptyBatchesToWaitFor: Long = 3,
    idfunksjon: (String) -> KafkaKeysResponse
): Sequence<List<Hendelse>> {
    val utfoertAv = Bruker(
        type = BrukerType.SYSTEM,
        id = applikasjonKonfigurasjon.applicationName
    )
    return periodeHendelseMeldinger
        .map { batch -> batch.map { (_, periodeMelding) -> tilPeriode(idfunksjon(periodeMelding.foedselsnummer).id,utfoertAv, periodeMelding) } }
        .zip(besvarelseHendelser) { perioder, besvarelser ->
            perioder + besvarelser
                .filter { it.second.endret }
                .map { (_, besvarelse) -> situasjonMottat(idfunksjon(besvarelse.foedselsnummer).id, utfoertAv, besvarelse) }
        }
        .zip(opplysningerFraVeilarbHendelser) { perioderOgBesvarelser, opplysningerFraVeilarb ->
            perioderOgBesvarelser + (opplysningerFraVeilarb.map { (_, opplysning) -> opplysning }
                .filterIsInstance<OpplysningerOmArbeidssoekerMottatt>()
                .map { it.copy(
                    id = idfunksjon(it.identitetsnummer).id,
                )}
                .map(::conditionallyAdd1MilliSecondToTimestamp))
        }
        .nLimitFilter(
            numberOfConsecutiveFalseBeforeForward = numberOfConsecutiveEmptyBatchesToWaitFor,
            numberOfConsecutiveTrueBeforeForwardAfterFirstTrigger = 2,
            predicate = Collection<Hendelse>::isNotEmpty
        )
}

fun conditionallyAdd1MilliSecondToTimestamp(hendelse: OpplysningerOmArbeidssoekerMottatt): OpplysningerOmArbeidssoekerMottatt {
    val newTimestamp = conditionallyAddOneMilliSecond(hendelse.metadata.tidspunkt)
    val newMetadata = hendelse.metadata.copy(tidspunkt = newTimestamp)
    return hendelse.copy(opplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker.copy(metadata = newMetadata))
}

context (PrometheusMeterRegistry)
fun Sequence<List<Hendelse>>.processBatches(
    consumerStatus: StatusConsumerRebalanceListener,
    eventlogTopic: String,
    producer: KafkaProducer<Long, Hendelse>,
    identitetsnummerTilKafkaKey: (String) -> KafkaKeysResponse,
) {
    val serializer = HendelseSerde().serializer()
    forEach { batch ->
        when {
            consumerStatus.isReady() && batch.isEmpty() -> hentDataFraDbOgSendTilTopic(
                eventlogTopic,
                producer,
                identitetsnummerTilKafkaKey
            )

            batch.isNotEmpty() -> skrivBatchTilDb(serializer = { hendelse -> serializer.serialize(null, hendelse) }, batch = batch)
            else -> logger.info("Venter på at alle topics skal bli klare")
        }
    }
}
