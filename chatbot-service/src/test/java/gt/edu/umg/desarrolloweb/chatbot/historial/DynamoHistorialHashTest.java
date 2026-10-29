package gt.edu.umg.desarrolloweb.chatbot.historial;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DynamoHistorialHashTest {

    @Test
    void laIpNoSeGuardaTalCual() {
        String hash = DynamoHistorialConversaciones.hash("190.56.10.20");
        assertThat(hash).hasSize(16).doesNotContain("190.56");
        // Misma IP → mismo hash: permite contar usuarios distintos
        assertThat(DynamoHistorialConversaciones.hash("190.56.10.20")).isEqualTo(hash);
        assertThat(DynamoHistorialConversaciones.hash("190.56.10.21")).isNotEqualTo(hash);
    }

    @Test
    void ipVaciaNoFalla() {
        assertThat(DynamoHistorialConversaciones.hash(null)).isEqualTo("desconocida");
        assertThat(DynamoHistorialConversaciones.hash(" ")).isEqualTo("desconocida");
    }
}
