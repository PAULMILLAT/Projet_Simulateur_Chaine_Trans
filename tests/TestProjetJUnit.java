import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Classe de test appellant les tests Java de manière compatible avec JUnit et Emma.
 *
 * @author Yann
 */
public class TestProjetJUnit {

    @Test
    public void testTP1() {
        assertTrue(TestTP1.executerTests(), "Les tests du TP1 doivent tous réussir.");
    }

    @Test
    public void testTP2() {
        assertTrue(TestTP2.executerTests(), "Les tests du TP2 doivent tous réussir.");
    }
}
