# Report TP2

Le format du fichier à lire semble être du BSON


Charger par exemple 1000 ou 10'000 noeuds

https://www.aminer.org/citation contient le format des données



# First technique

## Multithread pour l'insertion

pas une bonne idée ACID --> Isolation


## Article par article
pas une bonne idée. Assyé de faire un unwind des articles


## Unwind des articles

```java
tx.run(new Query("UNWIND $articles as ar " +
    "MERGE (a:ARTICLE {_id:ar._id}) " +
    "SET a.title = ar.title " +
    "WITH ar, a UNWIND ar.authors as ath " +
    "MERGE (b:AUTHOR {_id:ath._id}) " +
    "SET b.name = ath.name " +
    "MERGE (b)-[:AUTHORED]->(a) " +
    "WITH ar, a UNWIND ar.references as ref " +
    "MERGE (r:ARTICLE {_id:ref}) " +
    "MERGE (a)-[:CITES]->(r) ",
    articles_map));
```

8.67 secondes pour 10'000 articles (1'000 par insertion)

75.301 secondes pour 100'000 articles (10'000 par insertions)
 secondes pour 100'000 articles (20'000 par insertions)

## Index du id pour faciliter les merge



## Limiter la longueur du titre