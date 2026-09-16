package simulateur;

import destinations.Destination;
import destinations.DestinationFinale;
import information.Information;
import sources.Source;
import sources.SourceAleatoire;
import sources.SourceFixe;
import transmetteurs.Emetteur;
import transmetteurs.Recepteur;
import transmetteurs.Transmetteur;
import transmetteurs.TransmetteurParfait;
import visualisations.SondeAnalogique;
import visualisations.SondeLogique;


/** La classe Simulateur permet de construire et simuler une chaîne de
 * transmission composée d'une Source, d'un nombre variable de
 * Transmetteur(s) et d'une Destination.
 * @author cousin
 * @author prou
 * @author Paul
 * @author Yann
 * @author Damien
 * @author Enzo
 */
public class Simulateur {

    /** indique si le Simulateur utilise des sondes d'affichage */
    private boolean affichage = false;

    /** indique si le Simulateur utilise un message généré de manière aléatoire (message imposé sinon) */
    private boolean messageAleatoire = true;

    /** indique si le Simulateur utilise un germe pour initialiser les générateurs aléatoires */
    private boolean aleatoireAvecGerme = false;

    /** la valeur de la semence utilisée pour les générateurs aléatoires */
    private Integer seed = null; // pas de semence par défaut

    /** la longueur du message aléatoire à transmettre si un message n'est pas imposé */
    private int nbBitsMess = 100;

    /** la chaîne de caractères correspondant à m dans l'argument -mess m */
    private String messageString = "100";

    // --- Paramètres de transmission analogique (TP2) ---

    /** indique si la simulation doit utiliser une chaîne analogique */
    private boolean transmissionAnalogique = false;

    /** forme d'onde pour la transmission analogique ("RZ", "NRZ", "NRZT") */
    private String formeOnde = "RZ";

    /** nombre d'échantillons par bit */
    private int nbEch = 30;

    /** amplitude minimale du signal */
    private float amplMin = 0.0f;

    /** amplitude maximale du signal */
    private float amplMax = 1.0f;

    // --- Composants de la chaîne ---

    /** le composant Source de la chaîne de transmission */
    private Source<Boolean> source = null;

    /** le composant Transmetteur parfait logique de la chaîne de transmission (TP1) */
    private Transmetteur<Boolean, Boolean> transmetteurLogique = null;

    /** le composant Émetteur (TP2) */
    private Emetteur emetteur = null;

    /** le composant Transmetteur analogique parfait (TP2) */
    private Transmetteur<Float, Float> transmetteurAnalogique = null;

    /** le composant Récepteur (TP2) */
    private Recepteur recepteur = null;

    /** le composant Destination de la chaîne de transmission */
    private Destination<Boolean> destination = null;

    /**
     * Le constructeur de Simulateur construit une chaîne de transmission
     * composée d'une Source, d'une Destination et de Transmetteurs.
     *
     * @param args le tableau des différents arguments.
     * @throws ArgumentsException si un des arguments est incorrect
     */
    public Simulateur(String[] args) throws ArgumentsException {
        // analyser et récupérer les arguments
        analyseArguments(args);

        // Instanciation de la source
        if (messageAleatoire) {
            if (aleatoireAvecGerme) {
                source = new SourceAleatoire(nbBitsMess, seed);
            } else {
                source = new SourceAleatoire(nbBitsMess);
            }
        } else {
            source = new SourceFixe(messageString);
        }

        // Instanciation de la destination commune
        destination = new DestinationFinale();

        final int nbPixels = 30;

        if (transmissionAnalogique) {
            // --- Chaîne analogique (TP2) ---
            emetteur = new Emetteur(formeOnde, nbEch, amplMin, amplMax);
            transmetteurAnalogique = new TransmetteurParfait<Float>();
            recepteur = new Recepteur(formeOnde, nbEch, amplMin, amplMax);

            // Connexion des sondes si demandé (-s)
            if (affichage) {
                source.connecter(new SondeLogique("Source", nbPixels));
                emetteur.connecter(new SondeAnalogique("Emetteur"));
                transmetteurAnalogique.connecter(new SondeAnalogique("Transmetteur"));
                recepteur.connecter(new SondeLogique("Recepteur", nbPixels));
            }

            // Connexion de la chaîne analogique
            source.connecter(emetteur);
            emetteur.connecter(transmetteurAnalogique);
            transmetteurAnalogique.connecter(recepteur);
            recepteur.connecter(destination);

        } else {
            // --- Chaîne logique (TP1) ---
            transmetteurLogique = new TransmetteurParfait<Boolean>();

            // Connexion des sondes logiques si demandé (-s)
            if (affichage) {
                source.connecter(new SondeLogique("Source", nbPixels));
                transmetteurLogique.connecter(new SondeLogique("Transmetteur", nbPixels));
            }

            // Connexion de la chaîne logique
            source.connecter(transmetteurLogique);
            transmetteurLogique.connecter(destination);
        }
    }

    /**
     * Extrait d'un tableau de chaînes de caractères les différentes options de la simulation.
     *
     * @param args le tableau des différents arguments.
     * @throws ArgumentsException si un des arguments est incorrect.
     */
    private void analyseArguments(String[] args) throws ArgumentsException {
        for (int i = 0; i < args.length; i++) {

            if (args[i].matches("-s")) {
                affichage = true;
            } else if (args[i].matches("-seed")) {
                aleatoireAvecGerme = true;
                i++;
                if (i >= args.length) {
                    throw new ArgumentsException("Valeur manquante pour le parametre -seed");
                }
                try {
                    seed = Integer.valueOf(args[i]);
                } catch (Exception e) {
                    throw new ArgumentsException("Valeur du parametre -seed invalide :" + args[i]);
                }
            } else if (args[i].matches("-mess")) {
                i++;
                if (i >= args.length) {
                    throw new ArgumentsException("Valeur manquante pour le parametre -mess");
                }
                messageString = args[i];
                if (args[i].matches("[0,1]{7,}")) {
                    messageAleatoire = false;
                    nbBitsMess = args[i].length();
                } else if (args[i].matches("[0-9]{1,6}")) {
                    messageAleatoire = true;
                    nbBitsMess = Integer.valueOf(args[i]);
                    if (nbBitsMess < 1) {
                        throw new ArgumentsException("Valeur du parametre -mess invalide : " + nbBitsMess);
                    }
                } else {
                    throw new ArgumentsException("Valeur du parametre -mess invalide : " + args[i]);
                }
            } else if (args[i].matches("-form")) {
                transmissionAnalogique = true;
                i++;
                if (i >= args.length) {
                    throw new ArgumentsException("Valeur manquante pour le parametre -form");
                }
                String f = args[i].toUpperCase();
                if (f.equals("NRZ") || f.equals("NRZT") || f.equals("RZ")) {
                    formeOnde = f;
                } else {
                    throw new ArgumentsException("Valeur du parametre -form invalide : " + args[i]);
                }
            } else if (args[i].matches("-nbEch")) {
                transmissionAnalogique = true;
                i++;
                if (i >= args.length) {
                    throw new ArgumentsException("Valeur manquante pour le parametre -nbEch");
                }
                try {
                    nbEch = Integer.parseInt(args[i]);
                    if (nbEch <= 0) {
                        throw new ArgumentsException("Valeur du parametre -nbEch invalide : " + args[i]);
                    }
                } catch (NumberFormatException e) {
                    throw new ArgumentsException("Valeur du parametre -nbEch invalide : " + args[i]);
                }
            } else if (args[i].matches("-ampl")) {
                transmissionAnalogique = true;
                if (i + 2 >= args.length) {
                    throw new ArgumentsException("Parametres manquants pour l'option -ampl (attendu : min max)");
                }
                i++;
                try {
                    amplMin = Float.parseFloat(args[i]);
                } catch (NumberFormatException e) {
                    throw new ArgumentsException("Valeur min du parametre -ampl invalide : " + args[i]);
                }
                i++;
                try {
                    amplMax = Float.parseFloat(args[i]);
                } catch (NumberFormatException e) {
                    throw new ArgumentsException("Valeur max du parametre -ampl invalide : " + args[i]);
                }
                if (amplMin >= amplMax) {
                    throw new ArgumentsException("Valeurs du parametre -ampl invalides : min doit etre strictement inferieur a max (" + amplMin + " >= " + amplMax + ")");
                }
            } else {
                throw new ArgumentsException("Option invalide :" + args[i]);
            }
        }

        // Vérification des contraintes sur les amplitudes (TP2 sujet slide 8)
        if (transmissionAnalogique) {
            if ("RZ".equalsIgnoreCase(formeOnde)) {
                // Contraintes RZ : Amax >= 0, Amin = 0, Amin < Amax
                if (amplMin != 0.0f) {
                    throw new ArgumentsException("Pour la forme RZ, l'amplitude min doit valoir 0.0 : " + amplMin);
                }
                if (amplMax < 0.0f) {
                    throw new ArgumentsException("Pour la forme RZ, l'amplitude max doit etre >= 0 : " + amplMax);
                }
            } else {
                // Contraintes NRZ et NRZT : Amax >= 0, Amin <= 0, Amin < Amax
                if (amplMin > 0.0f) {
                    throw new ArgumentsException("Pour la forme " + formeOnde + ", l'amplitude min doit etre <= 0 : " + amplMin);
                }
                if (amplMax < 0.0f) {
                    throw new ArgumentsException("Pour la forme " + formeOnde + ", l'amplitude max doit etre >= 0 : " + amplMax);
                }
            }
        }
    }

    /**
     * Effectue un envoi de message par la source de la chaîne de transmission.
     *
     * @throws Exception si un problème survient lors de l'exécution
     */
    public void execute() throws Exception {
        source.emettre();
    }

    /**
     * Calcule le taux d'erreur binaire (TEB) en comparant les bits émis
     * avec les bits reçus par la destination.
     *
     * @return la valeur du TEB (entre 0.0f et 1.0f).
     */
    public float calculTauxErreurBinaire() {
        Information<Boolean> infoEmise = source.getInformationEmise();
        Information<Boolean> infoRecue = destination.getInformationRecue();

        if (infoEmise == null || infoRecue == null || infoEmise.nbElements() == 0) {
            return 0.0f;
        }

        int nbErreurs = 0;
        int nbElementsEmis = infoEmise.nbElements();
        int nbElementsRecus = infoRecue.nbElements();
        int nbElementsCommuns = Math.min(nbElementsEmis, nbElementsRecus);

        for (int i = 0; i < nbElementsCommuns; i++) {
            if (!infoEmise.iemeElement(i).equals(infoRecue.iemeElement(i))) {
                nbErreurs++;
            }
        }

        nbErreurs += Math.abs(nbElementsEmis - nbElementsRecus);

        return (float) nbErreurs / (float) nbElementsEmis;
    }

    /**
     * Renvoie la source utilisée par le simulateur.
     * @return la source
     */
    public Source<Boolean> getSource() {
        return this.source;
    }

    /**
     * Renvoie la destination utilisée par le simulateur.
     * @return la destination
     */
    public Destination<Boolean> getDestination() {
        return this.destination;
    }

    /**
     * Renvoie l'émetteur de la chaîne analogique.
     * @return l'émetteur (ou null si transmission logique)
     */
    public Emetteur getEmetteur() {
        return this.emetteur;
    }

    /**
     * Renvoie le récepteur de la chaîne analogique.
     * @return le récepteur (ou null si transmission logique)
     */
    public Recepteur getRecepteur() {
        return this.recepteur;
    }

    /**
     * Renvoie le transmetteur analogique parfait.
     * @return le transmetteur analogique (ou null si transmission logique)
     */
    public Transmetteur<Float, Float> getTransmetteurAnalogique() {
        return this.transmetteurAnalogique;
    }

    /**
     * Renvoie le transmetteur logique.
     * @return le transmetteur logique (ou null si transmission analogique)
     */
    public Transmetteur<Boolean, Boolean> getTransmetteurLogique() {
        return this.transmetteurLogique;
    }

    /**
     * Indique si la chaîne est analogique.
     * @return true si analogique, false si logique
     */
    public boolean isTransmissionAnalogique() {
        return this.transmissionAnalogique;
    }

    /**
     * Fonction principale : instancie le Simulateur et affiche le TEB.
     *
     * @param args arguments de simulation.
     */
    public static void main(String[] args) {
        Simulateur simulateur = null;

        try {
            simulateur = new Simulateur(args);
        } catch (Exception e) {
            System.out.println(e);
            System.exit(-1);
        }

        try {
            simulateur.execute();
            String s = "java  Simulateur  ";
            for (int i = 0; i < args.length; i++) {
                s += args[i] + "  ";
            }
            System.out.println(s + "  =>   TEB : " + simulateur.calculTauxErreurBinaire());
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            System.exit(-2);
        }
    }
}
