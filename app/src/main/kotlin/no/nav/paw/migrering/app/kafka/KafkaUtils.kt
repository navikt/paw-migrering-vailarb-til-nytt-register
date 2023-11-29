package no.nav.paw.migrering.app

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.migrering.app.konfigurasjon.KafkaKonfigurasjon
import no.nav.paw.migrering.app.konfigurasjon.medKeySerde
import no.nav.paw.migrering.app.konfigurasjon.medValueSerde
import no.nav.paw.migrering.app.konfigurasjon.properties
import no.nav.paw.migrering.app.serde.HendelseSerde
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serdes
import java.io.Closeable

fun hendelseProducer(kafkaKonfigurasjon: KafkaKonfigurasjon) = KafkaProducer<Long, Hendelse>(
    kafkaKonfigurasjon.properties.medKeySerde(
        Serdes.Long()
    ).medValueSerde(HendelseSerde())
)

fun <T1 : Closeable, T2 : Closeable, T3 : Closeable, R> use(t1: T1, t2: T2, t3: T3, f: (T1, T2, T3) -> R): R {
    t1.use {
        t2.use {
            t3.use {
                return f(t1, t2, t3)
            }
        }
    }
}
