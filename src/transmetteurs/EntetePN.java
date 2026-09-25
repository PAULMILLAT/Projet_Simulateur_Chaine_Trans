package transmetteurs;

import java.util.Random;

/**
 * Sequence d'en-tete de sondage, partagee entre l'emission
 * ({@link InsertionEntete}) et la reception ({@code RecepteurSonde}), et
 * utilisee pour l'estimation du canal (delais/amplitudes des trajets
 * indirects) par correlation.
 *
 * Il s'agit d'une sequence pseudo-aleatoire fixe (graine figee dans le
 * code, donc identique aux deux extremites de la chaine sans avoir besoin
 * d'etre transmise ni configuree). Ce n'est pas une vraie sequence a
 * longueur maximale (sequence m / LFSR), dont l'autocorrelation serait
 * quasi parfaite : c'est une simplification volontaire pour ce TP, avec
 * une autocorrelation seulement "raisonnablement bonne". A ameliorer si
 * l'estimation du canal se revele trop sensible aux faux pics.
 *
 * @author Damien
 */
public final class EntetePN {

    /** Longueur (en bits) de la sequence d'en-tete */
    public static final int LONGUEUR = 31;

    /** La sequence d'en-tete elle-meme, fixe et partagee emission/reception */
    public static final boolean[] SEQUENCE = genererSequence(LONGUEUR);

    private EntetePN() {
        // classe utilitaire, non instanciable
    }

    private static boolean[] genererSequence(int longueur) {
        boolean[] seq = new boolean[longueur];
        // Graine arbitraire mais figee : garantit que l'emetteur et le
        // recepteur generent exactement la meme sequence, independamment
        // de la graine (-seed) utilisee par ailleurs pour la simulation.
        Random r = new Random(20261003L);
        for (int i = 0; i < longueur; i++) {
            seq[i] = r.nextBoolean();
        }
        return seq;
    }
}
