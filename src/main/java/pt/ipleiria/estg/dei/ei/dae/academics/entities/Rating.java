package pt.ipleiria.estg.dei.ei.dae.academics.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Max(5)
    @Min(1)
    @NotNull
    private int stars; // 1 - 5 estrelas

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @Column
    private Date updatedAt;

    public Rating() {
        this.createdAt = new Date();
    }

    public Rating(Long id, int stars, User user, Publication publication, Date updatedAt) {
        this.id = id;
        this.stars = stars;
        this.user = user;
        this.publication = publication;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    @Max(5)
    @Min(1)
    @NotNull
    public int getStars() {
        return stars;
    }

    public User getUser() {
        return user;
    }

    public Publication getPublication() {
        return publication;
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

    public void setStars(@Max(5) @Min(1) @NotNull int stars) {
        this.stars = stars;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    // metodos auxiliares

    public void update() {
        this.updatedAt = new Date();
    }

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}
