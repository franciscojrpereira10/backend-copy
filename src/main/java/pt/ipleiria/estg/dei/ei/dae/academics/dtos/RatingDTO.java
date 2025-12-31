package pt.ipleiria.estg.dei.ei.dae.academics.dtos;

import pt.ipleiria.estg.dei.ei.dae.academics.entities.Rating;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RatingDTO implements Serializable {
    private Long id;
    private int stars;
    private String username;
    private Long userId;
    private Long publicationId;
    private Date createdAt;
    private Date updatedAt;

    private Double average;
    private Integer count;
    private Map<Integer, Integer> distribution;
    private Integer userRating;

    public RatingDTO() {
    }

    public RatingDTO(Long id, int stars, String username, Long userId,
                     Long publicationId, Date createdAt, Date updatedAt) {
        this.id = id;
        this.stars = stars;
        this.username = username;
        this.userId = userId;
        this.publicationId = publicationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public int getStars() {
        return stars;
    }

    public String getUsername() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPublicationId() {
        return publicationId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setPublicationId(Long publicationId) {
        this.publicationId = publicationId;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getAverage() { return average; }
    public void setAverage(Double average) { this.average = average; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Map<Integer, Integer> getDistribution() { return distribution; }
    public void setDistribution(Map<Integer, Integer> distribution) { this.distribution = distribution; }

    public Integer getUserRating() { return userRating; }
    public void setUserRating(Integer userRating) { this.userRating = userRating; }

    public static RatingDTO from(Rating rating) {
        RatingDTO dto = new RatingDTO();
        dto.setId(rating.getId());
        dto.setStars(rating.getStars());

        // Dados do utilizador
        if (rating.getUser() != null) {
            dto.setUserId(rating.getUser().getId());
            dto.setUsername(rating.getUser().getUsername());
        }

        // Dados da publicação
        if (rating.getPublication() != null) {
            dto.setPublicationId(rating.getPublication().getId());
        }

        dto.setCreatedAt(rating.getCreatedAt());
        dto.setUpdatedAt(rating.getUpdatedAt());

        return dto;
    }

    public static List<RatingDTO> from(List<Rating> ratings) {
        return ratings.stream()
                .map(RatingDTO::from)
                .collect(Collectors.toList());
    }

}