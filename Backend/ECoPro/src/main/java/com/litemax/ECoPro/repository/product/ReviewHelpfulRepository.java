package com.litemax.ECoPro.repository.product;

import com.litemax.ECoPro.entity.product.ReviewHelpful;
import com.litemax.ECoPro.entity.product.Review;
import com.litemax.ECoPro.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewHelpfulRepository extends JpaRepository<ReviewHelpful, Long> {
    
    Optional<ReviewHelpful> findByUserAndReview(User user, Review review);
    
    @Query("SELECT COUNT(rh) FROM ReviewHelpful rh WHERE rh.review = :review AND rh.isHelpful = true")
    Long countHelpfulVotes(@Param("review") Review review);
    
    @Query("SELECT COUNT(rh) FROM ReviewHelpful rh WHERE rh.review = :review AND rh.isHelpful = false")
    Long countUnhelpfulVotes(@Param("review") Review review);
}
