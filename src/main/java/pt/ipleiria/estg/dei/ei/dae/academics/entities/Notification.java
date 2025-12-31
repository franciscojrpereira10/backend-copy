package pt.ipleiria.estg.dei.ei.dae.academics.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.ipleiria.estg.dei.ei.dae.academics.enums.NotificationType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "notifications")
public class Notification implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String title;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(length = 1000, nullable = false)
    private String message;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    private boolean readFlag;

    private Date readAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long relatedEntityId;

    private String relatedEntityType;

    public Notification() {
        createdAt = new Date();
        readFlag = false;
    }

    public Notification(Long id, String title, NotificationType type, String message, Date readAt, User user, Long relatedEntityId, String relatedEntityType) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.message = message;
        this.readAt = readAt;
        this.user = user;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }

    public Long getId() {
        return id;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public @NotNull Date getCreatedAt() {
        return createdAt;
    }

    public boolean isReadFlag() {
        return readFlag;
    }

    public Date getReadAt() {
        return readAt;
    }

    public User getUser() {
        return user;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreatedAt(@NotNull Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setReadFlag(boolean readFlag) {
        this.readFlag = readFlag;
    }

    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    // metodos auxiliares

    public void markAsRead() {
        this.readFlag = true;
        this.readAt = new Date();
    }

    public void markAsUnread() {
        this.readFlag = false;
        this.readAt = null;
    }
}