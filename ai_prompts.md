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

