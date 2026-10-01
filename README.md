# Projet_Simulateur_Chaine_Trans
Ce projet simule un système de transmission en Java.

Il est réalisé dans le cadre de notre deuxième année de formation d'ingénieur spécialisé en informatique, réseaux et télécommunications à IMT Atlantique.

## Lancer une simulation
Exécutez le script `simulateur` à la racine du dossier.

### Options 

`-mess m`
précise le message ou la longueur du message à émettre :
Si m est une suite de 0 et de 1 de longueur au moins égale à 7, m est
le message à émettre. Si m comporte au plus 6 chiffres décimaux et
correspond à la représentation en base 10 d'un entier, cet entier est
la longueur du message que le simulateur doit générer et transmettre.
Par défaut le simulateur doit générer et transmettre un message de
longueur 100.

`-s`
indique l’utilisation des sondes.
Par défaut le simulateur n’utilise pas de sondes

`-seed v`
précise l’utilisation d’une semence pour l’initialisation des
générateurs aléatoires du simulateur. v doit être une valeur
entière. L’utilisation d’une semence permet de rejouer à l’identique
une simulation (à la fois pour le message émis et le bruitage s’il est
activé). Par défaut le simulateur n’utilise pas de semence pour
initialiser ses générateurs aléatoires.

`-codeur`
active l'utilisation d'un codage de canal (codeur en émission et décodeur en réception).
Le codeur transforme chaque bit en un paquet de 3 bits (0 -> 010, 1 -> 101),
facilitant la synchronisation (jamais plus de 2 bits consécutifs identiques) et
permettant au récepteur de détecter et corriger une erreur par paquet de 3 bits.
Par défaut le simulateur n'utilise pas de codage de canal.

## Détails techniques
Ce projet est développé sous Windows, mais livré dans un format compatible avec l'environnement Debian de l'école.
