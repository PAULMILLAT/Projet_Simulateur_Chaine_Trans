# Simulateur de chaîne de transmission - SIT213

Projet réalisé dans le cadre du module SIT213 en deuxième année de formation d'ingénieur par apprentissage (FIP) à IMT Atlantique.

## Présentation

Ce logiciel simule une chaîne de transmission numérique en Java. Il permet d'étudier et de mesurer les performances (via le Taux d'Erreur Binaire (TEB)) à travers différentes configurations :
1. Chaîne logique parfaite (transmission bit à bit).
2. Chaîne analogique en bande de base (formes d'onde RZ, NRZ, NRZT, échantillonnage, seuillage).
3. Canal de transmission bruité (bruit blanc gaussien additif paramétré par son Eb/N0).
4. Canal à trajets multiples (interférences entre symboles, décalages et atténuations, diagramme de l'œil, sondage de canal et égalisation).
5. Codage de canal (codage par répétition avec inversion, détection et correction d'erreurs).

## Utilisation

L'exécution du simulateur s'effectue exclusivement à l'aide du script `simulateur` situé à la racine du projet :

```bash
./simulateur [options]
```

### Options disponibles

| Option | Paramètres | Description | Valeur par défaut |
| :--- | :--- | :--- | :--- |
| `-mess` | `m` | Message à émettre :<br>- Si `m` est une suite de `0` et `1` d'au moins 7 caractères, c'est le message émis.<br>- Si `m` est un entier positif (au plus 6 chiffres), il indique le nombre de bits aléatoires à générer. | `100` (bits aléatoires) |
| `-s` |  | Active l'affichage des sondes graphiques pour visualiser les signaux à différentes étapes de la chaîne. | Pas d'affichage |
| `-seed` | `v` | Graine entière `v` pour initialiser les générateurs pseudo-aléatoires (message et bruit). Permet de rejouer une simulation à l'identique. | Aléatoire (pas de graine) |
| `-form` | `f` | Forme d'onde analogique : `RZ`, `NRZ` ou `NRZT`. Active la chaîne analogique. | `RZ` *(si chaîne analogique)* |
| `-nbEch` | `ne` | Nombre d'échantillons par bit (`ne` entier > 0). Active la chaîne analogique. | `30` |
| `-ampl` | `min max` | Amplitudes min et max du signal analogique (flottants, `min < max`). Active la chaîne analogique.<br>- Forme `RZ` : `min = 0.0` et `max >= 0.0`<br>- Formes `NRZ` / `NRZT` : `min <= 0.0` et `max >= 0.0` | `0.0 1.0` |
| `-snrpb` *(ou `-snr`)* | `snr` | Rapport signal sur bruit par bit ($E_b/N_0$) en dB (flottant). Active le canal analogique bruité (AWGN). | Canal parfait (sans bruit) |
| `-ti` | `dt1 ar1 [dt2 ar2 ...]` | Active le canal à trajets multiples (jusqu'à 5 trajets indirects). Chaque trajet est défini par un retard `dt` en échantillons (`dt >= 0`) et une amplitude relative `ar` (flottant) par rapport au trajet direct ($dt_0=0, ar_0=1$). | Trajet direct seul |
| `-oeil` | *(aucun)* | Active l'affichage du diagramme de l'œil du signal reçu. Active la chaîne analogique. | Pas d'affichage |
| `-sondage` | *(aucun)* | Active le sondage du canal par séquence d'en-tête (PN) et l'égaliseur en réception pour compenser les trajets indirects. Active la chaîne analogique. | Désactivé |
| `-codeur` | *(aucun)* | Active le codeur en émission et le décodeur en réception ($0 \to 010$, $1 \to 101$). Permet de détecter et corriger une erreur par triplet de bits. Compatible en logique comme en analogique. | Désactivé |

### Format de sortie

En fin d'exécution, le simulateur affiche sur la sortie standard la commande exécutée ainsi que le TEB calculé entre les bits émis et reçus :

```text
java  Simulateur  [arguments]  =>   TEB : <valeur>
```

`TEB : 0.0` indique une transmission sans aucune erreur binaire.

## Exemples d'utilisation

### 1. Transmission logique (TP1)
* Simulation par défaut (100 bits aléatoires) :
  ```bash
  ./simulateur
  ```
* Message fixe imposé :
  ```bash
  ./simulateur -mess 0110101100
  ```
* Message aléatoire de 10 000 bits reproductible :
  ```bash
  ./simulateur -mess 10000 -seed 42
  ```

### 2. Transmission analogique sans bruit (TP2)
* Modulation NRZ avec 30 échantillons par bit et amplitudes symétriques :
  ```bash
  ./simulateur -form NRZ -ampl -1.0 1.0 -nbEch 30
  ```
* Modulation NRZT avec sondes graphiques :
  ```bash
  ./simulateur -form NRZT -ampl -5.0 5.0 -s
  ```

### 3. Canal bruité (TP3)
* Transmission NRZ sous un rapport $E_b/N_0 = 8\text{ dB}$ :
  ```bash
  ./simulateur -mess 100000 -form NRZ -ampl -1.0 1.0 -snrpb 8
  ```
* Transmission bruitée avec visualisation des signaux :
  ```bash
  ./simulateur -form NRZ -snrpb 3 -s
  ```

### 4. Trajets multiples et égalisation (TP4)
* Canal à 1 trajet indirect retardé de 30 échantillons avec une atténuation de 0.5 :
  ```bash
  ./simulateur -mess 1000 -form RZ -ti 30 0.5
  ```
* Canal à 2 trajets indirects avec visualisation du diagramme de l'œil :
  ```bash
  ./simulateur -mess 2000 -form NRZ -ti 30 0.4 60 -0.2 -oeil
  ```
* Compensation des trajets indirects via sondage et égalisation :
  ```bash
  ./simulateur -mess 5000 -form RZ -nbEch 30 -ti 60 0.5 -sondage
  ```

### 5. Codage de canal (TP5)
* Transmission logique avec codage :
  ```bash
  ./simulateur -mess 01010101 -codeur
  ```
* Transmission analogique bruitée avec codage de canal :
  ```bash
  ./simulateur -mess 50000 -form NRZ -snrpb 5 -codeur
  ```
* Canal multi-trajets bruité avec sondage et codage :
  ```bash
  ./simulateur -mess 5000 -form RZ -nbEch 30 -ti 60 0.5 -snrpb 10 -sondage -codeur
  ```

## Scripts du projet

Le projet fournit les scripts shell requis pour le cycle de vie du livrable :

* `./compile` : compile les sources Java (`src/` et `tests/`) dans le répertoire `bin/`.
* `./simulateur [options]` : lance la simulation via la commande unique.
* `./runTests` : lance l'ensemble des tests (tests unitaires Java, tests JUnit et tests CLI d'intégration).
* `./genDoc` : génère la documentation Javadoc dans le répertoire `docs/`.
* `./cleanAll` : nettoie les répertoires `bin/` et `docs/` et supprime les archives temporaires.
* `./genDeliverable` : prépare l'archive `.tar.gz` pour la livraison.

## Organisation des répertoires

* `src/` : code source Java de la chaîne de transmission (destinations, information, simulateur, sources, transmetteurs, visualisations).
* `tests/` : classes de tests unitaires et JUnit.
* `lib/` : dépendances (archive console JUnit 6.1.3).
* `bin/` : binaires compilés.
* `docs/` : documentation Javadoc générée.

