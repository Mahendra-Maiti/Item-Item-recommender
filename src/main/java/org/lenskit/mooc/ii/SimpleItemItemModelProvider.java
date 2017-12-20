package org.lenskit.mooc.ii;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.apache.commons.lang3.tuple.Pair;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.entities.CommonAttributes;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.Ratings;
import org.lenskit.inject.Transient;
import org.lenskit.util.IdBox;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.io.ObjectStream;
import org.lenskit.util.math.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


public class SimpleItemItemModelProvider implements Provider<SimpleItemItemModel> {
    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemModelProvider.class);

    private final DataAccessObject dao;

    /**
     * Construct the model provider.
     * @param dao The data access object.
     */
    @Inject
    public SimpleItemItemModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
    }

    /**
     * Construct the item-item model.
     * @return The item-item model.
     */
    @Override
    public SimpleItemItemModel get() {
        Map<Long,Long2DoubleMap> itemVectors = Maps.newHashMap();
        Long2DoubleMap itemMeans = new Long2DoubleOpenHashMap();


        try (ObjectStream<IdBox<List<Rating>>> stream = dao.query(Rating.class)
                                                           .groupBy(CommonAttributes.ITEM_ID)
                                                           .stream()) {
            for (IdBox<List<Rating>> item : stream) {
                long itemId = item.getId();
                List<Rating> itemRatings = item.getValue();


                Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap(Ratings.itemRatingVector(itemRatings));




                // Compute and store the item's mean.
                double mean = Vectors.mean(ratings);
                itemMeans.put(itemId, mean); //store the item id and mean of it's rating vector

                // Mean center the ratings.
                for (Map.Entry<Long, Double> entry : ratings.entrySet()) {
                    entry.setValue(entry.getValue() - mean);
                }

                itemVectors.put(itemId, LongUtils.frozenMap(ratings));
            }
        }

        // Map items to vectors (maps) of item similarities.
        Map<Long,Long2DoubleMap> itemSimilarities = Maps.newHashMap();

        for(Map.Entry<Long,Long2DoubleMap> e1: itemVectors.entrySet())
        { //iterate each item and store it's similarity matrix
            Long main_item_id=e1.getKey();
            Long2DoubleMap main_rating_vector=e1.getValue();
            Long2DoubleMap similarity_map = new Long2DoubleOpenHashMap();


            for(Map.Entry<Long, Long2DoubleMap> e2: itemVectors.entrySet())
            {
                Long other_item_id=e2.getKey();
                Long2DoubleMap other_rating_vector=e2.getValue();

                Double similarity_val=calculate_similarity(main_rating_vector,other_rating_vector);

                if(similarity_val>0) //ignore negative similarities
                    similarity_map.put(other_item_id,similarity_val);

            }
            itemSimilarities.put(main_item_id,similarity_map);

        }


        // Ignore nonpositive similarities

        return new SimpleItemItemModel(LongUtils.frozenMap(itemMeans), itemSimilarities);
    }

    public Double calculate_similarity(Long2DoubleMap t_item_map, Long2DoubleMap item_map)
    {

        Double t_sq=new Double(0);
        Double u_sq=new Double(0);

        for(Map.Entry<Long,Double> entry: t_item_map.entrySet()) //calculating values for target user
        {
            Double new_val=entry.getValue();
            t_sq+=new_val*new_val;
        }

        for(Map.Entry<Long,Double> e1: item_map.entrySet()) //calculating values for neighbors
        {
            Double new_val=e1.getValue();
            u_sq+=new_val*new_val;
        }

        Double denominator=Math.sqrt(t_sq)*Math.sqrt(u_sq);

        Double similarity_val=new Double(Vectors.dotProduct(t_item_map,item_map)/denominator);


        return similarity_val;

    }
}
