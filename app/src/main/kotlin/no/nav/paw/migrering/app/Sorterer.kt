package no.nav.paw.migrering.app

import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.SituasjonMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import org.apache.kafka.common.header.Headers
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant


fun KStream<Long, Hendelse>.sorter(
    tilstandDbNavn: String
): KStream<Long, Hendelse> {
    val processorSupplier = { TilstandsLaster(tilstandDbNavn) }
    return process(processorSupplier, Named.`as`("sorter_hendelser"), tilstandDbNavn)
}

class TilstandsLaster(
    private val tilstandDbNavn: String
) : Processor<Long, Hendelse, Long, Hendelse> {

    private val logger = LoggerFactory.getLogger(TilstandsLaster::class.java)
    private var tilstandsDb: KeyValueStore<Long, Tilstand>? = null
    private var context: ProcessorContext<Long, Hendelse>? = null

    override fun init(context: ProcessorContext<Long, Hendelse>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(tilstandDbNavn)
        context?.schedule(Duration.ofSeconds(10), PunctuationType.WALL_CLOCK_TIME) {
            val now = Instant.ofEpochMilli(it)
            logger.info("Kjører timer sjekk: $now")
            (tilstandsDb?.all()?.asSequence() ?: emptySequence())
                .filter { kv -> kv?.value != null }
                .filter { kv ->
                    kv.value.sistEndret.isBefore(now.minusSeconds(10))
                        .also { sendUt ->
                            logger.info("Sjekker om vi skal sende ut: ${kv.key} $sendUt")
                        }
                }
                .map { kv -> kv.key to kv.value.avsluttet + kv.value.startet + kv.value.situasjonMottatt }
                .forEach { (key, hendelser) ->
                    hendelser
                        .sortedBy { hendelse -> hendelse.metadata.tidspunkt }
                        .forEach { hendelse ->
                            context.forward(
                                Record(
                                    key,
                                    hendelse,
                                    now.toEpochMilli()
                                )
                            )
                        }
                    tilstandsDb?.delete(key)
                }
        }
    }

    override fun process(record: Record<Long, Hendelse>?) {
        if (record == null) return
        process(
            requireNotNull(context) { "Context er ikke initialisert" },
            requireNotNull(tilstandsDb) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        ctx: ProcessorContext<Long, Hendelse>,
        db: KeyValueStore<Long, Tilstand>,
        record: Record<Long, Hendelse>
    ) {
        logger.info("Prosesserer key: ${record.key()}")
        val tilstand: Tilstand? = db.get(record.key())
        logger.info("Leste tilstand: '$tilstand'")
        val nyTilstand = tilstand.leggTilHendelse(record.value())
        db.put(record.key(), nyTilstand)
        logger.info("Lagret tilstand: '$nyTilstand'")
    }

    override fun close() {
        super.close()
        tilstandsDb?.close()
        tilstandsDb = null
        context = null
    }
}
