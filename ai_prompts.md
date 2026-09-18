# Prompts IA utilisés lors de la réalisation de ce projet
Par soucis de transparence, ce document contient la majeure partie des prompts IA que nous avons utilisés lors de la réalisation de ce projet.

La majorité des prompts non concluants (absence de réponse pertinente de l'IA ou prompts mal formulés) n'apparaissent pas dans ce document. 

## Prompts du TP1
### Lors de la configuration de notre environnement de développement
* J'ai un fichier console .vv, comment ouvrir la vm ?
* Comment cloner un repo github pour le mettre sur mon PC via vscode ?
* comment annuler mon dernier commit via l'interface vscode ?

### Lors du développement du logiciel
* Mon collègue a utilisé import information.Information; dans son code SourceFixe.java. je dois faire le code SourceAleatoire.java. Explique moi à quoi sert information.information.
* Explique moi comment gérer/utiliser l'hérédité des classes de mon projet, je suis débutant
* Ecris moi le fichier runTests pour tester l'étape 1 de mon projet
    * Par manque de temps, je n'ai pas eu le temps de créer de vrai tests Java. Les tests via commandes bash ont donc entièrement été générés par IA et vont probablement être supprimés/remplacés dans le futur par un travail plus propre.
* Reprends mon code et ajoute le balisage pour la javadoc et ajoute les commentaires que j'ai oublié
* Refais une passe sur mon code et propose des amélioration pour le code 

## Prompts du TP2
* Quand il y avait un merge conflict dans vsc avant avec GitHub, vsc m'ouvrait une interface pour choisir en quelques clics quels lignes garder. Maintenant ça m'affiche juste une pop up avec une erreur
* Le message est juste qu'il ne peut pas merge car y'a un conflit
    * Gemini m'a aidé à débogger le problème. Après quelques manipulations (redémarrage de VSCode et git reset notamment), le problème à disparu et j'ai pu à nouveau résoudre les merge conflicts, qui seront forcément nombreux au court du projet étant donné que nous développons à 4 en même temps.
* J'ai une erreur Error: Could not find or load main class tests.TestSimulateur
Caused by: java.lang.ClassNotFoundException: tests.TestSimulateur quand j'execute .\runtests
    * Le script runtests n'arrivait pas à ouvrir les fichiers .class des tests car le script compile ne prenait pas en charge. Sur recommandation de l'IA, nous avons amélioré ces deux derniers scripts. L'IA nous a aussi prévenu que genDoc ne prenait pas non plus en charge les tests, ce que nous avons corrigé
* Comment faire une liste à points en latex ?
* Comment ajouter des acronymes en latex ?
* comment faire un saut de paragraphe avec une ligne vide entre les deux paragraphes en latex ?
* J'ai un code java, et des tests java que je lance depuis un script bash. Mais j'aimerait pouvoir aussi lancer les tests depuis l'onglet testing de vscode. Il faut que j'ecrive un fichier de configuration ou que je change quelque chose dans mes tests ?
    * Dans un premier temps, nous avions écris les tests Java de manière à ce qu'ils fonctionnent avec le script ./runTests, sans penser au fait qu'il serait plus facile de vérifier leur coverage avec Emma dans VSCode s'il s'agissait de tests JUnit, comme nous l'avions vu en classe. L'IA Copilot nous a recommandé de créer un projet Maven, ce qui est exagéré au vu de la simplicité avec laquelle nous avons réussi à utiliser JUnit sur VSCode en SIT211. J'ai donc fait la requête suivante :
        * J'avais réussi à le faire sur un autre projet, sans maven ou truc compliqué, juste avec un fichier .classpath et .project
            * Copilot m'a donc suggéré de simplement compléter le fichier .classpath (ce que j'ai fait en m'inspirant du fichier .classpath que j'avais modifié en SIT211) et d'adapter les tests à JUnit. Cela a provoqué une erreur sur les fichiers de tests. Ces derniers refusaient d'appartenir au package Tests, mais pouvaient toujours être exécutés via le script `runtests`. Par manque de temps, et puisque nous n'avons constaté aucune regression fonctionelle, nous avons livré l'itération 2 avec ce petit problème.

## Prompts du TP3
* Pourquoi mes fichiers de test refusent d'apartenir au package test.
    * Comme je m'en doutait, le problème venait du fichier .classpath récemment créé pour tenter de faire fonctionner JUnit et Emma dans nos environnements VSCode. Copilot m'a précisé que le problème venait de la ligne <classpathentry kind="src" path="tests"/> qui implique que Java doit considérer les tests comme la racine des packages, et s'attendait donc à ce que les fichiers directement dedans soient dans le package par défaut (sans package tests). Après vérification, il apparait que dans le TP2-3 de SIT211, des fichiers tests n'étaient pas non plus dans le package test ; j'aurai surement pu trouver la solution moi-même en creusant un peu. Fort de cet apprentissage, j'ai donc aussi mis à jour le script runTests pour qu'il prenne en compte le fait que les classes de test n'apartiennent plus au package test.

* Pour confirmer le code (Enzo) j'ai utiliser Mistral Vibe car je préfère les IA avec des droits aux données plus strictes et fait un simple prompt à partir de Gemini qui expliquera mieux le projet que moi : 

### TP2.1 Génération du prompte par Gemini 
Prompt générar par Gemini : 
"""
Voici le prompt prêt à être copié-collé pour l'autre IA :

---

Agis en tant qu'expert en ingénierie logicielle Java et en télécommunications. Analyse le code source fourni ci-dessous pour vérifier s'il implémente correctement et intégralement les exigences de l'**Étape 2** du projet de simulation de chaîne de transmission (SIT 213, IMT Atlantique).

### Rappel des spécifications de l'Étape 2 : « Transmission non bruitée d'un signal analogique »

L'architecture doit implémenter la chaîne suivante :
`Source` -> `Emetteur` -> `Transmetteur analogique parfait` -> `Recepteur` -> `Destination`

Points clés à vérifier :

1. **Architecture orientée objet & Types génériques :**
* Présence d'un **Émetteur** modélisé comme un `Transmetteur<Boolean, Float>` (conversion du signal logique en analogique/forme d'onde).

* Présence d'un **Transmetteur analogique « parfait »** (canal idéal sans bruit) modélisé comme un `Transmetteur<Float, Float>` qui retransmet fidèlement l'information reçue.

* Présence d'un **Récepteur** modélisé comme un `Transmetteur<Float, Boolean>` (décision/restitution des bits à partir du signal analogique).

* Respect scrupuleux de l'héritage et des interfaces imposées (`Source<T>`, `Transmetteur<R, E>`, `Destination<T>`, `Information<T>`).

2. **Sondes :**
* Présence et connexion des 4 sondes : deux sondes logiques (après la source et après le récepteur) et deux sondes analogiques (en sortie d'émetteur et en sortie de canal/entrée de récepteur).

3. **Logique de simulation et TEB :**
* Calcul du Taux d'Erreur Binaire (TEB) dans le programme principal en comparant l'information émise par la source et l'information reçue par la destination. En l'absence de bruit, le TEB attendu doit être strictement nul (0,0).

4. **Qualité logicielle :**
* Robustesse du code, gestion des exceptions, présence de la Javadoc et respect des conventions Java.

---

### Attentes de ton retour :

1. **Verdict global :** Le code valide-t-il l'étape 2 ?
2. **Conformité architecturale :** Vérification détaillée classe par classe par rapport au diagramme et aux types de données.
3. **Erreurs ou écarts constatés :** Bugs, oublis de connexion, incohérences de typage ou calculs erronés.
""" 


### TP2.2 Demande à Mistral de voir si le code est conforme : 

Long text expliquant le code, si il est bon au niveau des attentes puis conclusion : 

"""
=== Lancement des tests Java - TP2 (Etape 2) ===
1. Tests des formes d'onde de l'Emetteur :
  [OK] Emetteur NRZ : nombre d'echantillons (2 bits x 10 ech = 20)
  [OK] Emetteur NRZ : echantillons a Amax (3.0f) pour le bit 1
  [OK] ...
Bilan TP2 : XX/XX tests reussis.  # Tous doivent être [OK]

🎉 CONCLUSION
Ton projet est 100% conforme à l'Étape 2 du SIT 213.

✅ Architecture parfaite
✅ 4 sondes correctement connectées
✅ TEB = 0.0 en l'absence de bruit
✅ Toutes les contraintes IMT respectées
✅ Tests unitaires passants
"""