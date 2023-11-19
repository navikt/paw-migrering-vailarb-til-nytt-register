package no.nav.paw.migrering.app

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.konfigurasjon.HendelseSortererKonfigurasjon
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Instant


fun KStream<Long, Hendelse>.sorter(
    konfigurasjon: HendelseSortererKonfigurasjon
): KStream<Long, Hendelse> {
    if (konfigurasjon.forsinkelse.isZero) return this
    val processorSupplier = { TilstandsLaster(konfigurasjon) }
    return process(processorSupplier, Named.`as`("sorter_hendelser"), konfigurasjon.tilstandsDbNavn)
}

class TilstandsLaster(
    private val konfigurasjon: HendelseSortererKonfigurasjon
) : Processor<Long, Hendelse, Long, Hendelse> {

    private val logger = LoggerFactory.getLogger(TilstandsLaster::class.java)
    private var tilstandsDb: KeyValueStore<Long, Tilstand>? = null
    private var context: ProcessorContext<Long, Hendelse>? = null

    override fun init(context: ProcessorContext<Long, Hendelse>?) {
        super.init(context)
        this.context = context
        tilstandsDb = context?.getStateStore(konfigurasjon.tilstandsDbNavn)
        context?.schedule(konfigurasjon.interval, konfigurasjon.punctuationType) {
            val now = Instant.ofEpochMilli(it)
            logger.info("Kjører timer sjekk: $now")
            (tilstandsDb?.all()?.asSequence() ?: emptySequence())
                .filter { kv -> kv?.value != null }
                .filter { kv ->
                    kv.value.sistEndret.isBefore(now.minus(konfigurasjon.forsinkelse))
                }
                .map { kv -> kv.key to kv.value.avsluttet + kv.value.startet + kv.value.situasjonMottatt }
                .onEach { (key, _) -> tilstandsDb?.delete(key) }
                .forEach { (key, hendelser) ->
                    hendelser
                        .sortedBy { hendelse -> hendelse.metadata.tidspunkt }
                        .forEach { hendelse ->
                            context.forward(
                                Record(
                                    key,
                                    hendelse,
                                    hendelse.metadata.tidspunkt.toEpochMilli()
                                )
                            )
                        }
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
        val tilstand: Tilstand? = db.get(record.key())
        val nyTilstand = tilstand.leggTilHendelse(record.value())
        db.put(record.key(), nyTilstand)
    }

    override fun close() {
        super.close()
        tilstandsDb?.close()
        tilstandsDb = null
        context = null
    }
}
