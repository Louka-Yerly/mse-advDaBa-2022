# Report TP2
Ce fichier fait office de rapport pour le TP2 sur neo4j: "Laboratory 2 – Diving deeper with Neo4j".

# Implementation
Les points ci-après explique la démarche utilisée pour ce travail pratique.

## Multithread pour l'insertion

La première technique d'optimisation qui a été testée est l'instertion par plusieurs Thread. Il a été remarqué que 
cela n'a pas amélioré la vitesse d'insertion

Cela vient du fait que le niveau d'isolation (i de ACID) ne permet pas cela.

## Insertion Article par article

Un des première méthode a été d'insérer article par article. Or, l'entête des échanges et la réservation de mémoire 
rendais ces opération très lente.

## Insertion par batch


Cette technique a permis de grouper les 

8.67 secondes pour 10'000 articles (1'000 par insertion)

75.301 secondes pour 100'000 articles (10'000 par insertions)
 secondes pour 100'000 articles (20'000 par insertions)

## Constraints and indexes

Pour essayer d'optimiser l'insertion, une contraintes d'unicité sur le champs "_id" a été mise en place. Ceci à 
permis de d'augmenter la rapidité de création des relations.

Un test d'ajout d'un indexe sur le champs "_id" a été fait. Cependant, cela n'a pas amélioré les performances. La 
gestion des indexes prenait plus de temps que le temps gagner lors de la recherche des noeuds pour la création des 
relationships.

## Utilisation de APOC

Afin d'opitmiser au mieux l'insertion, les best practices pour de grandes updates de la DB a été recherchées. Une 
source du site neo4j s'est montrée particulièrement pertinante [(large update with neo4j)](https://neo4j.
com/blog/nodes-2019-best-practices-to-make-large-updates-in-neo4j/).
com/blog/nodes-2019-best-practices-to-make-large-updates-in-neo4j/].

Cet article conseil l'utilisation de APOC. Ceci est en effet nécessaire pour les très grandes transactions. Dans mon 
cas, sans l'utilisation de APOC, les transaction de mise en place des relations crashait car trop grande.

APOC est une bibliothèque de procédures et de fonctions Cypher personnalisées pour Neo4j. Il offre une grande 
variété de fonctionnalités qui vont au-delà de ce qui est disponible nativement dans Cypher.

Dans le cadre de ce projet, la fonction apoc.periodic.iterat() a été utilisée

## Améliorations possibles


Les améliorations listées ci-après pourraient être réalisées:
 - Réaliser le formatage du fichier dblpv13.json à la volée. Cela permettrait d'éviter des problèmes de 
   synchronisation entre thread. De plus cela éviterait de devoir stocker un deuxième fichier de 17 GiB.
 - Insérer la DB en entier.

# Conclusion

Pour terminer, le meilleur rapport de performance obtenu a été celui présenté ci-dessous:
```json
{
  "number_of_articles": 100000, 
  "memoryMB_db": 3000,
  "memoryMB_app": 3000,
  "seconds": 88.327
}
```
Il est à noté que le temps "seconds" a été calculé dans l'application JAVA. Le temps total de build docker est de 2 
minutes et 24 secondes.

L'extrait de code ci-dessous illuste cela: 
```shell-session
# docker-compose up
...
app_1  | Inserter: Finish inserting relationship
app_1  | Elapsed time: 88.327 (seconds)
app_1  | [INFO] ------------------------------------------------------------------------
app_1  | [INFO] BUILD SUCCESS
app_1  | [INFO] ------------------------------------------------------------------------
app_1  | [INFO] Total time:  02:24 min
app_1  | [INFO] Finished at: 2023-01-08T17:02:06Z
app_1  | [INFO] ------------------------------------------------------------------------
```

D'autres tests ont également été réalisés:

```json
{
  "test_1": {
    "number_of_articles": 500000,
    "memoryMB_db": 3000,
    "memoryMB_app": 3000,
    "seconds": 589.89
  },
  "test_2": {
    "number_of_articles": 1000000,
    "memoryMB_db": 3000,
    "memoryMB_app": 3000,
    "seconds": 1581.945
  }
}
```

