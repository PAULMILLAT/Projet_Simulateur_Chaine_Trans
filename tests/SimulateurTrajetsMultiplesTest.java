/* package simulateur; */

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.awt.GraphicsEnvironment;

import simulateur.ArgumentsException;
import simulateur.Simulateur;
import transmetteurs.TransmetteurAnalogiqueTrajetsMultiples;

/**
 * Tests de la prise en compte des options -ti/-sondage/-oeil par
 * {@link Simulateur}, et test de non-régression pour le bug de
 * faux-positif de détection d'en-tête dans l'ancien Recepteur (cf.
 * échanges avec Damien : le Recepteur de base déclenchait à tort son
 * mode "sondage" sur un canal parfait sans -sondage, pour des messages
 * RZ suffisamment longs).
 */
public class SimulateurTrajetsMultiplesTest {

    @Test
    void argumentTiRejettePlusDeCinqTrajets() {
        String[] args = {
            "-mess", "1000", "-form", "RZ", "-nbEch", "30",
            "-ti", "10", "0.1", "20", "0.1", "30", "0.1", "40", "0.1", "50", "0.1", "60", "0.1"
        };
        assertThrows(ArgumentsException.class, () -> new Simulateur(args));
    }

    @Test
    void argumentTiRejetteCoupleIncomplet() {
        String[] args = {
            "-mess", "1000", "-form", "RZ", "-nbEch", "30",
            "-ti", "10" // dt sans ar
        };
        assertThrows(ArgumentsException.class, () -> new Simulateur(args));
    }

    @Test
    void argumentTiValideEstCorrectementParse() throws Exception {
        String[] args = {
            "-mess", "1000", "-form", "RZ", "-nbEch", "30",
            "-ti", "60", "0.5", "90", "-0.25"
        };
        Simulateur simulateur = new Simulateur(args);
        simulateur.execute();

        assertTrue(simulateur.isTrajetsMultiples());
        assertEquals(2, simulateur.getNbTrajets());

        TransmetteurAnalogiqueTrajetsMultiples canal =
            (TransmetteurAnalogiqueTrajetsMultiples) simulateur.getTransmetteurAnalogique();
        assertArrayEquals(new int[]{60, 90}, canal.getDecalages());
        assertArrayEquals(new float[]{0.5f, -0.25f}, canal.getAmplitudesRelatives(), 1e-6f);
    }

    @Test
    void chaineCompleteAvecSondageDonneUnTebNulSurUnSeulTrajet() throws Exception {
        String[] args = {
            "-mess", "5000", "-form", "RZ", "-nbEch", "30",
            "-ti", "60", "0.5", "-sondage"
        };
        Simulateur simulateur = new Simulateur(args);
        simulateur.execute();

        assertEquals(0.0f, simulateur.calculTauxErreurBinaire(), 1e-6f);
    }

    @Test
    void chaineCompleteAvecSondageDonneUnTebFaibleSurDeuxTrajets() throws Exception {
        String[] args = {
            "-mess", "5000", "-form", "RZ", "-nbEch", "30",
            "-ti", "90", "0.3", "60", "-0.2", "-sondage"
        };
        Simulateur simulateur = new Simulateur(args);
        simulateur.execute();

        assertTrue(simulateur.calculTauxErreurBinaire() < 0.01f);
    }

    /**
     * Test de non-régression : sur un canal parfait, SANS -ti et SANS
     * -sondage, le TEB doit toujours être nul, quelle que soit la
     * graine. Ce test aurait échoué avec l'ancien Recepteur.java (le
     * faux-positif de détection d'en-tête produisait un TEB ~0.5 pour
     * environ 60% des graines en RZ).
     */
    @Test
    void chaineDeBaseSansTiSansSondageDonneToujoursUnTebNulEnRZ() throws Exception {
        for (int seed = 1; seed <= 10; seed++) {
            String[] args = {
                "-mess", "5000", "-seed", String.valueOf(seed),
                "-form", "RZ", "-nbEch", "30"
            };
            Simulateur simulateur = new Simulateur(args);
            simulateur.execute();

            assertEquals(0.0f, simulateur.calculTauxErreurBinaire(), 1e-6f,
                "TEB non nul sur canal parfait (seed=" + seed + ") : "
                + "possible regression du faux-positif de detection d'entete");
        }
    }

    @Test
    void optionOeilNaAucunEffetSurLeTeb() throws Exception {
        // -oeil ouvre une fenetre graphique (VueOeil) : ignore ce test sur
        // une machine/CI sans affichage plutot que de le faire echouer.
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        String[] args = {
            "-mess", "2000", "-seed", "1", "-form", "RZ", "-nbEch", "30",
            "-ti", "60", "0.4", "-sondage", "-oeil"
        };
        Simulateur simulateur = new Simulateur(args);
        simulateur.execute();

        assertTrue(simulateur.isDiagrammeOeil());
        assertTrue(simulateur.calculTauxErreurBinaire() < 0.01f);
    }
}
