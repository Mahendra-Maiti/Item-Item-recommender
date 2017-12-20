# Item-Item Collaborative Filtering

In this assignment, a simple implementation of item-item collaborative filtering is done. 

## Implementing Item-Item Collaborative Filtering

The classes used are:

`SimpleItemItemModelBuilder`: Builds the item-item model from the rating data

`SimpleItemItemScorer`: Scores items with item-item collaborative filtering

`SimpleItemBasedItemScorer`: Finds similar items

The provided `SimpleItemItemModel` class stores the precomputed similarity matrix.

### Computing Similarities

The `SimpleItemItemModelBuilder` class computes the similarities between items and stores them
in the model. It also needs to create a vector mapping each item ID to its mean rating, for use
by the item scorer. The following configuration decisions are used:

-   Each item rating vector is normalized by subtracting the **item's** mean rating from each rating prior to computing similarities.
-   Cosine similarity are used between normalized item rating vectors.
-   Only neighbors with positive similarities (> 0) are stored.


The similarity matrix is in the form of a `Map` from `Long`s (items) to `Long2DoubleMap`s (their neighborhoods).  Each `Long2DoubleMap` stores a neighborhood, where each neighbor's id (the key) is associated with a similarity score (the value).

### Scoring Items

The `SimpleItemItemScorer` class uses the model of neighborhoods to actually compute scores.
The items are scored using the weighted average of the users' ratings for similar items.

At most 20 neighbors are used to score each item; if the user has rated more neighboring items than that, only the most similar ones are used.

The user's ratings are normalized by subtracting the **item's** mean rating from each rating prior to
averaging (this is necessary to get good results with the item-mean normalization above). The item mean ratings are obtained from the model class.  The resulting score function is as follows, where ![equation](http://latex.codecogs.com/gif.latex?$w_{ij}&space;=&space;\mathrm{sim}(i,j)$) , the similarity between the two items:
 ![equation](http://latex.codecogs.com/gif.latex?$&space;$s(i;u)&space;=&space;\mu_i&space;&plus;&space;\frac{\sum_{j&space;\in&space;I_u}&space;(r_{uj}&space;-&space;\mu_j)&space;w_{ij}}{\sum_{j&space;\in&space;I_u}&space;|w_{ij}|}$&space;$)

### Basket Recommendation

The item-item similarity matrix isn't just useful for generating personalized recommendations.
It is also useful for ‘find similar items’ features.

The LensKit `ItemBasedItemScorer` and `ItemBasedItemRecommender` interfaces provide this functionality. `ItemBasedItemScorer` is like `ItemScorer`, except that it scores items with respect to a set of items rather than a user.

The item-based item scorer receives a `basket` (the set of reference items) and `items` (the set of items to score) vector, similar to `ItemScorer`.  For our implementation, each item is scored with the *sum* of its similarity to each of the reference items in the basket.


## Example Output

### Predictions

Command:

    ./gradlew predict -PuserId=320 -PitemIds=153,260,527,588

Output:

    predictions for user 320:
      153 (Batman Forever (1995)): 2.476
      260 (Star Wars: Episode IV - A New Hope (1977)): 4.262
      527 (Schindler's List (1993)): 4.167
      588 (Aladdin (1992)): 3.565

### Recommendations

Command:

    ./gradlew recommend -PuserId=320

Output:

    recommendations for user 320:
      7502 (Band of Brothers (2001)): 4.484
      1224 (Henry V (1989)): 4.423
      858 (Godfather, The (1972)): 4.408
      318 (Shawshank Redemption, The (1994)): 4.403
      1203 (12 Angry Men (1957)): 4.386
      3462 (Modern Times (1936)): 4.379
      99114 (Django Unchained (2012)): 4.376
      4973 (Amelie (Fabuleux destin d'Am?lie Poulain, Le) (2001)): 4.376
      898 (Philadelphia Story, The (1940)): 4.371
      922 (Sunset Blvd. (a.k.a. Sunset Boulevard) (1950)): 4.357

### Similar Items

Command:

    ./gradlew itemBasedRecommend -PitemIds=153,260,527,588

Output:

    1196 (Star Wars: Episode V - The Empire Strikes Back (1980)): 1.103
    1210 (Star Wars: Episode VI - Return of the Jedi (1983)): 1.099
    364 (Lion King, The (1994)): 1.012
    595 (Beauty and the Beast (1991)): 1.005
    1 (Toy Story (1995)): 0.925
    500 (Mrs. Doubtfire (1993)): 0.893
    5349 (Spider-Man (2002)): 0.891
    480 (Jurassic Park (1993)): 0.888
    1291 (Indiana Jones and the Last Crusade (1989)): 0.885
    150 (Apollo 13 (1995)): 0.871

