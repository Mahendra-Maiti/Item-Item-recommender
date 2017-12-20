package org.lenskit.mooc.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleSortedMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.results.Results;
import org.lenskit.util.ScoredIdAccumulator;
import org.lenskit.util.TopNScoredIdAccumulator;
import org.lenskit.util.collections.LongUtils;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;

public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final DataAccessObject dao;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, DataAccessObject dao) {
        model = m;
        this.dao = dao;
        neighborhoodSize = 20;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);
        int count=0;

        for(Map.Entry<Long,Double> entry: ratings.entrySet())
        {
            // Normalize the user's ratings by subtracting the item mean from each one.
            Long item_ID=entry.getKey();
            Double old_rating=entry.getValue();
            Double mean_rating=itemMeans.get(item_ID);
            entry.setValue(old_rating-mean_rating);
        }


        List<Result> results = new ArrayList<>();

        for (long item: items ) {
            Double score=new Double(0);
            Double numerator=new Double(0);
            Double denominator=new Double(0);
            Double item_mean_val=itemMeans.get(item);
            int nb_count=0;
            int counter=0;
            Long2DoubleMap neighbors=model.getNeighbors(item);



            List<Result> res=new ArrayList<>();

            for(Map.Entry<Long,Double> e: neighbors.entrySet())
            {
                res.add(Results.create(e.getKey(),e.getValue()));

            }
            //sorting the neighbors according to similarity value
            Collections.sort(res, new Comparator<Result>() {
                @Override
                public int compare(Result result, Result t1) {
                    return result.getScore()>t1.getScore()?-1:(result.getScore()<t1.getScore()?1:0);
                }
            });


             while(nb_count<neighborhoodSize && nb_count<(neighbors.size()))
            {
                Result r=res.get(counter);
                Long item_ID=r.getId();
                Double similarity_val=r.getScore();
                if(ratings.containsKey(item_ID) && item!=item_ID)
                {
                    numerator+=ratings.get(item_ID)*similarity_val;
                    denominator+=similarity_val;
                    nb_count++;

                }

                counter++;
                if(counter==neighbors.size())
                    break;
            }

            Double predicted_rating=item_mean_val+(numerator/denominator);

            results.add(Results.create(item,predicted_rating));
        }

        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        List<Rating> history = dao.query(Rating.class)
                                  .withAttribute(CommonAttributes.USER_ID, user)
                                  .get();

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            ratings.put(r.getItemId(), r.getValue());
        }

        return ratings;
    }


}
