package transmetteurs;

import java.util.ArrayList;
import java.util.List;

import destinations.DestinationInterface;
import information.Information;
import information.InformationNonConformeException;

/**
 * Récepteur adapté au canal à trajets indirects, utilisant le sondage par
 * en-tête connue ({@link EntetePN}) : il estime le canal (décalages et
 * amplitudes relatives des trajets indirects, 5 au maximum) par corrélation
 * entre le signal reçu et la forme d'onde de référence de l'en-tête, égalise
 * le signal reçu par inversion causale du canal ainsi estimé, démodule par
 * décision de seuil (même logique que {@link Recepteur}), puis retire
 * l'en-tête décodée avant de transmettre le message utile.
 *
 * Aucun paramètre du canal (décalages, amplitudes) n'est connu à l'avance :
 * seule la séquence de bits de l'en-tête l'est, de façon identique à
 * l'émission (cf. {@link InsertionEntete}).
 *
 * Limites assumées pour ce TP (à améliorer si besoin) :
 * <ul>
 *   <li>les décalages recherchés sont bornés à dt &ge; nbEch : en dessous
 *       d'une période bit, un décalage est indiscernable de la forme du
 *       pulse lui-même avec des formes d'onde NRZ/RZ/NRZT (plateaux de
 *       nbEch échantillons) — un vrai trajet indirect plus rapproché que
 *       cela ne sera donc pas détecté ;</li>
 *   <li>l'estimation par corrélation traite chaque décalage candidat
 *       indépendamment (un matched filter par décalage), ce qui reste une
 *       approximation lorsque plusieurs trajets indirects sont proches ou
 *       de forte amplitude, plutôt qu'une estimation conjointe rigoureuse ;</li>
 *   <li>la plage de décalages recherchée est bornée à la moitié de la
 *       longueur de l'en-tête (recouvrement "en-tête vs en-tête" d'au
 *       moins 50%, cf. javadoc de {@link #estimerCanal}) ;</li>
 *   <li>le seuil de détection d'un pic ({@link #SEUIL_DETECTION}) est une
 *       valeur empirique, à ajuster si des trajets faibles ne sont pas
 *       détectés ou si du bruit est détecté à tort comme un trajet ;</li>
 *   <li>l'égalisation n'est stable que si chaque trajet indirect est
 *       strictement plus faible que le trajet direct (|ar| &lt; 1, cf.
 *       {@link #egaliser}) : au-delà, elle est désactivée entièrement
 *       plutôt que de dégrader le signal (canal "non minimum de phase",
 *       hors de portée de cette méthode d'inversion causale simple).</li>
 * </ul>
 *
 * @author Damien
 */
public class RecepteurSonde extends Transmetteur<Float, Boolean> {

    /** Forme d'onde utilisée ("NRZ", "NRZT" ou "RZ") */
    private String formeOnde;

    /** Nombre d'échantillons par bit */
    private int nbEch;

    /** Amplitude minimale */
    private float amplMin;

    /** Amplitude maximale */
    private float amplMax;

    /** Nombre maximal de trajets indirects recherchés (cohérent avec la limite de -ti) */
    private static final int MAX_TRAJETS = 5;

    /** Seuil (en gain relatif au trajet direct) au-dessus duquel un pic de corrélation est retenu */
    private static final float SEUIL_DETECTION = 0.15f;

    /**
     * Seuil de stabilité de l'égaliseur causal : au-delà, l'inversion par
     * récurrence (IIR) diverge au lieu de corriger le signal (cf. javadoc
     * de {@link #egaliser}). Si un trajet estimé a une amplitude relative
     * dont la valeur absolue dépasse ce seuil, l'égalisation est
     * désactivée entièrement pour ne pas dégrader le signal davantage
     * qu'un récepteur non adapté.
     */
    private static final float SEUIL_STABILITE = 0.95f;

    /** Décalages (en échantillons) des trajets indirects détectés lors de la dernière réception */
    private int[] decalagesEstimes = new int[0];

    /** Amplitudes relatives des trajets indirects détectés lors de la dernière réception */
    private float[] amplitudesEstimees = new float[0];

    /**
     * Constructeur par défaut : forme RZ, 30 échantillons par bit,
     * amplMin = 0.0f, amplMax = 1.0f.
     */
    public RecepteurSonde() {
        this("RZ", 30, 0.0f, 1.0f);
    }

    /**
     * Constructeur paramétré du récepteur avec sondage de canal.
     *
     * @param formeOnde la forme d'onde ("NRZ", "NRZT" ou "RZ")
     * @param nbEch le nombre d'échantillons par bit
     * @param amplMin l'amplitude minimale
     * @param amplMax l'amplitude maximale
     */
    public RecepteurSonde(String formeOnde, int nbEch, float amplMin, float amplMax) {
        super();
        this.formeOnde = formeOnde;
        this.nbEch = nbEch;
        this.amplMin = amplMin;
        this.amplMax = amplMax;
    }

    /**
     * Reçoit le signal analogique (en-tête + message), estime le canal par
     * corrélation avec la référence de l'en-tête, égalise, démodule, puis
     * retire l'en-tête décodée avant d'émettre le message utile.
     *
     * @param information le signal analogique reçu (en-tête + message, éventuellement bruité et/ou multitrajet)
     * @throws InformationNonConformeException si l'information est nulle
     */
    @Override
    public void recevoir(Information<Float> information) throws InformationNonConformeException {
        if (information == null) {
            throw new InformationNonConformeException("L'information recue est nulle.");
        }
        this.informationRecue = information;
        this.informationEmise = new Information<Boolean>();

        int nbEchantillonsTotal = information.nbElements();
        if (nbEchantillonsTotal == 0 || this.nbEch <= 0) {
            this.decalagesEstimes = new int[0];
            this.amplitudesEstimees = new float[0];
            this.emettre();
            return;
        }

        float[] recu = new float[nbEchantillonsTotal];
        int idx = 0;
        for (Float f : information) {
            recu[idx++] = f;
        }

        // 1. Reconstruction de la forme d'onde de reference attendue pour
        // l'en-tete connu (via un Emetteur interne, pour garantir une
        // correspondance exacte avec la modulation reellement utilisee)
        float[] reference = genererFormeOndeReference(EntetePN.SEQUENCE);

        // 2. Estimation du canal (decalages/amplitudes des trajets indirects)
        // par correlation entre le signal recu et cette reference
        estimerCanal(recu, reference);

        // --- DEBUG TEMPORAIRE : a retirer une fois le sondage valide ---
        System.out.println("[RecepteurSonde] " + this.decalagesEstimes.length + " trajet(s) detecte(s) :");
        for (int i = 0; i < this.decalagesEstimes.length; i++) {
            System.out.println("    dt=" + this.decalagesEstimes[i] + "  ar=" + this.amplitudesEstimees[i]);
        }
        // --- FIN DEBUG ---

        // 3. Egalisation : inversion causale du canal estime, appliquee a
        // l'ensemble du signal recu (en-tete + message)
        float[] directEstime = egaliser(recu);

        // 4. Retrait du padding du aux trajets retardes : la longueur utile
        // du signal egalise correspond a la longueur reellement emise
        int decalMax = 0;
        for (int d : this.decalagesEstimes) {
            if (d > decalMax) {
                decalMax = d;
            }
        }
        int longueurUtile = Math.max(0, directEstime.length - decalMax);

        // 5. Decision par seuil, bit par bit (meme logique que Recepteur)
        int nbBits = longueurUtile / this.nbEch;
        float seuil = (this.amplMin + this.amplMax) / 2.0f;
        int tiers1 = this.nbEch / 3;
        int tiers2 = 2 * this.nbEch / 3;

        boolean[] tousLesBits = new boolean[nbBits];
        for (int k = 0; k < nbBits; k++) {
            int debutBit = k * this.nbEch;
            float somme = 0.0f;
            int nbEchPris = 0;

            if ("NRZ".equalsIgnoreCase(this.formeOnde)) {
                for (int i = 0; i < this.nbEch; i++) {
                    somme += directEstime[debutBit + i];
                    nbEchPris++;
                }
            } else {
                for (int i = tiers1; i < tiers2; i++) {
                    somme += directEstime[debutBit + i];
                    nbEchPris++;
                }
            }

            float moyenne = (nbEchPris > 0) ? (somme / nbEchPris) : 0.0f;
            tousLesBits[k] = moyenne > seuil;
        }

        // 6. Retrait de l'en-tete decodee (longueur fixe et connue des deux
        // extremites) : seul le message utile est transmis a la destination
        int debutMessage = Math.min(EntetePN.LONGUEUR, nbBits);
        for (int k = debutMessage; k < nbBits; k++) {
            this.informationEmise.add(tousLesBits[k]);
        }

        this.emettre();
    }

    /**
     * Régénère la forme d'onde analogique de référence (sans bruit, sans
     * trajet indirect) attendue pour une séquence de bits donnée, en
     * réutilisant un {@link Emetteur} interne configuré à l'identique de
     * celui de la chaîne (mêmes formeOnde/nbEch/amplMin/amplMax).
     *
     * @param bits la séquence de bits à moduler (ici, l'en-tête de sondage)
     * @return le signal analogique de référence correspondant
     */
    private float[] genererFormeOndeReference(boolean[] bits) {
        Information<Boolean> infoBits = new Information<Boolean>();
        for (boolean b : bits) {
            infoBits.add(b);
        }
        Emetteur emetteurReference = new Emetteur(this.formeOnde, this.nbEch, this.amplMin, this.amplMax);
        try {
            emetteurReference.recevoir(infoBits);
        } catch (InformationNonConformeException e) {
            // Ne peut pas se produire : infoBits n'est jamais nulle ici
        }
        Information<Float> sortie = emetteurReference.getInformationEmise();
        float[] resultat = new float[sortie.nbElements()];
        int i = 0;
        for (Float f : sortie) {
            resultat[i++] = f;
        }
        return resultat;
    }

    /**
     * Estime les décalages et amplitudes relatives des trajets indirects
     * par corrélation (matched filter par décalage candidat) entre le
     * signal reçu et la référence de l'en-tête. Met à jour
     * {@link #decalagesEstimes} et {@link #amplitudesEstimees}.
     *
     * Deux précautions nécessaires avec des formes d'onde "larges"
     * (plateau de nbEch échantillons par bit, cas de NRZ/RZ/NRZT) :
     * <ul>
     *   <li>les décalages dt &lt; nbEch sont exclus de la recherche : à
     *       cette échelle, un décalage plus petit qu'une période bit est
     *       indiscernable de l'autocorrélation propre du pulse lui-même
     *       (des échantillons consécutifs d'un même plateau sont quasi
     *       identiques), ce qui produirait un "trajet" fantôme à très
     *       fort gain proche de dt=0 ;</li>
     *   <li>la sélection des pics est gloutonne (type matching pursuit) :
     *       on prend le décalage de plus fort gain restant, on supprime
     *       tout son voisinage avant de chercher le suivant, pour éviter
     *       de retenir plusieurs décalages quasi identiques correspondant
     *       au même trajet réel.</li>
     * </ul>
     *
     * Point critique : les amplitudes NRZ/RZ/NRZT (par défaut 0.0f/1.0f)
     * ne sont pas centrées sur zéro, donc chaque bit porte une composante
     * continue (DC) importante. Un simple produit scalaire recu.reference
     * est alors dominé par cette DC (un terme quasi constant, indépendant
     * du décalage testé), ce qui écrase la variation utile liée au vrai
     * décalage et empêche toute détection fiable. On retire donc la
     * moyenne (locale pour la fenêtre reçue, globale pour la référence)
     * avant de corréler : le gain estimé à chaque décalage candidat est
     * alors la pente de régression linéaire entre les deux signaux
     * centrés, insensible à leur composante continue commune.
     *
     * @param recu le signal analogique complet reçu
     * @param reference la forme d'onde de référence de l'en-tête
     */

    private float[] estimerCanal(float[] recu, float[] reference) {
        int longueurRef = reference.length;
        float[] gainParDecalage = new float[recu.length];

        this.decalagesEstimes = new int[0];
        this.amplitudesEstimees = new float[0];

        if (longueurRef <= nbEch || recu.length < longueurRef) {
            return gainParDecalage;
        }

        /*
        * Le trajet direct est implicite dans le modèle du récepteur :
        *
        *     y[n] = x[n] + somme(ar[k] * x[n-dt[k]])
        *
        * On retire donc le trajet direct connu de l'en-tête.
        */
        float[] signalConnu = new float[longueurRef];

        for (int n = 0; n < longueurRef; n++) {
            signalConnu[n] = recu[n] - reference[n];
        }

        /*
        * On ne recherche que les retards laissant au moins 50 %
        * de l'en-tête disponible.
        *
        * Cela évite notamment de commencer à corréler avec le
        * contenu inconnu du message.
        */
        int dtMin = nbEch;
        int maxRecherche = longueurRef / 2;

        if (maxRecherche < dtMin) {
            return gainParDecalage;
        }

        List<Integer> decalagesTrouves = new ArrayList<>();

        /*
        * Matrice des signaux correspondant aux trajets candidats.
        *
        * Chaque colonne correspond à :
        *
        *     reference[n - dt]
        *
        * pour n >= dt.
        */
        for (int trajet = 0; trajet < MAX_TRAJETS; trajet++) {

            int meilleurDt = -1;
            double meilleureCorrelation = 0.0;

            /*
            * Calcul du résidu courant.
            *
            * Au premier passage :
            *     residu = signalConnu
            *
            * Ensuite :
            *     residu = signalConnu - somme(gain_i * trajet_i)
            */
            float[] residu = signalConnu.clone();

            if (!decalagesTrouves.isEmpty()) {
                float[] gains = resoudreGains(
                    signalConnu,
                    reference,
                    decalagesTrouves
                );

                for (int n = 0; n < longueurRef; n++) {
                    float estimation = 0.0f;

                    for (int k = 0; k < decalagesTrouves.size(); k++) {
                        int dt = decalagesTrouves.get(k);

                        if (n >= dt) {
                            estimation += gains[k] * reference[n - dt];
                        }
                    }

                    residu[n] -= estimation;
                }
            }

            /*
            * Recherche du prochain retard.
            *
            * On utilise uniquement la partie connue de l'en-tête :
            *
            *     n = dt ... longueurRef-1
            *
            * soit une longueur de :
            *
            *     longueurRef - dt
            */
            for (int dt = dtMin; dt <= maxRecherche; dt++) {

                boolean dejaPresent = false;
                for (int ancienDt : decalagesTrouves) {
                    if (ancienDt == dt) {
                        dejaPresent = true;
                        break;
                    }
                }

                if (dejaPresent) {
                    continue;
                }

                int longueurUtile = longueurRef - dt;

                if (longueurUtile <= 0) {
                    continue;
                }

                double sommeR = 0.0;
                double sommeX = 0.0;

                for (int n = 0; n < longueurUtile; n++) {
                    sommeR += residu[dt + n];
                    sommeX += reference[n];
                }

                double moyenneR = sommeR / longueurUtile;
                double moyenneX = sommeX / longueurUtile;

                double covariance = 0.0;
                double energie = 0.0;

                for (int n = 0; n < longueurUtile; n++) {
                    double r = residu[dt + n] - moyenneR;
                    double x = reference[n] - moyenneX;

                    covariance += r * x;
                    energie += x * x;
                }

                if (energie <= 0.0) {
                    continue;
                }

                /*
                * Corrélation normalisée.
                *
                * On normalise également par l'énergie du résidu
                * afin de comparer correctement les différents dt.
                */
                double energieResidu = 0.0;

                for (int n = 0; n < longueurUtile; n++) {
                    double r = residu[dt + n] - moyenneR;
                    energieResidu += r * r;
                }

                if (energieResidu <= 0.0) {
                    continue;
                }

                double correlation =
                    Math.abs(covariance)
                    / Math.sqrt(energie * energieResidu);

                if (correlation > meilleureCorrelation) {
                    meilleureCorrelation = correlation;
                    meilleurDt = dt;
                }
            }

            /*
            * Plus aucun candidat suffisamment corrélé.
            */
            if (meilleurDt < 0 ||
                meilleureCorrelation < 0.20) {
                break;
            }

            decalagesTrouves.add(meilleurDt);
        }

        /*
        * Maintenant que les positions sont connues, on estime
        * TOUS les gains simultanément par moindres carrés.
        *
        * C'est cette étape qui évite que deux trajets proches
        * (par exemple 180 et 196) se "volent" leur amplitude.
        */
        if (!decalagesTrouves.isEmpty()) {

            float[] gains = resoudreGains(
                signalConnu,
                reference,
                decalagesTrouves
            );

            this.decalagesEstimes =
                new int[decalagesTrouves.size()];

            this.amplitudesEstimees =
                new float[decalagesTrouves.size()];

            for (int i = 0; i < decalagesTrouves.size(); i++) {

                int dt = decalagesTrouves.get(i);
                float gain = gains[i];

                this.decalagesEstimes[i] = dt;
                this.amplitudesEstimees[i] = gain;

                if (dt >= 0 && dt < gainParDecalage.length) {
                    gainParDecalage[dt] = gain;
                }
            }
        }

        return gainParDecalage;
    }

    private float[] resoudreGains(
            float[] signal,
            float[] reference,
            List<Integer> decalages) {

        int nbTrajets = decalages.size();
        int longueurRef = reference.length;

        float[][] matrice = new float[nbTrajets][nbTrajets];
        float[] secondMembre = new float[nbTrajets];

        /*
        * Construction du système :
        *
        *     (X^T X) a = X^T y
        *
        * avec :
        *
        *     y[n] = somme(a_k * reference[n-dt_k])
        */
        for (int i = 0; i < nbTrajets; i++) {

            int dtI = decalages.get(i);

            for (int j = 0; j < nbTrajets; j++) {

                int dtJ = decalages.get(j);

                double somme = 0.0;

                for (int n = 0; n < longueurRef; n++) {

                    int indiceI = n - dtI;
                    int indiceJ = n - dtJ;

                    if (indiceI >= 0 && indiceJ >= 0) {
                        somme +=
                            reference[indiceI]
                            * reference[indiceJ];
                    }
                }

                matrice[i][j] = (float) somme;
            }

            double sommeY = 0.0;

            for (int n = 0; n < longueurRef; n++) {

                int indice = n - dtI;

                if (indice >= 0) {
                    sommeY +=
                        signal[n]
                        * reference[indice];
                }
            }

            secondMembre[i] = (float) sommeY;
        }

        /*
        * Résolution de (X^T X)a = X^T y
        * par élimination de Gauss avec pivot partiel.
        */
        for (int colonne = 0; colonne < nbTrajets; colonne++) {

            int pivot = colonne;
            float valeurPivot =
                Math.abs(matrice[pivot][colonne]);

            for (int ligne = colonne + 1;
                ligne < nbTrajets;
                ligne++) {

                float valeur =
                    Math.abs(matrice[ligne][colonne]);

                if (valeur > valeurPivot) {
                    valeurPivot = valeur;
                    pivot = ligne;
                }
            }

            if (valeurPivot < 1e-8f) {
                continue;
            }

            if (pivot != colonne) {

                float[] tmpLigne = matrice[colonne];
                matrice[colonne] = matrice[pivot];
                matrice[pivot] = tmpLigne;

                float tmp = secondMembre[colonne];
                secondMembre[colonne] = secondMembre[pivot];
                secondMembre[pivot] = tmp;
            }

            for (int ligne = colonne + 1;
                ligne < nbTrajets;
                ligne++) {

                float facteur =
                    matrice[ligne][colonne]
                    / matrice[colonne][colonne];

                for (int j = colonne;
                    j < nbTrajets;
                    j++) {

                    matrice[ligne][j] -=
                        facteur * matrice[colonne][j];
                }

                secondMembre[ligne] -=
                    facteur * secondMembre[colonne];
            }
        }

        float[] gains = new float[nbTrajets];

        for (int i = nbTrajets - 1; i >= 0; i--) {

            float somme = secondMembre[i];

            for (int j = i + 1;
                j < nbTrajets;
                j++) {

                somme -= matrice[i][j] * gains[j];
            }

            if (Math.abs(matrice[i][i]) > 1e-8f) {
                gains[i] = somme / matrice[i][i];
            } else {
                gains[i] = 0.0f;
            }
        }

        return gains;
    }

    /**
     * Egalise le signal reçu par inversion causale du canal estimé :
     * directEstime[n] = recu[n] - somme_k( amplitudesEstimees[k] * directEstime[n - decalagesEstimes[k]] ),
     * avec directEstime[m] = 0 pour m &lt; 0.
     *
     * Cette récurrence n'est stable que si chaque |amplitudesEstimees[k]|
     * &lt; 1 : elle correspond à un filtre à rétroaction (IIR) qui, à
     * chaque "tour de boucle" (tous les decalagesEstimes[k] échantillons),
     * multiplie l'erreur résiduelle par amplitudesEstimees[k]. Si un
     * trajet indirect est aussi fort ou plus fort que le trajet direct
     * (|ar| &ge; 1, canal "non minimum de phase"), cette inversion diverge
     * géométriquement au lieu de corriger le signal - dans ce cas,
     * l'égalisation est désactivée (le signal est renvoyé inchangé),
     * ce qui reste toujours préférable à une divergence qui détruirait
     * complètement la démodulation.
     *
     * @param recu le signal analogique complet reçu
     * @return une estimation du signal du trajet direct, débarrassé des échos estimés
     *         (ou le signal reçu inchangé si l'inversion serait instable)
     */
    private float[] egaliser(float[] recu) {
        for (float ar : this.amplitudesEstimees) {
            if (Math.abs(ar) >= SEUIL_STABILITE) {
                // --- DEBUG TEMPORAIRE ---
                System.out.println("[RecepteurSonde] egalisation DESACTIVEE (|ar|=" + ar + " >= " + SEUIL_STABILITE + ")");
                // --- FIN DEBUG ---
                return recu.clone();
            }
        }
        // --- DEBUG TEMPORAIRE ---
        System.out.println("[RecepteurSonde] egalisation appliquee normalement");
        // --- FIN DEBUG ---

        float[] directEstime = new float[recu.length];
        for (int n = 0; n < recu.length; n++) {
            float valeur = recu[n];
            for (int k = 0; k < this.decalagesEstimes.length; k++) {
                int m = n - this.decalagesEstimes[k];
                if (m >= 0) {
                    valeur -= this.amplitudesEstimees[k] * directEstime[m];
                }
            }
            directEstime[n] = valeur;
        }
        return directEstime;
    }

    /**
     * Émet le message démodulé (en-tête retirée) vers toutes les destinations connectées.
     *
     * @throws InformationNonConformeException si une anomalie survient
     */
    @Override
    public void emettre() throws InformationNonConformeException {
        for (DestinationInterface<Boolean> destination : this.destinationsConnectees) {
            destination.recevoir(this.informationEmise);
        }
    }

    // --- Accesseurs ---

    public String getFormeOnde() { return this.formeOnde; }

    public int getNbEch() { return this.nbEch; }

    public float getAmplMin() { return this.amplMin; }

    public float getAmplMax() { return this.amplMax; }

    /** @return les decalages (en echantillons) des trajets indirects detectes lors de la derniere reception */
    public int[] getDecalagesEstimes() { return this.decalagesEstimes; }

    /** @return les amplitudes relatives des trajets indirects detectes lors de la derniere reception */
    public float[] getAmplitudesEstimees() { return this.amplitudesEstimees; }
}
