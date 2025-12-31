package pt.ipleiria.estg.dei.ei.dae.academics.entities;


import pt.ipleiria.estg.dei.ei.dae.academics.enums.ActivityType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "activities")

public class Activity implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type; // UPLOAD, EDIT, COMMENT, RATING, etc.

    @Column(nullable = false)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "entity_type")
    private String entityType; // "Publication", "Comment", etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date timestamp;

    public Activity() {

    }

    public Activity(Long id, ActivityType type, String description, User user, String entityType, Long entityId, Date timestamp) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.user = user;
        this.entityType = entityType;
        this.entityId = entityId;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public ActivityType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public User getUser() {
        return user;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
