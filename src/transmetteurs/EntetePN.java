package transmetteurs;

/**
 * Sequence d'en-tete de sondage, partagee entre l'emission
 * ({@link InsertionEntete}) et la reception ({@code RecepteurSonde}), et
 * utilisee pour l'estimation du canal (delais/amplitudes des trajets
 * indirects) par correlation.
 *
 * Il s'agit d'une vraie sequence a longueur maximale (sequence m), issue
 * d'un LFSR de Fibonacci a 6 bits avec taps aux positions 0 et 5
 * (0-indexees), periode 63 = 2^6 - 1. Sequence figee en dur (calculee et
 * verifiee hors ligne par calcul, pas de LFSR regenere a l'execution -
 * cf. l'historique du projet : une premiere implementation avait un bug
 * de taps donnant une periode beaucoup plus courte que prevue, non
 * detecte avant test).
 *
 * Autocorrelation circulaire verifiee (sequence bipolaire +-1) : 63 a
 * decalage nul, exactement -1 a tous les autres decalages (quasi ideale).
 * Plus longue serie de bits identiques consecutifs : 6.
 *
 * Portee sur 63 bits plutot que 31 (premiere version) pour reduire le
 * plancher de bruit statistique de l'estimation de canal par correlation
 * (ce plancher decroit en 1/sqrt(N) avec la longueur N de l'en-tete) :
 * des trajets fantomes etaient encore observes avec l'en-tete a 31 bits
 * sur des messages courts.
 *
 * Fixe et codee en dur : identique aux deux extremites de la chaine sans
 * avoir besoin d'etre transmise ni configuree.
 *
 * @author Damien
 */
public final class EntetePN {

    /** Longueur (en bits) de la sequence d'en-tete */
    public static final int LONGUEUR = 63;

    /** La sequence d'en-tete elle-meme, fixe et partagee emission/reception */
    public static final boolean[] SEQUENCE = {
        false, false, false, false, false, true, true, true, true, true,
        true, false, true, false, true, false, true, true, false, false,
        true, true, false, true, true, true, false, true, true, false,
        true, false, false, true, false, false, true, true, true, false,
        false, false, true, false, true, true, true, true, false, false,
        true, false, true, false, false, false, true, true, false, false,
        false, false, true
    };

    private EntetePN() {
        // classe utilitaire, non instanciable
    }
}
