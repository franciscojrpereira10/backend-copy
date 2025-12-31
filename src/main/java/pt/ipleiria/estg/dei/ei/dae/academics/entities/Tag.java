package pt.ipleiria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "tags")
@NamedQueries({
        @NamedQuery(
                name = "getAllTags",
                query = "SELECT t FROM Tag t"
        ),
        @NamedQuery(
                name = "getTagByName",
                query = "SELECT t FROM Tag t WHERE t.name = :name"
        )
})
public class Tag implements Serializable {

    //Esta acabado
    //IDS NAS CLASSES TÊM DE SER LONG COM (L) MAIUSCULO

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column
    private boolean visible = true;

    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private List<Publication> publications;

    @ManyToMany(mappedBy = "subscribedTags", fetch = FetchType.LAZY)
    private List<User> subscribers;

    public Tag() {
        publications = new ArrayList<>();
        subscribers = new ArrayList<>();
        this.createdAt = new Date();
    }

    public Tag(long id, String name, boolean visible, User createdBy) {
        this.id = id;
        this.name = name;
        this.visible = visible;
        this.createdBy = createdBy;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return visible;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public List<Publication> getPublications() {
        return publications;
    }

    public List<User> getSubscribers() {
        return subscribers;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    public void setSubscribers(List<User> subscribers) {
        this.subscribers = subscribers;
    }

    //metodos auxiliares

    public int getPublicationCount() {
        return publications != null ? publications.size() : 0;
    }
}

